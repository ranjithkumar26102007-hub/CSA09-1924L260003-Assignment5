import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database Configuration Manager for Task B3.
 *
 * Course: CSA09 – Programming in Java
 * Student: R. Ranjith Kumar | Register Number: 1924L260003
 */
public class DbConfig {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/csa09_assignment5?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "";

    private static String dbUrl = DEFAULT_URL;
    private static String dbUser = DEFAULT_USER;
    private static String dbPass = DEFAULT_PASS;

    static {
        Path propFile = Paths.get("db.properties");
        if (Files.exists(propFile)) {
            try (InputStream in = new FileInputStream(propFile.toFile())) {
                Properties props = new Properties();
                props.load(in);
                if (props.getProperty("db.url") != null) dbUrl = props.getProperty("db.url");
                if (props.getProperty("db.user") != null) dbUser = props.getProperty("db.user");
                if (props.getProperty("db.password") != null) dbPass = props.getProperty("db.password");
            } catch (Exception ignored) {}
        }
        String envUrl = System.getenv("CSA09_DB_URL");
        if (envUrl != null && !envUrl.isBlank()) dbUrl = envUrl;
        String envUser = System.getenv("CSA09_DB_USER");
        if (envUser != null && !envUser.isBlank()) dbUser = envUser;
        String envPass = System.getenv("CSA09_DB_PASS");
        if (envPass != null) dbPass = envPass;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }

    public static String getDbUrl() { return dbUrl; }
}
