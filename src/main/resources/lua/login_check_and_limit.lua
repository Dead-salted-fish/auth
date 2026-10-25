-- 参数说明：
-- KEYS[1]: 用户锁定状态key
-- KEYS[2]: 用户登录失败次数key
-- KEYS[3]: ip锁定状态key
-- KEYS[4]: ip登录失败次数key

-- ARGV[1]: 登录结果 ("true":成功, "false":失败) ← 注意是字符串
-- ARGV[2]: 最大失败次数
-- ARGV[3]: 发生锁定时，锁定时间 ("600"秒 = 10分钟)
-- ARGV[4]: 失败次数计数过期时间 ("180"秒 = 3分钟)

local login_result = ARGV[1]  -- 字符串："true" 或 "false"
local account_max_failures = tonumber(ARGV[2])  -- 账户最大失败次数
local lock_duration = tonumber(ARGV[3]) -- 锁定时间
local count_expire = tonumber(ARGV[4]) -- 计数过期时间

-- 检查IP是否已被锁定
local ip_is_locked = redis.call('get', KEYS[3])
if ip_is_locked then
    return {
        'iplocked',  -- 状态
        redis.call('ttl', KEYS[3]), -- 剩余锁定时间
        0, -- 剩余尝试次数
        'rejected' -- 行为
    }
end

-- 检查用户是否已被锁定
local is_locked = redis.call('get', KEYS[1])
if is_locked then
    return {
        'locked',  -- 状态
        redis.call('ttl', KEYS[1]), -- 剩余锁定时间
         0, -- 剩余尝试次数
       'rejected' -- 行为
    }
end

-- 处理登录成功
if login_result == 'true' then  -- 比较字符串
    -- 清除失败计数
    redis.call('del', KEYS[1])
    redis.call('del', KEYS[2])
    return {
        'success',
         0,
        account_max_failures,
         'reset' -- 行为
    }
end

-- 处理登录失败（login_result == 'false'）
-- 增加失败次数
local fail_count = redis.call('incr', KEYS[2])

-- 如果是第一次失败，设置过期时间
if fail_count == 1 then
    redis.call('expire', KEYS[2], count_expire)
end

-- 检查是否达到锁定阈值
if fail_count >= account_max_failures then
    -- 先检查是否已经存在锁定
    local existing_lock_ttl = redis.call('ttl', KEYS[1])

    if existing_lock_ttl > 0 then
        -- 已经锁定，保持原有剩余时间
        return {
            'locked',
            existing_lock_ttl,
            0,
            'rejected'
        }
    else
        -- 首次锁定，设置新的锁定时间；值中记录 最大失败次数:当前失败次数
        redis.call('set', KEYS[1], 'locked:' .. max_failures .. ':' .. fail_count, 'EX', lock_duration)
        redis.call('del', KEYS[2])
        -- 检查IP是否已被锁定
        local current_ip_lock_ttl = redis.call('ttl', KEYS[3])
        if current_ip_lock_ttl <= 0 then
            local ip_fail_count = redis.call('incr', KEYS[4])
            if ip_fail_count == 1 then
                redis.call('expire', KEYS[4], count_expire)
            end
            if ip_fail_count >= account_max_failures then
                redis.call('set', KEYS[3], 'iplocked', 'EX', lock_duration)
                return {
                    'iplocked',
                    lock_duration,
                    0,
                    'locked'
                }
            end
        end

        return {
             'locked',
             lock_duration,
            0,
            'rejected'
        }
    end
end

-- 返回剩余尝试次数
local remaining = account_max_failures - fail_count
return {
     'failed',
    redis.call('ttl', KEYS[1]),
     remaining,
     'counted'
}