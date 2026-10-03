package com.lld.auth.security.userDetails;

import com.lld.auth.exception.UserAccountLockException;
import com.lld.auth.user.entity.SysRole;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.entity.UserRole;
import com.lld.auth.user.service.IUserRoleService;
import com.lld.auth.user.service.SysRoleService;
import com.lld.auth.user.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private IUserRoleService userRoleService ;
    @Autowired
    private SysRoleService sysRoleService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 获取用户信息
        SysUser user = sysUserService.getUserByUserName(username);
        if(user == null){
            throw new UsernameNotFoundException("没有此用户");
        }else if (user.getStatus().equals("0")){
            throw new UserAccountLockException("用户已被锁定");
        }

        // 获取用户角色
        List<UserRole> userRoles = userRoleService.getUserRolesByUserIds(Collections.singletonList(user.getId()));
       //  获取所有角色
        Map<Long, String> roleMap = sysRoleService.getAllRoles().stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRole));

        // 补充用户角色和权限 将角色转换为GrantedAuthority
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (UserRole userRole : userRoles) {
            Long roleId = userRole.getRoleId();
            authorities.add(new SimpleGrantedAuthority( "ROLE_" + roleMap.get(roleId)));
        }
        user.setUserAuthorities(authorities);

        return user;
    }
}
