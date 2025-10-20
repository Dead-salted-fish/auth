package com.lld.auth.aspect;


import com.lld.auth.annotation.RequireRole;
import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import com.lld.auth.user.service.SysUserService;
import com.lld.saltedfishutils.web.result.ReturnResult;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Aspect
@Component
public class RoleAspect {

    private final HttpServletRequest httpServletRequest;
    private SysUserService sysUserService;

    public RoleAspect(HttpServletRequest httpServletRequest, SysUserService sysUserService) {
        this.httpServletRequest = httpServletRequest;
        this.sysUserService = sysUserService;
    }

    @Around("@annotation(com.lld.auth.annotation.RequireRole) || @within(com.lld.auth.annotation.RequireRole)")
    public Object checkUserRole(ProceedingJoinPoint joinPoint) throws Throwable {

        // 获取方法上的注解
        RequireRole methodAnnotation = ((MethodSignature) joinPoint.getSignature())
                .getMethod()
                .getAnnotation(RequireRole.class);

        // 获取类上的注解
        RequireRole classAnnotation = joinPoint.getTarget().getClass()
                .getAnnotation(RequireRole.class);

        String[] requiredRoles = (methodAnnotation != null) ? methodAnnotation.roles()
                : (classAnnotation != null ? classAnnotation.roles() : new String[0]);

        if (requiredRoles == null || requiredRoles.length == 0) {
            System.out.println("RequireRole注解种的roles参数不能为空");
            return ReturnResult.error("无权限访问");
        }
        if (requiredRoles.length == 1 && requiredRoles[0].equals("noAuthentication")) {
            return joinPoint.proceed();

        }

        Boolean havePermissions = false;
        List<String> roles = getCurrentUserRole();
        for (String requiredRole : requiredRoles) {
            if (roles.contains(requiredRole)) {
                havePermissions = true;
                break;
            }
        }

        if (havePermissions) {
            return joinPoint.proceed();
        } else {
            return ReturnResult.error("无权限访问");
        }
    }

    private List<String> getCurrentUserRole() {
        //获取用户id,根据id查角色
        MyUsernamePasswordAuthenticationToken authentication = (MyUsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        Long userId = authentication.getUserId();
        List<String> rolesList = sysUserService.getRolesByUserId(userId);

        return rolesList;
    }
}
