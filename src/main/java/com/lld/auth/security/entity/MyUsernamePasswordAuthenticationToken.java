package com.lld.auth.security.entity;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class MyUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private Long UserId;

    public MyUsernamePasswordAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public MyUsernamePasswordAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities,Long UserId) {
        super(principal, credentials,authorities);
        this.UserId = UserId;
    }

    public Long getUserId() {
        return UserId;
    }


}
