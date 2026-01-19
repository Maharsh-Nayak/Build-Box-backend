const express = require('express');
const httpProxy = require('http-proxy');

const app = express();
const PORT = 8000;

// S3 base path
const BASE_PATH =
  'https://buildbox-frontend.s3.ap-south-1.amazonaws.com/';

// Create proxy
const proxy = httpProxy.createProxyServer({
  changeOrigin: true,
});

// Main reverse proxy middleware
app.use((req, res) => {
  try {
    const hostname = req.hostname;
    // userid.projectname.localhost

    const parts = hostname.split('.');

    if (parts.length < 3) {
      return res.status(400).send('Invalid domain format');
    }

    const userId = parts[0];
    const projectName = parts[1];

    const target = `${BASE_PATH}/${userId}/${projectName}/Frontend/index.html`;

    console.log(`[PROXY] ${hostname}${req.url} → ${target}`);

    proxy.web(req, res, { target });

  } catch (err) {
    console.error(err);
    res.status(500).send('Reverse proxy error');
  }
});

// Fix root path → index.html
proxy.on('proxyReq', (proxyReq, req) => {
  if (req.url === '/') {
    proxyReq.path += 'index.html';
  }
});

// Handle proxy errors
proxy.on('error', (err, req, res) => {
  console.error('Proxy error:', err.message);
  res.writeHead(502, { 'Content-Type': 'text/plain' });
  res.end('Bad Gateway');
});

// Start server
app.listen(PORT, () => {
  console.log(` Reverse Proxy running on port ${PORT}`);
});
