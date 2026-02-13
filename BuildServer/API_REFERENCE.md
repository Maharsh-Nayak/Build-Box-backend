# BuildServer API Quick Reference

## Base URL
```
http://localhost:9192
```

---

## 📝 Available Endpoints

### 1. Trigger Local Build
**Endpoint:** `POST /api/builds/test-local`  
**Description:** Build and deploy an application from local `builds/` directory

**Request Body:**
```json
{
  "projectId": "demo-node-app",
  "runtime": "node"
}
```

**Runtime Options:**
- `"node"` - For Node.js applications
- `"python"` - For Python/Flask applications

**Response:**
```json
{
  "projectId": "demo-node-app",
  "status": "LOCAL_BUILD_STARTED"
}
```

**Example:**
```bash
curl -X POST http://localhost:9192/api/builds/test-local \
  -H "Content-Type: application/json" \
  -d '{"projectId": "demo-node-app", "runtime": "node"}'
```

---

### 2. Trigger S3 Build
**Endpoint:** `POST /api/builds`  
**Description:** Download code from S3, build, and deploy

**Request Body:**
```json
{
  "projectId": "my-project",
  "runtime": "node"
}
```

**Response:**
```json
{
  "projectId": "my-project",
  "status": "BUILD_STARTED"
}
```

**Example:**
```bash
curl -X POST http://localhost:9192/api/builds \
  -H "Content-Type: application/json" \
  -d '{"projectId": "my-project", "runtime": "node"}'
```

---

### 3. Get Task Status
**Endpoint:** `GET /api/builds/tasks/{projectId}`  
**Description:** Retrieve current status and details of a running task

**Path Parameter:**
- `projectId` - The project identifier

**Response:**
```json
{
  "projectId": "demo-node-app",
  "taskArn": "arn:aws:ecs:ap-south-1:788184849410:task/buildserver-cluster-1/183ab2b03e8a404b9e629bc384e20173",
  "runtime": "node",
  "host": "35.154.102.57",
  "hostPort": 32783,
  "status": "RUNNING",
  "startedAt": "2026-02-13T11:25:07.865280Z",
  "lastActivity": "2026-02-13T11:25:07.865280Z"
}
```

**Example:**
```bash
curl -X GET http://localhost:9192/api/builds/tasks/demo-node-app
```

---

### 4. Get Task Logs
**Endpoint:** `GET /api/builds/tasks/{projectId}/logs`  
**Description:** Retrieve CloudWatch logs for a task

**Path Parameter:**
- `projectId` - The project identifier

**Response:**
```json
[
  "2026-02-13 11:25:10 - Server running on port 3000",
  "2026-02-13 11:25:11 - Request received",
  ...
]
```

**Example:**
```bash
curl -X GET http://localhost:9192/api/builds/tasks/demo-node-app/logs
```

---

### 5. Stop Task
**Endpoint:** `DELETE /api/builds/tasks/{projectId}`  
**Description:** Stop a running ECS task and clean up resources

**Path Parameter:**
- `projectId` - The project identifier

**Response:**
```json
{
  "projectId": "demo-node-app",
  "status": "STOPPED",
  "message": "Task stopped, port closed, registry cleaned"
}
```

**Example:**
```bash
curl -X DELETE http://localhost:9192/api/builds/tasks/demo-node-app
```

---

## 🧪 Testing Workflow

### Complete Test Flow

1. **Start a Local Build**
   ```bash
   curl -X POST http://localhost:9192/api/builds/test-local \
     -H "Content-Type: application/json" \
     -d '{"projectId": "demo-node-app", "runtime": "node"}'
   ```

2. **Wait 10-15 seconds for build to complete**

3. **Check Task Status**
   ```bash
   curl -X GET http://localhost:9192/api/builds/tasks/demo-node-app
   ```

4. **View Logs (optional)**
   ```bash
   curl -X GET http://localhost:9192/api/builds/tasks/demo-node-app/logs
   ```

5. **Stop the Task**
   ```bash
   curl -X DELETE http://localhost:9192/api/builds/tasks/demo-node-app
   ```

---

## 🗂️ Available Test Projects

Located in: `/Users/kavyapatel/Desktop/buildbox/Build-Box-backend/BuildServer/builds/`

| Project ID | Runtime | Description |
|------------|---------|-------------|
| `test-node-app` | `node` | Simple Node.js HTTP server |
| `demo-node-app` | `node` | Demo Node.js application |
| `demo-flask-app` | `python` | Demo Flask application |

---

## 🔧 Task Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Task is starting up |
| `RUNNING` | Task is active and accessible |
| `STOPPED` | Task has been stopped |
| `UNKNOWN` | Task state cannot be determined |

---

## 💡 Tips

1. **Local Testing**: Use `/api/builds/test-local` endpoint for projects in the `builds/` directory
2. **Production**: Use `/api/builds` endpoint when code is stored in S3
3. **Monitoring**: Poll the task status endpoint to track deployment progress
4. **Cleanup**: Always stop tasks when done to save AWS resources
5. **Logs**: Use the logs endpoint to debug application issues

---

## 🚨 Error Handling

### Task Not Found (404)
```json
HTTP 404 Not Found
```
**Cause:** Project ID doesn't exist in the registry  
**Solution:** Verify the project ID or trigger a new build

### Build Errors
Check the error log file:
```bash
cat build_error_{projectId}.log
```

---

## 🔐 Configuration Requirements

Make sure these are configured in `application.properties`:

```properties
# Build paths
build.base.dir=/path/to/builds
build.dockerfiles.dir=/path/to/dockerfiles

# AWS
aws.region=ap-south-1
ecr.registry.uri=YOUR_ECR_URI
ecs.cluster.name=YOUR_CLUSTER

# Redis
spring.data.redis.host=YOUR_REDIS_HOST
spring.data.redis.port=12608
```

---

## 📞 Health Check

Check if the server is running:
```bash
curl -I http://localhost:9192/api/builds/tasks/nonexistent
```

Should return `404 Not Found` (which means the server is responding)
