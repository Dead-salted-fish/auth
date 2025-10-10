-- 用户心跳执行的脚本

-- KEYS[1]: 在线状态位图key
-- KEYS[2]: 活跃时间有序集合key
-- KEYS[3]: 今日在线位图key

-- ARGV[1]: 用户ID
-- ARGV[2]: 当前时间戳


-- 参数验证
if #KEYS < 3 then
    return {err = "KEY参数不足，需要43KEY"}
end

if #ARGV < 2 then
    return {err = "ARG参数不足，需要2个参数"}
end


local onlineBitmapKey = KEYS[1]
local activityZsetKey = KEYS[2]
local todayBitmapKey = KEYS[3]


local userId = tonumber(ARGV[1])
local currentTime = tonumber(ARGV[2])



if not currentTime or currentTime <= 0 then
    return {err = "时间戳必须为正整数"}
end

local userIdNum = tonumber(userId)
if not userIdNum then
    return {err = "用户ID必须为数字"}
end



-- 1. 设置用户在线状态（位图）
redis.call('SETBIT', onlineBitmapKey, userId, 1)

-- 2. 更新用户活跃时间（有序集合）
redis.call('ZADD', activityZsetKey, currentTime, userId)

-- 3. 设置用户今日登录状态（位图）
-- 注意：今日位图不需要设置过期时间，因为每天都会生成新的key
redis.call('SETBIT', todayBitmapKey, userId, 1)

-- 返回操作结果
return {
      "success", -- status
      true, -- online_set
      true, -- activity_updated
      true, -- today_online_logged
}