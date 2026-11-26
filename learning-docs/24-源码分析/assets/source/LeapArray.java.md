<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.csp:sentinel-core:1.8.10 | path: com/alibaba/csp/sentinel/slots/statistic/base/LeapArray.java -->
<!-- 正文引用：[[源码:LeapArray.java#L起-L止]] ；代码块头部：(src: LeapArray.java#L起-L止) -->

# LeapArray.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.csp:sentinel-core:1.8.10` |
| 路径 | `com/alibaba/csp/sentinel/slots/statistic/base/LeapArray.java` |
| 行数 | 420 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/csp/sentinel-core/1.8.10/sentinel-core-1.8.10-sources.jar |
| 抓取时间 | 2026-11-25T15:33:29.848Z |

```java
    1 | /*
    2 |  * Copyright 1999-2018 Alibaba Group Holding Ltd.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      http://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | package com.alibaba.csp.sentinel.slots.statistic.base;
   17 | 
   18 | import java.util.ArrayList;
   19 | import java.util.List;
   20 | import java.util.concurrent.atomic.AtomicReferenceArray;
   21 | import java.util.concurrent.locks.ReentrantLock;
   22 | 
   23 | import com.alibaba.csp.sentinel.util.AssertUtil;
   24 | import com.alibaba.csp.sentinel.util.TimeUtil;
   25 | 
   26 | /**
   27 |  * <p>
   28 |  * Basic data structure for statistic metrics in Sentinel.
   29 |  * </p>
   30 |  * <p>
   31 |  * Leap array use sliding window algorithm to count data. Each bucket cover {@code windowLengthInMs} time span,
   32 |  * and the total time span is {@link #intervalInMs}, so the total bucket amount is:
   33 |  * {@code sampleCount = intervalInMs / windowLengthInMs}.
   34 |  * </p>
   35 |  *
   36 |  * @param <T> type of statistic data
   37 |  * @author jialiang.linjl
   38 |  * @author Eric Zhao
   39 |  * @author Carpenter Lee
   40 |  */
   41 | public abstract class LeapArray<T> {
   42 | 
   43 |     protected int windowLengthInMs;
   44 |     protected int sampleCount;
   45 |     protected int intervalInMs;
   46 |     private double intervalInSecond;
   47 | 
   48 |     protected final AtomicReferenceArray<WindowWrap<T>> array;
   49 | 
   50 |     /**
   51 |      * The conditional (predicate) update lock is used only when current bucket is deprecated.
   52 |      */
   53 |     private final ReentrantLock updateLock = new ReentrantLock();
   54 | 
   55 |     /**
   56 |      * The total bucket count is: {@code sampleCount = intervalInMs / windowLengthInMs}.
   57 |      *
   58 |      * @param sampleCount  bucket count of the sliding window
   59 |      * @param intervalInMs the total time interval of this {@link LeapArray} in milliseconds
   60 |      */
   61 |     public LeapArray(int sampleCount, int intervalInMs) {
   62 |         AssertUtil.isTrue(sampleCount > 0, "bucket count is invalid: " + sampleCount);
   63 |         AssertUtil.isTrue(intervalInMs > 0, "total time interval of the sliding window should be positive");
   64 |         AssertUtil.isTrue(intervalInMs % sampleCount == 0, "time span needs to be evenly divided");
   65 | 
   66 |         this.windowLengthInMs = intervalInMs / sampleCount;
   67 |         this.intervalInMs = intervalInMs;
   68 |         this.intervalInSecond = intervalInMs / 1000.0;
   69 |         this.sampleCount = sampleCount;
   70 | 
   71 |         this.array = new AtomicReferenceArray<>(sampleCount);
   72 |     }
   73 | 
   74 |     /**
   75 |      * Get the bucket at current timestamp.
   76 |      *
   77 |      * @return the bucket at current timestamp
   78 |      */
   79 |     public WindowWrap<T> currentWindow() {
   80 |         return currentWindow(TimeUtil.currentTimeMillis());
   81 |     }
   82 | 
   83 |     /**
   84 |      * Create a new statistic value for bucket.
   85 |      *
   86 |      * @param timeMillis current time in milliseconds
   87 |      * @return the new empty bucket
   88 |      */
   89 |     public abstract T newEmptyBucket(long timeMillis);
   90 | 
   91 |     /**
   92 |      * Reset given bucket to provided start time and reset the value.
   93 |      *
   94 |      * @param startTime  the start time of the bucket in milliseconds
   95 |      * @param windowWrap current bucket
   96 |      * @return new clean bucket at given start time
   97 |      */
   98 |     protected abstract WindowWrap<T> resetWindowTo(WindowWrap<T> windowWrap, long startTime);
   99 | 
  100 |     private int calculateTimeIdx(/*@Valid*/ long timeMillis) {
  101 |         long timeId = timeMillis / windowLengthInMs;
  102 |         // Calculate current index so we can map the timestamp to the leap array.
  103 |         return (int)(timeId % array.length());
  104 |     }
  105 | 
  106 |     protected long calculateWindowStart(/*@Valid*/ long timeMillis) {
  107 |         return timeMillis - timeMillis % windowLengthInMs;
  108 |     }
  109 | 
  110 |     /**
  111 |      * Get bucket item at provided timestamp.
  112 |      *
  113 |      * @param timeMillis a valid timestamp in milliseconds
  114 |      * @return current bucket item at provided timestamp if the time is valid; null if time is invalid
  115 |      */
  116 |     public WindowWrap<T> currentWindow(long timeMillis) {
  117 |         if (timeMillis < 0) {
  118 |             return null;
  119 |         }
  120 | 
  121 |         int idx = calculateTimeIdx(timeMillis);
  122 |         // Calculate current bucket start time.
  123 |         long windowStart = calculateWindowStart(timeMillis);
  124 | 
  125 |         /*
  126 |          * Get bucket item at given time from the array.
  127 |          *
  128 |          * (1) Bucket is absent, then just create a new bucket and CAS update to circular array.
  129 |          * (2) Bucket is up-to-date, then just return the bucket.
  130 |          * (3) Bucket is deprecated, then reset current bucket.
  131 |          */
  132 |         while (true) {
  133 |             WindowWrap<T> old = array.get(idx);
  134 |             if (old == null) {
  135 |                 /*
  136 |                  *     B0       B1      B2    NULL      B4
  137 |                  * ||_______|_______|_______|_______|_______||___
  138 |                  * 200     400     600     800     1000    1200  timestamp
  139 |                  *                             ^
  140 |                  *                          time=888
  141 |                  *            bucket is empty, so create new and update
  142 |                  *
  143 |                  * If the old bucket is absent, then we create a new bucket at {@code windowStart},
  144 |                  * then try to update circular array via a CAS operation. Only one thread can
  145 |                  * succeed to update, while other threads yield its time slice.
  146 |                  */
  147 |                 WindowWrap<T> window = new WindowWrap<T>(windowLengthInMs, windowStart, newEmptyBucket(timeMillis));
  148 |                 if (array.compareAndSet(idx, null, window)) {
  149 |                     // Successfully updated, return the created bucket.
  150 |                     return window;
  151 |                 } else {
  152 |                     // Contention failed, the thread will yield its time slice to wait for bucket available.
  153 |                     Thread.yield();
  154 |                 }
  155 |             } else if (windowStart == old.windowStart()) {
  156 |                 /*
  157 |                  *     B0       B1      B2     B3      B4
  158 |                  * ||_______|_______|_______|_______|_______||___
  159 |                  * 200     400     600     800     1000    1200  timestamp
  160 |                  *                             ^
  161 |                  *                          time=888
  162 |                  *            startTime of Bucket 3: 800, so it's up-to-date
  163 |                  *
  164 |                  * If current {@code windowStart} is equal to the start timestamp of old bucket,
  165 |                  * that means the time is within the bucket, so directly return the bucket.
  166 |                  */
  167 |                 return old;
  168 |             } else if (windowStart > old.windowStart()) {
  169 |                 /*
  170 |                  *   (old)
  171 |                  *             B0       B1      B2    NULL      B4
  172 |                  * |_______||_______|_______|_______|_______|_______||___
  173 |                  * ...    1200     1400    1600    1800    2000    2200  timestamp
  174 |                  *                              ^
  175 |                  *                           time=1676
  176 |                  *          startTime of Bucket 2: 400, deprecated, should be reset
  177 |                  *
  178 |                  * If the start timestamp of old bucket is behind provided time, that means
  179 |                  * the bucket is deprecated. We have to reset the bucket to current {@code windowStart}.
  180 |                  * Note that the reset and clean-up operations are hard to be atomic,
  181 |                  * so we need a update lock to guarantee the correctness of bucket update.
  182 |                  *
  183 |                  * The update lock is conditional (tiny scope) and will take effect only when
  184 |                  * bucket is deprecated, so in most cases it won't lead to performance loss.
  185 |                  */
  186 |                 if (updateLock.tryLock()) {
  187 |                     try {
  188 |                         // Successfully get the update lock, now we reset the bucket.
  189 |                         return resetWindowTo(old, windowStart);
  190 |                     } finally {
  191 |                         updateLock.unlock();
  192 |                     }
  193 |                 } else {
  194 |                     // Contention failed, the thread will yield its time slice to wait for bucket available.
  195 |                     Thread.yield();
  196 |                 }
  197 |             } else if (windowStart < old.windowStart()) {
  198 |                 // Should not go through here, as the provided time is already behind.
  199 |                 return new WindowWrap<T>(windowLengthInMs, windowStart, newEmptyBucket(timeMillis));
  200 |             }
  201 |         }
  202 |     }
  203 | 
  204 |     /**
  205 |      * Get the previous bucket item before provided timestamp.
  206 |      *
  207 |      * @param timeMillis a valid timestamp in milliseconds
  208 |      * @return the previous bucket item before provided timestamp
  209 |      */
  210 |     public WindowWrap<T> getPreviousWindow(long timeMillis) {
  211 |         if (timeMillis < 0) {
  212 |             return null;
  213 |         }
  214 |         int idx = calculateTimeIdx(timeMillis - windowLengthInMs);
  215 |         timeMillis = timeMillis - windowLengthInMs;
  216 |         WindowWrap<T> wrap = array.get(idx);
  217 | 
  218 |         if (wrap == null || isWindowDeprecated(wrap)) {
  219 |             return null;
  220 |         }
  221 | 
  222 |         if (wrap.windowStart() + windowLengthInMs < (timeMillis)) {
  223 |             return null;
  224 |         }
  225 | 
  226 |         return wrap;
  227 |     }
  228 | 
  229 |     /**
  230 |      * Get the previous bucket item for current timestamp.
  231 |      *
  232 |      * @return the previous bucket item for current timestamp
  233 |      */
  234 |     public WindowWrap<T> getPreviousWindow() {
  235 |         return getPreviousWindow(TimeUtil.currentTimeMillis());
  236 |     }
  237 | 
  238 |     /**
  239 |      * Get statistic value from bucket for provided timestamp.
  240 |      *
  241 |      * @param timeMillis a valid timestamp in milliseconds
  242 |      * @return the statistic value if bucket for provided timestamp is up-to-date; otherwise null
  243 |      */
  244 |     public T getWindowValue(long timeMillis) {
  245 |         if (timeMillis < 0) {
  246 |             return null;
  247 |         }
  248 |         int idx = calculateTimeIdx(timeMillis);
  249 | 
  250 |         WindowWrap<T> bucket = array.get(idx);
  251 | 
  252 |         if (bucket == null || !bucket.isTimeInWindow(timeMillis)) {
  253 |             return null;
  254 |         }
  255 | 
  256 |         return bucket.value();
  257 |     }
  258 | 
  259 |     /**
  260 |      * Check if a bucket is deprecated, which means that the bucket
  261 |      * has been behind for at least an entire window time span.
  262 |      *
  263 |      * @param windowWrap a non-null bucket
  264 |      * @return true if the bucket is deprecated; otherwise false
  265 |      */
  266 |     public boolean isWindowDeprecated(/*@NonNull*/ WindowWrap<T> windowWrap) {
  267 |         return isWindowDeprecated(TimeUtil.currentTimeMillis(), windowWrap);
  268 |     }
  269 | 
  270 |     public boolean isWindowDeprecated(long time, WindowWrap<T> windowWrap) {
  271 |         return time - windowWrap.windowStart() > intervalInMs;
  272 |     }
  273 | 
  274 |     /**
  275 |      * Get valid bucket list for entire sliding window.
  276 |      * The list will only contain "valid" buckets.
  277 |      *
  278 |      * @return valid bucket list for entire sliding window.
  279 |      */
  280 |     public List<WindowWrap<T>> list() {
  281 |         return list(TimeUtil.currentTimeMillis());
  282 |     }
  283 | 
  284 |     public List<WindowWrap<T>> list(long validTime) {
  285 |         int size = array.length();
  286 |         List<WindowWrap<T>> result = new ArrayList<WindowWrap<T>>(size);
  287 | 
  288 |         for (int i = 0; i < size; i++) {
  289 |             WindowWrap<T> windowWrap = array.get(i);
  290 |             if (windowWrap == null || isWindowDeprecated(validTime, windowWrap)) {
  291 |                 continue;
  292 |             }
  293 |             result.add(windowWrap);
  294 |         }
  295 | 
  296 |         return result;
  297 |     }
  298 | 
  299 |     /**
  300 |      * Get all buckets for entire sliding window including deprecated buckets.
  301 |      *
  302 |      * @return all buckets for entire sliding window
  303 |      */
  304 |     public List<WindowWrap<T>> listAll() {
  305 |         int size = array.length();
  306 |         List<WindowWrap<T>> result = new ArrayList<WindowWrap<T>>(size);
  307 | 
  308 |         for (int i = 0; i < size; i++) {
  309 |             WindowWrap<T> windowWrap = array.get(i);
  310 |             if (windowWrap == null) {
  311 |                 continue;
  312 |             }
  313 |             result.add(windowWrap);
  314 |         }
  315 | 
  316 |         return result;
  317 |     }
  318 | 
  319 |     /**
  320 |      * Get aggregated value list for entire sliding window.
  321 |      * The list will only contain value from "valid" buckets.
  322 |      *
  323 |      * @return aggregated value list for entire sliding window
  324 |      */
  325 |     public List<T> values() {
  326 |         return values(TimeUtil.currentTimeMillis());
  327 |     }
  328 | 
  329 |     public List<T> values(long timeMillis) {
  330 |         if (timeMillis < 0) {
  331 |             return new ArrayList<T>();
  332 |         }
  333 |         int size = array.length();
  334 |         List<T> result = new ArrayList<T>(size);
  335 | 
  336 |         for (int i = 0; i < size; i++) {
  337 |             WindowWrap<T> windowWrap = array.get(i);
  338 |             if (windowWrap == null || isWindowDeprecated(timeMillis, windowWrap)) {
  339 |                 continue;
  340 |             }
  341 |             result.add(windowWrap.value());
  342 |         }
  343 |         return result;
  344 |     }
  345 | 
  346 |     /**
  347 |      * Get the valid "head" bucket of the sliding window for provided timestamp.
  348 |      * Package-private for test.
  349 |      *
  350 |      * @param timeMillis a valid timestamp in milliseconds
  351 |      * @return the "head" bucket if it exists and is valid; otherwise null
  352 |      */
  353 |     WindowWrap<T> getValidHead(long timeMillis) {
  354 |         // Calculate index for expected head time.
  355 |         int idx = calculateTimeIdx(timeMillis + windowLengthInMs);
  356 | 
  357 |         WindowWrap<T> wrap = array.get(idx);
  358 |         if (wrap == null || isWindowDeprecated(wrap)) {
  359 |             return null;
  360 |         }
  361 | 
  362 |         return wrap;
  363 |     }
  364 | 
  365 |     /**
  366 |      * Get the valid "head" bucket of the sliding window at current timestamp.
  367 |      *
  368 |      * @return the "head" bucket if it exists and is valid; otherwise null
  369 |      */
  370 |     public WindowWrap<T> getValidHead() {
  371 |         return getValidHead(TimeUtil.currentTimeMillis());
  372 |     }
  373 | 
  374 |     /**
  375 |      * Get sample count (total amount of buckets).
  376 |      *
  377 |      * @return sample count
  378 |      */
  379 |     public int getSampleCount() {
  380 |         return sampleCount;
  381 |     }
  382 | 
  383 |     /**
  384 |      * Get total interval length of the sliding window in milliseconds.
  385 |      *
  386 |      * @return interval in second
  387 |      */
  388 |     public int getIntervalInMs() {
  389 |         return intervalInMs;
  390 |     }
  391 | 
  392 |     /**
  393 |      * Get total interval length of the sliding window.
  394 |      *
  395 |      * @return interval in second
  396 |      */
  397 |     public double getIntervalInSecond() {
  398 |         return intervalInSecond;
  399 |     }
  400 | 
  401 |     public void debug(long time) {
  402 |         StringBuilder sb = new StringBuilder();
  403 |         List<WindowWrap<T>> lists = list(time);
  404 |         sb.append("Thread_").append(Thread.currentThread().getId()).append("_");
  405 |         for (WindowWrap<T> window : lists) {
  406 |             sb.append(window.windowStart()).append(":").append(window.value().toString());
  407 |         }
  408 |         System.out.println(sb.toString());
  409 |     }
  410 | 
  411 |     public long currentWaiting() {
  412 |         // TODO: default method. Should remove this later.
  413 |         return 0;
  414 |     }
  415 | 
  416 |     public void addWaiting(long time, int acquireCount) {
  417 |         // Do nothing by default.
  418 |         throw new UnsupportedOperationException();
  419 |     }
  420 | }
```
