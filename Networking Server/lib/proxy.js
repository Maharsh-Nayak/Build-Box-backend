const httpProxy = require('http-proxy');
const axios = require('axios');
const redis = require('./redis');
const analytics = require('./analytics');
const config = require('../config');

const proxy = httpProxy.createProxyServer({});

// Error handling for the proxy
proxy.on('error', (err, req, res) => {
    console.error('Proxy Error:', err);
    if (!res.headersSent) {
        res.status(502).send('Bad Gateway');
    }
});

class ReverseProxy {
    constructor() {
        this.proxy = proxy;
    }

    async getProjectOwner(projectName) {
        try {
            if (!projectName || projectName === '') {
                return 'unknown';
            }

            // Call the project API to get owner information by project name
            const response = await axios.get(`${config.FRONTEND_SERVER_URL}/api/projects/by-name/${projectName}/owner`);
            
            // Return the owner's user ID
            return response.data.userId?.toString() || 'unknown';
        } catch (err) {
            console.error(`[Proxy] Error fetching owner for project ${projectName}:`, err.message);
            return 'unknown';
        }
    }

    async handleRequest(req, res) {
        const hostname = req.hostname;
        const startTime = Date.now();

        // Handle Platform-level API calls (from Main UI)
        if (req.url.startsWith('/api') && (hostname === 'localhost' || hostname === '127.0.0.1')) {
            return await this.handleApiRequest(req, res);
        }

        // Parse Hostname project subdomains
        let type = 'FRONTEND';
        let project = '';
        let user = 'unknown'; // Will be populated after we know the project

        const parts = hostname.split('.');

        if (hostname.endsWith('buildbox.com')) {
            // production pattern: api.project-name.buildbox.com OR project-name.buildbox.com
            // Note: We don't use hostname-based user anymore, we look it up from project DB
            if (parts[0] === 'api') {
                type = 'BACKEND';
                project = parts[1];
            } else {
                type = 'FRONTEND';
                project = parts[0];
            }
        } else {
            // localhost / dev pattern
            // usage: api.demo-node-app.localhost OR demo-node-app.localhost
            if (parts[0] === 'api') {
                type = 'BACKEND';
                project = parts[1];
            } else {
                type = 'FRONTEND';
                project = parts[0];
            }
        }

        // Fetch project owner from database (works for both localhost and production)
        if (project) {
            user = await this.getProjectOwner(project);
        }

        console.log(`[Proxy] Request: ${req.method} ${req.url} | Host: ${hostname} | Type: ${type} | Project: ${project} | User: ${user}`);

        // Capture response to record analytics
        const originalEnd = res.end;
        res.end = function(...args) {
            const durationMs = Date.now() - startTime;
            const bytesIn = req.get('content-length') || 0;
            const bytesOut = res.get('content-length') || 0;

            // Record analytics event
            analytics.recordEvent({
                projectId: project,
                accountId: user,
                eventType: 'REQUEST',
                path: req.url,
                method: req.method,
                statusCode: res.statusCode,
                durationMs: durationMs,
                bytesIn: parseInt(bytesIn) || 0,
                bytesOut: parseInt(bytesOut) || 0,
                source: type === 'FRONTEND' ? 'frontend' : 'backend',
                ipAddress: req.ip || req.connection.remoteAddress || 'unknown',
                userAgent: req.get('user-agent') || 'unknown'
            }).catch(err => {
                console.error('[Proxy] Error recording analytics:', err);
            });

            originalEnd.apply(res, args);
        };

        if (type === 'FRONTEND') {
            await this.handleFrontendRequest(req, res, user, project);
        } else {
            await this.handleBackendRequest(req, res, user, project);
        }
    }

