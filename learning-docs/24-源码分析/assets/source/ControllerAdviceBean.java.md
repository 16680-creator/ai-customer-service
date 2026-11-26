<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/ControllerAdviceBean.java -->
<!-- 正文引用：[[源码:ControllerAdviceBean.java#L起-L止]] ；代码块头部：(src: ControllerAdviceBean.java#L起-L止) -->

# ControllerAdviceBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/ControllerAdviceBean.java` |
| 行数 | 282 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.083Z |

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
   17 | package org.springframework.web.method;
   18 | 
   19 | import java.lang.reflect.AnnotatedElement;
   20 | import java.lang.reflect.Method;
   21 | import java.util.ArrayList;
   22 | import java.util.List;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.aop.scope.ScopedProxyUtils;
   27 | import org.springframework.beans.factory.BeanFactory;
   28 | import org.springframework.beans.factory.BeanFactoryUtils;
   29 | import org.springframework.beans.factory.ListableBeanFactory;
   30 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   31 | import org.springframework.beans.factory.config.BeanDefinition;
   32 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   33 | import org.springframework.beans.factory.support.RootBeanDefinition;
   34 | import org.springframework.context.ApplicationContext;
   35 | import org.springframework.context.ConfigurableApplicationContext;
   36 | import org.springframework.core.OrderComparator;
   37 | import org.springframework.core.Ordered;
   38 | import org.springframework.core.annotation.OrderUtils;
   39 | import org.springframework.util.Assert;
   40 | import org.springframework.util.ClassUtils;
   41 | import org.springframework.web.bind.annotation.ControllerAdvice;
   42 | 
   43 | /**
   44 |  * Encapsulates information about an {@link ControllerAdvice @ControllerAdvice}
   45 |  * Spring-managed bean without necessarily requiring it to be instantiated.
   46 |  * The {@link #findAnnotatedBeans(ApplicationContext)} method can be used to
   47 |  * discover such beans.
   48 |  *
   49 |  * <p>This class is internal to Spring Framework and is not meant to be used
   50 |  * by applications to manually create {@code @ControllerAdvice} beans.
   51 |  *
   52 |  * @author Rossen Stoyanchev
   53 |  * @author Brian Clozel
   54 |  * @author Juergen Hoeller
   55 |  * @author Sam Brannen
   56 |  * @since 3.2
   57 |  */
   58 | public class ControllerAdviceBean implements Ordered {
   59 | 
   60 | 	private final String beanName;
   61 | 
   62 | 	private final boolean isSingleton;
   63 | 
   64 | 	/**
   65 | 	 * Reference to the resolved bean instance, potentially lazily retrieved
   66 | 	 * via the {@code BeanFactory}.
   67 | 	 */
   68 | 	private @Nullable Object resolvedBean;
   69 | 
   70 | 	private final @Nullable Class<?> beanType;
   71 | 
   72 | 	private final HandlerTypePredicate beanTypePredicate;
   73 | 
   74 | 	private final BeanFactory beanFactory;
   75 | 
   76 | 	private @Nullable Integer order;
   77 | 
   78 | 
   79 | 	/**
   80 | 	 * Create a {@code ControllerAdviceBean} using the given bean name,
   81 | 	 * {@code BeanFactory}, and {@link ControllerAdvice @ControllerAdvice}
   82 | 	 * annotation.
   83 | 	 * @param beanName the name of the bean
   84 | 	 * @param beanFactory a {@code BeanFactory} to retrieve the bean type initially
   85 | 	 * and later to resolve the actual bean
   86 | 	 * @param controllerAdvice the {@code @ControllerAdvice} annotation for the bean
   87 | 	 * @since 5.2
   88 | 	 */
   89 | 	public ControllerAdviceBean(String beanName, BeanFactory beanFactory, ControllerAdvice controllerAdvice) {
   90 | 		Assert.hasText(beanName, "Bean name must contain text");
   91 | 		Assert.notNull(beanFactory, "BeanFactory must not be null");
   92 | 		Assert.isTrue(beanFactory.containsBean(beanName), () -> "BeanFactory [" + beanFactory +
   93 | 				"] does not contain specified controller advice bean '" + beanName + "'");
   94 | 		Assert.notNull(controllerAdvice, "ControllerAdvice must not be null");
   95 | 
   96 | 		this.beanName = beanName;
   97 | 		this.isSingleton = beanFactory.isSingleton(beanName);
   98 | 		this.beanType = getBeanType(beanName, beanFactory);
   99 | 		this.beanTypePredicate = createBeanTypePredicate(controllerAdvice);
  100 | 		this.beanFactory = beanFactory;
  101 | 	}
  102 | 
  103 | 
  104 | 	/**
  105 | 	 * Get the order value for the contained bean.
  106 | 	 * <p>The order value is lazily retrieved using the following algorithm and cached.
  107 | 	 * Note, however, that a {@link ControllerAdvice @ControllerAdvice} bean that is
  108 | 	 * configured as a scoped bean &mdash; for example, as a request-scoped or
  109 | 	 * session-scoped bean &mdash; will not be eagerly resolved. Consequently,
  110 | 	 * {@link Ordered} is not honored for scoped {@code @ControllerAdvice} beans.
  111 | 	 * <ul>
  112 | 	 * <li>If the {@linkplain #resolveBean resolved bean} implements {@link Ordered},
  113 | 	 * use the value returned by {@link Ordered#getOrder()}.</li>
  114 | 	 * <li>If the {@linkplain org.springframework.context.annotation.Bean factory method}
  115 | 	 * is known, use the value returned by {@link OrderUtils#getOrder(AnnotatedElement)}.
  116 | 	 * <li>If the {@linkplain #getBeanType() bean type} is known, use the value returned
  117 | 	 * by {@link OrderUtils#getOrder(Class, int)} with {@link Ordered#LOWEST_PRECEDENCE}
  118 | 	 * used as the default order value.</li>
  119 | 	 * <li>Otherwise use {@link Ordered#LOWEST_PRECEDENCE} as the default, fallback
  120 | 	 * order value.</li>
  121 | 	 * </ul>
  122 | 	 * @see #resolveBean()
  123 | 	 */
  124 | 	@Override
  125 | 	public int getOrder() {
  126 | 		if (this.order == null) {
  127 | 			Object resolvedBean = null;
  128 | 			String targetBeanName = ScopedProxyUtils.getTargetBeanName(this.beanName);
  129 | 			boolean isScopedProxy = this.beanFactory.containsBean(targetBeanName);
  130 | 			// Avoid eager @ControllerAdvice bean resolution for scoped proxies,
  131 | 			// since attempting to do so during context initialization would result
  132 | 			// in an exception due to the current absence of the scope. For example,
  133 | 			// an HTTP request or session scope is not active during initialization.
  134 | 			if (!isScopedProxy && !ScopedProxyUtils.isScopedTarget(this.beanName)) {
  135 | 				resolvedBean = resolveBean();
  136 | 			}
  137 | 
  138 | 			if (resolvedBean instanceof Ordered ordered) {
  139 | 				this.order = ordered.getOrder();
  140 | 			}
  141 | 			else {
  142 | 				if (this.beanFactory instanceof ConfigurableBeanFactory cbf) {
  143 | 					try {
  144 | 						BeanDefinition bd = cbf.getMergedBeanDefinition(this.beanName);
  145 | 						if (bd instanceof RootBeanDefinition rbd) {
  146 | 							Method factoryMethod = rbd.getResolvedFactoryMethod();
  147 | 							if (factoryMethod != null) {
  148 | 								this.order = OrderUtils.getOrder(factoryMethod);
  149 | 							}
  150 | 						}
  151 | 					}
  152 | 					catch (NoSuchBeanDefinitionException ex) {
  153 | 						// ignore -> probably a manually registered singleton
  154 | 					}
  155 | 				}
  156 | 				if (this.order == null) {
  157 | 					if (this.beanType != null) {
  158 | 						this.order = OrderUtils.getOrder(this.beanType, Ordered.LOWEST_PRECEDENCE);
  159 | 					}
  160 | 					else {
  161 | 						this.order = Ordered.LOWEST_PRECEDENCE;
  162 | 					}
  163 | 				}
  164 | 			}
  165 | 		}
  166 | 		return this.order;
  167 | 	}
  168 | 
  169 | 	/**
  170 | 	 * Return the type of the contained bean.
  171 | 	 * <p>If the bean type is a CGLIB-generated class, the original user-defined
  172 | 	 * class is returned.
  173 | 	 */
  174 | 	public @Nullable Class<?> getBeanType() {
  175 | 		return this.beanType;
  176 | 	}
  177 | 
  178 | 	/**
  179 | 	 * Get the bean instance for this {@code ControllerAdviceBean}, if necessary
  180 | 	 * resolving the bean name through the {@link BeanFactory}.
  181 | 	 * <p>Once the bean instance has been resolved it will be cached if it is a
  182 | 	 * singleton, thereby avoiding repeated lookups in the {@code BeanFactory}.
  183 | 	 */
  184 | 	public Object resolveBean() {
  185 | 		if (this.resolvedBean == null) {
  186 | 			Object resolvedBean = this.beanFactory.getBean(this.beanName);
  187 | 			// Don't cache non-singletons (for example, prototypes).
  188 | 			if (!this.isSingleton) {
  189 | 				return resolvedBean;
  190 | 			}
  191 | 			this.resolvedBean = resolvedBean;
  192 | 		}
  193 | 		return this.resolvedBean;
  194 | 	}
  195 | 
  196 | 	/**
  197 | 	 * Check whether the given bean type should be advised by this
  198 | 	 * {@code ControllerAdviceBean}.
  199 | 	 * @param beanType the type of the bean to check
  200 | 	 * @since 4.0
  201 | 	 * @see ControllerAdvice
  202 | 	 */
  203 | 	public boolean isApplicableToBeanType(@Nullable Class<?> beanType) {
  204 | 		return this.beanTypePredicate.test(beanType);
  205 | 	}
  206 | 
  207 | 
  208 | 	@Override
  209 | 	public boolean equals(@Nullable Object other) {
  210 | 		return (this == other || (other instanceof ControllerAdviceBean that &&
  211 | 				this.beanName.equals(that.beanName) && this.beanFactory == that.beanFactory));
  212 | 	}
  213 | 
  214 | 	@Override
  215 | 	public int hashCode() {
  216 | 		return this.beanName.hashCode();
  217 | 	}
  218 | 
  219 | 	@Override
  220 | 	public String toString() {
  221 | 		return this.beanName;
  222 | 	}
  223 | 
  224 | 
  225 | 	/**
  226 | 	 * Find beans annotated with {@link ControllerAdvice @ControllerAdvice} in the
  227 | 	 * given {@link ApplicationContext} and wrap them as {@code ControllerAdviceBean}
  228 | 	 * instances.
  229 | 	 * <p>Note that the {@code ControllerAdviceBean} instances in the returned list
  230 | 	 * are sorted using {@link OrderComparator#sort(List)}.
  231 | 	 * @see #findAnnotatedBeans(ListableBeanFactory)
  232 | 	 */
  233 | 	public static List<ControllerAdviceBean> findAnnotatedBeans(ApplicationContext context) {
  234 | 		return findAnnotatedBeans((ListableBeanFactory) context);
  235 | 	}
  236 | 
  237 | 	/**
  238 | 	 * Find beans annotated with {@link ControllerAdvice @ControllerAdvice} in the
  239 | 	 * given {@link ListableBeanFactory} and wrap them as {@code ControllerAdviceBean}
  240 | 	 * instances.
  241 | 	 * <p>Note that the {@code ControllerAdviceBean} instances in the returned list
  242 | 	 * are sorted using {@link OrderComparator#sort(List)}.
  243 | 	 * @since 7.0
  244 | 	 * @see #getOrder()
  245 | 	 * @see OrderComparator
  246 | 	 * @see Ordered
  247 | 	 */
  248 | 	public static List<ControllerAdviceBean> findAnnotatedBeans(ListableBeanFactory beanFactory) {
  249 | 		if (beanFactory instanceof ConfigurableApplicationContext cac) {
  250 | 			// Use internal BeanFactory for potential downcast to ConfigurableBeanFactory in getOrder().
  251 | 			beanFactory = cac.getBeanFactory();
  252 | 		}
  253 | 		List<ControllerAdviceBean> adviceBeans = new ArrayList<>();
  254 | 		for (String name : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Object.class)) {
  255 | 			if (!ScopedProxyUtils.isScopedTarget(name)) {
  256 | 				ControllerAdvice controllerAdvice = beanFactory.findAnnotationOnBean(name, ControllerAdvice.class);
  257 | 				if (controllerAdvice != null) {
  258 | 					// Use the @ControllerAdvice annotation found by findAnnotationOnBean()
  259 | 					// in order to avoid a subsequent lookup of the same annotation.
  260 | 					adviceBeans.add(new ControllerAdviceBean(name, beanFactory, controllerAdvice));
  261 | 				}
  262 | 			}
  263 | 		}
  264 | 		OrderComparator.sort(adviceBeans);
  265 | 		return adviceBeans;
  266 | 	}
  267 | 
  268 | 	private static @Nullable Class<?> getBeanType(String beanName, BeanFactory beanFactory) {
  269 | 		Class<?> beanType = beanFactory.getType(beanName);
  270 | 		return (beanType != null ? ClassUtils.getUserClass(beanType) : null);
  271 | 	}
  272 | 
  273 | 	private static HandlerTypePredicate createBeanTypePredicate(ControllerAdvice controllerAdvice) {
  274 | 		return HandlerTypePredicate.builder()
  275 | 				.basePackage(controllerAdvice.basePackages())
  276 | 				.basePackageClass(controllerAdvice.basePackageClasses())
  277 | 				.assignableType(controllerAdvice.assignableTypes())
  278 | 				.annotation(controllerAdvice.annotations())
  279 | 				.build();
  280 | 	}
  281 | 
  282 | }
```
