<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: META-INF/scripts/request_rate_limiter.lua -->
<!-- 正文引用：[[源码:request_rate_limiter.lua#L起-L止]] ；代码块头部：(src: request_rate_limiter.lua#L起-L止) -->

# request_rate_limiter.lua

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `META-INF/scripts/request_rate_limiter.lua` |
| 行数 | 27 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T08:52:06.173Z |

```java
    1 | redis.replicate_commands()
    2 | 
    3 | local tokens_key = KEYS[1]
    4 | local timestamp_key = KEYS[2]
    5 | 
    6 | local rate = tonumber(ARGV[1])
    7 | local capacity = tonumber(ARGV[2])
    8 | local now = tonumber(ARGV[3]) or redis.call('TIME')[1]
    9 | local requested = tonumber(ARGV[4])
   10 | 
   11 | local fill_time = capacity / rate
   12 | local ttl = math.floor(fill_time * 2)
   13 | 
   14 | local last_tokens = tonumber(redis.call("get", tokens_key)) or capacity
   15 | local last_refreshed = tonumber(redis.call("get", timestamp_key)) or 0
   16 | 
   17 | local delta = math.max(0, now-last_refreshed)
   18 | local filled_tokens = math.min(capacity, last_tokens+(delta*rate))
   19 | local allowed = filled_tokens >= requested
   20 | local new_tokens = allowed and filled_tokens - requested or filled_tokens
   21 | 
   22 | if ttl > 0 then
   23 |   redis.call("setex", tokens_key, ttl, new_tokens)
   24 |   redis.call("setex", timestamp_key, ttl, now)
   25 | end
   26 | 
   27 | return { allowed and 1 or 0, new_tokens }
```
