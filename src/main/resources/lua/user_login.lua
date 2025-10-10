-- 用户登录执行的脚本
-- 参数说明：
-- KEYS[1]: token存储key
-- KEYS[2]: 在线状态位图key
-- KEYS[3]: 活跃时间有序集合key
-- KEYS[4]: 今日在线位图key

-- ARGV[1]: token值
-- ARGV[2]: token过期时间
-- ARGV[3]: 用户ID
-- ARGV[4]: 当前时间戳


-- 参数验证
if #KEYS < 4 then
    return {err = "KEY参数不足，需要4个KEY"}
end

if #ARGV < 4 then
    return {err = "ARG参数不足，需要4个参数"}
end

local tokenKey = KEYS[1]
local onlineBitmapKey = KEYS[2]
local activityZsetKey = KEYS[3]
local todayBitmapKey = KEYS[4]

local tokenValue = ARGV[1]
local tokenExpireSeconds = tonumber(ARGV[2])
local userId = tonumber(ARGV[3])
local currentTime = tonumber(ARGV[4])


-- 验证数字参数
if not tokenExpireSeconds or tokenExpireSeconds <= 0 then
    return {err = "过期时间必须为正整数"}
end

if not currentTime or currentTime <= 0 then
    return {err = "时间戳必须为正整数"}
end

local userIdNum = tonumber(userId)
if not userIdNum then
    return {err = "用户ID必须为数字"}
end

-- 1. 存储token（设置过期时间）
redis.call('SET', tokenKey, tokenValue, 'EX', tokenExpireSeconds)

-- 2. 设置用户在线状态（位图）
redis.call('SETBIT', onlineBitmapKey, userId, 1)

-- 3. 更新用户活跃时间（有序集合）
redis.call('ZADD', activityZsetKey, currentTime, userId)

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