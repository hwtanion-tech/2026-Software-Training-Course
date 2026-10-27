package cn.edu.campus.news.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** 手动配置后执行只读连接检查；不建库、不建表，也不写入数据。 */
@Service
public class DatabaseProbe {
    private final boolean enabled;
    private final String url;
    private final String username;
    private final String password;

    public DatabaseProbe(@Value("${campus.database.enabled}") boolean enabled,
                         @Value("${campus.database.url}") String url,
                         @Value("${campus.database.username}") String username,
                         @Value("${campus.database.password}") String password) {
        this.enabled = enabled;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Result check() {
        if (!enabled) {
            return new Result("NOT_CONFIGURED", "未启用 MySQL 检查，请按 README 配置后重启后端。");
        }
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", "3000");
        properties.setProperty("socketTimeout", "3000");
        try (var connection = DriverManager.getConnection(url, properties);
             var statement = connection.prepareStatement("SELECT 1")) {
            statement.setQueryTimeout(3);
            try (var result = statement.executeQuery()) {
                if (result.next() && result.getInt(1) == 1) {
                    return new Result("UP", "MySQL 连接及 SELECT 1 检查成功。");
                }
                return new Result("DOWN", "数据库未返回预期结果。");
            }
        } catch (SQLException exception) {
            // 不向浏览器或日志暴露数据库地址、用户名、密码和原始异常。
            return new Result("DOWN", "MySQL 检查失败，请核对服务、数据库名及本地凭据。");
        }
    }

    public record Result(String status, String message) { }
}
