package com.lld.auth.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;

public class JWTUtils {

    // 密钥
    private static final String SECRET_KEY = "your-auth0-secret-key";

    // Issuer (iss) 和 Audience (aud)
    private static final String ISSUER = "your-issuer";
    private static final String AUDIENCE = "your-audience";

    private static Integer EXPIRATION_TIME_IN_MINUTES = 30;
    /**
     * 创建一个带有自定义声明的 JWT。
     *
     * @param userId 用户ID
     * @return 生成的 JWT 字符串
     */
    public static String generateToken(String userId) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + 1000 * 60 * EXPIRATION_TIME_IN_MINUTES); // 设置过期时间（例如30分钟）

        return JWT.create()
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .withSubject(userId)
                .withIssuedAt(now)
                .withExpiresAt(expirationDate)
                // 添加其他自定义声明...
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }

    /**
     * 验证并解析 JWT。
     *
     * @param jwtToken 待验证的 JWT 字符串
     * @return 如果 JWT 有效，则返回解码后的 JWT 对象；否则返回 null
     */
    public static DecodedJWT validateToken(String jwtToken) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .withAudience(AUDIENCE)
                    .build();

            return verifier.verify(jwtToken);
        } catch (Exception e) {
            // 处理验证失败的情况
            return null;
        }
    }
}

