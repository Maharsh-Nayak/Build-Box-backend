const http = require('http');

const PORT = process.env.PORT || 3000;

const server = http.createServer((req, res) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'healthy', timestamp: new Date().toISOString() }));
  } else {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(`
      <html>
        <head><title>Demo App</title></head>
        <body style="font-family: Arial; text-align: center; padding: 50px;">
          <h1>🚀 Hello from ECS!</h1>
          <p>This Node.js app is running on AWS ECS (EC2 launch type)</p>
          <p>Container Port: ${PORT}</p>
          <p>Timestamp: ${new Date().toISOString()}</p>
        </body>
      </html>
    `);
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running on http://0.0.0.0:${PORT}`);
});
