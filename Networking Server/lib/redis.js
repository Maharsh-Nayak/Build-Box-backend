const Redis = require('ioredis');
const config = require('../config.js');

class RedisClient {
    constructor() {
        this.client = new Redis({
            ...config.REDIS_Config,
            db: 0
        });

        // In-memory route cache with TTL
        this.routeCache = new Map();
        this.CACHE_TTL_MS = 60 * 1000; // 60 seconds

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

    /**
     * Get a cached route for a project slug.
     * Returns { host, port } or null.
     */
    async getRoute(slug) {
        // Check in-memory cache first
        const cached = this.routeCache.get(slug);
        if (cached && (Date.now() - cached.timestamp < this.CACHE_TTL_MS)) {
            return cached.data;
        }

        // Fallback to Redis
        const key = `buildbox:route:${slug}`;
        try {
            const data = await this.client.get(key);
            if (!data) return null;
            const parsed = JSON.parse(data);
            this.routeCache.set(slug, { data: parsed, timestamp: Date.now() });
            return parsed;
        } catch (error) {
            console.error(`Error fetching route for ${slug}:`, error);
            return null;
        }
    }

    /**
     * Cache a route for a project slug.
     */
    async setRoute(slug, host, port) {
        const key = `buildbox:route:${slug}`;
        const data = JSON.stringify({ host, port });
        try {
            await this.client.set(key, data, 'EX', 300); // 5 min TTL in Redis
            this.routeCache.set(slug, { data: { host, port }, timestamp: Date.now() });
        } catch (error) {
            console.error(`Error setting route for ${slug}:`, error);
        }
    }

    /**
     * Check deployment status from cache.
     */
    async getDeploymentStatus(slug) {
        const key = `buildbox:deployment:${slug}`;
        try {
            const data = await this.client.get(key);
            return data ? JSON.parse(data) : null;
        } catch (error) {
            console.error(`Error fetching deployment status for ${slug}:`, error);
            return null;
        }
    }
}

module.exports = new RedisClient();