    async handleFrontendRequest(req, res, user, project) {
        // Target: S3 Bucket
        // Path Rewrite: / -> /<user>/<project>/Frontend/index.html
        //               /assets/foo -> /<user>/<project>/Frontend/assets/foo

        const s3Prefix = `/${user}/${project}/Frontend`;
        let targetPath = req.url;

        if (targetPath === '/') {
            targetPath = '/index.html';
        }

        req.url = s3Prefix + targetPath;

        console.log(`[Frontend] Proxying to S3: ${config.S3_BUCKET_URL}${req.url}`);

        this.proxy.web(req, res, {
            target: config.S3_BUCKET_URL,
            changeOrigin: true,
            secure: true // S3 sends https
        });
    }

    async handleBackendRequest(req, res, user, project) {
        // 1. Check Status via BuildServer API (as per Prompt Request)
        try {
            const statusUrl = `${config.BUILD_SERVER_URL}/internal/apps/${user}/${project}/status`;
            const statusRes = await axios.get(statusUrl);
            const statusData = statusRes.data; // { state: "RUNNING", host: "...", port: ... } OR { state: "STOPPED" }

            if (statusData.state === 'RUNNING') {
                const routing = statusData.routing;

                if (routing && routing.mode === 'alb') {
                    // ALB-First Routing Mode
                    console.log(`[Backend] ALB Mode: Proxying to ${routing.targetUrl} with Host: ${routing.hostHeader}`);

                    this.proxy.web(req, res, {
                        target: routing.targetUrl,
                        changeOrigin: true,
                        headers: {
                            host: routing.hostHeader
                        }
                    });
                    return;
                }

                // Default / Direct Routing Mode
                const target = `http://${statusData.host}:${statusData.port}`;
                console.log(`[Backend] Direct Mode: Proxying to Running Task: ${target}`);

                this.proxy.web(req, res, {
                    target: target,
                    changeOrigin: true
                });
                return;
            } else {
                // STOPPED -> Trigger Cold Start
                console.log(`[Backend] App Stopped. Triggering Cold Start...`);

                // Call Start
                await axios.post(`${config.BUILD_SERVER_URL}/internal/apps/${user}/${project}/start`);

                // Wait
                const success = await this.waitForRunning(user, project);
                if (success) {
                    // Get Info Again
                    const newStatus = await axios.get(statusUrl);
                    const t = newStatus.data;
                    const routing = t.routing;

                    if (routing && routing.mode === 'alb') {
                        console.log(`[Backend] Cold Start (ALB Mode) Complete. Proxying to: ${routing.targetUrl}`);
                        this.proxy.web(req, res, {
                            target: routing.targetUrl,
                            changeOrigin: true,
                            headers: { host: routing.hostHeader }
                        });
                    } else {
                        const target = `http://${t.host}:${t.port}`;
                        console.log(`[Backend] Cold Start (Direct Mode) Complete. Proxying to: ${target}`);
                        this.proxy.web(req, res, { target: target, changeOrigin: true });
                    }
                } else {
                    res.status(503).send('Service Unavailable - Failed to start app');
                }
            }
        } catch (error) {
            console.error('[Backend] Error:', error.message);
            res.status(500).send('Internal Proxy Error');
        }
    }

    async waitForRunning(user, project) {
        const statusUrl = `${config.BUILD_SERVER_URL}/internal/apps/${user}/${project}/status`;
        for (let i = 0; i < 30; i++) { // 30 attempts
            await new Promise(r => setTimeout(r, 1000));
            try {
                const res = await axios.get(statusUrl);
                if (res.data.state === 'RUNNING') return true;
            } catch (e) {
                // ignore errors during poll
            }
        }
        return false;
    }

    async handleApiRequest(req, res) {
        let target;
        // Route to Log Analytics or Frontend Deployment Server
        if (req.url.startsWith('/api/v2/buildLogs') || req.url.startsWith('/api/logs')) {
            target = config.LOG_ANALYTICS_URL;
        } else {
            target = config.FRONTEND_SERVER_URL;
        }

        console.log(`[API Proxy] ${req.method} ${req.url} -> ${target}`);
        this.proxy.web(req, res, {
            target: target,
            changeOrigin: true
        });
    }
}

module.exports = new ReverseProxy();
