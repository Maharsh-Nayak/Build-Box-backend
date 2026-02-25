const Redis = require('ioredis');
const config = require('../config.js');

class AnalyticsCollector {
    constructor() {
        this.redis = new Redis({
            ...config.REDIS_Config,
            db: 0
        });

        this.redis.on('connect', () => {
            console.log('✅ Analytics: Connected to Redis');
        });

        this.redis.on('error', (err) => {
            console.error('❌ Analytics Redis Error:', err);
        });
    }

    /**
     * Record an analytics event to Redis Stream
     * @param {Object} event - Analytics event data
     */
    async recordEvent(event) {
        try {
            const analyticsEvent = {
                projectId: event.projectId || 'unknown',
                accountId: event.accountId || 'unknown',
                eventType: event.eventType || 'REQUEST',
                path: event.path || '/',
                method: event.method || 'GET',
                statusCode: event.statusCode || 0,
                durationMs: event.durationMs || 0,
                bytesIn: event.bytesIn || 0,
                bytesOut: event.bytesOut || 0,
                source: event.source || 'unknown', // 'frontend' or 'backend'
                ipAddress: event.ipAddress || 'unknown',
                userAgent: event.userAgent || 'unknown',
                timestamp: new Date().toISOString()
            };

            // Push to Redis Stream: 'analytics-events' with individual fields
            const fields = [];
            Object.entries(analyticsEvent).forEach(([key, value]) => {
                fields.push(key, String(value));
            });
            
            await this.redis.xadd(
                'analytics-events',
                '*',
                ...fields
            );

            // Optionally log to console (for debugging)
            if (process.env.DEBUG_ANALYTICS) {
                console.log(`[Analytics] Event recorded:`, analyticsEvent);
            }
        } catch (error) {
            console.error('[Analytics] Error recording event:', error.message);
            // Don't throw - analytics failure shouldn't break the proxy
        }
    }

    /**
     * Close Redis connection
     */
    async close() {
        await this.redis.quit();
    }
}

module.exports = new AnalyticsCollector();
