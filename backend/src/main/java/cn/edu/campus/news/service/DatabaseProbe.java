package cn.edu.campus.news.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import javax.sql.DataSource;

/** 手动配置后执行只读连接检查；不建库、不建表，也不写入数据。 */
@Service
public class DatabaseProbe {
    private final DataSource dataSource;
    private final String mode;

    public DatabaseProbe(DataSource dataSource, @Value("${campus.mode}") String mode) {
        this.dataSource = dataSource;
        this.mode = mode;
    }

    public Result check() {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT 1")) {
            statement.setQueryTimeout(3);
            try (var result = statement.executeQuery()) {
                if (result.next() && result.getInt(1) == 1) {
                    return new Result("UP", mode, "当前业务数据库连接检查成功");
                }
                return new Result("DOWN", mode, "数据库未返回预期结果");
            }
        } catch (SQLException exception) {
            // 不向浏览器或日志暴露数据库地址、用户名、密码和原始异常。
            return new Result("DOWN", mode, "数据库连接失败，请检查本地配置");
        }
    }

    public record Result(String status, String mode, String message) { }
}
