package cn.edu.campus.news.repository;

import cn.edu.campus.news.domain.Models.UserAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;
    public UserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Optional<UserAccount> byUsername(String username) {
        return jdbc.query("SELECT id,username,password_hash,display_name,role,enabled FROM sys_user WHERE username=?",
                (rs,n) -> new UserAccount(rs.getLong("id"), rs.getString("username"), rs.getString("password_hash"),
                        rs.getString("display_name"), rs.getString("role"), rs.getBoolean("enabled")), username)
                .stream().findFirst();
    }
    public long count() { return jdbc.queryForObject("SELECT COUNT(*) FROM sys_user", Long.class); }
    public void create(String username, String hash, String displayName, String role) {
        jdbc.update("INSERT INTO sys_user(username,password_hash,display_name,role) VALUES (?,?,?,?)",
                username, hash, displayName, role);
    }
}
