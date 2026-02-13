const express = require('express');
const config = require('./config');
const proxy = require('./lib/proxy');

const app = express();

app.use(async (req, res, next) => {
    try {
        const hostname = req.hostname;
        const subdomain = hostname.split('.')[0];

        // Skip health checks or internal routes which might not have a subdomain
        if (hostname === 'localhost' || !subdomain) {
            return res.status(200).send('BuildBox Reverse Proxy Running');
        }

        await proxy.handleRequest(req, res);
    } catch (error) {
        console.error('Request processing error:', error);
        res.status(500).send('Internal Proxy Error');
    }
});

app.listen(config.PORT, () => {
    console.log(`Reverse Proxy running on port ${config.PORT}`);
    console.log(`BuildServer URL: ${config.BUILD_SERVER_URL}`);
    console.log(`Nginx URL: ${config.NGINX_URL}`);
});
