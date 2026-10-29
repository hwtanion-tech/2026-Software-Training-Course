package cn.edu.campus.news.controller;

import cn.edu.campus.news.common.ApiResponse;
import cn.edu.campus.news.domain.Models.*;
import cn.edu.campus.news.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/accounts")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service=service; }
    @GetMapping public ApiResponse<?> list(@RequestParam(defaultValue="") String keyword,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size) {
        return ApiResponse.ok(service.list(keyword,page,size));
    }
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody AccountCreate input) {
        return ApiResponse.ok(service.create(input));
    }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable long id,@Valid @RequestBody AccountUpdate input) {
        return ApiResponse.ok(service.update(id,input));
    }
}
