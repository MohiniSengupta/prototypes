import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class UserManagement {
    private static JedisPool pool = new JedisPool("localhost", 6379);

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\nUser Management System");
                System.out.println("1. Create User");
                System.out.println("2. Get User Details");
                System.out.println("3. Update User Email");
                System.out.println("4. Delete User");
                System.out.println("5. List All Users");
                System.out.println("6. Exit");
                System.out.print("Choose an option: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // consume newline

                switch (choice) {
                    case 1:
                        createUser(scanner);
                        break;
                    case 2:
                        getUserDetails(scanner);
                        break;
                    case 3:
                        updateUserEmail(scanner);
                        break;
                    case 4:
                        deleteUser(scanner);
                        break;
                    case 5:
                        listAllUsers();
                        break;
                    case 6:
                        pool.close();
                        return;
                    default:
                        System.out.println("Invalid option!");
                }
            }
        }
    }

    private static void createUser(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter age: ");
        String age = scanner.nextLine();

        try (Jedis jedis = pool.getResource()) {
            // Check if user exists
            if (jedis.exists("user:" + username)) {
                System.out.println("User already exists!");
                return;
            }

            // Store user data as a hash
            Map<String, String> userData = new HashMap<>();
            userData.put("email", email);
            userData.put("age", age);
            userData.put("created_at", String.valueOf(System.currentTimeMillis()));

            jedis.hset("user:" + username, userData);
            // Add to user list
            jedis.sadd("users", username);
            System.out.println("User created successfully!");
        }
    }

    private static void getUserDetails(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        try (Jedis jedis = pool.getResource()) {
            Map<String, String> userData = jedis.hgetAll("user:" + username);
            if (userData.isEmpty()) {
                System.out.println("User not found!");
                return;
            }

            System.out.println("\nUser Details:");
            System.out.println("Username: " + username);
            System.out.println("Email: " + userData.get("email"));
            System.out.println("Age: " + userData.get("age"));
            System.out.println("Created at: " + userData.get("created_at"));
        }
    }

    private static void updateUserEmail(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter new email: ");
        String newEmail = scanner.nextLine();

        try (Jedis jedis = pool.getResource()) {
            if (!jedis.exists("user:" + username)) {
                System.out.println("User not found!");
                return;
            }

            jedis.hset("user:" + username, "email", newEmail);
            System.out.println("Email updated successfully!");
        }
    }

    private static void deleteUser(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        try (Jedis jedis = pool.getResource()) {
            if (!jedis.exists("user:" + username)) {
                System.out.println("User not found!");
                return;
            }

            jedis.del("user:" + username);
            jedis.srem("users", username);
            System.out.println("User deleted successfully!");
        }
    }

    private static void listAllUsers() {
        try (Jedis jedis = pool.getResource()) {
            System.out.println("\nAll Users:");
            for (String username : jedis.smembers("users")) {
                Map<String, String> userData = jedis.hgetAll("user:" + username);
                System.out.println("\nUsername: " + username);
                System.out.println("Email: " + userData.get("email"));
                System.out.println("Age: " + userData.get("age"));
            }
        }
    }
}
