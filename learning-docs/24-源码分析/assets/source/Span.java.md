<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: io.opentelemetry:opentelemetry-api:1.50.0 | path: io/opentelemetry/api/trace/Span.java -->
<!-- 正文引用：[[源码:Span.java#L起-L止]] ；代码块头部：(src: Span.java#L起-L止) -->

# Span.java

| 项 | 值 |
| --- | --- |
| 来源 | `io.opentelemetry:opentelemetry-api:1.50.0` |
| 路径 | `io/opentelemetry/api/trace/Span.java` |
| 行数 | 469 |
| 在线地址 | https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-api/1.50.0/opentelemetry-api-1.50.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:30.120Z |

```java
    1 | /*
    2 |  * Copyright The OpenTelemetry Authors
    3 |  * SPDX-License-Identifier: Apache-2.0
    4 |  */
    5 | 
    6 | package io.opentelemetry.api.trace;
    7 | 
    8 | import static java.util.concurrent.TimeUnit.NANOSECONDS;
    9 | import static java.util.concurrent.TimeUnit.SECONDS;
   10 | 
   11 | import io.opentelemetry.api.common.AttributeKey;
   12 | import io.opentelemetry.api.common.Attributes;
   13 | import io.opentelemetry.api.internal.ApiUsageLogger;
   14 | import io.opentelemetry.context.Context;
   15 | import io.opentelemetry.context.ImplicitContextKeyed;
   16 | import java.time.Instant;
   17 | import java.util.concurrent.TimeUnit;
   18 | import javax.annotation.Nullable;
   19 | import javax.annotation.concurrent.ThreadSafe;
   20 | 
   21 | /**
   22 |  * An interface that represents a span. It has an associated {@link SpanContext}.
   23 |  *
   24 |  * <p>Spans are created by the {@link SpanBuilder#startSpan} method.
   25 |  *
   26 |  * <p>{@code Span} <b>must</b> be ended by calling {@link #end()}.
   27 |  */
   28 | @ThreadSafe
   29 | public interface Span extends ImplicitContextKeyed {
   30 | 
   31 |   /**
   32 |    * Returns the {@link Span} from the current {@link Context}, falling back to a default, no-op
   33 |    * {@link Span} if there is no span in the current context.
   34 |    */
   35 |   static Span current() {
   36 |     Span span = Context.current().get(SpanContextKey.KEY);
   37 |     return span == null ? getInvalid() : span;
   38 |   }
   39 | 
   40 |   /**
   41 |    * Returns the {@link Span} from the specified {@link Context}, falling back to a default, no-op
   42 |    * {@link Span} if there is no span in the context.
   43 |    */
   44 |   static Span fromContext(Context context) {
   45 |     if (context == null) {
   46 |       ApiUsageLogger.log("context is null");
   47 |       return Span.getInvalid();
   48 |     }
   49 |     Span span = context.get(SpanContextKey.KEY);
   50 |     return span == null ? getInvalid() : span;
   51 |   }
   52 | 
   53 |   /**
   54 |    * Returns the {@link Span} from the specified {@link Context}, or {@code null} if there is no
   55 |    * span in the context.
   56 |    */
   57 |   @Nullable
   58 |   static Span fromContextOrNull(Context context) {
   59 |     if (context == null) {
   60 |       ApiUsageLogger.log("context is null");
   61 |       return null;
   62 |     }
   63 |     return context.get(SpanContextKey.KEY);
   64 |   }
   65 | 
   66 |   /**
   67 |    * Returns an invalid {@link Span}. An invalid {@link Span} is used when tracing is disabled,
   68 |    * usually because there is no OpenTelemetry SDK installed.
   69 |    */
   70 |   static Span getInvalid() {
   71 |     return PropagatedSpan.INVALID;
   72 |   }
   73 | 
   74 |   /**
   75 |    * Returns a non-recording {@link Span} that holds the provided {@link SpanContext} but has no
   76 |    * functionality. It will not be exported and all tracing operations are no-op, but it can be used
   77 |    * to propagate a {@link SpanContext} downstream.
   78 |    */
   79 |   static Span wrap(SpanContext spanContext) {
   80 |     if (spanContext == null) {
   81 |       ApiUsageLogger.log("context is null");
   82 |       return getInvalid();
   83 |     }
   84 |     return PropagatedSpan.create(spanContext);
   85 |   }
   86 | 
   87 |   /**
   88 |    * Sets an attribute to the {@code Span}. If the {@code Span} previously contained a mapping for
   89 |    * the key, the old value is replaced by the specified value.
   90 |    *
   91 |    * <p>Empty String "" and null are valid attribute {@code value}s, but not valid keys.
   92 |    *
   93 |    * <p>Note: Providing a null value is a no-op and will not remove previously set values.
   94 |    *
   95 |    * <p>Note: It is strongly recommended to use {@link #setAttribute(AttributeKey, Object)}, and
   96 |    * pre-allocate your keys, if possible.
   97 |    *
   98 |    * @param key the key for this attribute.
   99 |    * @param value the value for this attribute.
  100 |    * @return this.
  101 |    */
  102 |   default Span setAttribute(String key, @Nullable String value) {
  103 |     return setAttribute(AttributeKey.stringKey(key), value);
  104 |   }
  105 | 
  106 |   /**
  107 |    * Sets an attribute to the {@code Span}. If the {@code Span} previously contained a mapping for
  108 |    * the key, the old value is replaced by the specified value.
  109 |    *
  110 |    * <p>Note: It is strongly recommended to use {@link #setAttribute(AttributeKey, Object)}, and
  111 |    * pre-allocate your keys, if possible.
  112 |    *
  113 |    * @param key the key for this attribute.
  114 |    * @param value the value for this attribute.
  115 |    * @return this.
  116 |    */
  117 |   default Span setAttribute(String key, long value) {
  118 |     return setAttribute(AttributeKey.longKey(key), value);
  119 |   }
  120 | 
  121 |   /**
  122 |    * Sets an attribute to the {@code Span}. If the {@code Span} previously contained a mapping for
  123 |    * the key, the old value is replaced by the specified value.
  124 |    *
  125 |    * <p>Note: It is strongly recommended to use {@link #setAttribute(AttributeKey, Object)}, and
  126 |    * pre-allocate your keys, if possible.
  127 |    *
  128 |    * @param key the key for this attribute.
  129 |    * @param value the value for this attribute.
  130 |    * @return this.
  131 |    */
  132 |   default Span setAttribute(String key, double value) {
  133 |     return setAttribute(AttributeKey.doubleKey(key), value);
  134 |   }
  135 | 
  136 |   /**
  137 |    * Sets an attribute to the {@code Span}. If the {@code Span} previously contained a mapping for
  138 |    * the key, the old value is replaced by the specified value.
  139 |    *
  140 |    * <p>Note: It is strongly recommended to use {@link #setAttribute(AttributeKey, Object)}, and
  141 |    * pre-allocate your keys, if possible.
  142 |    *
  143 |    * @param key the key for this attribute.
  144 |    * @param value the value for this attribute.
  145 |    * @return this.
  146 |    */
  147 |   default Span setAttribute(String key, boolean value) {
  148 |     return setAttribute(AttributeKey.booleanKey(key), value);
  149 |   }
  150 | 
  151 |   /**
  152 |    * Sets an attribute to the {@code Span}. If the {@code Span} previously contained a mapping for
  153 |    * the key, the old value is replaced by the specified value.
  154 |    *
  155 |    * <p>Note: Providing a null value is a no-op.
  156 |    *
  157 |    * @param key the key for this attribute.
  158 |    * @param value the value for this attribute.
  159 |    * @return this.
  160 |    */
  161 |   <T> Span setAttribute(AttributeKey<T> key, @Nullable T value);
  162 | 
  163 |   /**
  164 |    * Sets an attribute to the {@code Span}. If the {@code Span} previously contained a mapping for
  165 |    * the key, the old value is replaced by the specified value.
  166 |    *
  167 |    * @param key the key for this attribute.
  168 |    * @param value the value for this attribute.
  169 |    * @return this.
  170 |    */
  171 |   default Span setAttribute(AttributeKey<Long> key, int value) {
  172 |     return setAttribute(key, (long) value);
  173 |   }
  174 | 
  175 |   /**
  176 |    * Sets attributes to the {@link Span}. If the {@link Span} previously contained a mapping for any
  177 |    * of the keys, the old values are replaced by the specified values.
  178 |    *
  179 |    * @param attributes the attributes
  180 |    * @return this.
  181 |    * @since 1.2.0
  182 |    */
  183 |   @SuppressWarnings("unchecked")
  184 |   default Span setAllAttributes(Attributes attributes) {
  185 |     if (attributes == null || attributes.isEmpty()) {
  186 |       return this;
  187 |     }
  188 |     attributes.forEach(
  189 |         (attributeKey, value) -> this.setAttribute((AttributeKey<Object>) attributeKey, value));
  190 |     return this;
  191 |   }
  192 | 
  193 |   /**
  194 |    * Adds an event to the {@link Span}. The timestamp of the event will be the current time.
  195 |    *
  196 |    * @param name the name of the event.
  197 |    * @return this.
  198 |    */
  199 |   default Span addEvent(String name) {
  200 |     return addEvent(name, Attributes.empty());
  201 |   }
  202 | 
  203 |   /**
  204 |    * Adds an event to the {@link Span} with the given {@code timestamp}, as nanos since epoch. Note,
  205 |    * this {@code timestamp} is not the same as {@link System#nanoTime()} but may be computed using
  206 |    * it, for example, by taking a difference of readings from {@link System#nanoTime()} and adding
  207 |    * to the span start time.
  208 |    *
  209 |    * <p>When possible, it is preferred to use {@link #addEvent(String)} at the time the event
  210 |    * occurred.
  211 |    *
  212 |    * @param name the name of the event.
  213 |    * @param timestamp the explicit event timestamp since epoch.
  214 |    * @param unit the unit of the timestamp
  215 |    * @return this.
  216 |    */
  217 |   default Span addEvent(String name, long timestamp, TimeUnit unit) {
  218 |     return addEvent(name, Attributes.empty(), timestamp, unit);
  219 |   }
  220 | 
  221 |   /**
  222 |    * Adds an event to the {@link Span} with the given {@code timestamp}, as nanos since epoch. Note,
  223 |    * this {@code timestamp} is not the same as {@link System#nanoTime()} but may be computed using
  224 |    * it, for example, by taking a difference of readings from {@link System#nanoTime()} and adding
  225 |    * to the span start time.
  226 |    *
  227 |    * <p>When possible, it is preferred to use {@link #addEvent(String)} at the time the event
  228 |    * occurred.
  229 |    *
  230 |    * @param name the name of the event.
  231 |    * @param timestamp the explicit event timestamp since epoch.
  232 |    * @return this.
  233 |    */
  234 |   default Span addEvent(String name, Instant timestamp) {
  235 |     if (timestamp == null) {
  236 |       return addEvent(name);
  237 |     }
  238 |     return addEvent(
  239 |         name, SECONDS.toNanos(timestamp.getEpochSecond()) + timestamp.getNano(), NANOSECONDS);
  240 |   }
  241 | 
  242 |   /**
  243 |    * Adds an event to the {@link Span} with the given {@link Attributes}. The timestamp of the event
  244 |    * will be the current time.
  245 |    *
  246 |    * @param name the name of the event.
  247 |    * @param attributes the attributes that will be added; these are associated with this event, not
  248 |    *     the {@code Span} as for {@code setAttribute()}.
  249 |    * @return this.
  250 |    */
  251 |   Span addEvent(String name, Attributes attributes);
  252 | 
  253 |   /**
  254 |    * Adds an event to the {@link Span} with the given {@link Attributes} and {@code timestamp}.
  255 |    * Note, this {@code timestamp} is not the same as {@link System#nanoTime()} but may be computed
  256 |    * using it, for example, by taking a difference of readings from {@link System#nanoTime()} and
  257 |    * adding to the span start time.
  258 |    *
  259 |    * <p>When possible, it is preferred to use {@link #addEvent(String)} at the time the event
  260 |    * occurred.
  261 |    *
  262 |    * @param name the name of the event.
  263 |    * @param attributes the attributes that will be added; these are associated with this event, not
  264 |    *     the {@code Span} as for {@code setAttribute()}.
  265 |    * @param timestamp the explicit event timestamp since epoch.
  266 |    * @param unit the unit of the timestamp
  267 |    * @return this.
  268 |    */
  269 |   Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit);
  270 | 
  271 |   /**
  272 |    * Adds an event to the {@link Span} with the given {@link Attributes} and {@code timestamp}.
  273 |    * Note, this {@code timestamp} is not the same as {@link System#nanoTime()} but may be computed
  274 |    * using it, for example, by taking a difference of readings from {@link System#nanoTime()} and
  275 |    * adding to the span start time.
  276 |    *
  277 |    * <p>When possible, it is preferred to use {@link #addEvent(String)} at the time the event
  278 |    * occurred.
  279 |    *
  280 |    * @param name the name of the event.
  281 |    * @param attributes the attributes that will be added; these are associated with this event, not
  282 |    *     the {@code Span} as for {@code setAttribute()}.
  283 |    * @param timestamp the explicit event timestamp since epoch.
  284 |    * @return this.
  285 |    */
  286 |   default Span addEvent(String name, Attributes attributes, Instant timestamp) {
  287 |     if (timestamp == null) {
  288 |       return addEvent(name, attributes);
  289 |     }
  290 |     return addEvent(
  291 |         name,
  292 |         attributes,
  293 |         SECONDS.toNanos(timestamp.getEpochSecond()) + timestamp.getNano(),
  294 |         NANOSECONDS);
  295 |   }
  296 | 
  297 |   /**
  298 |    * Sets the status to the {@code Span}.
  299 |    *
  300 |    * <p>If used, this will override the default {@code Span} status. Default status code is {@link
  301 |    * StatusCode#UNSET}.
  302 |    *
  303 |    * <p>Only the value of the last call will be recorded, and implementations are free to ignore
  304 |    * previous calls.
  305 |    *
  306 |    * @param statusCode the {@link StatusCode} to set.
  307 |    * @return this.
  308 |    */
  309 |   default Span setStatus(StatusCode statusCode) {
  310 |     return setStatus(statusCode, "");
  311 |   }
  312 | 
  313 |   /**
  314 |    * Sets the status to the {@code Span}.
  315 |    *
  316 |    * <p>If used, this will override the default {@code Span} status. Default status code is {@link
  317 |    * StatusCode#UNSET}.
  318 |    *
  319 |    * <p>Only the value of the last call will be recorded, and implementations are free to ignore
  320 |    * previous calls.
  321 |    *
  322 |    * @param statusCode the {@link StatusCode} to set.
  323 |    * @param description the description of the {@code Status}.
  324 |    * @return this.
  325 |    */
  326 |   Span setStatus(StatusCode statusCode, String description);
  327 | 
  328 |   /**
  329 |    * Records information about the {@link Throwable} to the {@link Span}.
  330 |    *
  331 |    * <p>Note that the EXCEPTION_ESCAPED value from the Semantic Conventions cannot be determined by
  332 |    * this function. You should record this attribute manually using {@link
  333 |    * #recordException(Throwable, Attributes)} if you know that an exception is escaping.
  334 |    *
  335 |    * @param exception the {@link Throwable} to record.
  336 |    * @return this.
  337 |    */
  338 |   default Span recordException(Throwable exception) {
  339 |     return recordException(exception, Attributes.empty());
  340 |   }
  341 | 
  342 |   /**
  343 |    * Records information about the {@link Throwable} to the {@link Span}.
  344 |    *
  345 |    * @param exception the {@link Throwable} to record.
  346 |    * @param additionalAttributes the additional {@link Attributes} to record.
  347 |    * @return this.
  348 |    */
  349 |   Span recordException(Throwable exception, Attributes additionalAttributes);
  350 | 
  351 |   /**
  352 |    * Updates the {@code Span} name.
  353 |    *
  354 |    * <p>If used, this will override the name provided via {@code Span.Builder}.
  355 |    *
  356 |    * <p>Upon this update, any sampling behavior based on {@code Span} name will depend on the
  357 |    * implementation.
  358 |    *
  359 |    * @param name the {@code Span} name.
  360 |    * @return this.
  361 |    */
  362 |   Span updateName(String name);
  363 | 
  364 |   /**
  365 |    * Adds a link to this {@code Span}.
  366 |    *
  367 |    * <p>Links are used to link {@link Span}s in different traces. Used (for example) in batching
  368 |    * operations, where a single batch handler processes multiple requests from different traces or
  369 |    * the same trace.
  370 |    *
  371 |    * <p>Implementations may ignore calls with an {@linkplain SpanContext#isValid() invalid span
  372 |    * context}.
  373 |    *
  374 |    * <p>Callers should prefer to add links before starting the span via {@link
  375 |    * SpanBuilder#addLink(SpanContext)} if possible.
  376 |    *
  377 |    * @param spanContext the context of the linked {@code Span}.
  378 |    * @return this.
  379 |    * @since 1.37.0
  380 |    */
  381 |   default Span addLink(SpanContext spanContext) {
  382 |     return addLink(spanContext, Attributes.empty());
  383 |   }
  384 | 
  385 |   /**
  386 |    * Adds a link to this {@code Span}.
  387 |    *
  388 |    * <p>Links are used to link {@link Span}s in different traces. Used (for example) in batching
  389 |    * operations, where a single batch handler processes multiple requests from different traces or
  390 |    * the same trace.
  391 |    *
  392 |    * <p>Implementations may ignore calls with an {@linkplain SpanContext#isValid() invalid span
  393 |    * context}.
  394 |    *
  395 |    * <p>Callers should prefer to add links before starting the span via {@link
  396 |    * SpanBuilder#addLink(SpanContext, Attributes)} if possible.
  397 |    *
  398 |    * @param spanContext the context of the linked {@code Span}.
  399 |    * @param attributes the attributes of the {@code Link}.
  400 |    * @return this.
  401 |    * @since 1.37.0
  402 |    */
  403 |   default Span addLink(SpanContext spanContext, Attributes attributes) {
  404 |     return this;
  405 |   }
  406 | 
  407 |   /**
  408 |    * Marks the end of {@code Span} execution.
  409 |    *
  410 |    * <p>Only the timing of the first end call for a given {@code Span} will be recorded, and
  411 |    * implementations are free to ignore all further calls.
  412 |    */
  413 |   void end();
  414 | 
  415 |   /**
  416 |    * Marks the end of {@code Span} execution with the specified timestamp.
  417 |    *
  418 |    * <p>Only the timing of the first end call for a given {@code Span} will be recorded, and
  419 |    * implementations are free to ignore all further calls.
  420 |    *
  421 |    * <p>Use this method for specifying explicit end options, such as end {@code Timestamp}. When no
  422 |    * explicit values are required, use {@link #end()}.
  423 |    *
  424 |    * @param timestamp the explicit timestamp from the epoch, for this {@code Span}. {@code 0}
  425 |    *     indicates current time should be used.
  426 |    * @param unit the unit of the timestamp
  427 |    */
  428 |   void end(long timestamp, TimeUnit unit);
  429 | 
  430 |   /**
  431 |    * Marks the end of {@code Span} execution with the specified timestamp.
  432 |    *
  433 |    * <p>Only the timing of the first end call for a given {@code Span} will be recorded, and
  434 |    * implementations are free to ignore all further calls.
  435 |    *
  436 |    * <p>Use this method for specifying explicit end options, such as end {@code Timestamp}. When no
  437 |    * explicit values are required, use {@link #end()}.
  438 |    *
  439 |    * @param timestamp the explicit timestamp from the epoch, for this {@code Span}. {@code 0}
  440 |    *     indicates current time should be used.
  441 |    */
  442 |   default void end(Instant timestamp) {
  443 |     if (timestamp == null) {
  444 |       end();
  445 |       return;
  446 |     }
  447 |     end(SECONDS.toNanos(timestamp.getEpochSecond()) + timestamp.getNano(), NANOSECONDS);
  448 |   }
  449 | 
  450 |   /**
  451 |    * Returns the {@code SpanContext} associated with this {@code Span}.
  452 |    *
  453 |    * @return the {@code SpanContext} associated with this {@code Span}.
  454 |    */
  455 |   SpanContext getSpanContext();
  456 | 
  457 |   /**
  458 |    * Returns {@code true} if this {@code Span} records tracing events (e.g. {@link
  459 |    * #addEvent(String)}, {@link #setAttribute(String, long)}).
  460 |    *
  461 |    * @return {@code true} if this {@code Span} records tracing events.
  462 |    */
  463 |   boolean isRecording();
  464 | 
  465 |   @Override
  466 |   default Context storeInContext(Context context) {
  467 |     return context.with(SpanContextKey.KEY, this);
  468 |   }
  469 | }
```
