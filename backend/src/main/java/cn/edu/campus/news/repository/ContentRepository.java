package cn.edu.campus.news.repository;

import cn.edu.campus.news.domain.Models.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class ContentRepository {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    public ContentRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; this.named=new NamedParameterJdbcTemplate(jdbc); }

    private String select(Kind kind) {
        String fields = kind == Kind.NEWS
                ? "c.category_id,k.name AS category_name,c.summary,c.author_name,NULL AS publisher_unit"
                : "NULL AS category_id,NULL AS category_name,'' AS summary,NULL AS author_name,c.publisher_unit";
        return "SELECT c.id,c.title,c.body,c.creator_id,c.status,c.published_at,c.created_at,c.updated_at,"
                + "u.display_name AS creator_name," + fields + " FROM " + kind.table
                + " c JOIN sys_user u ON u.id=c.creator_id"
                + (kind == Kind.NEWS ? " JOIN news_category k ON k.id=c.category_id" : "");
    }
    private RowMapper<Content> mapper(Kind kind) {
        return (rs,n) -> new Content(rs.getLong("id"), kind == Kind.NEWS ? "news" : "notices",
                rs.getObject("category_id", Long.class), rs.getString("category_name"), rs.getString("title"),
                rs.getString("summary"),rs.getString("body"),rs.getString("author_name"),rs.getString("publisher_unit"),
                rs.getLong("creator_id"),rs.getString("creator_name"),rs.getString("status"),
                time(rs,"published_at"), time(rs,"created_at"), time(rs,"updated_at"));
    }
    private LocalDateTime time(ResultSet rs,String field) throws SQLException {
        var value=rs.getTimestamp(field); return value == null ? null : value.toLocalDateTime();
    }
    public Optional<Content> find(Kind kind,long id,boolean lock) {
        // 先锁内容主表，不锁分类、用户联表，避免无关内容间的锁竞争。
        if (lock) jdbc.queryForList("SELECT id FROM " + kind.table + " WHERE id=? FOR UPDATE", id);
        return jdbc.query(select(kind)+" WHERE c.id=?",mapper(kind),id).stream().findFirst();
    }
    public Page<Content> list(Kind kind,String keyword,Long categoryId,String status,Long creatorId,int page,int size,boolean publicView) {
        StringBuilder where=new StringBuilder(" WHERE 1=1");
        Map<String,Object> params=new HashMap<>();
        if (!keyword.isEmpty()) {
            where.append(" AND c.title LIKE :keyword ESCAPE '!'");
            params.put("keyword", "%"+keyword.replace("!","!!").replace("%","!%").replace("_","!_")+"%");
        }
        if (categoryId != null && kind == Kind.NEWS) { where.append(" AND c.category_id=:category");params.put("category",categoryId); }
        if (status != null) { where.append(" AND c.status=:status");params.put("status",status); }
        if (creatorId != null) { where.append(" AND c.creator_id=:creator");params.put("creator",creatorId); }
        long total=named.queryForObject("SELECT COUNT(*) FROM "+kind.table+" c"+where,params,Long.class);
        params.put("limit",size);params.put("offset",(page-1)*size);
        String order=publicView ? "c.published_at DESC,c.id DESC" : "c.updated_at DESC,c.id DESC";
        var items=named.query(select(kind)+where+" ORDER BY "+order+" LIMIT :limit OFFSET :offset",params,mapper(kind));
        return new Page<>(items,total,page,size,(total+size-1)/size);
    }
    public long create(Kind kind,ContentInput input,long creatorId) {
        String sql=kind == Kind.NEWS
                ? "INSERT INTO news(title,summary,body,category_id,author_name,creator_id,status) VALUES (?,?,?,?,?,?,'DRAFT')"
                : "INSERT INTO notice(title,body,publisher_unit,creator_id,status) VALUES (?,?,?,?,'DRAFT')";
        var key=new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement=connection.prepareStatement(sql,new String[]{"id"});
            statement.setString(1,input.title());
            if (kind == Kind.NEWS) {
                statement.setString(2,input.summary());statement.setString(3,input.body());
                statement.setLong(4,input.categoryId());statement.setString(5,input.authorName());statement.setLong(6,creatorId);
            } else {
                statement.setString(2,input.body());statement.setString(3,input.publisherUnit());statement.setLong(4,creatorId);
            }
            return statement;
        },key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
    public void update(Kind kind,long id,ContentInput input,String status) {
        if (kind == Kind.NEWS) jdbc.update("UPDATE news SET title=?,summary=?,body=?,category_id=?,author_name=?,status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                input.title(),input.summary(),input.body(),input.categoryId(),input.authorName(),status,id);
        else jdbc.update("UPDATE notice SET title=?,body=?,publisher_unit=?,status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
                input.title(),input.body(),input.publisherUnit(),status,id);
    }
    public void status(Kind kind,long id,String status,LocalDateTime publishedAt) {
        jdbc.update("UPDATE "+kind.table+" SET status=?,published_at=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",status,publishedAt,id);
    }
    public void delete(Kind kind,long id) { jdbc.update("DELETE FROM "+kind.table+" WHERE id=?",id); }
}
