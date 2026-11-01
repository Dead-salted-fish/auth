-- 参数说明：
-- KEYS[1]: 用户锁定状态key
-- KEYS[2]: 用户登录失败次数key
-- KEYS[3]: ip锁定状态key
-- KEYS[4]: ip登录失败次数key

-- ARGV[1]: 登录结果 ("true":成功, "false":失败) ← 注意是字符串
-- ARGV[2]: 最大失败次数
-- ARGV[3]: 发生锁定时，锁定时间 ("600"秒 = 10分钟)
-- ARGV[4]: 失败次数计数过期时间 ("180"秒 = 3分钟)
-- ARGV[5]: ip最大失败次数

local login_result = ARGV[1]  -- 字符串："true" 或 "false"
local account_max_failures = tonumber(ARGV[2])  -- 账户最大失败次数
local lock_duration = tonumber(ARGV[3]) -- 锁定时间
local count_expire = tonumber(ARGV[4]) -- 计数过期时间
local ip_max_failures = tonumber(ARGV[5])

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

-- 1) 账户失败次数
local fail_count = redis.call('incr', KEYS[2])
if fail_count == 1 then
    redis.call('expire', KEYS[2], count_expire)
end

-- 2) IP 失败次数：每次失败都累加
local ip_fail_count = redis.call('incr', KEYS[4])
if ip_fail_count == 1 then
    redis.call('expire', KEYS[4], count_expire)
end

-- 3) 先判 IP 是否达到失败阈值（衡量的是失败次数，而非锁定次数）
if ip_fail_count >= ip_max_failures then
    redis.call('set', KEYS[3], 'iplocked:' .. ip_max_failures .. ':' .. ip_fail_count, 'EX', lock_duration)
    return {
        'iplocked',
        lock_duration,
        0,
        'locked'
    }
end

if fail_count >= account_max_failures then
    local existing_lock_ttl = redis.call('ttl', KEYS[1])
    if existing_lock_ttl > 0 then
        return {
            'locked',
            existing_lock_ttl,
            0,
            'rejected'
        }
    else
        redis.call('set', KEYS[1], 'locked:' .. account_max_failures .. ':' .. fail_count, 'EX', lock_duration)
        redis.call('del', KEYS[2])
        return {
            'locked',
            lock_duration,
            0,
            'rejected'
        }
    end
end

-- 5) 普通失败，返回剩余尝试次数
local remaining = account_max_failures - fail_count
return {
    'failed',
    redis.call('ttl', KEYS[1]),
    remaining,
    'counted'
}





