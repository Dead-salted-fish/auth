package com.lld.auth.utils;

import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUserUtils {

    /**
     * 获取当前认证用户的ID
     * @return 当前用户ID
     * @throws AuthenticationCredentialsNotFoundException 如果用户未认证
     * @throws IllegalStateException 如果认证类型不匹配或用户ID为空
     */
    public static Long getCurrentUserId() {
        // 1. 获取认证对象
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

//        // 2. 检查是否已认证
//        if (authentication == null || !authentication.isAuthenticated()) {
//            throw new AuthenticationCredentialsNotFoundException("用户未认证");
//        }

        // 3. 检查认证类型
        if (!(authentication instanceof MyUsernamePasswordAuthenticationToken)) {
            throw new IllegalStateException(
                    "不支持的认证类型: " + authentication.getClass().getName() +
                            ", 期望类型: MyUsernamePasswordAuthenticationToken");
        }

        // 4. 获取用户ID
        Long userId = ((MyUsernamePasswordAuthenticationToken) authentication).getUserId();

        // 5. 检查用户ID是否有效
        if (userId == null) {
            throw new IllegalStateException("认证信息中用户ID为空");
        }

        return userId;
    }

    /**
     * 安全获取当前用户ID（可空版本）
     * @return 当前用户ID，未认证或类型不匹配时返回null
     */
    public static Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查当前用户是否具有指定ID
     * @param targetUserId 要检查的用户ID
     * @return 如果当前用户ID匹配则返回true
     */
    public static boolean isCurrentUser(Long targetUserId) {
        if (targetUserId == null) {
            return false;
        }
        Long currentUserId = getCurrentUserIdOrNull();
        return targetUserId.equals(currentUserId);
    }
}
