require('dotenv').config();

module.exports = {
    PORT: process.env.PORT || 8000,
    REDIS_Config: {
        host: process.env.REDIS_HOST,
        port: process.env.REDIS_PORT,
        password: process.env.REDIS_PASSWORD,
        username: process.env.REDIS_USERNAME,
        tls: process.env.REDIS_TLS === 'true' ? {} : undefined
    },
    BUILD_SERVER_URL: process.env.BUILD_SERVER_URL || 'http://localhost:9192',
    NGINX_URL: process.env.NGINX_URL || 'http://localhost:8080', // Internal Nginx for running tasks
    S3_BUCKET_URL: process.env.S3_BUCKET_URL || 'https://buildbox-frontend.s3.ap-south-1.amazonaws.com'
};
