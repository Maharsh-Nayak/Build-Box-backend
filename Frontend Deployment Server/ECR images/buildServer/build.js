const { spawn } = require('child_process');
const path = require('path');
const Redis = require('ioredis');

// --- Configuration ---
const REDIS_CONFIG = {
    host: process.env.REDIS_HOST,
    port: process.env.REDIS_PORT,
    password: process.env.REDIS_PASSWORD,
    tls: {
        ca: require('fs').readFileSync('/etc/ssl/certs/ca-certificates.crt')
    }
};

const PROJECT_ID = process.env.PROJECT_ID;
const BUILD_ID = process.env.BUILD_ID; // e.g., "logs:uuid"
const REPO_URL = process.env.GIT_URL;

// --- Redis Client ---
const publisher = new Redis(REDIS_CONFIG);

// --- Helper Functions ---

async function pushLog(message) {
    try {
        // XADD logs:build_id * log "message"
        await publisher.xadd(`logs:${BUILD_ID}`, '*', 'log', message);
        console.log(message); // Also log to internal stdout for debugging
    } catch (err) {
        console.error('Redis Push Error:', err);
    }
}

async function executeCommand(command, args, cwd) {
    return new Promise((resolve, reject) => {
        const proc = spawn(command, args, { cwd, shell: true });

        // Stream Stdout
        proc.stdout.on('data', (data) => {
            const lines = data.toString().split('\n');
            lines.forEach(line => {
                if (line.trim()) pushLog(line.trim());
            });
        });

        // Stream Stderr
        proc.stderr.on('data', (data) => {
            const lines = data.toString().split('\n');
            lines.forEach(line => {
                if (line.trim()) pushLog(line.trim());
            });
        });

        proc.on('close', (code) => {
            if (code === 0) {
                resolve();
            } else {
                reject(new Error(`Command "${command} ${args.join(' ')}" failed with code ${code}`));
            }
        });
    });
}

// --- Main Build Pipeline ---

async function main() {
    try {
        await pushLog(`➡️  INITIATING BUILD: ${BUILD_ID}`);

        // 1. Clone Repo
        await pushLog('➡️  STEP: CLONE_REPO');
        await executeCommand('git', ['clone', REPO_URL, 'repo'], '/app');

        const repoDir = path.join('/app', 'repo');
        const frontendDir = path.join(repoDir, process.env.FRONTENT_DIR || 'frontend'); // Safety fallback
        const backendDir = path.join(repoDir, process.env.BACKEND_DIR || 'backend');

        // 2. Install Frontend Deps
        await pushLog('➡️  STEP: INSTALL_FRONTEND_DEPS');
        await executeCommand('npm', ['install'], frontendDir);

        // 3. Build Frontend
        await pushLog('➡️  STEP: BUILD_FRONTEND');
        await executeCommand('npm', ['run', 'build', '--', '--base=./'], frontendDir);

        // 4. Upload Frontend
        await pushLog('➡️  STEP: UPLOAD_FRONTEND');
        // Using AWS CLI via spawn is perfectly fine and often simpler than AWS SDK for "sync"
        await executeCommand('aws', [
            's3', 'sync', 'dist/',
            `s3://buildbox-frontend/${process.env.USER_ID}/${process.env.PROJECT_NAME}/Frontend`,
            '--delete'
        ], frontendDir);

        // 5. Install Backend Deps
        await pushLog('➡️  STEP: INSTALL_BACKEND_DEPS');
        await executeCommand('npm', ['install'], backendDir);

        // 6. Upload Backend
        await pushLog('➡️  STEP: UPLOAD_BACKEND');
        await executeCommand('aws', [
            's3', 'sync', './',
            `s3://buildbox-frontend/${process.env.USER_ID}/${process.env.PROJECT_NAME}/Backend`,
            '--delete'
        ], backendDir);

        await pushLog('✅ BUILD SUCCESS');
        await pushLog('__BUILD_STATUS__:SUCCESS');

    } catch (error) {
        await pushLog(`❌ BUILD FAILED: ${error.message}`);
        await pushLog('__BUILD_STATUS__:FAILED');
        process.exit(1);
    } finally {
        publisher.quit();
    }
}

main();