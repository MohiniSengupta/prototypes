import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RouteByUser {
    private final String leaderJdbcUrl;
    private final String followerJdbcUrl;
    private final String dbUser;
    private final String dbPassword;

    // Tracks users who just wrote and should be routed to leader ONCE for read-your-write.
    private final Set<String> usersWithPendingLeaderRead = new HashSet<>();

    public RouteByUser(String leaderJdbcUrl, String followerJdbcUrl, String dbUser, String dbPassword) {
        this.leaderJdbcUrl = Objects.requireNonNull(leaderJdbcUrl);
        this.followerJdbcUrl = Objects.requireNonNull(followerJdbcUrl);
        this.dbUser = Objects.requireNonNull(dbUser);
        this.dbPassword = Objects.requireNonNull(dbPassword);
    }

    public void writeMessage(String userId, String content) throws SQLException {
        try (Connection conn = DriverManager.getConnection(leaderJdbcUrl, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement("INSERT INTO messages(user_id, content) VALUES (?, ?)");) {
            ps.setString(1, userId);
            ps.setString(2, content);
            int rows = ps.executeUpdate();
            System.out.printf("%-6s | node=%-7s | user=%-10s | rows=%d%n", "WRITE", "leader", userId, rows);
            usersWithPendingLeaderRead.add(userId);
        }
    }

    public void readLatestForUser(String requesterUserId, String targetUserId) throws SQLException {
        boolean stickToLeader = usersWithPendingLeaderRead.contains(requesterUserId);
        String jdbc = stickToLeader ? leaderJdbcUrl : followerJdbcUrl;
        String node = stickToLeader ? "leader" : "follower";

        try (Connection conn = DriverManager.getConnection(jdbc, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, user_id, content, created_at FROM messages WHERE user_id = ? ORDER BY id DESC LIMIT 1");) {
            ps.setString(1, targetUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String userId = rs.getString("user_id");
                    String content = rs.getString("content");
                    String createdAt = rs.getString("created_at");
                    System.out.printf("%-6s | node=%-7s | req=%-10s | target=%-10s | id=%-4d | at=%s%n",
                            "READ", node, requesterUserId, userId, id, createdAt);
                    System.out.printf("%-6s | %-12s %s%n", "", "content:", content);
                } else {
                    System.out.printf("%-6s | node=%-7s | req=%-10s | target=%-10s | %s%n",
                            "READ", node, requesterUserId, targetUserId, "not found");
                }
            }
        }

        // One-shot stickiness consumed
        if (stickToLeader) {
            usersWithPendingLeaderRead.remove(requesterUserId);
        }
    }

    public static void main(String[] args) throws Exception {
        // Ensure PostgreSQL JDBC driver is loaded (helpful in some environments)
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC driver not found on classpath. See README for download instructions.");
            throw e;
        }

        String leaderUrl = getenvOrDefault("LEADER_JDBC_URL", "jdbc:postgresql://localhost:5432/appdb");
        String followerUrl = getenvOrDefault("FOLLOWER_JDBC_URL", "jdbc:postgresql://localhost:5433/appdb");
        String user = getenvOrDefault("DB_USER", "app");
        String password = getenvOrDefault("DB_PASSWORD", "app_pw");

        RouteByUser demo = new RouteByUser(leaderUrl, followerUrl, user, password);

        // 1) Writer userA writes a message -> goes to leader
        demo.writeMessage("userA", "hello from A");

        // 2) userA immediately reads (read-your-write consistency -> leader)
        demo.readLatestForUser("userA", "userA");

        // 3) Another user reads userA's data (may hit replica lag -> follower)
        demo.readLatestForUser("userC", "userA");

        // 4) Another user reads userX's pre-seeded data (follower)
        demo.readLatestForUser("userX", "userX");

        // 5) Optional: userA reads userX (now routed to follower, one-shot stickiness already consumed)
        demo.readLatestForUser("userA", "userX");

        System.out.println("Done.");
    }

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? def : v;
    }
}

