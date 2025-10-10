-- KEYS[1]: online:users:bitmap (用户实时在线状态位图)
-- KEYS[2]: user:activity:zset (用户活动时间有序集合)
-- KEYS[3]: user:status:online (在线用户状态位图)
-- KEYS[4]: user:status:away (离开用户状态位图)
-- KEYS[5]: user:status:busy (忙碌用户状态位图)
-- ARGV[1]: 当前时间戳
-- ARGV[2]: 超时时间（毫秒）
-- ARGV[3]: 最大处理数量（防止脚本执行时间过长）


local current_time = tonumber(ARGV[1])
if current_time == nil then
    error("Cannot convert ARGV[1] to number: " .. ARGV[1])
end
local timeout_ms = tonumber(ARGV[2])
local max_process = tonumber(ARGV[3])
local timeout_threshold = current_time - timeout_ms

-- 获取超时用户（限制数量防止阻塞）
local timeout_users = redis.call('ZRANGEBYSCORE', KEYS[2], 0, timeout_threshold, 'LIMIT', 0, max_process)

if #timeout_users == 0 then
    return {0, {}} -- 没有超时用户
end

local removed_count = 0
local detailed_stats = {}

-- 批量处理超时用户
for i, user_id in ipairs(timeout_users) do
    -- 清除在线状态
    redis.call('SETBIT', KEYS[1], user_id, 0)

--     -- 检查用户原有状态并统计
--     local was_online = redis.call('GETBIT', KEYS[3], user_id)
--     local was_away = redis.call('GETBIT', KEYS[4], user_id)
--     local was_busy = redis.call('GETBIT', KEYS[5], user_id)
--
--     if was_online == 1 then
--         online_count = online_count + 1
--         redis.call('SETBIT', KEYS[3], user_id, 0)
--     elseif was_away == 1 then
--         away_count = away_count + 1
--         redis.call('SETBIT', KEYS[4], user_id, 0)
--     elseif was_busy == 1 then
--         busy_count = busy_count + 1
--         redis.call('SETBIT', KEYS[5], user_id, 0)
--     end

--     记录详细统计
    table.insert(detailed_stats, {
        user_id
    })

    removed_count = removed_count + 1
end

-- 从有序集合中移除已处理的超时用户
if #timeout_users > 0 then
    redis.call('ZREMRANGEBYSCORE', KEYS[2], 0, timeout_threshold)
end

-- 返回统计结果
return {
    removed_count,          -- 总共清理的用户数
    detailed_stats          -- 详细统计信息
}