package com.lld.auth.exception;

import org.springframework.security.core.AuthenticationException;
/**
 *   用户账户锁定异常
 * **/
public class UserAccountLockException extends AuthenticationException {
    public UserAccountLockException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public UserAccountLockException(String msg) {
        super(msg);
    }
}
