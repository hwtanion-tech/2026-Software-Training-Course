package cn.edu.campus.news.service;

import cn.edu.campus.news.common.BusinessException;
import cn.edu.campus.news.domain.Models.*;
import cn.edu.campus.news.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

@Service
public class ContentService {
    private final ContentRepository contents;
    private final CategoryRepository categories;
    private final CurrentUser current;
    public ContentService(ContentRepository contents,CategoryRepository categories,CurrentUser current) {
        this.contents=contents;this.categories=categories;this.current=current;
    }
    public Page<Content> list(Kind kind,String keyword,Long categoryId,String status,int page,int size,boolean publicView) {
        if (page<1 || page>1000000 || size<1 || size>50) throw new BusinessException(400,"页码必须为1至1000000，每页条数为1至50");
        String query=keyword == null ? "" : keyword.trim();
        if (query.length()>200) throw new BusinessException(400,"搜索关键词最多200字");
        if (categoryId != null && (categoryId<1 || kind!=Kind.NEWS)) throw new BusinessException(400,"分类参数不正确");
        Long owner=null;
        if (publicView) status="PUBLISHED";
        else {
            var user=current.required();
            if (!user.admin()) owner=user.id();
            if (status != null && !Set.of("DRAFT","PUBLISHED","WITHDRAWN").contains(status)) throw new BusinessException(400,"内容状态不正确");
        }
        return contents.list(kind,query,categoryId,status,owner,page,size,publicView);
    }
    public Content detail(Kind kind,long id,boolean publicView) {
        Content content=find(kind,id,false);
        if (publicView) {
            if (!"PUBLISHED".equals(content.status())) throw new BusinessException(404,"内容不存在或未发布");
        } else checkOwner(content);
        return content;
    }
    @Transactional
    public Content create(Kind kind,ContentInput input) {
        var user=current.required();
        long id=contents.create(kind,validate(kind,input),user.id());
        return find(kind,id,false);
    }
    @Transactional
    public Content update(Kind kind,long id,ContentInput input) {
        Content existing=find(kind,id,true);checkOwner(existing);
        var value=validate(kind,input);
        String next="PUBLISHED".equals(existing.status()) ? "DRAFT" : existing.status();
        contents.update(kind,id,value,next);
        return find(kind,id,false);
    }
    @Transactional
    public Content publish(Kind kind,long id) {
        Content existing=find(kind,id,true);checkOwner(existing);
        if ("PUBLISHED".equals(existing.status())) throw new BusinessException(409,"内容已经发布，请勿重复发布");
        validate(kind,new ContentInput(existing.title(),existing.summary(),existing.body(),existing.categoryId(),existing.authorName(),existing.publisherUnit()));
        contents.status(kind,id,"PUBLISHED",LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        return find(kind,id,false);
    }
    @Transactional
    public Content withdraw(Kind kind,long id) {
        Content existing=find(kind,id,true);checkOwner(existing);
        if (!"PUBLISHED".equals(existing.status())) throw new BusinessException(409,"只有已发布内容可以撤回");
        contents.status(kind,id,"WITHDRAWN",existing.publishedAt());
        return find(kind,id,false);
    }
    @Transactional
    public void delete(Kind kind,long id) {
        Content existing=find(kind,id,true);checkOwner(existing);
        if ("PUBLISHED".equals(existing.status())) throw new BusinessException(409,"请先撤回已发布内容，再删除");
        contents.delete(kind,id);
    }
    private Content find(Kind kind,long id,boolean lock) {
        return contents.find(kind,id,lock).orElseThrow(() -> new BusinessException(404,"内容不存在或未发布"));
    }
    private void checkOwner(Content content) {
        var user=current.required();
        if (!user.admin() && user.id()!=content.creatorId()) throw new BusinessException(403,"只能维护自己创建的内容");
    }
    private ContentInput validate(Kind kind,ContentInput input) {
        String title=trim(input.title()),body=trim(input.body());
        if (title.isBlank() || body.isBlank()) throw new BusinessException(400,"标题和正文不能为空");
        if (title.length()>200 || body.length()>50000) throw new BusinessException(400,"标题或正文超过长度限制");
        if (kind == Kind.NEWS) {
            if (input.categoryId()==null) throw new BusinessException(400,"请选择新闻分类");
            var category=categories.find(input.categoryId()).orElseThrow(() -> new BusinessException(400,"分类不存在"));
            if (!category.enabled()) throw new BusinessException(400,"该分类已停用，请选择其他分类");
            if (trim(input.authorName()).isBlank()) throw new BusinessException(400,"作者不能为空");
        } else if (trim(input.publisherUnit()).isBlank()) throw new BusinessException(400,"发布单位不能为空");
        return new ContentInput(title,trim(input.summary()),body,input.categoryId(),trim(input.authorName()),trim(input.publisherUnit()));
    }
    private String trim(String value) { return value==null ? "" : value.strip(); }
}
