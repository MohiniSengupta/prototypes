import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;
import java.util.Set;

public class RedisPatterns {
    private static final JedisPool pool = new JedisPool("localhost", 6379);

    public static void main(String[] args) {
        try (Jedis jedis = pool.getResource()) {
            // 1. Caching with TTL
            cacheWithTTL(jedis);

            // 2. Rate Limiting
            boolean canProceed = checkRateLimit(jedis, "user123", 5, 60);
            System.out.println("Rate limit allows request: " + canProceed);

            // 3. Session Management
            handleSession(jedis, "user123");

            // 4. Leaderboard
            updateLeaderboard(jedis);

            // 5. Object Caching
            cacheUserProfile(jedis, "user123");
        } finally {
            pool.close();
        }
    }

    // 1. Caching with TTL
    private static void cacheWithTTL(Jedis jedis) {
        // Cache-aside pattern
        String cachedValue = jedis.get("expensive:computation");
        if (cachedValue == null) {
            cachedValue = "computed_value"; // Simulate expensive computation
            jedis.setex("expensive:computation", 3600, cachedValue); // 1 hour TTL
        }
        System.out.println("Cached value: " + cachedValue);
    }

    // 2. Rate Limiting
    private static boolean checkRateLimit(Jedis jedis, String user, int maxRequests, int windowSeconds) {
        String key = "ratelimit:" + user;
        long requests = jedis.incr(key);
        
        if (requests == 1) {
            jedis.expire(key, windowSeconds);
        }
        
        return requests <= maxRequests;
    }

    // 3. Session Management
    private static void handleSession(Jedis jedis, String userId) {
        // Store session with TTL
        SetParams params = new SetParams().ex(1800); // 30 minutes
        jedis.set("session:" + userId, "{\"lastAccess\":\"2023-01-01\"}", params);
        
        // Update user's active sessions set
        jedis.sadd("active:sessions", userId);
    }

    // 4. Leaderboard using Sorted Set
    private static void updateLeaderboard(Jedis jedis) {
        jedis.zadd("leaderboard", 100, "player1");
        jedis.zadd("leaderboard", 200, "player2");
        jedis.zadd("leaderboard", 150, "player3");

        // Get top 3 players
        Set<String> topPlayers = jedis.zrevrange("leaderboard", 0, 2);
        System.out.println("Top 3 players: " + topPlayers);
        
        // Get player2's rank (0-based)
        long rank = jedis.zrevrank("leaderboard", "player2");
        System.out.println("Player2 rank: " + (rank + 1));
    }

    // 5. Object Caching using Hash
    private static void cacheUserProfile(Jedis jedis, String userId) {
        // Store user profile as hash
        jedis.hset("user:" + userId, "name", "John Doe");
        jedis.hset("user:" + userId, "email", "john@example.com");
        jedis.hset("user:" + userId, "lastLogin", String.valueOf(System.currentTimeMillis()));

        // Set TTL for the entire hash
        jedis.expire("user:" + userId, 3600); // 1 hour

        // Atomic increment for visit count
        jedis.hincrBy("user:" + userId, "visits", 1);
    }
}
