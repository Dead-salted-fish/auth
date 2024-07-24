package com.lld.auth.exception;

import org.springframework.security.core.AuthenticationException;

public class UserAccountLockException extends AuthenticationException {
    public UserAccountLockException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public UserAccountLockException(String msg) {
        super(msg);
    }
}
