package cn.edu.campus.news.controller;

import cn.edu.campus.news.common.ApiResponse;
import cn.edu.campus.news.domain.Models.*;
import cn.edu.campus.news.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ContentController {
    private final ContentService service;
    public ContentController(ContentService service) { this.service=service; }
    @GetMapping("/public/{kind:news|notices}")
    public ApiResponse<?> publicList(@PathVariable String kind,@RequestParam(defaultValue="") String keyword,
            @RequestParam(required=false) Long categoryId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size) {
        return ApiResponse.ok(service.list(Kind.parse(kind),keyword,categoryId,null,page,size,true));
    }
    @GetMapping("/public/{kind:news|notices}/{id}")
    public ApiResponse<?> publicDetail(@PathVariable String kind,@PathVariable long id) { return ApiResponse.ok(service.detail(Kind.parse(kind),id,true)); }
    @GetMapping("/manage/{kind:news|notices}")
    public ApiResponse<?> manageList(@PathVariable String kind,@RequestParam(defaultValue="") String keyword,
            @RequestParam(required=false) Long categoryId,@RequestParam(required=false) String status,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size) {
        return ApiResponse.ok(service.list(Kind.parse(kind),keyword,categoryId,status,page,size,false));
    }
    @GetMapping("/manage/{kind:news|notices}/{id}")
    public ApiResponse<?> manageDetail(@PathVariable String kind,@PathVariable long id) { return ApiResponse.ok(service.detail(Kind.parse(kind),id,false)); }
    @PostMapping("/manage/{kind:news|notices}")
    public ApiResponse<?> create(@PathVariable String kind,@Valid @RequestBody ContentInput input) { return ApiResponse.ok(service.create(Kind.parse(kind),input)); }
    @PutMapping("/manage/{kind:news|notices}/{id}")
    public ApiResponse<?> update(@PathVariable String kind,@PathVariable long id,@Valid @RequestBody ContentInput input) { return ApiResponse.ok(service.update(Kind.parse(kind),id,input)); }
    @PostMapping("/manage/{kind:news|notices}/{id}/publish")
    public ApiResponse<?> publish(@PathVariable String kind,@PathVariable long id) { return ApiResponse.ok(service.publish(Kind.parse(kind),id)); }
    @PostMapping("/manage/{kind:news|notices}/{id}/withdraw")
    public ApiResponse<?> withdraw(@PathVariable String kind,@PathVariable long id) { return ApiResponse.ok(service.withdraw(Kind.parse(kind),id)); }
    @DeleteMapping("/manage/{kind:news|notices}/{id}")
    public ApiResponse<?> delete(@PathVariable String kind,@PathVariable long id) { service.delete(Kind.parse(kind),id);return ApiResponse.ok(null); }
}
