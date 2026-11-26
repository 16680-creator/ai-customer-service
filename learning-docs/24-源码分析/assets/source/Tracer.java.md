<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: io.opentelemetry:opentelemetry-api:1.50.0 | path: io/opentelemetry/api/trace/Tracer.java -->
<!-- 正文引用：[[源码:Tracer.java#L起-L止]] ；代码块头部：(src: Tracer.java#L起-L止) -->

# Tracer.java

| 项 | 值 |
| --- | --- |
| 来源 | `io.opentelemetry:opentelemetry-api:1.50.0` |
| 路径 | `io/opentelemetry/api/trace/Tracer.java` |
| 行数 | 72 |
| 在线地址 | https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-api/1.50.0/opentelemetry-api-1.50.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:30.283Z |

```java
    1 | /*
    2 |  * Copyright The OpenTelemetry Authors
    3 |  * SPDX-License-Identifier: Apache-2.0
    4 |  */
    5 | 
    6 | package io.opentelemetry.api.trace;
    7 | 
    8 | import javax.annotation.concurrent.ThreadSafe;
    9 | 
   10 | /**
   11 |  * Tracer is the interface for {@link Span} creation and interaction with the in-process context.
   12 |  *
   13 |  * <p>Users may choose to use manual or automatic Context propagation. Because of that this class
   14 |  * offers APIs to facilitate both usages.
   15 |  *
   16 |  * <p>The automatic context propagation is done using {@link io.opentelemetry.context.Context} which
   17 |  * is a gRPC independent implementation for in-process Context propagation mechanism which can carry
   18 |  * scoped-values across API boundaries and between threads. Users of the library must propagate the
   19 |  * {@link io.opentelemetry.context.Context} between different threads.
   20 |  *
   21 |  * <p>Example usage with automatic context propagation:
   22 |  *
   23 |  * <pre>{@code
   24 |  * class MyClass {
   25 |  *   private static final Tracer tracer =
   26 |  *     openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
   27 |  *   void doWork() {
   28 |  *     Span span = tracer.spanBuilder("MyClass.DoWork").startSpan();
   29 |  *     try (Scope ignored = span.makeCurrent()) {
   30 |  *       Span.current().addEvent("Starting the work.");
   31 |  *       doWorkInternal();
   32 |  *       Span.current().addEvent("Finished working.");
   33 |  *     } finally {
   34 |  *       span.end();
   35 |  *     }
   36 |  *   }
   37 |  * }
   38 |  * }</pre>
   39 |  *
   40 |  * <p>Example usage with manual context propagation:
   41 |  *
   42 |  * <pre>{@code
   43 |  * class MyClass {
   44 |  *   private static final Tracer tracer =
   45 |  *     openTelemetry.getTracer("instrumentation-library-name", "1.0.0");
   46 |  *   void doWork(Span parent) {
   47 |  *     Span childSpan = tracer.spanBuilder("MyChildSpan")
   48 |  *         setParent(parent).startSpan();
   49 |  *     childSpan.addEvent("Starting the work.");
   50 |  *     try {
   51 |  *       doSomeWork(childSpan); // Manually propagate the new span down the stack.
   52 |  *     } finally {
   53 |  *       // To make sure we end the span even in case of an exception.
   54 |  *       childSpan.end();  // Manually end the span.
   55 |  *     }
   56 |  *   }
   57 |  * }
   58 |  * }</pre>
   59 |  */
   60 | @ThreadSafe
   61 | public interface Tracer {
   62 | 
   63 |   /**
   64 |    * Returns a {@link SpanBuilder} to create and start a new {@link Span}.
   65 |    *
   66 |    * <p>See {@link SpanBuilder} for usage examples.
   67 |    *
   68 |    * @param spanName The name of the returned Span.
   69 |    * @return a {@code Span.Builder} to create and start a new {@code Span}.
   70 |    */
   71 |   SpanBuilder spanBuilder(String spanName);
   72 | }
```
