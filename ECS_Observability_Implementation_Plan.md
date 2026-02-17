# BuildBox — Real-Time ECS Observability Pipeline

**Goal**: Stream real-time app logs + lifecycle events from ECS containers to the frontend dashboard, similar to Vercel/Railway.

**Architecture**: Two Lambda functions writing to Redis Streams → existing SSE pipeline in `Log_Analytics_Server`

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ECS Task (no changes)                       │
│  ┌─────────────────────────────────────┐                            │
│  │  User App (stdout / stderr)         │                            │
│  └──────────┬──────────────────────────┘                            │
│             │                                                       │
└─────────────┼───────────────────────────────────────────────────────┘
              │
              │  awslogs driver (already configured)
              │
    ┌─────────▼───────────┐          ┌──────────────────────────┐
    │  CloudWatch Logs    │          │  EventBridge (automatic)  │
    │  /ecs/user-node-app │          │  ECS Task State Changes   │
    └─────────┬───────────┘          └────────────┬─────────────┘
              │                                   │
              │ Subscription Filter               │ EventBridge Rule
              │                                   │
    ┌─────────▼───────────┐          ┌────────────▼─────────────┐
    │  Lambda 1:          │          │  Lambda 2:                │
    │  log-forwarder      │          │  ecs-event-handler        │
    │                     │          │                           │
    │  Receives app logs  │          │  Receives lifecycle:      │
    │  (console.log, etc) │          │  PENDING/RUNNING/STOPPED  │
    └─────────┬───────────┘          └────────────┬─────────────┘
              │                                   │
              │  XADD runtime-logs:{projectId}    │  XADD runtime-logs:{projectId}
              │                                   │  SET project:{projectId}:status
              │                                   │
    ┌─────────▼───────────────────────────────────▼─────────────┐
    │                    Aiven Redis (SSL)                       │
    │  Stream: runtime-logs:{projectId}                         │
    │  Key:    project:{projectId}:status                       │
    └─────────────────────────┬─────────────────────────────────┘
                              │
                              │ SSE (new endpoint)
                              │
                    ┌─────────▼───────────┐
                    │  Log_Analytics_Server│
                    │  :9012              │
                    │  GET /api/v2/runtime│
                    │  /{projectId}/logs  │
                    └─────────┬───────────┘
                              │
                    ┌─────────▼───────────┐
                    │  Frontend Dashboard  │
                    └─────────────────────┘
```

---

## Pre-requisites

| Item | Value |
|------|-------|
| AWS Region | `ap-south-1` |
| ECS Cluster | `buildserver-cluster-1` |
| Log Groups (already exist) | `/ecs/user-node-app`, `/ecs/user-python-app` |
| Redis | `rediss://default:<password>@valkey-319df50f-maharshnayak5-038f.l.aivencloud.com:12608` |
| Lambda Runtime | Node.js 20.x |
| VPC Required? | ❌ No (Aiven Redis is public with SSL) |
| ECS `startedBy` field | Already set as `buildserver-{projectId}` in `EcsService.java` |
| Log stream format | `{projectId}/user-node-app/{taskId}` (set in `EcsService.java`) |

---

## Part 1: ECS Event Handler Lambda (EventBridge)

> **Start here** — simpler, fewer events, easy to test.

### What It Captures

| ECS Event | When | What Lambda Writes |
|-----------|------|--------------------|
| `PENDING` | Task scheduled | `⏳ Container is starting...` |
| `RUNNING` | Container healthy | `✅ Container is now RUNNING` + updates status key |
| `STOPPED` | Container exited | `❌ Container STOPPED — exit code, reason` + updates status key |

### 1.1 Project Structure

```
lambda/ecs-event-handler/
├── index.mjs
├── package.json
```

### 1.2 `package.json`

```json
{
  "name": "ecs-event-handler",
  "version": "1.0.0",
  "type": "module",
  "dependencies": {
    "redis": "^4.6.13"
  }
}
```

### 1.3 `index.mjs`

