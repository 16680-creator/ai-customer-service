<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/ProxyConfig.java -->
<!-- 正文引用：[[源码:ProxyConfig.java#L起-L止]] ；代码块头部：(src: ProxyConfig.java#L起-L止) -->

# ProxyConfig.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/ProxyConfig.java` |
| 行数 | 199 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:57.467Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.aop.framework;
   18 | 
   19 | import java.io.Serializable;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.util.Assert;
   24 | 
   25 | /**
   26 |  * Convenience superclass for configuration used in creating proxies,
   27 |  * to ensure that all proxy creators have consistent properties.
   28 |  *
   29 |  * @author Rod Johnson
   30 |  * @author Juergen Hoeller
   31 |  * @see AdvisedSupport
   32 |  */
   33 | public class ProxyConfig implements Serializable {
   34 | 
   35 | 	/** use serialVersionUID from Spring 1.2 for interoperability. */
   36 | 	private static final long serialVersionUID = -8409359707199703185L;
   37 | 
   38 | 
   39 | 	private @Nullable Boolean proxyTargetClass;
   40 | 
   41 | 	private @Nullable Boolean optimize;
   42 | 
   43 | 	private @Nullable Boolean opaque;
   44 | 
   45 | 	private @Nullable Boolean exposeProxy;
   46 | 
   47 | 	private @Nullable Boolean frozen;
   48 | 
   49 | 
   50 | 	/**
   51 | 	 * Set whether to proxy the target class directly, instead of just proxying
   52 | 	 * specific interfaces. Default is "false".
   53 | 	 * <p>Set this to "true" to force proxying for the TargetSource's exposed
   54 | 	 * target class. If that target class is an interface, a JDK proxy will be
   55 | 	 * created for the given interface. If that target class is any other class,
   56 | 	 * a CGLIB proxy will be created for the given class.
   57 | 	 * <p>Note: Depending on the configuration of the concrete proxy factory,
   58 | 	 * the proxy-target-class behavior will also be applied if no interfaces
   59 | 	 * have been specified (and no interface autodetection is activated).
   60 | 	 * @see org.springframework.aop.TargetSource#getTargetClass()
   61 | 	 */
   62 | 	public void setProxyTargetClass(boolean proxyTargetClass) {
   63 | 		this.proxyTargetClass = proxyTargetClass;
   64 | 	}
   65 | 
   66 | 	/**
   67 | 	 * Return whether to proxy the target class directly as well as any interfaces.
   68 | 	 */
   69 | 	public boolean isProxyTargetClass() {
   70 | 		return (this.proxyTargetClass != null && this.proxyTargetClass);
   71 | 	}
   72 | 
   73 | 	/**
   74 | 	 * Set whether proxies should perform aggressive optimizations.
   75 | 	 * The exact meaning of "aggressive optimizations" will differ
   76 | 	 * between proxies, but there is usually some tradeoff.
   77 | 	 * Default is "false".
   78 | 	 * <p>With Spring's current proxy options, this flag effectively
   79 | 	 * enforces CGLIB proxies (similar to {@link #setProxyTargetClass})
   80 | 	 * but without any class validation checks (for final methods etc).
   81 | 	 */
   82 | 	public void setOptimize(boolean optimize) {
   83 | 		this.optimize = optimize;
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Return whether proxies should perform aggressive optimizations.
   88 | 	 */
   89 | 	public boolean isOptimize() {
   90 | 		return (this.optimize != null && this.optimize);
   91 | 	}
   92 | 
   93 | 	/**
   94 | 	 * Set whether proxies created by this configuration should be prevented
   95 | 	 * from being cast to {@link Advised} to query proxy status.
   96 | 	 * <p>Default is "false", meaning that any AOP proxy can be cast to
   97 | 	 * {@link Advised}.
   98 | 	 */
   99 | 	public void setOpaque(boolean opaque) {
  100 | 		this.opaque = opaque;
  101 | 	}
  102 | 
  103 | 	/**
  104 | 	 * Return whether proxies created by this configuration should be
  105 | 	 * prevented from being cast to {@link Advised}.
  106 | 	 */
  107 | 	public boolean isOpaque() {
  108 | 		return (this.opaque != null && this.opaque);
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * Set whether the proxy should be exposed by the AOP framework as a
  113 | 	 * ThreadLocal for retrieval via the AopContext class. This is useful
  114 | 	 * if an advised object needs to call another advised method on itself.
  115 | 	 * (If it uses {@code this}, the invocation will not be advised).
  116 | 	 * <p>Default is "false", in order to avoid unnecessary extra interception.
  117 | 	 * This means that no guarantees are provided that AopContext access will
  118 | 	 * work consistently within any method of the advised object.
  119 | 	 */
  120 | 	public void setExposeProxy(boolean exposeProxy) {
  121 | 		this.exposeProxy = exposeProxy;
  122 | 	}
  123 | 
  124 | 	/**
  125 | 	 * Return whether the AOP proxy will expose the AOP proxy for
  126 | 	 * each invocation.
  127 | 	 */
  128 | 	public boolean isExposeProxy() {
  129 | 		return (this.exposeProxy != null && this.exposeProxy);
  130 | 	}
  131 | 
  132 | 	/**
  133 | 	 * Set whether this config should be frozen.
  134 | 	 * <p>When a config is frozen, no advice changes can be made. This is
  135 | 	 * useful for optimization, and useful when we don't want callers to
  136 | 	 * be able to manipulate configuration after casting to Advised.
  137 | 	 */
  138 | 	public void setFrozen(boolean frozen) {
  139 | 		this.frozen = frozen;
  140 | 	}
  141 | 
  142 | 	/**
  143 | 	 * Return whether the config is frozen, and no advice changes can be made.
  144 | 	 */
  145 | 	public boolean isFrozen() {
  146 | 		return (this.frozen != null && this.frozen);
  147 | 	}
  148 | 
  149 | 
  150 | 	/**
  151 | 	 * Copy configuration from the other config object.
  152 | 	 * @param other object to copy configuration from
  153 | 	 */
  154 | 	public void copyFrom(ProxyConfig other) {
  155 | 		Assert.notNull(other, "Other ProxyConfig object must not be null");
  156 | 		this.proxyTargetClass = other.proxyTargetClass;
  157 | 		this.optimize = other.optimize;
  158 | 		this.opaque = other.opaque;
  159 | 		this.exposeProxy = other.exposeProxy;
  160 | 		this.frozen = other.frozen;
  161 | 	}
  162 | 
  163 | 	/**
  164 | 	 * Copy default settings from the other config object,
  165 | 	 * for settings that have not been locally set.
  166 | 	 * @param other object to copy configuration from
  167 | 	 * @since 7.0
  168 | 	 */
  169 | 	public void copyDefault(ProxyConfig other) {
  170 | 		Assert.notNull(other, "Other ProxyConfig object must not be null");
  171 | 		if (this.proxyTargetClass == null) {
  172 | 			this.proxyTargetClass = other.proxyTargetClass;
  173 | 		}
  174 | 		if (this.optimize == null) {
  175 | 			this.optimize = other.optimize;
  176 | 		}
  177 | 		if (this.opaque == null) {
  178 | 			this.opaque = other.opaque;
  179 | 		}
  180 | 		if (this.exposeProxy == null) {
  181 | 			this.exposeProxy = other.exposeProxy;
  182 | 		}
  183 | 		if (this.frozen == null) {
  184 | 			this.frozen = other.frozen;
  185 | 		}
  186 | 	}
  187 | 
  188 | 	@Override
  189 | 	public String toString() {
  190 | 		StringBuilder sb = new StringBuilder();
  191 | 		sb.append("proxyTargetClass=").append(this.proxyTargetClass).append("; ");
  192 | 		sb.append("optimize=").append(this.optimize).append("; ");
  193 | 		sb.append("opaque=").append(this.opaque).append("; ");
  194 | 		sb.append("exposeProxy=").append(this.exposeProxy).append("; ");
  195 | 		sb.append("frozen=").append(this.frozen);
  196 | 		return sb.toString();
  197 | 	}
  198 | 
  199 | }
```
