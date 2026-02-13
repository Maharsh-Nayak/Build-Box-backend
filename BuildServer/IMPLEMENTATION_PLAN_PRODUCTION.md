# Implementation Plan: Production-Ready BuildBox Server

This plan outlines the steps required to transition the `BuildServer` from an MVP state to a production-ready infrastructure, strictly following the **Senior Platform Engineer** guidelines for security, routing, and lifecycle management.

---

## Phase 1: Networking Layer Refactor (High Priority)
**Goal**: Remove infrastructure management from the JVM and implement a declarative routing model.

### 1.1 Refactor `NginxRoutingService` 
- [ ] **Remove Lifecycle Code**: Delete `initNginx()`, `startNginxContainer()`, and all references to Docker containers.
- [ ] **Declarative Config Writer**: Modify `addRoute` to write only to a dedicated directory (e.g., `/etc/nginx/sites-enabled/`).
- [ ] **Atomic Reload**: Switch from `docker exec` to a host-level `nginx -s reload` command.
- [ ] **Clean removal**: Ensure `removeRoute` actually deletes the file and triggers a reload.

### 1.2 Decommission `SecurityGroupService`
- [ ] **Delete Service**: Remove `SecurityGroupService.java` entirely.
- [ ] **Static Rule Documentation**: Document exactly which static ports must be open in AWS (Entry Proxy 8080 -> Internal Bridge Range).
- [ ] **Cleanup**: Remove `securityGroupService` calls from `TaskPortDiscoveryService` and `TaskCleanupScheduler`.

### 1.3 Update Configuration
- [ ] **Hardened Paths**: Add `nginx.conf.path` to `application.properties` pointing to a persistent system directory.
- [ ] **Permission Setup**: Create a setup script to grant the `buildserver` user permission to write to Nginx config directories.

---

## Phase 2: State Management & Persistence
**Goal**: Ensure the platform can survive a server restart without losing track of running apps.

### 2.1 Persistence Layer for TaskRegistry
- [ ] **Redis Integration**: Switch from `ConcurrentHashMap` to Redis/Valkey (already partially configured in your properties).
- [ ] **Schema Update**: Store `TaskInfo` as a serialized JSON or Hash in Redis.
- [ ] **Recovery Logic**: Implement a "Sync" on startup that checks ECS for running tasks and rebuilds the Nginx config files if they are missing.

---

## Phase 3: Hardware & Security Hardening
**Goal**: Move away from hardcoded secrets and insecure paths.

### 3.1 IAM Role Deployment
- [ ] **Refactor AwsConfig**: Update `S3Client`, `EcsClient`, and `Ec2Client` to use `DefaultCredentialsProvider` instead of `StaticCredentialsProvider`.
- [ ] **Requirement Documentation**: List the IAM policies (S3 Read/Write, ECS RunTask, ECR Read/Write) required for the EC2 Host.

### 3.2 Path Hardening
- [ ] **Move Builds**: Change `build.base.dir` from user desktop/tmp to a standard service directory (e.g., `/var/lib/buildbox/builds`).
- [ ] **Docker Socket Security**: Ensure the BuildServer user has minimal necessary access to the Docker group.

---

## Phase 4: Observability & User Experience
**Goal**: Provide better feedback to users during long build processes.

### 4.1 CloudWatch Log Streaming
- [ ] **Log Retrieval**: Implement a service to fetch real-time logs from CloudWatch for a specific `projectId`.
- [ ] **API Endpoint**: Create `GET /api/tasks/{projectId}/logs` for the frontend to consume.

### 4.2 Error Handling
- [ ] **Status Mapping**: Update the DTOs to include specific failure reasons (e.g., `DOCKER_BUILD_FAILED`, `S3_DOWNLOAD_ERROR`, `ECS_CAPACITY_EXCEEDED`).

---

## Phase 5: Frontend Hosting (MVP Launch)
**Goal**: Deploy the static UI to act as the platform control center.

### 5.1 S3 / CloudFront Setup
- [ ] **Automation**: Create a script/service to upload the Frontend build files to the `buildbox-frontend` bucket.
- [ ] **Public Entry Point**: Configure CloudFront to serve the UI and route `/api/*` requests to the EC2 host.

---

## 🚀 Execution Strategy

1. **Immediate Refactor**: Apply Phase 1.1 and 1.2 today to fix the critical design flaws.
2. **Infrastructure Prep**: Run a "System Setup" script on the EC2 host to create Nginx directories and set permissions.
3. **End-to-End Test**: Trigger a build via the Node.js Proxy and verify the Nginx route is created and reachable on Port 8080.