```javascript
import { createClient } from 'redis';

// Redis connection (cached across warm invocations)
let redis;
async function getRedis() {
  if (!redis || !redis.isOpen) {
    redis = createClient({ url: process.env.REDIS_URL });
    redis.on('error', (err) => console.error('Redis error:', err));
    await redis.connect();
  }
  return redis;
}

export const handler = async (event) => {
  console.log('Received event:', JSON.stringify(event));

  const detail = event.detail;
  if (!detail) return { statusCode: 400, body: 'No detail in event' };

  // Extract projectId from "startedBy" field
  // EcsService.java sets: .startedBy("buildserver-" + projectId)
  const startedBy = detail.startedBy || '';
  if (!startedBy.startsWith('buildserver-')) {
    console.log('Not a BuildBox task, skipping. startedBy:', startedBy);
    return { statusCode: 200, body: 'Skipped — not a BuildBox task' };
  }
  const projectId = startedBy.replace('buildserver-', '');

  const status     = detail.lastStatus;
  const taskArn    = detail.taskArn;
  const timestamp  = String(Date.now());
  const streamKey  = `runtime-logs:${projectId}`;
  const statusKey  = `project:${projectId}:status`;

  const client = await getRedis();

  // Always update status key
  await client.set(statusKey, JSON.stringify({
    status,
    taskArn,
    updatedAt: timestamp
  }));
  await client.expire(statusKey, 86400);

  // Write lifecycle event to log stream
  let logMessage;

  switch (status) {
    case 'PENDING':
    case 'PROVISIONING':
      logMessage = '⏳ Container is starting...';
      break;

    case 'RUNNING':
      logMessage = '✅ Container is now RUNNING';
      break;

    case 'STOPPED': {
      const container = detail.containers?.[0] || {};
      const exitCode  = container.exitCode ?? 'unknown';
      const reason    = detail.stoppedReason || 'No reason provided';
      const stoppedAt = detail.stoppedAt || 'unknown';
      const startedAt = detail.startedAt || 'unknown';

      logMessage = [
        '❌ Container STOPPED',
        `   Exit code : ${exitCode}`,
        `   Reason    : ${reason}`,
        `   Started   : ${startedAt}`,
        `   Stopped   : ${stoppedAt}`
      ].join('\n');
      break;
    }

    case 'DEPROVISIONING':
      logMessage = '🔄 Container shutting down...';
      break;

    default:
      logMessage = `ℹ️ Task status: ${status}`;
  }

  await client.xAdd(streamKey, '*', {
    log: logMessage,
    source: 'system',
    timestamp
  });
  await client.expire(streamKey, 86400);

  console.log(`✅ Processed: project=${projectId} status=${status}`);
  return { statusCode: 200, body: `Processed ${projectId} → ${status}` };
};
```

### 1.4 Deploy Commands

```bash
# Install & package
cd lambda/ecs-event-handler
npm install
zip -r ecs-event-handler.zip .

# Create Lambda
aws lambda create-function \
  --function-name ecs-event-handler \
  --runtime nodejs20.x \
  --handler index.handler \
  --zip-file fileb://ecs-event-handler.zip \
  --role arn:aws:iam::ACCOUNT:role/lambda-basic-execution \
  --timeout 15 \
  --memory-size 128 \
  --environment "Variables={REDIS_URL=rediss://default:<password>@valkey-319df50f-maharshnayak5-038f.l.aivencloud.com:12608}"
```

### 1.5 Create EventBridge Rule

```bash
# Create rule (filter: only your cluster's events)
aws events put-rule \
  --name "buildbox-ecs-task-events" \
  --description "Captures ECS task state changes for BuildBox" \
  --event-pattern '{
    "source": ["aws.ecs"],
    "detail-type": ["ECS Task State Change"],
    "detail": {
      "clusterArn": ["arn:aws:ecs:ap-south-1:ACCOUNT:cluster/buildserver-cluster-1"]
    }
  }'

# Connect rule → Lambda
aws events put-targets \
  --rule "buildbox-ecs-task-events" \
  --targets '[{
    "Id": "ecs-event-handler-target",
    "Arn": "arn:aws:lambda:ap-south-1:ACCOUNT:function:ecs-event-handler"
  }]'

# Allow EventBridge to invoke Lambda
aws lambda add-permission \
  --function-name ecs-event-handler \
  --statement-id allow-eventbridge \
  --action lambda:InvokeFunction \
  --principal events.amazonaws.com \
  --source-arn arn:aws:events:ap-south-1:ACCOUNT:rule/buildbox-ecs-task-events
```

### 1.6 Test

```bash
# Invoke manually with a fake event
aws lambda invoke \
  --function-name ecs-event-handler \
  --payload '{
    "source": "aws.ecs",
    "detail-type": "ECS Task State Change",
    "detail": {
      "clusterArn": "arn:aws:ecs:ap-south-1:ACCOUNT:cluster/buildserver-cluster-1",
      "taskArn": "arn:aws:ecs:ap-south-1:ACCOUNT:task/test-123",
      "lastStatus": "RUNNING",
      "startedBy": "buildserver-test-project-1",
      "containers": [{"name": "user-node-app", "lastStatus": "RUNNING"}]
    }
  }' \
  /dev/stdout

# Verify in Redis
redis-cli -u "rediss://..." XRANGE runtime-logs:test-project-1 - +
```

---

