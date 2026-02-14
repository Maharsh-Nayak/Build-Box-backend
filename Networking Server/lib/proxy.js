const httpProxy = require('http-proxy');
const axios = require('axios');
const redis = require('./redis');
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

    async handleRequest(req, res) {
        const hostname = req.hostname;
        // Parse Hostname
        // Production: (api.)?{project}.{user}.buildbox.com
        // Dev: (api.)?{project}.localhost (User part might be missing in dev, fallback to default user)

        let type = 'FRONTEND'; // Default
        let project = '';
        let user = 'test-user'; // Default for dev

        const parts = hostname.split('.');

        if (hostname.endsWith('buildbox.com')) {
            // production pattern
            if (parts[0] === 'api') {
                type = 'BACKEND';
                project = parts[1];
                user = parts[2];
            } else {
                type = 'FRONTEND';
                project = parts[0];
                user = parts[1];
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

        console.log(`[Proxy] Request: ${req.method} ${req.url} | Host: ${hostname} | Type: ${type} | Project: ${project} | User: ${user}`);

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
}

module.exports = new ReverseProxy();
