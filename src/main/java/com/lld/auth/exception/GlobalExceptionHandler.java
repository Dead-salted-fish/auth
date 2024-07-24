package com.lld.auth.exception;

import com.lld.saltedfishutils.utils.ReturnResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ReturnResult HandleException(Exception e) {
        System.out.println("====");
        System.out.println(e);
        return ReturnResult.error(e.getMessage());
    }


}
