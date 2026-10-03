-- 用户登录执行的脚本
-- 参数说明：
-- KEYS[1]: 用户token
-- KEYS[2]: 用户版本号
-- KEYS[3]: 在线用户bitmap 的key
-- KEYS[4]: 活跃用户 bitmap 的key
-- KEYS[5]: 今日登录过的用户  bitmap 的key

-- ARGV[1]: jwt token 的 jit
-- ARGV[2]: 用户版本号
-- ARGV[3]: 用户token的值
-- ARGV[4]: 用户token的过期时间



-- 参数验证
if #KEYS < 5 then
    return {err = "KEY参数不足，需要4个KEY"}
end

if #ARGV < 5 then
    return {err = "ARG参数不足，需要4个参数"}
end

local userTokenKey = KEYS[1]
local userVersionKey = KEYS[2]
local onlineBitmapKey = KEYS[3]
local activityZsetKey = KEYS[4]
local todayBitmapKey = KEYS[5]

local jit = ARGV[1]
local userVersionValue = ARGV[2]
local userTokenValue = ARGV[3]
local tokenExpireSeconds = tonumber(ARGV[4])
local userId = tonumber(ARGV[5])

-- 验证数字参数
if not tokenExpireSeconds or tokenExpireSeconds <= 0 then
    return {err = "过期时间必须为正整数"}
end

--if not currentTime or currentTime <= 0 then
--    return {err = "时间戳必须为正整数"}
--end

--local userIdNum = tonumber(userId)
--if not userIdNum then
--    return {err = "用户ID必须为数字"}
--end

-- 1. 存储token（设置过期时间）
redis.call('HSET', userTokenKey, jit, userTokenValue)
redis.call('EXPIRE', userTokenKey, tokenExpireSeconds)

-- 1.1 存储用户版本号
redis.call('SET', userVersionKey, userVersionValue)


-- 2. 设置用户在线状态（位图）
-- 2026-07-12 22:49:30  不记录用户在线状态了
-- redis.call('SETBIT', onlineBitmapKey, userId, 1)

-- 3. 更新用户活跃时间（有序集合）
-- 2026-07-12 22:49:30  不记录用户活跃时间了
-- redis.call('ZADD', activityZsetKey, currentTime, userId)

-- 4. 设置用户今日登录状态（位图）
-- 注意：今日位图不需要设置过期时间，因为每天都会生成新的key
redis.call('SETBIT', todayBitmapKey, userId, 1)

-- 返回操作结果
return {
      "success", -- status
      true, -- token_stored
      true, -- online_set
      true, -- activity_updated
      true, -- today_online_logged
      tokenExpireSeconds   -- token_expire
}