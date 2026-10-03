-- 获取用户会话信息和版本号
-- KEYS[1] 用户会话信息 key
-- KEYS[2] 用户会话版本 key
-- ARGV[1] 用户会话信息字段
-- 返回用户会话信息和版本号


local session = redis.call('HGET', KEYS[1], ARGV[1])
local version = redis.call('GET', KEYS[2])
return {session, version}