package com.lld.auth.security.entity;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class AuthWhitelist implements InitializingBean {


    private static final AntPathMatcher ANT = new AntPathMatcher();

    @Value("${security.whitelist.auth-anonymous}")
    private List<String> anonymous;

    public List<String> getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(List<String> v) {
        this.anonymous = v;
    }

    public boolean isAnonymous(String rawPath) {
        return isAnonymous(rawPath, "");
    }

    public boolean isAnonymous(String rawPath, String contextPath) {
        return ant(anonymous, normalize(rawPath, contextPath));
    }

    public String[] anonymousArray() {
        return anonymous.toArray(new String[0]);
    }

    public List<String> anonymousList() {
        return List.copyOf(anonymous);
    }

    @Override
    public void afterPropertiesSet() {
        if (anonymous == null || anonymous.isEmpty()) {
            throw new IllegalStateException(
                    "security.whitelist.auth-anonymous 未配置，auth 拒绝以空匿名清单启动");
        }
        for (String p : anonymous) {
            if (!p.startsWith("/") || p.contains("*") || p.contains("?")) {
                throw new IllegalStateException(
                        "匿名路径必须是精确路径（禁止通配符，否则会放行管理接口）: " + p);
            }
        }
    }

    private static String normalize(String rawPath, String contextPath) {
        String p = UriUtils.decode(rawPath, StandardCharsets.UTF_8);
        if (contextPath != null && !contextPath.isEmpty() && p.startsWith(contextPath)) {
            p = p.substring(contextPath.length());
        }
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p.isEmpty() ? "/" : p;
    }

    private static boolean ant(List<String> patterns, String path) {
        for (String pattern : patterns) {
            if (ANT.match(pattern, path)) return true;
        }
        return false;
    }
}
