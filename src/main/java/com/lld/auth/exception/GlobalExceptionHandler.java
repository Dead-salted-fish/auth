package com.lld.auth.exception;

import com.lld.saltedfishutils.utils.ReturnResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/**
 *  全局异常处理
 * **/
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ReturnResult HandleException(Exception e) {
        e.printStackTrace();
        System.out.println("全局异常捕获");
        return ReturnResult.error(e.getMessage());
    }


}
