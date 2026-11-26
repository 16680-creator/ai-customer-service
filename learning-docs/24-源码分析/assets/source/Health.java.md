<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-health:4.2.0 | path: org/springframework/boot/health/contributor/Health.java -->
<!-- 正文引用：[[源码:Health.java#L起-L止]] ；代码块头部：(src: Health.java#L起-L止) -->

# Health.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-health:4.2.0` |
| 路径 | `org/springframework/boot/health/contributor/Health.java` |
| 行数 | 323 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-health/4.2.0/spring-boot-health-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:02.564Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.health.contributor;
    6 | 
    7 | import java.util.Collections;
    8 | import java.util.LinkedHashMap;
    9 | import java.util.Map;
   10 | 
   11 | import com.fasterxml.jackson.annotation.JsonInclude;
   12 | import com.fasterxml.jackson.annotation.JsonInclude.Include;
   13 | import com.fasterxml.jackson.annotation.JsonUnwrapped;
   14 | import org.jspecify.annotations.Nullable;
   15 | 
   16 | import org.springframework.util.Assert;
   17 | 
   18 | /**
   19 |  * The health of a component or subsystem in including its {@link Status} as well as any
   20 |  * additional contextual details. Subclasses should expect to be serialized and presented
   21 |  * to the user, typically as JSON.
   22 |  * <p>
   23 |  * {@link Health} instances can be created by using {@link Builder}'s fluent API. Typical
   24 |  * usage in a {@link HealthIndicator} would be:
   25 |  *
   26 |  * <pre class="code">
   27 |  * try {
   28 |  * 	// do some test to determine state of component
   29 |  * 	return Health.up().withDetail("version", "1.1.2").build();
   30 |  * }
   31 |  * catch (Exception ex) {
   32 |  * 	return Health.down(ex).build();
   33 |  * }
   34 |  * </pre>
   35 |  *
   36 |  * @author Christian Dupuis
   37 |  * @author Phillip Webb
   38 |  * @author Michael Pratt
   39 |  * @since 4.0.0
   40 |  */
   41 | public final class Health {
   42 | 
   43 | 	private final Status status;
   44 | 
   45 | 	private final Map<String, Object> details;
   46 | 
   47 | 	/**
   48 | 	 * Create a new {@link Health} instance with the specified status and details.
   49 | 	 * @param builder the Builder to use
   50 | 	 */
   51 | 	private Health(Builder builder) {
   52 | 		Assert.notNull(builder, "'builder' must not be null");
   53 | 		this.status = builder.status;
   54 | 		this.details = Collections.unmodifiableMap(builder.details);
   55 | 	}
   56 | 
   57 | 	Health(Status status, Map<String, Object> details) {
   58 | 		this.status = status;
   59 | 		this.details = details;
   60 | 	}
   61 | 
   62 | 	/**
   63 | 	 * Return the status of the health.
   64 | 	 * @return the status (never {@code null})
   65 | 	 */
   66 | 	@JsonUnwrapped
   67 | 	public Status getStatus() {
   68 | 		return this.status;
   69 | 	}
   70 | 
   71 | 	/**
   72 | 	 * Return the details of the health.
   73 | 	 * @return the details (or an empty map)
   74 | 	 */
   75 | 	@JsonInclude(Include.NON_EMPTY)
   76 | 	public Map<String, Object> getDetails() {
   77 | 		return this.details;
   78 | 	}
   79 | 
   80 | 	Health withoutDetails() {
   81 | 		return (this.details.isEmpty()) ? this : status(getStatus()).build();
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	public boolean equals(Object obj) {
   86 | 		if (obj == this) {
   87 | 			return true;
   88 | 		}
   89 | 		if (obj instanceof Health other) {
   90 | 			return this.status.equals(other.status) && this.details.equals(other.details);
   91 | 		}
   92 | 		return false;
   93 | 	}
   94 | 
   95 | 	@Override
   96 | 	public int hashCode() {
   97 | 		int hashCode = this.status.hashCode();
   98 | 		return 13 * hashCode + this.details.hashCode();
   99 | 	}
  100 | 
  101 | 	@Override
  102 | 	public String toString() {
  103 | 		return getStatus() + " " + getDetails();
  104 | 	}
  105 | 
  106 | 	/**
  107 | 	 * Create a new {@link Builder} instance with an {@link Status#UNKNOWN} status.
  108 | 	 * @return a new {@link Builder} instance
  109 | 	 */
  110 | 	public static Builder unknown() {
  111 | 		return status(Status.UNKNOWN);
  112 | 	}
  113 | 
  114 | 	/**
  115 | 	 * Create a new {@link Builder} instance with an {@link Status#UP} status.
  116 | 	 * @return a new {@link Builder} instance
  117 | 	 */
  118 | 	public static Builder up() {
  119 | 		return status(Status.UP);
  120 | 	}
  121 | 
  122 | 	/**
  123 | 	 * Create a new {@link Builder} instance with an {@link Status#DOWN} status and the
  124 | 	 * specified exception details.
  125 | 	 * @param ex the exception
  126 | 	 * @return a new {@link Builder} instance
  127 | 	 */
  128 | 	public static Builder down(Throwable ex) {
  129 | 		return down().withException(ex);
  130 | 	}
  131 | 
  132 | 	/**
  133 | 	 * Create a new {@link Builder} instance with a {@link Status#DOWN} status.
  134 | 	 * @return a new {@link Builder} instance
  135 | 	 */
  136 | 	public static Builder down() {
  137 | 		return status(Status.DOWN);
  138 | 	}
  139 | 
  140 | 	/**
  141 | 	 * Create a new {@link Builder} instance with an {@link Status#OUT_OF_SERVICE} status.
  142 | 	 * @return a new {@link Builder} instance
  143 | 	 */
  144 | 	public static Builder outOfService() {
  145 | 		return status(Status.OUT_OF_SERVICE);
  146 | 	}
  147 | 
  148 | 	/**
  149 | 	 * Create a new {@link Builder} instance with a specific status code.
  150 | 	 * @param statusCode the status code
  151 | 	 * @return a new {@link Builder} instance
  152 | 	 */
  153 | 	public static Builder status(String statusCode) {
  154 | 		return status(new Status(statusCode));
  155 | 	}
  156 | 
  157 | 	/**
  158 | 	 * Create a new {@link Builder} instance with a specific {@link Status}.
  159 | 	 * @param status the status
  160 | 	 * @return a new {@link Builder} instance
  161 | 	 */
  162 | 	public static Builder status(Status status) {
  163 | 		return new Builder(status);
  164 | 	}
  165 | 
  166 | 	/**
  167 | 	 * Builder for creating immutable {@link Health} instances.
  168 | 	 */
  169 | 	public static class Builder {
  170 | 
  171 | 		private Status status;
  172 | 
  173 | 		private final Map<String, Object> details;
  174 | 
  175 | 		private @Nullable Throwable exception;
  176 | 
  177 | 		/**
  178 | 		 * Create new Builder instance.
  179 | 		 */
  180 | 		public Builder() {
  181 | 			this.status = Status.UNKNOWN;
  182 | 			this.details = new LinkedHashMap<>();
  183 | 		}
  184 | 
  185 | 		/**
  186 | 		 * Create new Builder instance, setting status to given {@code status}.
  187 | 		 * @param status the {@link Status} to use
  188 | 		 */
  189 | 		public Builder(Status status) {
  190 | 			Assert.notNull(status, "'status' must not be null");
  191 | 			this.status = status;
  192 | 			this.details = new LinkedHashMap<>();
  193 | 		}
  194 | 
  195 | 		/**
  196 | 		 * Create new Builder instance, setting status to given {@code status} and details
  197 | 		 * to given {@code details}.
  198 | 		 * @param status the {@link Status} to use
  199 | 		 * @param details the details {@link Map} to use
  200 | 		 */
  201 | 		public Builder(Status status, Map<String, ?> details) {
  202 | 			Assert.notNull(status, "'status' must not be null");
  203 | 			Assert.notNull(details, "'details' must not be null");
  204 | 			this.status = status;
  205 | 			this.details = new LinkedHashMap<>(details);
  206 | 		}
  207 | 
  208 | 		/**
  209 | 		 * Record detail for given {@link Exception}.
  210 | 		 * @param exception the exception
  211 | 		 * @return this {@link Builder} instance
  212 | 		 */
  213 | 		public Builder withException(Throwable exception) {
  214 | 			Assert.notNull(exception, "'exception' must not be null");
  215 | 			this.exception = exception;
  216 | 			return withDetail("error", exception.getClass().getName() + ": " + exception.getMessage());
  217 | 		}
  218 | 
  219 | 		/**
  220 | 		 * Record detail using given {@code key} and {@code value}.
  221 | 		 * @param key the detail key
  222 | 		 * @param value the detail value
  223 | 		 * @return this {@link Builder} instance
  224 | 		 */
  225 | 		public Builder withDetail(String key, Object value) {
  226 | 			Assert.notNull(key, "'key' must not be null");
  227 | 			Assert.notNull(value, "'value' must not be null");
  228 | 			this.details.put(key, value);
  229 | 			return this;
  230 | 		}
  231 | 
  232 | 		/**
  233 | 		 * Record details from the given {@code details} map. Keys from the given map
  234 | 		 * replace any existing keys if there are duplicates.
  235 | 		 * @param details map of details
  236 | 		 * @return this {@link Builder} instance
  237 | 		 */
  238 | 		public Builder withDetails(Map<String, ?> details) {
  239 | 			Assert.notNull(details, "'details' must not be null");
  240 | 			this.details.putAll(details);
  241 | 			return this;
  242 | 		}
  243 | 
  244 | 		/**
  245 | 		 * Set status to {@link Status#UNKNOWN} status.
  246 | 		 * @return this {@link Builder} instance
  247 | 		 */
  248 | 		public Builder unknown() {
  249 | 			return status(Status.UNKNOWN);
  250 | 		}
  251 | 
  252 | 		/**
  253 | 		 * Set status to {@link Status#UP} status.
  254 | 		 * @return this {@link Builder} instance
  255 | 		 */
  256 | 		public Builder up() {
  257 | 			return status(Status.UP);
  258 | 		}
  259 | 
  260 | 		/**
  261 | 		 * Set status to {@link Status#DOWN} and add details for given {@link Throwable}.
  262 | 		 * @param ex the exception
  263 | 		 * @return this {@link Builder} instance
  264 | 		 */
  265 | 		public Builder down(Throwable ex) {
  266 | 			return down().withException(ex);
  267 | 		}
  268 | 
  269 | 		/**
  270 | 		 * Set status to {@link Status#DOWN}.
  271 | 		 * @return this {@link Builder} instance
  272 | 		 */
  273 | 		public Builder down() {
  274 | 			return status(Status.DOWN);
  275 | 		}
  276 | 
  277 | 		/**
  278 | 		 * Set status to {@link Status#OUT_OF_SERVICE}.
  279 | 		 * @return this {@link Builder} instance
  280 | 		 */
  281 | 		public Builder outOfService() {
  282 | 			return status(Status.OUT_OF_SERVICE);
  283 | 		}
  284 | 
  285 | 		/**
  286 | 		 * Set status to given {@code statusCode}.
  287 | 		 * @param statusCode the status code
  288 | 		 * @return this {@link Builder} instance
  289 | 		 */
  290 | 		public Builder status(String statusCode) {
  291 | 			return status(new Status(statusCode));
  292 | 		}
  293 | 
  294 | 		/**
  295 | 		 * Set status to given {@link Status} instance.
  296 | 		 * @param status the status
  297 | 		 * @return this {@link Builder} instance
  298 | 		 */
  299 | 		public Builder status(Status status) {
  300 | 			this.status = status;
  301 | 			return this;
  302 | 		}
  303 | 
  304 | 		/**
  305 | 		 * Create a new {@link Health} instance with the previously specified code and
  306 | 		 * details.
  307 | 		 * @return a new {@link Health} instance
  308 | 		 */
  309 | 		public Health build() {
  310 | 			return new Health(this);
  311 | 		}
  312 | 
  313 | 		/**
  314 | 		 * Return the {@link Exception}.
  315 | 		 * @return the exception or {@code null} if the builder has no exception
  316 | 		 */
  317 | 		@Nullable Throwable getException() {
  318 | 			return this.exception;
  319 | 		}
  320 | 
  321 | 	}
  322 | 
  323 | }
```
