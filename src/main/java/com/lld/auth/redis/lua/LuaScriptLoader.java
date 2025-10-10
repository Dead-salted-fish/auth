package com.lld.auth.redis.lua;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 脚本加载器
 */

@Component
class LuaScriptLoader {

    private final StringRedisTemplate stringRedisTemplate;
    private final Map<String, String> scriptShaMap = new ConcurrentHashMap<>();


    public LuaScriptLoader(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    // 加载脚本
    public <T> RedisScript<T> loadScript(String scriptName,  Class<T> resultType,String fallbackSha) {
        return new RedisScript<T>() {
            @Override
            public String getSha1() {
                return scriptShaMap.computeIfAbsent(scriptName, k -> {
                    try {
                        String scriptContent = loadScriptContent(k);
                        return stringRedisTemplate.execute(
                                (RedisCallback<String>) connection ->
                                        connection.scriptLoad(scriptContent.getBytes())
                        ).toString();
                    } catch (Exception e) {

                        return fallbackSha;
                    }
                });
            }

            @Override
            public Class<T> getResultType() {
                return resultType;
            }

            @Override
            public String getScriptAsString() {
                return loadScriptContent(scriptName);
            }
        };
    }

    // 加载脚本，支持泛型返回类型（无fallbackSha版本）
    public <T> RedisScript<T> loadScript(String scriptName, Class<T> resultType) {
        return loadScript(scriptName, resultType, "默认fallbackSha");
    }


    /**
     * 从classpath加载脚本内容
     * @param scriptName 脚本文件名
     * @return 脚本内容
     */
    private String loadScriptContent(String scriptName) {
        try {
            Resource resource = new ClassPathResource("lua/" + scriptName);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Lua script: " + scriptName, e);
        }
    }
}
