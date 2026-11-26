<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/bind/Bindable.java -->
<!-- 正文引用：[[源码:Bindable.java#L起-L止]] ；代码块头部：(src: Bindable.java#L起-L止) -->

# Bindable.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/bind/Bindable.java` |
| 行数 | 335 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:53.019Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.bind;
    6 | 
    7 | import java.lang.annotation.Annotation;
    8 | import java.lang.reflect.Array;
    9 | import java.util.Arrays;
   10 | import java.util.EnumSet;
   11 | import java.util.List;
   12 | import java.util.Map;
   13 | import java.util.Set;
   14 | import java.util.function.Supplier;
   15 | 
   16 | import org.jspecify.annotations.Nullable;
   17 | 
   18 | import org.springframework.boot.context.properties.source.ConfigurationProperty;
   19 | import org.springframework.core.ResolvableType;
   20 | import org.springframework.core.style.ToStringCreator;
   21 | import org.springframework.util.Assert;
   22 | import org.springframework.util.ObjectUtils;
   23 | 
   24 | /**
   25 |  * Source that can be bound by a {@link Binder}.
   26 |  *
   27 |  * @param <T> the source type
   28 |  * @author Phillip Webb
   29 |  * @author Madhura Bhave
   30 |  * @since 2.0.0
   31 |  * @see Bindable#of(Class)
   32 |  * @see Bindable#of(ResolvableType)
   33 |  */
   34 | public final class Bindable<T> {
   35 | 
   36 | 	private static final Annotation[] NO_ANNOTATIONS = {};
   37 | 
   38 | 	private static final EnumSet<BindRestriction> NO_BIND_RESTRICTIONS = EnumSet.noneOf(BindRestriction.class);
   39 | 
   40 | 	private final ResolvableType type;
   41 | 
   42 | 	private final ResolvableType boxedType;
   43 | 
   44 | 	private final @Nullable Supplier<T> value;
   45 | 
   46 | 	private final Annotation[] annotations;
   47 | 
   48 | 	private final EnumSet<BindRestriction> bindRestrictions;
   49 | 
   50 | 	private final @Nullable BindMethod bindMethod;
   51 | 
   52 | 	private Bindable(ResolvableType type, ResolvableType boxedType, @Nullable Supplier<T> value,
   53 | 			Annotation[] annotations, EnumSet<BindRestriction> bindRestrictions, @Nullable BindMethod bindMethod) {
   54 | 		this.type = type;
   55 | 		this.boxedType = boxedType;
   56 | 		this.value = value;
   57 | 		this.annotations = annotations;
   58 | 		this.bindRestrictions = bindRestrictions;
   59 | 		this.bindMethod = bindMethod;
   60 | 	}
   61 | 
   62 | 	/**
   63 | 	 * Return the type of the item to bind.
   64 | 	 * @return the type being bound
   65 | 	 */
   66 | 	public ResolvableType getType() {
   67 | 		return this.type;
   68 | 	}
   69 | 
   70 | 	/**
   71 | 	 * Return the boxed type of the item to bind.
   72 | 	 * @return the boxed type for the item being bound
   73 | 	 */
   74 | 	public ResolvableType getBoxedType() {
   75 | 		return this.boxedType;
   76 | 	}
   77 | 
   78 | 	/**
   79 | 	 * Return a supplier that provides the object value or {@code null}.
   80 | 	 * @return the value or {@code null}
   81 | 	 */
   82 | 	public @Nullable Supplier<T> getValue() {
   83 | 		return this.value;
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Return any associated annotations that could affect binding.
   88 | 	 * @return the associated annotations
   89 | 	 */
   90 | 	public Annotation[] getAnnotations() {
   91 | 		return this.annotations;
   92 | 	}
   93 | 
   94 | 	/**
   95 | 	 * Return a single associated annotations that could affect binding.
   96 | 	 * @param <A> the annotation type
   97 | 	 * @param type annotation type
   98 | 	 * @return the associated annotation or {@code null}
   99 | 	 */
  100 | 	@SuppressWarnings("unchecked")
  101 | 	public <A extends Annotation> @Nullable A getAnnotation(Class<A> type) {
  102 | 		for (Annotation annotation : this.annotations) {
  103 | 			if (type.isInstance(annotation)) {
  104 | 				return (A) annotation;
  105 | 			}
  106 | 		}
  107 | 		return null;
  108 | 	}
  109 | 
  110 | 	/**
  111 | 	 * Returns {@code true} if the specified bind restriction has been added.
  112 | 	 * @param bindRestriction the bind restriction to check
  113 | 	 * @return if the bind restriction has been added
  114 | 	 * @since 2.5.0
  115 | 	 */
  116 | 	public boolean hasBindRestriction(BindRestriction bindRestriction) {
  117 | 		return this.bindRestrictions.contains(bindRestriction);
  118 | 	}
  119 | 
  120 | 	/**
  121 | 	 * Returns the {@link BindMethod method} to be used to bind this bindable, or
  122 | 	 * {@code null} if no specific binding method is required.
  123 | 	 * @return the bind method or {@code null}
  124 | 	 * @since 3.0.8
  125 | 	 */
  126 | 	public @Nullable BindMethod getBindMethod() {
  127 | 		return this.bindMethod;
  128 | 	}
  129 | 
  130 | 	@Override
  131 | 	public boolean equals(Object obj) {
  132 | 		if (this == obj) {
  133 | 			return true;
  134 | 		}
  135 | 		if (obj == null || getClass() != obj.getClass()) {
  136 | 			return false;
  137 | 		}
  138 | 		Bindable<?> other = (Bindable<?>) obj;
  139 | 		boolean result = true;
  140 | 		result = result && nullSafeEquals(this.type.resolve(), other.type.resolve());
  141 | 		result = result && nullSafeEquals(this.annotations, other.annotations);
  142 | 		result = result && nullSafeEquals(this.bindRestrictions, other.bindRestrictions);
  143 | 		result = result && nullSafeEquals(this.bindMethod, other.bindMethod);
  144 | 		return result;
  145 | 	}
  146 | 
  147 | 	@Override
  148 | 	public int hashCode() {
  149 | 		return ObjectUtils.nullSafeHash(this.type, this.annotations, this.bindRestrictions, this.bindMethod);
  150 | 	}
  151 | 
  152 | 	@Override
  153 | 	public String toString() {
  154 | 		ToStringCreator creator = new ToStringCreator(this);
  155 | 		creator.append("type", this.type);
  156 | 		creator.append("value", (this.value != null) ? "provided" : "none");
  157 | 		creator.append("annotations", this.annotations);
  158 | 		creator.append("bindMethod", this.bindMethod);
  159 | 		return creator.toString();
  160 | 	}
  161 | 
  162 | 	private boolean nullSafeEquals(@Nullable Object o1, @Nullable Object o2) {
  163 | 		return ObjectUtils.nullSafeEquals(o1, o2);
  164 | 	}
  165 | 
  166 | 	/**
  167 | 	 * Create an updated {@link Bindable} instance with the specified annotations.
  168 | 	 * @param annotations the annotations
  169 | 	 * @return an updated {@link Bindable}
  170 | 	 */
  171 | 	public Bindable<T> withAnnotations(Annotation @Nullable ... annotations) {
  172 | 		return new Bindable<>(this.type, this.boxedType, this.value,
  173 | 				(annotations != null) ? annotations : NO_ANNOTATIONS, NO_BIND_RESTRICTIONS, this.bindMethod);
  174 | 	}
  175 | 
  176 | 	/**
  177 | 	 * Create an updated {@link Bindable} instance with an existing value. Implies that
  178 | 	 * Java Bean binding will be used.
  179 | 	 * @param existingValue the existing value
  180 | 	 * @return an updated {@link Bindable}
  181 | 	 */
  182 | 	public Bindable<T> withExistingValue(@Nullable T existingValue) {
  183 | 		Assert.isTrue(existingValue == null || this.type.isArray() || boxedTypeIsInstanceOf(existingValue),
  184 | 				() -> "'existingValue' must be an instance of " + this.type);
  185 | 		Assert.state(this.bindMethod != BindMethod.VALUE_OBJECT,
  186 | 				() -> "An existing value cannot be provided when binding as a value object");
  187 | 		Supplier<T> value = (existingValue != null) ? () -> existingValue : null;
  188 | 		return new Bindable<>(this.type, this.boxedType, value, this.annotations, this.bindRestrictions,
  189 | 				BindMethod.JAVA_BEAN);
  190 | 	}
  191 | 
  192 | 	private boolean boxedTypeIsInstanceOf(T existingValue) {
  193 | 		Class<?> resolved = this.boxedType.resolve();
  194 | 		return resolved != null && resolved.isInstance(existingValue);
  195 | 	}
  196 | 
  197 | 	/**
  198 | 	 * Create an updated {@link Bindable} instance with a value supplier.
  199 | 	 * @param suppliedValue the supplier for the value
  200 | 	 * @return an updated {@link Bindable}
  201 | 	 */
  202 | 	public Bindable<T> withSuppliedValue(@Nullable Supplier<T> suppliedValue) {
  203 | 		return new Bindable<>(this.type, this.boxedType, suppliedValue, this.annotations, this.bindRestrictions,
  204 | 				this.bindMethod);
  205 | 	}
  206 | 
  207 | 	/**
  208 | 	 * Create an updated {@link Bindable} instance with additional bind restrictions.
  209 | 	 * @param additionalRestrictions any additional restrictions to apply
  210 | 	 * @return an updated {@link Bindable}
  211 | 	 * @since 2.5.0
  212 | 	 */
  213 | 	public Bindable<T> withBindRestrictions(BindRestriction... additionalRestrictions) {
  214 | 		EnumSet<BindRestriction> bindRestrictions = EnumSet.copyOf(this.bindRestrictions);
  215 | 		bindRestrictions.addAll(Arrays.asList(additionalRestrictions));
  216 | 		return new Bindable<>(this.type, this.boxedType, this.value, this.annotations, bindRestrictions,
  217 | 				this.bindMethod);
  218 | 	}
  219 | 
  220 | 	/**
  221 | 	 * Create an updated {@link Bindable} instance with a specific bind method. To use
  222 | 	 * {@link BindMethod#VALUE_OBJECT value object binding}, the current instance must not
  223 | 	 * have an existing or supplied value.
  224 | 	 * @param bindMethod the method to use to bind the bindable
  225 | 	 * @return an updated {@link Bindable}
  226 | 	 * @since 3.0.8
  227 | 	 */
  228 | 	public Bindable<T> withBindMethod(@Nullable BindMethod bindMethod) {
  229 | 		Assert.state(bindMethod != BindMethod.VALUE_OBJECT || this.value == null,
  230 | 				() -> "Value object binding cannot be used with an existing or supplied value");
  231 | 		return new Bindable<>(this.type, this.boxedType, this.value, this.annotations, this.bindRestrictions,
  232 | 				bindMethod);
  233 | 	}
  234 | 
  235 | 	/**
  236 | 	 * Create a new {@link Bindable} of the type of the specified instance with an
  237 | 	 * existing value equal to the instance.
  238 | 	 * @param <T> the source type
  239 | 	 * @param instance the instance (must not be {@code null})
  240 | 	 * @return a {@link Bindable} instance
  241 | 	 * @see #of(ResolvableType)
  242 | 	 * @see #withExistingValue(Object)
  243 | 	 */
  244 | 	@SuppressWarnings("unchecked")
  245 | 	public static <T> Bindable<T> ofInstance(T instance) {
  246 | 		Assert.notNull(instance, "'instance' must not be null");
  247 | 		Class<T> type = (Class<T>) instance.getClass();
  248 | 		return of(type).withExistingValue(instance);
  249 | 	}
  250 | 
  251 | 	/**
  252 | 	 * Create a new {@link Bindable} of the specified type.
  253 | 	 * @param <T> the source type
  254 | 	 * @param type the type (must not be {@code null})
  255 | 	 * @return a {@link Bindable} instance
  256 | 	 * @see #of(ResolvableType)
  257 | 	 */
  258 | 	public static <T> Bindable<T> of(Class<T> type) {
  259 | 		Assert.notNull(type, "'type' must not be null");
  260 | 		return of(ResolvableType.forClass(type));
  261 | 	}
  262 | 
  263 | 	/**
  264 | 	 * Create a new {@link Bindable} {@link List} of the specified element type.
  265 | 	 * @param <E> the element type
  266 | 	 * @param elementType the list element type
  267 | 	 * @return a {@link Bindable} instance
  268 | 	 */
  269 | 	public static <E> Bindable<List<E>> listOf(Class<E> elementType) {
  270 | 		return of(ResolvableType.forClassWithGenerics(List.class, elementType));
  271 | 	}
  272 | 
  273 | 	/**
  274 | 	 * Create a new {@link Bindable} {@link Set} of the specified element type.
  275 | 	 * @param <E> the element type
  276 | 	 * @param elementType the set element type
  277 | 	 * @return a {@link Bindable} instance
  278 | 	 */
  279 | 	public static <E> Bindable<Set<E>> setOf(Class<E> elementType) {
  280 | 		return of(ResolvableType.forClassWithGenerics(Set.class, elementType));
  281 | 	}
  282 | 
  283 | 	/**
  284 | 	 * Create a new {@link Bindable} {@link Map} of the specified key and value type.
  285 | 	 * @param <K> the key type
  286 | 	 * @param <V> the value type
  287 | 	 * @param keyType the map key type
  288 | 	 * @param valueType the map value type
  289 | 	 * @return a {@link Bindable} instance
  290 | 	 */
  291 | 	public static <K, V> Bindable<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
  292 | 		return of(ResolvableType.forClassWithGenerics(Map.class, keyType, valueType));
  293 | 	}
  294 | 
  295 | 	/**
  296 | 	 * Create a new {@link Bindable} of the specified type.
  297 | 	 * @param <T> the source type
  298 | 	 * @param type the type (must not be {@code null})
  299 | 	 * @return a {@link Bindable} instance
  300 | 	 * @see #of(Class)
  301 | 	 */
  302 | 	public static <T> Bindable<T> of(ResolvableType type) {
  303 | 		Assert.notNull(type, "'type' must not be null");
  304 | 		ResolvableType boxedType = box(type);
  305 | 		return new Bindable<>(type, boxedType, null, NO_ANNOTATIONS, NO_BIND_RESTRICTIONS, null);
  306 | 	}
  307 | 
  308 | 	private static ResolvableType box(ResolvableType type) {
  309 | 		Class<?> resolved = type.resolve();
  310 | 		if (resolved != null && resolved.isPrimitive()) {
  311 | 			Object array = Array.newInstance(resolved, 1);
  312 | 			Class<?> wrapperType = Array.get(array, 0).getClass();
  313 | 			return ResolvableType.forClass(wrapperType);
  314 | 		}
  315 | 		if (resolved != null && resolved.isArray()) {
  316 | 			return ResolvableType.forArrayComponent(box(type.getComponentType()));
  317 | 		}
  318 | 		return type;
  319 | 	}
  320 | 
  321 | 	/**
  322 | 	 * Restrictions that can be applied when binding values.
  323 | 	 *
  324 | 	 * @since 2.5.0
  325 | 	 */
  326 | 	public enum BindRestriction {
  327 | 
  328 | 		/**
  329 | 		 * Do not bind direct {@link ConfigurationProperty} matches.
  330 | 		 */
  331 | 		NO_DIRECT_PROPERTY
  332 | 
  333 | 	}
  334 | 
  335 | }
```
