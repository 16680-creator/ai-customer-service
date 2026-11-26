<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/support/ShortcutConfigurable.java -->
<!-- 正文引用：[[源码:ShortcutConfigurable.java#L起-L止]] ；代码块头部：(src: ShortcutConfigurable.java#L起-L止) -->

# ShortcutConfigurable.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/support/ShortcutConfigurable.java` |
| 行数 | 305 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T07:50:18.322Z |

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
   17 | package org.springframework.cloud.gateway.support;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collections;
   21 | import java.util.HashMap;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.function.Supplier;
   25 | import java.util.stream.Collectors;
   26 | 
   27 | import org.springframework.beans.factory.BeanFactory;
   28 | import org.springframework.context.expression.BeanFactoryResolver;
   29 | import org.springframework.core.env.Environment;
   30 | import org.springframework.expression.AccessException;
   31 | import org.springframework.expression.BeanResolver;
   32 | import org.springframework.expression.ConstructorResolver;
   33 | import org.springframework.expression.EvaluationContext;
   34 | import org.springframework.expression.Expression;
   35 | import org.springframework.expression.IndexAccessor;
   36 | import org.springframework.expression.MethodResolver;
   37 | import org.springframework.expression.OperatorOverloader;
   38 | import org.springframework.expression.PropertyAccessor;
   39 | import org.springframework.expression.TypeComparator;
   40 | import org.springframework.expression.TypeConverter;
   41 | import org.springframework.expression.TypeLocator;
   42 | import org.springframework.expression.TypedValue;
   43 | import org.springframework.expression.common.TemplateParserContext;
   44 | import org.springframework.expression.spel.standard.SpelExpressionParser;
   45 | import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
   46 | import org.springframework.expression.spel.support.SimpleEvaluationContext;
   47 | import org.springframework.lang.Nullable;
   48 | import org.springframework.util.Assert;
   49 | 
   50 | import static org.springframework.context.ConfigurableApplicationContext.SYSTEM_ENVIRONMENT_BEAN_NAME;
   51 | import static org.springframework.context.ConfigurableApplicationContext.SYSTEM_PROPERTIES_BEAN_NAME;
   52 | 
   53 | /**
   54 |  * @author Spencer Gibb
   55 |  */
   56 | public interface ShortcutConfigurable {
   57 | 
   58 | 	static String normalizeKey(String key, int entryIdx, ShortcutConfigurable argHints, Map<String, String> args) {
   59 | 		// RoutePredicateFactory has name hints and this has a fake key name
   60 | 		// replace with the matching key hint
   61 | 		if (key.startsWith(NameUtils.GENERATED_NAME_PREFIX) && !argHints.shortcutFieldOrder().isEmpty()
   62 | 				&& entryIdx < args.size() && entryIdx < argHints.shortcutFieldOrder().size()) {
   63 | 			key = argHints.shortcutFieldOrder().get(entryIdx);
   64 | 		}
   65 | 		return key;
   66 | 	}
   67 | 
   68 | 	static Object getValue(SpelExpressionParser parser, BeanFactory beanFactory, String entryValue) {
   69 | 		Object value;
   70 | 		String rawValue = entryValue;
   71 | 		if (rawValue != null) {
   72 | 			rawValue = rawValue.trim();
   73 | 		}
   74 | 		if (rawValue != null && rawValue.startsWith("#{") && entryValue.endsWith("}")) {
   75 | 			// assume it's spel
   76 | 			GatewayEvaluationContext context = new GatewayEvaluationContext(beanFactory);
   77 | 			Expression expression = parser.parseExpression(entryValue, new TemplateParserContext());
   78 | 			value = expression.getValue(context);
   79 | 		}
   80 | 		else {
   81 | 			value = entryValue;
   82 | 		}
   83 | 		return value;
   84 | 	}
   85 | 
   86 | 	default ShortcutType shortcutType() {
   87 | 		return ShortcutType.DEFAULT;
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Returns hints about the number of args and the order for shortcut parsing.
   92 | 	 * @return the list of hints
   93 | 	 */
   94 | 	default List<String> shortcutFieldOrder() {
   95 | 		return Collections.emptyList();
   96 | 	}
   97 | 
   98 | 	default String shortcutFieldPrefix() {
   99 | 		return "";
  100 | 	}
  101 | 
  102 | 	enum ShortcutType {
  103 | 
  104 | 		/**
  105 | 		 * Default shortcut type.
  106 | 		 */
  107 | 		DEFAULT {
  108 | 			@Override
  109 | 			public Map<String, Object> normalize(Map<String, String> args, ShortcutConfigurable shortcutConf,
  110 | 					SpelExpressionParser parser, BeanFactory beanFactory) {
  111 | 				Map<String, Object> map = new HashMap<>();
  112 | 				int entryIdx = 0;
  113 | 				for (Map.Entry<String, String> entry : args.entrySet()) {
  114 | 					String key = normalizeKey(entry.getKey(), entryIdx, shortcutConf, args);
  115 | 					Object value = getValue(parser, beanFactory, entry.getValue());
  116 | 
  117 | 					map.put(key, value);
  118 | 					entryIdx++;
  119 | 				}
  120 | 				return map;
  121 | 			}
  122 | 		},
  123 | 
  124 | 		/**
  125 | 		 * List shortcut type.
  126 | 		 */
  127 | 		GATHER_LIST {
  128 | 			@Override
  129 | 			public Map<String, Object> normalize(Map<String, String> args, ShortcutConfigurable shortcutConf,
  130 | 					SpelExpressionParser parser, BeanFactory beanFactory) {
  131 | 				Map<String, Object> map = new HashMap<>();
  132 | 				// field order should be of size 1
  133 | 				List<String> fieldOrder = shortcutConf.shortcutFieldOrder();
  134 | 				Assert.isTrue(fieldOrder != null && fieldOrder.size() == 1,
  135 | 						"Shortcut Configuration Type GATHER_LIST must have shortcutFieldOrder of size 1");
  136 | 				String fieldName = fieldOrder.get(0);
  137 | 				map.put(fieldName,
  138 | 						args.values()
  139 | 							.stream()
  140 | 							.map(value -> getValue(parser, beanFactory, value))
  141 | 							.collect(Collectors.toList()));
  142 | 				return map;
  143 | 			}
  144 | 		},
  145 | 
  146 | 		/**
  147 | 		 * List is all elements except last which is a boolean flag.
  148 | 		 */
  149 | 		GATHER_LIST_TAIL_FLAG {
  150 | 			@Override
  151 | 			public Map<String, Object> normalize(Map<String, String> args, ShortcutConfigurable shortcutConf,
  152 | 					SpelExpressionParser parser, BeanFactory beanFactory) {
  153 | 				Map<String, Object> map = new HashMap<>();
  154 | 				// field order should be of size 1
  155 | 				List<String> fieldOrder = shortcutConf.shortcutFieldOrder();
  156 | 				Assert.isTrue(fieldOrder != null && fieldOrder.size() == 2,
  157 | 						"Shortcut Configuration Type GATHER_LIST_HEAD must have shortcutFieldOrder of size 2");
  158 | 				List<String> values = new ArrayList<>(args.values());
  159 | 				if (!values.isEmpty()) {
  160 | 					// strip boolean flag if last entry is true or false
  161 | 					int lastIdx = values.size() - 1;
  162 | 					String lastValue = values.get(lastIdx);
  163 | 					if ("true".equalsIgnoreCase(lastValue) || "false".equalsIgnoreCase(lastValue)
  164 | 							|| lastValue == null) {
  165 | 						values = values.subList(0, lastIdx);
  166 | 						map.put(fieldOrder.get(1), getValue(parser, beanFactory, lastValue));
  167 | 					}
  168 | 				}
  169 | 				String fieldName = fieldOrder.get(0);
  170 | 				map.put(fieldName,
  171 | 						values.stream()
  172 | 							.map(value -> getValue(parser, beanFactory, value))
  173 | 							.collect(Collectors.toList()));
  174 | 				return map;
  175 | 			}
  176 | 		};
  177 | 
  178 | 		public abstract Map<String, Object> normalize(Map<String, String> args, ShortcutConfigurable shortcutConf,
  179 | 				SpelExpressionParser parser, BeanFactory beanFactory);
  180 | 
  181 | 	}
  182 | 
  183 | 	class GatewayBeanFactoryResolver extends BeanFactoryResolver {
  184 | 
  185 | 		public GatewayBeanFactoryResolver(BeanFactory beanFactory) {
  186 | 			super(beanFactory);
  187 | 		}
  188 | 
  189 | 		@Override
  190 | 		public Object resolve(EvaluationContext context, String beanName) throws AccessException {
  191 | 			if (SYSTEM_ENVIRONMENT_BEAN_NAME.equals(beanName) || SYSTEM_PROPERTIES_BEAN_NAME.equals(beanName)) {
  192 | 				throw new AccessException(beanName
  193 | 						+ " is not accessible when spring.cloud.gateway.restrictive-property-accessor.enabled=true");
  194 | 			}
  195 | 			return super.resolve(context, beanName);
  196 | 		}
  197 | 
  198 | 	}
  199 | 
  200 | 	class GatewayEvaluationContext implements EvaluationContext {
  201 | 
  202 | 		private final BeanFactoryResolver beanFactoryResolver;
  203 | 
  204 | 		private final SimpleEvaluationContext delegate;
  205 | 
  206 | 		public GatewayEvaluationContext(BeanFactory beanFactory) {
  207 | 			this.beanFactoryResolver = new ShortcutConfigurable.GatewayBeanFactoryResolver(beanFactory);
  208 | 			Environment env = beanFactory.getBean(Environment.class);
  209 | 			boolean restrictive = env.getProperty(
  210 | 					"spring.cloud.gateway.server.webflux.restrictive-property-accessor.enabled", Boolean.class, true);
  211 | 			if (restrictive) {
  212 | 				delegate = SimpleEvaluationContext.forPropertyAccessors(new RestrictivePropertyAccessor())
  213 | 					.withMethodResolvers((context, targetObject, name, argumentTypes) -> null)
  214 | 					.withAssignmentDisabled()
  215 | 					.build();
  216 | 			}
  217 | 			else {
  218 | 				delegate = SimpleEvaluationContext.forReadOnlyDataBinding().build();
  219 | 			}
  220 | 		}
  221 | 
  222 | 		@Override
  223 | 		public TypedValue getRootObject() {
  224 | 			return delegate.getRootObject();
  225 | 		}
  226 | 
  227 | 		@Override
  228 | 		public List<PropertyAccessor> getPropertyAccessors() {
  229 | 			return delegate.getPropertyAccessors();
  230 | 		}
  231 | 
  232 | 		@Override
  233 | 		public List<ConstructorResolver> getConstructorResolvers() {
  234 | 			return delegate.getConstructorResolvers();
  235 | 		}
  236 | 
  237 | 		@Override
  238 | 		public List<MethodResolver> getMethodResolvers() {
  239 | 			return delegate.getMethodResolvers();
  240 | 		}
  241 | 
  242 | 		@Override
  243 | 		@Nullable
  244 | 		public BeanResolver getBeanResolver() {
  245 | 			return this.beanFactoryResolver;
  246 | 		}
  247 | 
  248 | 		@Override
  249 | 		public TypeLocator getTypeLocator() {
  250 | 			return delegate.getTypeLocator();
  251 | 		}
  252 | 
  253 | 		@Override
  254 | 		public TypeConverter getTypeConverter() {
  255 | 			return delegate.getTypeConverter();
  256 | 		}
  257 | 
  258 | 		@Override
  259 | 		public TypeComparator getTypeComparator() {
  260 | 			return delegate.getTypeComparator();
  261 | 		}
  262 | 
  263 | 		@Override
  264 | 		public OperatorOverloader getOperatorOverloader() {
  265 | 			return delegate.getOperatorOverloader();
  266 | 		}
  267 | 
  268 | 		@Override
  269 | 		public void setVariable(String name, Object value) {
  270 | 			delegate.setVariable(name, value);
  271 | 		}
  272 | 
  273 | 		@Override
  274 | 		@Nullable
  275 | 		public Object lookupVariable(String name) {
  276 | 			return delegate.lookupVariable(name);
  277 | 		}
  278 | 
  279 | 		@Override
  280 | 		public List<IndexAccessor> getIndexAccessors() {
  281 | 			return delegate.getIndexAccessors();
  282 | 		}
  283 | 
  284 | 		@Override
  285 | 		public TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
  286 | 			return delegate.assignVariable(name, valueSupplier);
  287 | 		}
  288 | 
  289 | 		@Override
  290 | 		public boolean isAssignmentEnabled() {
  291 | 			return delegate.isAssignmentEnabled();
  292 | 		}
  293 | 
  294 | 	}
  295 | 
  296 | 	class RestrictivePropertyAccessor extends ReflectivePropertyAccessor {
  297 | 
  298 | 		@Override
  299 | 		public boolean canRead(EvaluationContext context, Object target, String name) {
  300 | 			return false;
  301 | 		}
  302 | 
  303 | 	}
  304 | 
  305 | }
```