## Part 2: Log Forwarder Lambda (CloudWatch Subscription Filter)

### What It Captures

Every line your app writes to stdout/stderr — `console.log("Hello")`, error stack traces, HTTP access logs, etc.

### 2.1 Project Structure

```
lambda/log-forwarder/
├── index.mjs
├── package.json    (same as above)
```

### 2.2 `index.mjs`

```javascript
import { createClient } from 'redis';
import zlib from 'zlib';

let redis;
async function getRedis() {
  if (!redis || !redis.isOpen) {
    redis = createClient({ url: process.env.REDIS_URL });
    redis.on('error', (err) => console.error('Redis error:', err));
    await redis.connect();
  }
  return redis;
}

export const handler = async (event) => {
  // CloudWatch sends gzipped base64 data
  const payload = Buffer.from(event.awslogs.data, 'base64');
  const parsed  = JSON.parse(zlib.gunzipSync(payload).toString());

  const logGroup  = parsed.logGroup;    // "/ecs/user-node-app"
  const logStream = parsed.logStream;   // "{projectId}/user-node-app/{taskId}"
  const logEvents = parsed.logEvents;

  // Extract projectId from log stream name
  // Format: "{projectId}/user-node-app/{taskId}"
  const projectId = logStream.split('/')[0];

  if (!projectId) {
    console.log('Could not extract projectId from stream:', logStream);
    return;
  }

  const client    = await getRedis();
  const streamKey = `runtime-logs:${projectId}`;

  // Batch all writes in one Redis pipeline (single round-trip)
  const pipeline = client.multi();

  for (const logEvent of logEvents) {
    const message = logEvent.message?.trim();
    if (!message) continue;

    pipeline.xAdd(streamKey, '*', {
      log: message,
      source: 'runtime',
      timestamp: String(logEvent.timestamp),
      logGroup: logGroup
    });
  }

  pipeline.expire(streamKey, 86400); // 24h TTL
  await pipeline.exec();

  console.log(`✅ Forwarded ${logEvents.length} logs for project=${projectId}`);
};
```

### 2.3 Deploy Commands

```bash
cd lambda/log-forwarder
npm install
zip -r log-forwarder.zip .

aws lambda create-function \
  --function-name log-forwarder \
  --runtime nodejs20.x \
  --handler index.handler \
  --zip-file fileb://log-forwarder.zip \
  --role arn:aws:iam::ACCOUNT:role/lambda-basic-execution \
  --timeout 30 \
  --memory-size 128 \
  --environment "Variables={REDIS_URL=rediss://default:<password>@valkey-319df50f-maharshnayak5-038f.l.aivencloud.com:12608}"
```

### 2.4 Attach Subscription Filters

```bash
# Allow CloudWatch to invoke Lambda
aws lambda add-permission \
  --function-name log-forwarder \
  --statement-id allow-cw-node \
  --action lambda:InvokeFunction \
  --principal logs.amazonaws.com \
  --source-arn "arn:aws:logs:ap-south-1:ACCOUNT:log-group:/ecs/user-node-app:*"

aws lambda add-permission \
  --function-name log-forwarder \
  --statement-id allow-cw-python \
  --action lambda:InvokeFunction \
  --principal logs.amazonaws.com \
  --source-arn "arn:aws:logs:ap-south-1:ACCOUNT:log-group:/ecs/user-python-app:*"

# Attach subscription filters
aws logs put-subscription-filter \
  --log-group-name "/ecs/user-node-app" \
  --filter-name "to-redis" \
  --filter-pattern "" \
  --destination-arn "arn:aws:lambda:ap-south-1:ACCOUNT:function:log-forwarder"

aws logs put-subscription-filter \
  --log-group-name "/ecs/user-python-app" \
  --filter-name "to-redis" \
  --filter-pattern "" \
  --destination-arn "arn:aws:lambda:ap-south-1:ACCOUNT:function:log-forwarder"
```

---

## Part 3: Log_Analytics_Server Changes

### 3.1 `Service/RuntimeLogService.java`

Same pattern as existing `BuildLogsService.java`, reading from `runtime-logs:` prefix:

