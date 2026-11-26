<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/core/env/MutablePropertySources.java -->
<!-- 正文引用：[[源码:MutablePropertySources.java#L起-L止]] ；代码块头部：(src: MutablePropertySources.java#L起-L止) -->

# MutablePropertySources.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/core/env/MutablePropertySources.java` |
| 行数 | 230 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:12:01.380Z |

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
   19 | import java.util.Iterator;
   20 | import java.util.List;
   21 | import java.util.Spliterator;
   22 | import java.util.concurrent.CopyOnWriteArrayList;
   23 | import java.util.stream.Stream;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | /**
   28 |  * The default implementation of the {@link PropertySources} interface.
   29 |  * Allows manipulation of contained property sources and provides a constructor
   30 |  * for copying an existing {@code PropertySources} instance.
   31 |  *
   32 |  * <p>Where <em>precedence</em> is mentioned in methods such as {@link #addFirst}
   33 |  * and {@link #addLast}, this is with regard to the order in which property sources
   34 |  * will be searched when resolving a given property with a {@link PropertyResolver}.
   35 |  *
   36 |  * @author Chris Beams
   37 |  * @author Juergen Hoeller
   38 |  * @since 3.1
   39 |  * @see PropertySourcesPropertyResolver
   40 |  */
   41 | public class MutablePropertySources implements PropertySources {
   42 | 
   43 | 	private final List<PropertySource<?>> propertySourceList = new CopyOnWriteArrayList<>();
   44 | 
   45 | 
   46 | 	/**
   47 | 	 * Create a new {@link MutablePropertySources} object.
   48 | 	 */
   49 | 	public MutablePropertySources() {
   50 | 	}
   51 | 
   52 | 	/**
   53 | 	 * Create a new {@code MutablePropertySources} from the given propertySources
   54 | 	 * object, preserving the original order of contained {@code PropertySource} objects.
   55 | 	 */
   56 | 	public MutablePropertySources(PropertySources propertySources) {
   57 | 		this();
   58 | 		for (PropertySource<?> propertySource : propertySources) {
   59 | 			addLast(propertySource);
   60 | 		}
   61 | 	}
   62 | 
   63 | 
   64 | 	@Override
   65 | 	public Iterator<PropertySource<?>> iterator() {
   66 | 		return this.propertySourceList.iterator();
   67 | 	}
   68 | 
   69 | 	@Override
   70 | 	public Spliterator<PropertySource<?>> spliterator() {
   71 | 		return this.propertySourceList.spliterator();
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public Stream<PropertySource<?>> stream() {
   76 | 		return this.propertySourceList.stream();
   77 | 	}
   78 | 
   79 | 	@Override
   80 | 	public boolean contains(String name) {
   81 | 		for (PropertySource<?> propertySource : this.propertySourceList) {
   82 | 			if (propertySource.getName().equals(name)) {
   83 | 				return true;
   84 | 			}
   85 | 		}
   86 | 		return false;
   87 | 	}
   88 | 
   89 | 	@Override
   90 | 	public @Nullable PropertySource<?> get(String name) {
   91 | 		for (PropertySource<?> propertySource : this.propertySourceList) {
   92 | 			if (propertySource.getName().equals(name)) {
   93 | 				return propertySource;
   94 | 			}
   95 | 		}
   96 | 		return null;
   97 | 	}
   98 | 
   99 | 
  100 | 	/**
  101 | 	 * Add the given property source object with the highest precedence.
  102 | 	 */
  103 | 	public void addFirst(PropertySource<?> propertySource) {
  104 | 		synchronized (this.propertySourceList) {
  105 | 			removeIfPresent(propertySource);
  106 | 			this.propertySourceList.add(0, propertySource);
  107 | 		}
  108 | 	}
  109 | 
  110 | 	/**
  111 | 	 * Add the given property source object with the lowest precedence.
  112 | 	 */
  113 | 	public void addLast(PropertySource<?> propertySource) {
  114 | 		synchronized (this.propertySourceList) {
  115 | 			removeIfPresent(propertySource);
  116 | 			this.propertySourceList.add(propertySource);
  117 | 		}
  118 | 	}
  119 | 
  120 | 	/**
  121 | 	 * Add the given property source object with precedence immediately higher
  122 | 	 * than the named relative property source.
  123 | 	 */
  124 | 	public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
  125 | 		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
  126 | 		synchronized (this.propertySourceList) {
  127 | 			removeIfPresent(propertySource);
  128 | 			int index = assertPresentAndGetIndex(relativePropertySourceName);
  129 | 			addAtIndex(index, propertySource);
  130 | 		}
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Add the given property source object with precedence immediately lower
  135 | 	 * than the named relative property source.
  136 | 	 */
  137 | 	public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
  138 | 		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
  139 | 		synchronized (this.propertySourceList) {
  140 | 			removeIfPresent(propertySource);
  141 | 			int index = assertPresentAndGetIndex(relativePropertySourceName);
  142 | 			addAtIndex(index + 1, propertySource);
  143 | 		}
  144 | 	}
  145 | 
  146 | 	/**
  147 | 	 * Return the precedence of the given property source, {@code -1} if not found.
  148 | 	 */
  149 | 	public int precedenceOf(PropertySource<?> propertySource) {
  150 | 		return this.propertySourceList.indexOf(propertySource);
  151 | 	}
  152 | 
  153 | 	/**
  154 | 	 * Remove and return the property source with the given name, {@code null} if not found.
  155 | 	 * @param name the name of the property source to find and remove
  156 | 	 */
  157 | 	public @Nullable PropertySource<?> remove(String name) {
  158 | 		synchronized (this.propertySourceList) {
  159 | 			int index = this.propertySourceList.indexOf(PropertySource.named(name));
  160 | 			return (index != -1 ? this.propertySourceList.remove(index) : null);
  161 | 		}
  162 | 	}
  163 | 
  164 | 	/**
  165 | 	 * Replace the property source with the given name with the given property source object.
  166 | 	 * @param name the name of the property source to find and replace
  167 | 	 * @param propertySource the replacement property source
  168 | 	 * @throws IllegalArgumentException if no property source with the given name is present
  169 | 	 * @see #contains
  170 | 	 */
  171 | 	public void replace(String name, PropertySource<?> propertySource) {
  172 | 		synchronized (this.propertySourceList) {
  173 | 			int index = assertPresentAndGetIndex(name);
  174 | 			this.propertySourceList.set(index, propertySource);
  175 | 		}
  176 | 	}
  177 | 
  178 | 	/**
  179 | 	 * Return the number of {@link PropertySource} objects contained.
  180 | 	 */
  181 | 	public int size() {
  182 | 		return this.propertySourceList.size();
  183 | 	}
  184 | 
  185 | 	@Override
  186 | 	public String toString() {
  187 | 		return this.propertySourceList.toString();
  188 | 	}
  189 | 
  190 | 
  191 | 	/**
  192 | 	 * Ensure that the given property source is not being added relative to itself.
  193 | 	 */
  194 | 	protected void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
  195 | 		String newPropertySourceName = propertySource.getName();
  196 | 		if (relativePropertySourceName.equals(newPropertySourceName)) {
  197 | 			throw new IllegalArgumentException(
  198 | 					"PropertySource named '" + newPropertySourceName + "' cannot be added relative to itself");
  199 | 		}
  200 | 	}
  201 | 
  202 | 	/**
  203 | 	 * Remove the given property source if it is present.
  204 | 	 */
  205 | 	protected void removeIfPresent(PropertySource<?> propertySource) {
  206 | 		this.propertySourceList.remove(propertySource);
  207 | 	}
  208 | 
  209 | 	/**
  210 | 	 * Add the given property source at a particular index in the list.
  211 | 	 */
  212 | 	private void addAtIndex(int index, PropertySource<?> propertySource) {
  213 | 		removeIfPresent(propertySource);
  214 | 		this.propertySourceList.add(index, propertySource);
  215 | 	}
  216 | 
  217 | 	/**
  218 | 	 * Assert that the named property source is present and return its index.
  219 | 	 * @param name {@linkplain PropertySource#getName() name of the property source} to find
  220 | 	 * @throws IllegalArgumentException if the named property source is not present
  221 | 	 */
  222 | 	private int assertPresentAndGetIndex(String name) {
  223 | 		int index = this.propertySourceList.indexOf(PropertySource.named(name));
  224 | 		if (index == -1) {
  225 | 			throw new IllegalArgumentException("PropertySource named '" + name + "' does not exist");
  226 | 		}
  227 | 		return index;
  228 | 	}
  229 | 
  230 | }
```
