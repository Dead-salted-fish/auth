-- KEYS[1]: online:users:bitmap (总在线用户位图)
-- KEYS[2]: user:status:online (在线状态位图)
-- KEYS[3]: user:status:away (离开状态位图)
-- KEYS[4]: user:status:busy (忙碌状态位图)
-- 返回值: 各种统计数量

local total_online = redis.call('BITCOUNT', KEYS[1])
local today_online_total = redis.call('BITCOUNT', KEYS[2])
-- local active_online = redis.call('BITCOUNT', KEYS[2])
-- local away_count = redis.call('BITCOUNT', KEYS[3])
-- local busy_count = redis.call('BITCOUNT', KEYS[4])

-- 计算真实在线数（排除 away 和 busy 状态）
local real_online = total_online

return {
    total_online,       -- 位图中标记的总数

    real_online ,        -- 真实在线数.目前和 total_online 相同，后面如有需要，区分离开，忙碌等状态
    today_online_total --今日在线过人数的总数
}