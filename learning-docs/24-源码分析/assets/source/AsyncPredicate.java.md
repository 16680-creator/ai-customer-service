<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/handler/AsyncPredicate.java -->
<!-- 正文引用：[[源码:AsyncPredicate.java#L起-L止]] ；代码块头部：(src: AsyncPredicate.java#L起-L止) -->

# AsyncPredicate.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/handler/AsyncPredicate.java` |
| 行数 | 170 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:26.364Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.cloud.gateway.handler;
   18 | 
   19 | import java.util.Objects;
   20 | import java.util.function.Function;
   21 | import java.util.function.Predicate;
   22 | 
   23 | import org.reactivestreams.Publisher;
   24 | import reactor.core.publisher.Mono;
   25 | 
   26 | import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
   27 | import org.springframework.cloud.gateway.support.HasConfig;
   28 | import org.springframework.cloud.gateway.support.Visitor;
   29 | import org.springframework.web.server.ServerWebExchange;
   30 | 
   31 | /**
   32 |  * @author Ben Hale
   33 |  */
   34 | public interface AsyncPredicate<T> extends Function<T, Publisher<Boolean>>, HasConfig {
   35 | 
   36 | 	default AsyncPredicate<T> and(AsyncPredicate<? super T> other) {
   37 | 		return new AndAsyncPredicate<>(this, other);
   38 | 	}
   39 | 
   40 | 	default AsyncPredicate<T> negate() {
   41 | 		return new NegateAsyncPredicate<>(this);
   42 | 	}
   43 | 
   44 | 	default AsyncPredicate<T> not(AsyncPredicate<? super T> other) {
   45 | 		return new NegateAsyncPredicate<>(other);
   46 | 	}
   47 | 
   48 | 	default AsyncPredicate<T> or(AsyncPredicate<? super T> other) {
   49 | 		return new OrAsyncPredicate<>(this, other);
   50 | 	}
   51 | 
   52 | 	default void accept(Visitor visitor) {
   53 | 		visitor.visit(this);
   54 | 	}
   55 | 
   56 | 	static AsyncPredicate<ServerWebExchange> from(Predicate<? super ServerWebExchange> predicate) {
   57 | 		return new DefaultAsyncPredicate<>(GatewayPredicate.wrapIfNeeded(predicate));
   58 | 	}
   59 | 
   60 | 	class DefaultAsyncPredicate<T> implements AsyncPredicate<T> {
   61 | 
   62 | 		private final Predicate<T> delegate;
   63 | 
   64 | 		public DefaultAsyncPredicate(Predicate<T> delegate) {
   65 | 			this.delegate = delegate;
   66 | 		}
   67 | 
   68 | 		@Override
   69 | 		public Publisher<Boolean> apply(T t) {
   70 | 			return Mono.just(delegate.test(t));
   71 | 		}
   72 | 
   73 | 		@Override
   74 | 		public String toString() {
   75 | 			return this.delegate.toString();
   76 | 		}
   77 | 
   78 | 		@Override
   79 | 		public void accept(Visitor visitor) {
   80 | 			if (delegate instanceof GatewayPredicate gatewayPredicate) {
   81 | 				gatewayPredicate.accept(visitor);
   82 | 			}
   83 | 		}
   84 | 
   85 | 	}
   86 | 
   87 | 	class NegateAsyncPredicate<T> implements AsyncPredicate<T> {
   88 | 
   89 | 		private final AsyncPredicate<? super T> predicate;
   90 | 
   91 | 		public NegateAsyncPredicate(AsyncPredicate<? super T> predicate) {
   92 | 			Objects.requireNonNull(predicate, "predicate AsyncPredicate must not be null");
   93 | 			this.predicate = predicate;
   94 | 		}
   95 | 
   96 | 		@Override
   97 | 		public Publisher<Boolean> apply(T t) {
   98 | 			return Mono.from(predicate.apply(t)).map(b -> !b);
   99 | 		}
  100 | 
  101 | 		@Override
  102 | 		public String toString() {
  103 | 			return String.format("!(%s)", this.predicate);
  104 | 		}
  105 | 
  106 | 	}
  107 | 
  108 | 	class AndAsyncPredicate<T> implements AsyncPredicate<T> {
  109 | 
  110 | 		private final AsyncPredicate<? super T> left;
  111 | 
  112 | 		private final AsyncPredicate<? super T> right;
  113 | 
  114 | 		public AndAsyncPredicate(AsyncPredicate<? super T> left, AsyncPredicate<? super T> right) {
  115 | 			Objects.requireNonNull(left, "Left AsyncPredicate must not be null");
  116 | 			Objects.requireNonNull(right, "Right AsyncPredicate must not be null");
  117 | 			this.left = left;
  118 | 			this.right = right;
  119 | 		}
  120 | 
  121 | 		@Override
  122 | 		public Publisher<Boolean> apply(T t) {
  123 | 			return Mono.from(left.apply(t)).flatMap(result -> !result ? Mono.just(false) : Mono.from(right.apply(t)));
  124 | 		}
  125 | 
  126 | 		@Override
  127 | 		public void accept(Visitor visitor) {
  128 | 			left.accept(visitor);
  129 | 			right.accept(visitor);
  130 | 		}
  131 | 
  132 | 		@Override
  133 | 		public String toString() {
  134 | 			return String.format("(%s && %s)", this.left, this.right);
  135 | 		}
  136 | 
  137 | 	}
  138 | 
  139 | 	class OrAsyncPredicate<T> implements AsyncPredicate<T> {
  140 | 
  141 | 		private final AsyncPredicate<? super T> left;
  142 | 
  143 | 		private final AsyncPredicate<? super T> right;
  144 | 
  145 | 		public OrAsyncPredicate(AsyncPredicate<? super T> left, AsyncPredicate<? super T> right) {
  146 | 			Objects.requireNonNull(left, "Left AsyncPredicate must not be null");
  147 | 			Objects.requireNonNull(right, "Right AsyncPredicate must not be null");
  148 | 			this.left = left;
  149 | 			this.right = right;
  150 | 		}
  151 | 
  152 | 		@Override
  153 | 		public Publisher<Boolean> apply(T t) {
  154 | 			return Mono.from(left.apply(t)).flatMap(result -> result ? Mono.just(true) : Mono.from(right.apply(t)));
  155 | 		}
  156 | 
  157 | 		@Override
  158 | 		public void accept(Visitor visitor) {
  159 | 			left.accept(visitor);
  160 | 			right.accept(visitor);
  161 | 		}
  162 | 
  163 | 		@Override
  164 | 		public String toString() {
  165 | 			return String.format("(%s || %s)", this.left, this.right);
  166 | 		}
  167 | 
  168 | 	}
  169 | 
  170 | }
```
