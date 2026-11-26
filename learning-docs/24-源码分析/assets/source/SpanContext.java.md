<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: io.opentelemetry:opentelemetry-api:1.50.0 | path: io/opentelemetry/api/trace/SpanContext.java -->
<!-- 正文引用：[[源码:SpanContext.java#L起-L止]] ；代码块头部：(src: SpanContext.java#L起-L止) -->

# SpanContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `io.opentelemetry:opentelemetry-api:1.50.0` |
| 路径 | `io/opentelemetry/api/trace/SpanContext.java` |
| 行数 | 158 |
| 在线地址 | https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-api/1.50.0/opentelemetry-api-1.50.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:30.612Z |

```java
    1 | /*
    2 |  * Copyright The OpenTelemetry Authors
    3 |  * SPDX-License-Identifier: Apache-2.0
    4 |  */
    5 | 
    6 | package io.opentelemetry.api.trace;
    7 | 
    8 | import io.opentelemetry.api.internal.ImmutableSpanContext;
    9 | import io.opentelemetry.api.internal.OtelEncodingUtils;
   10 | import javax.annotation.concurrent.Immutable;
   11 | 
   12 | /**
   13 |  * A class that represents a span context. A span context contains the state that must propagate to
   14 |  * child {@link Span}s and across process boundaries. It contains the identifiers (a {@link TraceId
   15 |  * trace_id} and {@link SpanId span_id}) associated with the {@link Span} and a set of options
   16 |  * (currently only whether the context is sampled or not), as well as the {@link TraceState
   17 |  * traceState} and the {@link boolean remote} flag.
   18 |  *
   19 |  * <p>Implementations of this interface *must* be immutable and have well-defined value-based
   20 |  * equals/hashCode implementations. If an implementation does not strictly conform to these
   21 |  * requirements, behavior of the OpenTelemetry APIs and default SDK cannot be guaranteed. It is
   22 |  * strongly suggested that you use the implementation that is provided here via {@link
   23 |  * #create(String, String, TraceFlags, TraceState)} or {@link #createFromRemoteParent(String,
   24 |  * String, TraceFlags, TraceState)}.
   25 |  */
   26 | @Immutable
   27 | public interface SpanContext {
   28 | 
   29 |   /**
   30 |    * Returns the invalid {@code SpanContext} that can be used for no-op operations.
   31 |    *
   32 |    * @return the invalid {@code SpanContext}.
   33 |    */
   34 |   static SpanContext getInvalid() {
   35 |     return ImmutableSpanContext.INVALID;
   36 |   }
   37 | 
   38 |   /**
   39 |    * Creates a new {@code SpanContext} with the given identifiers and options.
   40 |    *
   41 |    * <p>If the traceId or the spanId are invalid (ie. do not conform to the requirements for
   42 |    * hexadecimal ids of the appropriate lengths), both will be replaced with the standard "invalid"
   43 |    * versions (i.e. all '0's). See {@link SpanId#isValid(CharSequence)} and {@link
   44 |    * TraceId#isValid(CharSequence)} for details.
   45 |    *
   46 |    * @param traceIdHex the trace identifier of the {@code SpanContext}.
   47 |    * @param spanIdHex the span identifier of the {@code SpanContext}.
   48 |    * @param traceFlags the trace flags of the {@code SpanContext}.
   49 |    * @param traceState the trace state for the {@code SpanContext}.
   50 |    * @return a new {@code SpanContext} with the given identifiers and options.
   51 |    */
   52 |   static SpanContext create(
   53 |       String traceIdHex, String spanIdHex, TraceFlags traceFlags, TraceState traceState) {
   54 |     return ImmutableSpanContext.create(
   55 |         traceIdHex,
   56 |         spanIdHex,
   57 |         traceFlags,
   58 |         traceState,
   59 |         /* remote= */ false,
   60 |         /* skipIdValidation= */ false);
   61 |   }
   62 | 
   63 |   /**
   64 |    * Creates a new {@code SpanContext} that was propagated from a remote parent, with the given
   65 |    * identifiers and options.
   66 |    *
   67 |    * <p>If the traceId or the spanId are invalid (ie. do not conform to the requirements for
   68 |    * hexadecimal ids of the appropriate lengths), both will be replaced with the standard "invalid"
   69 |    * versions (i.e. all '0's). See {@link SpanId#isValid(CharSequence)} and {@link
   70 |    * TraceId#isValid(CharSequence)} for details.
   71 |    *
   72 |    * @param traceIdHex the trace identifier of the {@code SpanContext}.
   73 |    * @param spanIdHex the span identifier of the {@code SpanContext}.
   74 |    * @param traceFlags the trace flags of the {@code SpanContext}.
   75 |    * @param traceState the trace state for the {@code SpanContext}.
   76 |    * @return a new {@code SpanContext} with the given identifiers and options.
   77 |    */
   78 |   static SpanContext createFromRemoteParent(
   79 |       String traceIdHex, String spanIdHex, TraceFlags traceFlags, TraceState traceState) {
   80 |     return ImmutableSpanContext.create(
   81 |         traceIdHex,
   82 |         spanIdHex,
   83 |         traceFlags,
   84 |         traceState,
   85 |         /* remote= */ true,
   86 |         /* skipIdValidation= */ false);
   87 |   }
   88 | 
   89 |   /**
   90 |    * Returns the trace identifier associated with this {@link SpanContext} as 32 character lowercase
   91 |    * hex String.
   92 |    *
   93 |    * @return the trace identifier associated with this {@link SpanContext} as lowercase hex.
   94 |    */
   95 |   String getTraceId();
   96 | 
   97 |   /**
   98 |    * Returns the trace identifier associated with this {@link SpanContext} as 16-byte array.
   99 |    *
  100 |    * @return the trace identifier associated with this {@link SpanContext} as 16-byte array.
  101 |    */
  102 |   default byte[] getTraceIdBytes() {
  103 |     return OtelEncodingUtils.bytesFromBase16(getTraceId(), TraceId.getLength());
  104 |   }
  105 | 
  106 |   /**
  107 |    * Returns the span identifier associated with this {@link SpanContext} as 16 character lowercase
  108 |    * hex String.
  109 |    *
  110 |    * @return the span identifier associated with this {@link SpanContext} as 16 character lowercase
  111 |    *     hex (base16) String.
  112 |    */
  113 |   String getSpanId();
  114 | 
  115 |   /**
  116 |    * Returns the span identifier associated with this {@link SpanContext} as 8-byte array.
  117 |    *
  118 |    * @return the span identifier associated with this {@link SpanContext} as 8-byte array.
  119 |    */
  120 |   default byte[] getSpanIdBytes() {
  121 |     return OtelEncodingUtils.bytesFromBase16(getSpanId(), SpanId.getLength());
  122 |   }
  123 | 
  124 |   /** Whether the span in this context is sampled. */
  125 |   default boolean isSampled() {
  126 |     return getTraceFlags().isSampled();
  127 |   }
  128 | 
  129 |   /**
  130 |    * Returns the trace flags associated with this {@link SpanContext}.
  131 |    *
  132 |    * @return the trace flags associated with this {@link SpanContext}.
  133 |    */
  134 |   TraceFlags getTraceFlags();
  135 | 
  136 |   /**
  137 |    * Returns the {@code TraceState} associated with this {@code SpanContext}.
  138 |    *
  139 |    * @return the {@code TraceState} associated with this {@code SpanContext}.
  140 |    */
  141 |   TraceState getTraceState();
  142 | 
  143 |   /**
  144 |    * Returns {@code true} if this {@code SpanContext} is valid.
  145 |    *
  146 |    * @return {@code true} if this {@code SpanContext} is valid.
  147 |    */
  148 |   default boolean isValid() {
  149 |     return TraceId.isValid(getTraceId()) && SpanId.isValid(getSpanId());
  150 |   }
  151 | 
  152 |   /**
  153 |    * Returns {@code true} if the {@code SpanContext} was propagated from a remote parent.
  154 |    *
  155 |    * @return {@code true} if the {@code SpanContext} was propagated from a remote parent.
  156 |    */
  157 |   boolean isRemote();
  158 | }
```
