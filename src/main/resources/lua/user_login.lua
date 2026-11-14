-- 用户登录执行的脚本
-- 参数说明：
-- KEYS[1]: 用户版本号
-- KEYS[2]: 当日活跃过用户bitmap
-- KEYS[3]: 用户refresh token 的 key


-- ARGV[1]: 用户版本号
-- ARGV[2]: 用户token的过期时间
-- ARGV[3]: 用户refreshtoken的jit值
-- ARGV[4]: 用户用户refresh token的 token值
-- ARGV[5]: 用户id



-- 参数验证
if #KEYS < 3 then
    return { err = "KEY参数不足，需要4个KEY" }
end

if #ARGV < 5 then
    return { err = "ARG参数不足，需要4个参数" }
end

local userVersionKey = KEYS[1]
local dailyActiveBitmapKey = KEYS[2]
local refreshTokenKey = KEYS[3]

local userVersionValue = ARGV[1]
local tokenExpireSeconds = tonumber(ARGV[2])
local refreshjit = ARGV[3]
local refreshTokenValue = ARGV[4]
local userId = ARGV[5]

-- 验证数字参数
if not tokenExpireSeconds or tokenExpireSeconds <= 0 then
    return { err = "过期时间必须为正整数" }
end

--if not currentTime or currentTime <= 0 then
--    return {err = "时间戳必须为正整数"}
--end

--local userIdNum = tonumber(userId)
--if not userIdNum then
--    return {err = "用户ID必须为数字"}
--end

-- 1. 存储token（设置过期时间）
-- 暂时不存 accestoken 只存 refresh token
-- redis.call('HSET', userTokenKey, jit, userTokenValue)
-- redis.call('EXPIRE', userTokenKey, tokenExpireSeconds)

-- refresh token 存储
redis.call('HSET', refreshTokenKey, refreshjit, refreshTokenValue)
redis.call('EXPIRE', refreshTokenKey, tokenExpireSeconds)

-- 1.1 存储用户版本号
redis.call('SET', userVersionKey, userVersionValue)
redis.call('EXPIRE', userVersionKey, tokenExpireSeconds)

-- 2. 设置用户在线状态（位图）
-- 2026-07-12 22:49:30  不记录用户在线状态了
-- redis.call('SETBIT', onlineBitmapKey, userId, 1)

-- 3. 更新用户活跃时间（有序集合）
-- 2026-07-12 22:49:30  不记录用户活跃时间了
-- redis.call('ZADD', activityZsetKey, currentTime, userId)

-- 4. 设置用户今日登录状态（位图）
-- 注意：今日位图不需要设置过期时间，因为每天都会生成新的key
redis.call('SETBIT', dailyActiveBitmapKey, userId, 1)

-- 返回操作结果
return {
    "success", -- status
    true, -- token_stored
    true, -- online_set
    true, -- activity_updated
    true, -- today_online_logged
    tokenExpireSeconds   -- token_expire
}