package cn.edu.campus.news.repository;

import cn.edu.campus.news.domain.Models.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepository {
    private final JdbcTemplate jdbc;
    public CategoryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Category> all(boolean enabledOnly) {
        return jdbc.query("SELECT * FROM news_category" + (enabledOnly ? " WHERE enabled=TRUE" : "") + " ORDER BY sort_order,id",
                (rs,n) -> new Category(rs.getLong("id"),rs.getString("name"),rs.getInt("sort_order"),rs.getBoolean("enabled")));
    }
    public Optional<Category> find(long id) { return all(false).stream().filter(c -> c.id()==id).findFirst(); }
    public void create(CategoryInput input) {
        jdbc.update("INSERT INTO news_category(name,sort_order,enabled) VALUES (?,?,?)", input.name().trim(), input.sortOrder(), input.enabled());
    }
    public int update(long id, CategoryInput input) {
        return jdbc.update("UPDATE news_category SET name=?,sort_order=?,enabled=? WHERE id=?",
                input.name().trim(),input.sortOrder(),input.enabled(),id);
    }
}
