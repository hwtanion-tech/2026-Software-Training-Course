package cn.edu.campus.news.controller;

import cn.edu.campus.news.common.*;
import cn.edu.campus.news.domain.Models.CategoryInput;
import cn.edu.campus.news.repository.CategoryRepository;
import cn.edu.campus.news.service.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CategoryController {
    private final CategoryRepository categories;
    private final CurrentUser current;
    public CategoryController(CategoryRepository categories,CurrentUser current) { this.categories=categories;this.current=current; }
    @GetMapping("/public/categories") public ApiResponse<?> publicCategories() { return ApiResponse.ok(categories.all(false)); }
    @GetMapping("/manage/categories") public ApiResponse<?> manageCategories() { current.required();return ApiResponse.ok(categories.all(false)); }
    @PostMapping("/admin/categories") public ApiResponse<?> create(@Valid @RequestBody CategoryInput input) {
        current.admin();categories.create(input);return ApiResponse.ok(categories.all(false));
    }
    @PutMapping("/admin/categories/{id}") public ApiResponse<?> update(@PathVariable long id,@Valid @RequestBody CategoryInput input) {
        current.admin();
        if (categories.update(id,input)==0) throw new BusinessException(404,"分类不存在");
        return ApiResponse.ok(categories.all(false));
    }
}
