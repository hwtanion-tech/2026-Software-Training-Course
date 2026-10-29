package cn.edu.campus.news.repository;

import cn.edu.campus.news.domain.Models.UserAccount;
import cn.edu.campus.news.domain.Models.AccountView;
import cn.edu.campus.news.domain.Models.Page;
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
    public Optional<UserAccount> byId(long id) {
        return jdbc.query("SELECT id,username,password_hash,display_name,role,enabled FROM sys_user WHERE id=?",
                (rs,n) -> new UserAccount(rs.getLong("id"),rs.getString("username"),rs.getString("password_hash"),
                        rs.getString("display_name"),rs.getString("role"),rs.getBoolean("enabled")),id).stream().findFirst();
    }
    public Page<AccountView> list(String keyword,int page,int size) {
        String term="%"+keyword.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        String where=" WHERE username LIKE ? ESCAPE '!' OR display_name LIKE ? ESCAPE '!'";
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM sys_user"+where,Long.class,term,term);
        var items=jdbc.query("SELECT id,username,display_name,role,enabled FROM sys_user"+where+" ORDER BY id LIMIT ? OFFSET ?",
                (rs,n) -> new AccountView(rs.getLong("id"),rs.getString("username"),rs.getString("display_name"),
                        rs.getString("role"),rs.getBoolean("enabled")),term,term,size,(page-1)*size);
        return new Page<>(items,total,page,size,(total+size-1)/size);
    }
    public void lockAccounts() {
        // 课程规模账号较少，管理写入统一串行，避免并发操作移除最后一个管理员。
        jdbc.queryForList("SELECT id FROM sys_user ORDER BY id FOR UPDATE");
    }
    public long enabledAdmins() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE role='ADMIN' AND enabled=TRUE",Long.class);
    }
    public void update(long id,String displayName,String role,boolean enabled,String hash) {
        jdbc.update("UPDATE sys_user SET display_name=?,role=?,enabled=?,password_hash=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                displayName,role,enabled,hash,id);
    }
    public void create(String username, String hash, String displayName, String role) {
        jdbc.update("INSERT INTO sys_user(username,password_hash,display_name,role) VALUES (?,?,?,?)",
                username, hash, displayName, role);
    }
}
