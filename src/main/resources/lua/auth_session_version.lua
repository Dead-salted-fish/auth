
-- KEYS[1] 用户版本号 key

local version = redis.call('GET', KEYS[1])
return {version}