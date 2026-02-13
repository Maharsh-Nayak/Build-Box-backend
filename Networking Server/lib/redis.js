const Redis = require('ioredis');
const config = require('../config.js');

class RedisClient {
    constructor() {
        this.client = new Redis({
            ...config.REDIS_Config,
            db: 0
        });

        this.client.on('connect', () => {
            console.log('✅ Connected to Redis');
        });

        this.client.on('error', (err) => {
            console.error('❌ Redis Error:', err);
        });
    }

    async getTask(projectId) {
        const key = `buildbox:task:${projectId}`;
        try {
            const data = await this.client.get(key);
            if (!data) return null;
            return JSON.parse(data);
        } catch (error) {
            console.error(`Error fetching task for ${projectId}:`, error);
            return null;
        }
    }
}

module.exports = new RedisClient();