```java
@Service
public class RuntimeLogService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final StreamReceiver<String, MapRecord<String,String,String>> receiver;

    @Autowired
    public RuntimeLogService(
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate,
            StreamReceiver<String, MapRecord<String,String,String>> receiver) {
        this.redisTemplate = redisTemplate;
        this.receiver = receiver;
    }

    public Flux<MapRecord<String,String,String>> streamRuntimeLogs(
            String projectId, String lastEventId) {

        String streamKey = "runtime-logs:" + projectId;

        // History
        Flux<MapRecord<String,String,String>> history =
            redisTemplate.opsForStream()
                .range(streamKey, Range.unbounded())
                .map(record -> {
                    Map<String, String> stringMap = new HashMap<>();
                    record.getValue().forEach((k, v) ->
                        stringMap.put(k.toString(), v.toString()));
                    return MapRecord.create(record.getStream(), stringMap)
                        .withId(record.getId());
                });

        // Live tail
        ReadOffset offset = lastEventId != null
            ? ReadOffset.from(lastEventId)
            : ReadOffset.latest();
        Flux<MapRecord<String,String,String>> live =
            receiver.receive(StreamOffset.create(streamKey, offset));

        return Flux.concat(history, live);
    }
}
```

### 3.2 `Controller/RuntimeLogController.java`

```java
@RestController
@RequestMapping("/api/v2/runtime")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RuntimeLogController {

    @Autowired
    private RuntimeLogService runtimeLogService;

    @GetMapping(value = "/{projectId}/logs",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamRuntimeLogs(
            @PathVariable String projectId,
            @RequestHeader(value = "Last-Event-ID", required = false)
            String lastEventId) {

        return runtimeLogService.streamRuntimeLogs(projectId, lastEventId)
            .map(record -> {
                String log    = record.getValue().getOrDefault("log", "");
                String source = record.getValue().getOrDefault("source", "runtime");

                // source|message — frontend splits to style differently
                String data = source + "|" + log;

                return ServerSentEvent.<String>builder()
                    .id(record.getId().getValue())
                    .event("log")
                    .data(data)
                    .build();
            });
    }
}
```

### 3.3 Frontend SSE Usage

```javascript
const evtSource = new EventSource(
  `http://log-analytics-server:9012/api/v2/runtime/${projectId}/logs`
);

evtSource.addEventListener('log', (e) => {
  const [source, ...parts] = e.data.split('|');
  const message = parts.join('|');
  // source = "system" (lifecycle) or "runtime" (app stdout)
  console.log(`[${source}] ${message}`);
});
```

---

## IAM Role for Both Lambdas

Minimal policy — no ECS/VPC permissions needed:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:ap-south-1:ACCOUNT:*"
    }
  ]
}
```

---

## Redis Key Reference

| Key Pattern | Type | Written By | TTL | Purpose |
|-------------|------|------------|-----|---------|
| `runtime-logs:{projectId}` | Stream | Both Lambdas | 24h | Unified log timeline |
| `project:{projectId}:status` | String (JSON) | ecs-event-handler | 24h | Current task status |
| `logs:{buildId}` | Stream | BuildServer (existing) | — | Build-time logs (unchanged) |

---

## Implementation Order

| Step | Task | Owner | Est. Time |
|------|------|-------|-----------|
| 1 | Deploy `ecs-event-handler` Lambda + EventBridge Rule | Backend | 1-2 hours |
| 2 | Test: start/stop ECS task → verify lifecycle events in Redis | Backend | 30 min |
| 3 | Deploy `log-forwarder` Lambda + Subscription Filters | Backend | 1-2 hours |
| 4 | Test: run ECS task → verify app logs in Redis | Backend | 30 min |
| 5 | Add `RuntimeLogService` + `RuntimeLogController` to Log_Analytics_Server | Backend | 1 hour |
| 6 | Test: `curl -N` the SSE endpoint → verify unified stream | Backend | 30 min |
| 7 | Frontend: connect to SSE, render log timeline with system/runtime styling | Frontend | 2-3 hours |

---

## Verification Checklist

- [ ] **EventBridge**: Stop an ECS task → `❌ Container STOPPED` appears in Redis stream
- [ ] **EventBridge**: Start an ECS task → `⏳ Starting...` then `✅ Running` in Redis
- [ ] **Log Forwarder**: App `console.log()` → message in Redis within 2-3 seconds
- [ ] **SSE Endpoint**: `curl -N http://localhost:9012/api/v2/runtime/{projectId}/logs` streams events
- [ ] **Status Key**: `GET project:{projectId}:status` returns correct JSON
- [ ] **TTL**: Keys auto-expire after 24 hours
- [ ] **Frontend**: Unified timeline shows both system events and app logs

---

## What the Frontend Log Timeline Should Look Like

```
[system]  ⏳ Container is starting...                       17:00:01
[system]  ✅ Container is now RUNNING                       17:00:05
[runtime] Server started on port 3000                       17:00:06
[runtime] Connected to database                             17:00:06
[runtime] GET /api/users 200 12ms                           17:00:15
[runtime] POST /api/orders 201 45ms                         17:00:18
[runtime] ERROR: Payment service timeout                    17:01:30
[system]  ❌ Container STOPPED — exit code: 137, OOM        17:02:00
```
