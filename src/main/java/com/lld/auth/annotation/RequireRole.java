package com.lld.auth.annotation;
import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD}) // 支持类和方法
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] roles() default {};
}
