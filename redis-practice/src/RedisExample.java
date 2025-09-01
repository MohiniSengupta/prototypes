import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisExample {
    public static void main(String[] args) {
        // Create a connection pool
        try (JedisPool pool = new JedisPool("localhost", 6379)) {
            
            // Get a connection from the pool
            try (Jedis jedis = pool.getResource()) {
                // Basic String operations
                jedis.set("greeting", "Hello from Redis!");
                String value = jedis.get("greeting");
                System.out.println("Retrieved value: " + value);

                // List operations
                jedis.lpush("fruits", "apple", "banana", "orange");
                System.out.println("List length: " + jedis.llen("fruits"));
                System.out.println("List items: " + jedis.lrange("fruits", 0, -1));

                // Hash operations
                jedis.hset("user:1", "name", "John");
                jedis.hset("user:1", "email", "john@example.com");
                System.out.println("User hash: " + jedis.hgetAll("user:1"));

                // Clean up
                jedis.del("greeting", "fruits", "user:1");
            }
        }
    }
}
