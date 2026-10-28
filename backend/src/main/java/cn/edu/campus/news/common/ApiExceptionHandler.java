package cn.edu.campus.news.common;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<?> business(BusinessException ex) {
        return ResponseEntity.status(ex.status()).body(ApiResponse.error("BUSINESS_ERROR", ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage()).findFirst().orElse("请检查输入内容");
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR", message));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed(Exception ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "参数格式不正确"));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).body(ApiResponse.error("CONFLICT", "名称重复或关联数据约束冲突"));
    }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<?> database(DataAccessException ex) {
        return ResponseEntity.status(503).body(ApiResponse.error("DATABASE_UNAVAILABLE", "数据库操作失败，请检查数据库连接与表结构"));
    }
}
