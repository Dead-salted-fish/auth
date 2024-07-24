package com.lld.auth.security.userDetails;

import com.lld.auth.exception.UserAccountLockException;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserService sysUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserService.getUserByUserName(username);
        if(user == null){
            throw new UsernameNotFoundException("用户名或密码错误");
        }else if (user.getStatus().equals("0")){
            throw new UserAccountLockException("用户已被锁定");
        }

        return user;
    }
}
