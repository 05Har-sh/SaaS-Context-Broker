local key = KEYS[1]

local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local ttl = tonumber(ARGV[4])

local tokens = tonumber(redis.call("HGET", key, "tokens"))
local last_refill = tonumber(redis.call("HGET", key, "last_refill"))

if tokens == nil then
    tokens = capacity
end

if last_refill == nil then
    last_refill = now
end

local elapsed = now - last_refill

local refilled_tokens = elapsed * refill_rate

tokens = math.min(capacity, tokens + refilled_tokens)

local allowed = 0

if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end

redis.call("HSET", key, "tokens", tokens)
redis.call("HSET", key, "last_refill", now)
redis.call("EXPIRE", key, ttl)

return allowed