<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/core/env/PropertySource.java -->
<!-- 正文引用：[[源码:PropertySource.java#L起-L止]] ；代码块头部：(src: PropertySource.java#L起-L止) -->

# PropertySource.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/core/env/PropertySource.java` |
| 行数 | 250 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:12:01.495Z |

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
   17 | package org.springframework.core.env;
   18 | 
   19 | import java.util.Objects;
   20 | 
   21 | import org.apache.commons.logging.Log;
   22 | import org.apache.commons.logging.LogFactory;
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.util.Assert;
   26 | import org.springframework.util.ObjectUtils;
   27 | 
   28 | /**
   29 |  * Abstract base class representing a source of name/value property pairs. The underlying
   30 |  * {@linkplain #getSource() source object} may be of any type {@code T} that encapsulates
   31 |  * properties. Examples include {@link java.util.Properties} objects, {@link java.util.Map}
   32 |  * objects, {@code ServletContext} and {@code ServletConfig} objects (for access to init
   33 |  * parameters). Explore the {@code PropertySource} type hierarchy to see provided
   34 |  * implementations.
   35 |  *
   36 |  * <p>{@code PropertySource} objects are not typically used in isolation, but rather
   37 |  * through a {@link PropertySources} object, which aggregates property sources and in
   38 |  * conjunction with a {@link PropertyResolver} implementation that can perform
   39 |  * precedence-based searches across the set of {@code PropertySources}.
   40 |  *
   41 |  * <p>{@code PropertySource} identity is determined not based on the content of
   42 |  * encapsulated properties, but rather based on the {@link #getName() name} of the
   43 |  * {@code PropertySource} alone. This is useful for manipulating {@code PropertySource}
   44 |  * objects when in collection contexts. See operations in {@link MutablePropertySources}
   45 |  * as well as the {@link #named(String)} and {@link #toString()} methods for details.
   46 |  *
   47 |  * <p>Note that when working with @{@link
   48 |  * org.springframework.context.annotation.Configuration Configuration} classes that
   49 |  * the @{@link org.springframework.context.annotation.PropertySource PropertySource}
   50 |  * annotation provides a convenient and declarative way of adding property sources to the
   51 |  * enclosing {@code Environment}.
   52 |  *
   53 |  * @author Chris Beams
   54 |  * @since 3.1
   55 |  * @param <T> the source type
   56 |  * @see PropertySources
   57 |  * @see PropertyResolver
   58 |  * @see PropertySourcesPropertyResolver
   59 |  * @see MutablePropertySources
   60 |  * @see org.springframework.context.annotation.PropertySource
   61 |  */
   62 | public abstract class PropertySource<T> {
   63 | 
   64 | 	protected final Log logger = LogFactory.getLog(getClass());
   65 | 
   66 | 	protected final String name;
   67 | 
   68 | 	protected final T source;
   69 | 
   70 | 
   71 | 	/**
   72 | 	 * Create a new {@code PropertySource} with the given name and source object.
   73 | 	 * @param name the associated name
   74 | 	 * @param source the source object
   75 | 	 */
   76 | 	public PropertySource(String name, T source) {
   77 | 		Assert.hasText(name, "Property source name must contain at least one character");
   78 | 		Assert.notNull(source, "Property source must not be null");
   79 | 		this.name = name;
   80 | 		this.source = source;
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Create a new {@code PropertySource} with the given name and with a new
   85 | 	 * {@code Object} instance as the underlying source.
   86 | 	 * <p>Often useful in testing scenarios when creating anonymous implementations
   87 | 	 * that never query an actual source but rather return hard-coded values.
   88 | 	 */
   89 | 	@SuppressWarnings("unchecked")
   90 | 	public PropertySource(String name) {
   91 | 		this(name, (T) new Object());
   92 | 	}
   93 | 
   94 | 
   95 | 	/**
   96 | 	 * Return the name of this {@code PropertySource}.
   97 | 	 * <p>See the {@linkplain PropertySource class-level Javadoc} for details
   98 | 	 * on property source identity and names.
   99 | 	 */
  100 | 	public String getName() {
  101 | 		return this.name;
  102 | 	}
  103 | 
  104 | 	/**
  105 | 	 * Return the underlying source object for this {@code PropertySource}.
  106 | 	 */
  107 | 	public T getSource() {
  108 | 		return this.source;
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * Return whether this {@code PropertySource} contains the given name.
  113 | 	 * <p>This implementation simply checks for a {@code null} return value
  114 | 	 * from {@link #getProperty(String)}. Subclasses may wish to implement
  115 | 	 * a more efficient algorithm if possible.
  116 | 	 * @param name the property name to find
  117 | 	 */
  118 | 	public boolean containsProperty(String name) {
  119 | 		return (getProperty(name) != null);
  120 | 	}
  121 | 
  122 | 	/**
  123 | 	 * Return the value associated with the given name,
  124 | 	 * or {@code null} if not found.
  125 | 	 * @param name the property to find
  126 | 	 * @see PropertyResolver#getRequiredProperty(String)
  127 | 	 */
  128 | 	public abstract @Nullable Object getProperty(String name);
  129 | 
  130 | 
  131 | 	/**
  132 | 	 * This {@code PropertySource} object is equal to the given object if:
  133 | 	 * <ul>
  134 | 	 * <li>they are the same instance
  135 | 	 * <li>the {@code name} properties for both objects are equal
  136 | 	 * </ul>
  137 | 	 * <p>No properties other than {@code name} are evaluated.
  138 | 	 */
  139 | 	@Override
  140 | 	public boolean equals(@Nullable Object other) {
  141 | 		return (this == other || (other instanceof PropertySource<?> that &&
  142 | 				ObjectUtils.nullSafeEquals(getName(), that.getName())));
  143 | 	}
  144 | 
  145 | 	/**
  146 | 	 * Return a hash code derived from the {@code name} property
  147 | 	 * of this {@code PropertySource} object.
  148 | 	 */
  149 | 	@Override
  150 | 	public int hashCode() {
  151 | 		return Objects.hashCode(getName());
  152 | 	}
  153 | 
  154 | 	/**
  155 | 	 * Produce concise output consisting of the simple class name and the
  156 | 	 * {@linkplain #getName() name} of this {@code PropertySource}.
  157 | 	 * <p>The {@linkplain #getSource() source object} is not included, since a
  158 | 	 * property source such as system properties or environment variables may
  159 | 	 * contain an arbitrary number of properties, potentially leading to
  160 | 	 * difficulties reading exception and log messages.
  161 | 	 */
  162 | 	@Override
  163 | 	public String toString() {
  164 | 		return getClass().getSimpleName() + " {name='" + getName() + "'}";
  165 | 	}
  166 | 
  167 | 
  168 | 	/**
  169 | 	 * Return a {@code PropertySource} implementation intended for collection
  170 | 	 * comparison purposes only.
  171 | 	 * <p>Primarily for internal use, but given a collection of {@code PropertySource}
  172 | 	 * objects, may be used as follows:
  173 | 	 * <pre class="code">
  174 | 	 * List&lt;PropertySource&lt;?&gt;&gt; sources = new ArrayList&lt;&gt;();
  175 | 	 * sources.add(new MapPropertySource("sourceA", mapA));
  176 | 	 * sources.add(new MapPropertySource("sourceB", mapB));
  177 | 	 * assert sources.contains(PropertySource.named("sourceA"));
  178 | 	 * assert sources.contains(PropertySource.named("sourceB"));
  179 | 	 * assert !sources.contains(PropertySource.named("sourceC"));</pre>
  180 | 	 * <p>The returned {@code PropertySource} will throw {@code UnsupportedOperationException}
  181 | 	 * if any methods other than {@code equals(Object)}, {@code hashCode()}, and {@code toString()}
  182 | 	 * are called.
  183 | 	 * @param name the name of the comparison {@code PropertySource} to be created
  184 | 	 * and returned
  185 | 	 */
  186 | 	public static PropertySource<?> named(String name) {
  187 | 		return new ComparisonPropertySource(name);
  188 | 	}
  189 | 
  190 | 
  191 | 	/**
  192 | 	 * {@code PropertySource} to be used as a placeholder in cases where an actual
  193 | 	 * property source cannot be eagerly initialized at application context
  194 | 	 * creation time.  For example, a {@code ServletContext}-based property source
  195 | 	 * must wait until the {@code ServletContext} object is available to its enclosing
  196 | 	 * {@code ApplicationContext}.  In such cases, a stub should be used to hold the
  197 | 	 * intended default position/order of the property source, then be replaced
  198 | 	 * during context refresh.
  199 | 	 * @see org.springframework.context.support.AbstractApplicationContext#initPropertySources()
  200 | 	 * @see org.springframework.web.context.support.StandardServletEnvironment
  201 | 	 * @see org.springframework.web.context.support.ServletContextPropertySource
  202 | 	 */
  203 | 	public static class StubPropertySource extends PropertySource<Object> {
  204 | 
  205 | 		public StubPropertySource(String name) {
  206 | 			super(name);
  207 | 		}
  208 | 
  209 | 		/**
  210 | 		 * Always returns {@code null}.
  211 | 		 */
  212 | 		@Override
  213 | 		public @Nullable String getProperty(String name) {
  214 | 			return null;
  215 | 		}
  216 | 	}
  217 | 
  218 | 
  219 | 	/**
  220 | 	 * A {@code PropertySource} implementation intended for collection comparison
  221 | 	 * purposes.
  222 | 	 *
  223 | 	 * @see PropertySource#named(String)
  224 | 	 */
  225 | 	static class ComparisonPropertySource extends StubPropertySource {
  226 | 
  227 | 		private static final String USAGE_ERROR =
  228 | 				"ComparisonPropertySource instances are for use with collection comparison only";
  229 | 
  230 | 		public ComparisonPropertySource(String name) {
  231 | 			super(name);
  232 | 		}
  233 | 
  234 | 		@Override
  235 | 		public Object getSource() {
  236 | 			throw new UnsupportedOperationException(USAGE_ERROR);
  237 | 		}
  238 | 
  239 | 		@Override
  240 | 		public boolean containsProperty(String name) {
  241 | 			throw new UnsupportedOperationException(USAGE_ERROR);
  242 | 		}
  243 | 
  244 | 		@Override
  245 | 		public @Nullable String getProperty(String name) {
  246 | 			throw new UnsupportedOperationException(USAGE_ERROR);
  247 | 		}
  248 | 	}
  249 | 
  250 | }
```
