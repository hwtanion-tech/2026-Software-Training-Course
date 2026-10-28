package cn.edu.campus.news.domain;

import cn.edu.campus.news.common.BusinessException;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

/** 请求与响应模型；密码哈希只存在于内部用户记录中。 */
public final class Models {
    private Models() { }
    public enum Kind {
        NEWS("news"), NOTICE("notice");
        public final String table;
        Kind(String table) { this.table = table; }
        public static Kind parse(String value) {
            return switch (value) {
                case "news" -> NEWS;
                case "notices" -> NOTICE;
                default -> throw new BusinessException(404, "内容类型不存在");
            };
        }
    }
    public record UserAccount(long id, String username, String passwordHash, String displayName,
                              String role, boolean enabled) {
        public UserView view() { return new UserView(id, username, displayName, role); }
    }
    public record UserView(long id, String username, String displayName, String role) {
        public boolean admin() { return "ADMIN".equals(role); }
    }
    public record Category(long id, String name, int sortOrder, boolean enabled) { }
    public record CategoryInput(@NotBlank(message="分类名称不能为空") @Size(max=50,message="分类名称最多50字") String name,
                                @Min(value=0,message="排序不能小于0") @Max(value=9999,message="排序不能超过9999") int sortOrder,
                                boolean enabled) { }
    public record Content(long id, String type, Long categoryId, String categoryName, String title,
                          String summary, String body, String authorName, String publisherUnit,
                          long creatorId, String creatorName, String status,
                          LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt) { }
    public record ContentInput(
            @NotBlank(message="标题不能为空") @Size(max=200,message="标题最多200字") String title,
            @Size(max=500,message="摘要最多500字") String summary,
            @NotBlank(message="正文不能为空") @Size(max=50000,message="正文最多50000字") String body,
            Long categoryId,
            @Size(max=50,message="作者最多50字") String authorName,
            @Size(max=100,message="发布单位最多100字") String publisherUnit) { }
    public record Page<T>(List<T> items, long total, int page, int size, long pages) { }
}
