<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/source/ConfigurationPropertyName.java -->
<!-- 正文引用：[[源码:ConfigurationPropertyName.java#L起-L止]] ；代码块头部：(src: ConfigurationPropertyName.java#L起-L止) -->

# ConfigurationPropertyName.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/source/ConfigurationPropertyName.java` |
| 行数 | 1210 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:54.238Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.source;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Collection;
    9 | import java.util.Collections;
   10 | import java.util.List;
   11 | import java.util.Locale;
   12 | import java.util.Map;
   13 | import java.util.function.Function;
   14 | import java.util.function.IntFunction;
   15 | 
   16 | import org.jspecify.annotations.Nullable;
   17 | 
   18 | import org.springframework.lang.Contract;
   19 | import org.springframework.util.Assert;
   20 | import org.springframework.util.StringUtils;
   21 | 
   22 | /**
   23 |  * A configuration property name composed of elements separated by dots. User created
   24 |  * names may contain the characters "{@code a-z}" "{@code 0-9}" and "{@code -}", they must
   25 |  * be lower-case and must start with an alphanumeric character. The "{@code -}" is used
   26 |  * purely for formatting, i.e. "{@code foo-bar}" and "{@code foobar}" are considered
   27 |  * equivalent.
   28 |  * <p>
   29 |  * The "{@code [}" and "{@code ]}" characters may be used to indicate an associative
   30 |  * index(i.e. a {@link Map} key or a {@link Collection} index). Indexes names are not
   31 |  * restricted and are considered case-sensitive.
   32 |  * <p>
   33 |  * Here are some typical examples:
   34 |  * <ul>
   35 |  * <li>{@code spring.main.banner-mode}</li>
   36 |  * <li>{@code server.hosts[0].name}</li>
   37 |  * <li>{@code log[org.springboot].level}</li>
   38 |  * </ul>
   39 |  *
   40 |  * @author Phillip Webb
   41 |  * @author Madhura Bhave
   42 |  * @since 2.0.0
   43 |  * @see #of(CharSequence)
   44 |  * @see ConfigurationPropertySource
   45 |  */
   46 | public final class ConfigurationPropertyName implements Comparable<ConfigurationPropertyName> {
   47 | 
   48 | 	private static final String EMPTY_STRING = "";
   49 | 
   50 | 	/**
   51 | 	 * An empty {@link ConfigurationPropertyName}.
   52 | 	 */
   53 | 	public static final ConfigurationPropertyName EMPTY = new ConfigurationPropertyName(Elements.EMPTY);
   54 | 
   55 | 	private final Elements elements;
   56 | 
   57 | 	private final @Nullable CharSequence[] uniformElements;
   58 | 
   59 | 	private int hashCode;
   60 | 
   61 | 	private @Nullable String[] string = new String[ToStringFormat.values().length];
   62 | 
   63 | 	private @Nullable Boolean hasDashedElement;
   64 | 
   65 | 	private @Nullable ConfigurationPropertyName systemEnvironmentLegacyName;
   66 | 
   67 | 	private ConfigurationPropertyName(Elements elements) {
   68 | 		this.elements = elements;
   69 | 		this.uniformElements = new CharSequence[elements.getSize()];
   70 | 	}
   71 | 
   72 | 	/**
   73 | 	 * Returns {@code true} if this {@link ConfigurationPropertyName} is empty.
   74 | 	 * @return {@code true} if the name is empty
   75 | 	 */
   76 | 	public boolean isEmpty() {
   77 | 		return this.elements.getSize() == 0;
   78 | 	}
   79 | 
   80 | 	/**
   81 | 	 * Return if the last element in the name is indexed.
   82 | 	 * @return {@code true} if the last element is indexed
   83 | 	 */
   84 | 	public boolean isLastElementIndexed() {
   85 | 		int size = getNumberOfElements();
   86 | 		return (size > 0 && isIndexed(size - 1));
   87 | 	}
   88 | 
   89 | 	/**
   90 | 	 * Return {@code true} if any element in the name is indexed.
   91 | 	 * @return if the element has one or more indexed elements
   92 | 	 * @since 2.2.10
   93 | 	 */
   94 | 	public boolean hasIndexedElement() {
   95 | 		for (int i = 0; i < getNumberOfElements(); i++) {
   96 | 			if (isIndexed(i)) {
   97 | 				return true;
   98 | 			}
   99 | 		}
  100 | 		return false;
  101 | 	}
  102 | 
  103 | 	/**
  104 | 	 * Return if the element in the name is indexed.
  105 | 	 * @param elementIndex the index of the element
  106 | 	 * @return {@code true} if the element is indexed
  107 | 	 */
  108 | 	boolean isIndexed(int elementIndex) {
  109 | 		return this.elements.getType(elementIndex).isIndexed();
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Return if the element in the name is indexed and numeric.
  114 | 	 * @param elementIndex the index of the element
  115 | 	 * @return {@code true} if the element is indexed and numeric
  116 | 	 */
  117 | 	public boolean isNumericIndex(int elementIndex) {
  118 | 		return this.elements.getType(elementIndex) == ElementType.NUMERICALLY_INDEXED;
  119 | 	}
  120 | 
  121 | 	/**
  122 | 	 * Return the last element in the name in the given form.
  123 | 	 * @param form the form to return
  124 | 	 * @return the last element
  125 | 	 */
  126 | 	public String getLastElement(Form form) {
  127 | 		int size = getNumberOfElements();
  128 | 		return (size != 0) ? getElement(size - 1, form) : EMPTY_STRING;
  129 | 	}
  130 | 
  131 | 	/**
  132 | 	 * Return an element in the name in the given form.
  133 | 	 * @param elementIndex the element index
  134 | 	 * @param form the form to return
  135 | 	 * @return the last element
  136 | 	 */
  137 | 	public String getElement(int elementIndex, Form form) {
  138 | 		CharSequence element = this.elements.get(elementIndex);
  139 | 		ElementType type = this.elements.getType(elementIndex);
  140 | 		if (type.isIndexed()) {
  141 | 			return element.toString();
  142 | 		}
  143 | 		if (form == Form.ORIGINAL) {
  144 | 			if (type != ElementType.NON_UNIFORM) {
  145 | 				return element.toString();
  146 | 			}
  147 | 			return convertToOriginalForm(element).toString();
  148 | 		}
  149 | 		if (form == Form.DASHED) {
  150 | 			if (type == ElementType.UNIFORM || type == ElementType.DASHED) {
  151 | 				return element.toString();
  152 | 			}
  153 | 			return convertToDashedElement(element).toString();
  154 | 		}
  155 | 		CharSequence uniformElement = this.uniformElements[elementIndex];
  156 | 		if (uniformElement == null) {
  157 | 			uniformElement = (type != ElementType.UNIFORM) ? convertToUniformElement(element) : element;
  158 | 			this.uniformElements[elementIndex] = uniformElement.toString();
  159 | 		}
  160 | 		return uniformElement.toString();
  161 | 	}
  162 | 
  163 | 	private CharSequence convertToOriginalForm(CharSequence element) {
  164 | 		return convertElement(element, false,
  165 | 				(ch, i) -> ch == '_' || ElementsParser.isValidChar(Character.toLowerCase(ch), i));
  166 | 	}
  167 | 
  168 | 	private CharSequence convertToDashedElement(CharSequence element) {
  169 | 		return convertElement(element, true, ElementsParser::isValidChar);
  170 | 	}
  171 | 
  172 | 	private CharSequence convertToUniformElement(CharSequence element) {
  173 | 		return convertElement(element, true, (ch, i) -> ElementsParser.isAlphaNumeric(ch));
  174 | 	}
  175 | 
  176 | 	private CharSequence convertElement(CharSequence element, boolean lowercase, ElementCharPredicate filter) {
  177 | 		StringBuilder result = new StringBuilder(element.length());
  178 | 		for (int i = 0; i < element.length(); i++) {
  179 | 			char ch = lowercase ? Character.toLowerCase(element.charAt(i)) : element.charAt(i);
  180 | 			if (filter.test(ch, i)) {
  181 | 				result.append(ch);
  182 | 			}
  183 | 		}
  184 | 		return result;
  185 | 	}
  186 | 
  187 | 	/**
  188 | 	 * Return the total number of elements in the name.
  189 | 	 * @return the number of elements
  190 | 	 */
  191 | 	public int getNumberOfElements() {
  192 | 		return this.elements.getSize();
  193 | 	}
  194 | 
  195 | 	/**
  196 | 	 * Create a new {@link ConfigurationPropertyName} by appending the given suffix.
  197 | 	 * @param suffix the elements to append
  198 | 	 * @return a new {@link ConfigurationPropertyName}
  199 | 	 * @throws InvalidConfigurationPropertyNameException if the result is not valid
  200 | 	 */
  201 | 	public ConfigurationPropertyName append(@Nullable String suffix) {
  202 | 		if (!StringUtils.hasLength(suffix)) {
  203 | 			return this;
  204 | 		}
  205 | 		Elements additionalElements = probablySingleElementOf(suffix);
  206 | 		return new ConfigurationPropertyName(this.elements.append(additionalElements));
  207 | 	}
  208 | 
  209 | 	/**
  210 | 	 * Create a new {@link ConfigurationPropertyName} by appending the given suffix.
  211 | 	 * @param suffix the elements to append
  212 | 	 * @return a new {@link ConfigurationPropertyName}
  213 | 	 * @since 2.5.0
  214 | 	 */
  215 | 	public ConfigurationPropertyName append(@Nullable ConfigurationPropertyName suffix) {
  216 | 		if (suffix == null) {
  217 | 			return this;
  218 | 		}
  219 | 		return new ConfigurationPropertyName(this.elements.append(suffix.elements));
  220 | 	}
  221 | 
  222 | 	/**
  223 | 	 * Return the parent of this {@link ConfigurationPropertyName} or
  224 | 	 * {@link ConfigurationPropertyName#EMPTY} if there is no parent.
  225 | 	 * @return the parent name
  226 | 	 */
  227 | 	public ConfigurationPropertyName getParent() {
  228 | 		int numberOfElements = getNumberOfElements();
  229 | 		return (numberOfElements <= 1) ? EMPTY : chop(numberOfElements - 1);
  230 | 	}
  231 | 
  232 | 	/**
  233 | 	 * Return a new {@link ConfigurationPropertyName} by chopping this name to the given
  234 | 	 * {@code size}. For example, {@code chop(1)} on the name {@code foo.bar} will return
  235 | 	 * {@code foo}.
  236 | 	 * @param size the size to chop
  237 | 	 * @return the chopped name
  238 | 	 */
  239 | 	public ConfigurationPropertyName chop(int size) {
  240 | 		if (size >= getNumberOfElements()) {
  241 | 			return this;
  242 | 		}
  243 | 		return new ConfigurationPropertyName(this.elements.chop(size));
  244 | 	}
  245 | 
  246 | 	/**
  247 | 	 * Return a new {@link ConfigurationPropertyName} by based on this name offset by
  248 | 	 * specific element index. For example, {@code subName(1)} on the name {@code foo.bar}
  249 | 	 * will return {@code bar}.
  250 | 	 * @param offset the element offset
  251 | 	 * @return the sub name
  252 | 	 * @since 2.5.0
  253 | 	 */
  254 | 	public ConfigurationPropertyName subName(int offset) {
  255 | 		if (offset == 0) {
  256 | 			return this;
  257 | 		}
  258 | 		if (offset == getNumberOfElements()) {
  259 | 			return EMPTY;
  260 | 		}
  261 | 		if (offset < 0 || offset > getNumberOfElements()) {
  262 | 			throw new IndexOutOfBoundsException("Offset: " + offset + ", NumberOfElements: " + getNumberOfElements());
  263 | 		}
  264 | 		return new ConfigurationPropertyName(this.elements.subElements(offset));
  265 | 	}
  266 | 
  267 | 	/**
  268 | 	 * Returns {@code true} if this element is an immediate parent of the specified name.
  269 | 	 * @param name the name to check
  270 | 	 * @return {@code true} if this name is an ancestor
  271 | 	 */
  272 | 	public boolean isParentOf(ConfigurationPropertyName name) {
  273 | 		Assert.notNull(name, "'name' must not be null");
  274 | 		if (getNumberOfElements() != name.getNumberOfElements() - 1) {
  275 | 			return false;
  276 | 		}
  277 | 		return isAncestorOf(name);
  278 | 	}
  279 | 
  280 | 	/**
  281 | 	 * Returns {@code true} if this element is an ancestor (immediate or nested parent) of
  282 | 	 * the specified name.
  283 | 	 * @param name the name to check
  284 | 	 * @return {@code true} if this name is an ancestor
  285 | 	 */
  286 | 	public boolean isAncestorOf(ConfigurationPropertyName name) {
  287 | 		Assert.notNull(name, "'name' must not be null");
  288 | 		if (getNumberOfElements() >= name.getNumberOfElements()) {
  289 | 			return false;
  290 | 		}
  291 | 		return endsWithElementsEqualTo(name);
  292 | 	}
  293 | 
  294 | 	@Override
  295 | 	public int compareTo(ConfigurationPropertyName other) {
  296 | 		return compare(this, other);
  297 | 	}
  298 | 
  299 | 	private int compare(ConfigurationPropertyName n1, ConfigurationPropertyName n2) {
  300 | 		int l1 = n1.getNumberOfElements();
  301 | 		int l2 = n2.getNumberOfElements();
  302 | 		int i1 = 0;
  303 | 		int i2 = 0;
  304 | 		while (i1 < l1 || i2 < l2) {
  305 | 			try {
  306 | 				ElementType type1 = (i1 < l1) ? n1.elements.getType(i1) : null;
  307 | 				ElementType type2 = (i2 < l2) ? n2.elements.getType(i2) : null;
  308 | 				String e1 = (i1 < l1) ? n1.getElement(i1++, Form.UNIFORM) : null;
  309 | 				String e2 = (i2 < l2) ? n2.getElement(i2++, Form.UNIFORM) : null;
  310 | 				int result = compare(e1, type1, e2, type2);
  311 | 				if (result != 0) {
  312 | 					return result;
  313 | 				}
  314 | 			}
  315 | 			catch (ArrayIndexOutOfBoundsException ex) {
  316 | 				throw new RuntimeException(ex);
  317 | 			}
  318 | 		}
  319 | 		return 0;
  320 | 	}
  321 | 
  322 | 	private int compare(@Nullable String e1, @Nullable ElementType type1, @Nullable String e2,
  323 | 			@Nullable ElementType type2) {
  324 | 		if (e1 == null) {
  325 | 			return -1;
  326 | 		}
  327 | 		if (e2 == null) {
  328 | 			return 1;
  329 | 		}
  330 | 		Assert.state(type1 != null, "'type1' must not be null");
  331 | 		Assert.state(type2 != null, "'type2' must not be null");
  332 | 		int result = Boolean.compare(type2.isIndexed(), type1.isIndexed());
  333 | 		if (result != 0) {
  334 | 			return result;
  335 | 		}
  336 | 		if (type1 == ElementType.NUMERICALLY_INDEXED && type2 == ElementType.NUMERICALLY_INDEXED) {
  337 | 			long v1 = Long.parseLong(e1);
  338 | 			long v2 = Long.parseLong(e2);
  339 | 			return Long.compare(v1, v2);
  340 | 		}
  341 | 		return e1.compareTo(e2);
  342 | 	}
  343 | 
  344 | 	@Override
  345 | 	public boolean equals(Object obj) {
  346 | 		if (obj == this) {
  347 | 			return true;
  348 | 		}
  349 | 		if (obj == null || obj.getClass() != getClass()) {
  350 | 			return false;
  351 | 		}
  352 | 		ConfigurationPropertyName other = (ConfigurationPropertyName) obj;
  353 | 		if (getNumberOfElements() != other.getNumberOfElements()) {
  354 | 			return false;
  355 | 		}
  356 | 		if (this.elements.canShortcutWithSource(ElementType.UNIFORM)
  357 | 				&& other.elements.canShortcutWithSource(ElementType.UNIFORM)) {
  358 | 			return toString().equals(other.toString());
  359 | 		}
  360 | 		if (hashCode() != other.hashCode()) {
  361 | 			return false;
  362 | 		}
  363 | 		if (toStringMatches(toString(), other.toString())) {
  364 | 			return true;
  365 | 		}
  366 | 		return endsWithElementsEqualTo(other);
  367 | 	}
  368 | 
  369 | 	private boolean toStringMatches(String s1, String s2) {
  370 | 		return s1.hashCode() == s2.hashCode() && s1.equals(s2);
  371 | 	}
  372 | 
  373 | 	private boolean endsWithElementsEqualTo(ConfigurationPropertyName name) {
  374 | 		for (int i = this.elements.getSize() - 1; i >= 0; i--) {
  375 | 			if (elementDiffers(this.elements, name.elements, i)) {
  376 | 				return false;
  377 | 			}
  378 | 		}
  379 | 		return true;
  380 | 	}
  381 | 
  382 | 	private boolean elementDiffers(Elements e1, Elements e2, int i) {
  383 | 		ElementType type1 = e1.getType(i);
  384 | 		ElementType type2 = e2.getType(i);
  385 | 		if (type1.allowsFastEqualityCheck() && type2.allowsFastEqualityCheck()) {
  386 | 			return !fastElementEquals(e1, e2, i);
  387 | 		}
  388 | 		if (type1.allowsDashIgnoringEqualityCheck() && type2.allowsDashIgnoringEqualityCheck()) {
  389 | 			return !dashIgnoringElementEquals(e1, e2, i);
  390 | 		}
  391 | 		return !defaultElementEquals(e1, e2, i);
  392 | 	}
  393 | 
  394 | 	private boolean fastElementEquals(Elements e1, Elements e2, int i) {
  395 | 		int length1 = e1.getLength(i);
  396 | 		int length2 = e2.getLength(i);
  397 | 		if (length1 == length2) {
  398 | 			int i1 = 0;
  399 | 			while (length1-- != 0) {
  400 | 				char ch1 = e1.charAt(i, i1);
  401 | 				char ch2 = e2.charAt(i, i1);
  402 | 				if (ch1 != ch2) {
  403 | 					return false;
  404 | 				}
  405 | 				i1++;
  406 | 			}
  407 | 			return true;
  408 | 		}
  409 | 		return false;
  410 | 	}
  411 | 
  412 | 	private boolean dashIgnoringElementEquals(Elements e1, Elements e2, int i) {
  413 | 		int l1 = e1.getLength(i);
  414 | 		int l2 = e2.getLength(i);
  415 | 		int i1 = 0;
  416 | 		int i2 = 0;
  417 | 		while (i1 < l1) {
  418 | 			if (i2 >= l2) {
  419 | 				return remainderIsDashes(e1, i, i1);
  420 | 			}
  421 | 			char ch1 = e1.charAt(i, i1);
  422 | 			char ch2 = e2.charAt(i, i2);
  423 | 			if (ch1 == '-') {
  424 | 				i1++;
  425 | 			}
  426 | 			else if (ch2 == '-') {
  427 | 				i2++;
  428 | 			}
  429 | 			else if (ch1 != ch2) {
  430 | 				return false;
  431 | 			}
  432 | 			else {
  433 | 				i1++;
  434 | 				i2++;
  435 | 			}
  436 | 		}
  437 | 		if (i2 < l2) {
  438 | 			if (e2.getType(i).isIndexed()) {
  439 | 				return false;
  440 | 			}
  441 | 			do {
  442 | 				char ch2 = e2.charAt(i, i2++);
  443 | 				if (ch2 != '-') {
  444 | 					return false;
  445 | 				}
  446 | 			}
  447 | 			while (i2 < l2);
  448 | 		}
  449 | 		return true;
  450 | 	}
  451 | 
  452 | 	private boolean defaultElementEquals(Elements e1, Elements e2, int i) {
  453 | 		int l1 = e1.getLength(i);
  454 | 		int l2 = e2.getLength(i);
  455 | 		boolean indexed1 = e1.getType(i).isIndexed();
  456 | 		boolean indexed2 = e2.getType(i).isIndexed();
  457 | 		int i1 = 0;
  458 | 		int i2 = 0;
  459 | 		while (i1 < l1) {
  460 | 			if (i2 >= l2) {
  461 | 				return remainderIsNotAlphanumeric(e1, i, i1);
  462 | 			}
  463 | 			char ch1 = indexed1 ? e1.charAt(i, i1) : Character.toLowerCase(e1.charAt(i, i1));
  464 | 			char ch2 = indexed2 ? e2.charAt(i, i2) : Character.toLowerCase(e2.charAt(i, i2));
  465 | 			if (!indexed1 && !ElementsParser.isAlphaNumeric(ch1)) {
  466 | 				i1++;
  467 | 			}
  468 | 			else if (!indexed2 && !ElementsParser.isAlphaNumeric(ch2)) {
  469 | 				i2++;
  470 | 			}
  471 | 			else if (ch1 != ch2) {
  472 | 				return false;
  473 | 			}
  474 | 			else {
  475 | 				i1++;
  476 | 				i2++;
  477 | 			}
  478 | 		}
  479 | 		if (i2 < l2) {
  480 | 			return remainderIsNotAlphanumeric(e2, i, i2);
  481 | 		}
  482 | 		return true;
  483 | 	}
  484 | 
  485 | 	private boolean remainderIsNotAlphanumeric(Elements elements, int element, int index) {
  486 | 		if (elements.getType(element).isIndexed()) {
  487 | 			return false;
  488 | 		}
  489 | 		int length = elements.getLength(element);
  490 | 		do {
  491 | 			char c = Character.toLowerCase(elements.charAt(element, index++));
  492 | 			if (ElementsParser.isAlphaNumeric(c)) {
  493 | 				return false;
  494 | 			}
  495 | 		}
  496 | 		while (index < length);
  497 | 		return true;
  498 | 	}
  499 | 
  500 | 	private boolean remainderIsDashes(Elements elements, int element, int index) {
  501 | 		if (elements.getType(element).isIndexed()) {
  502 | 			return false;
  503 | 		}
  504 | 		int length = elements.getLength(element);
  505 | 		do {
  506 | 			char c = elements.charAt(element, index++);
  507 | 			if (c != '-') {
  508 | 				return false;
  509 | 			}
  510 | 		}
  511 | 		while (index < length);
  512 | 		return true;
  513 | 	}
  514 | 
  515 | 	@Override
  516 | 	public int hashCode() {
  517 | 		int hashCode = this.hashCode;
  518 | 		Elements elements = this.elements;
  519 | 		if (hashCode == 0 && elements.getSize() != 0) {
  520 | 			for (int elementIndex = 0; elementIndex < elements.getSize(); elementIndex++) {
  521 | 				hashCode = 31 * hashCode + elements.hashCode(elementIndex);
  522 | 			}
  523 | 			this.hashCode = hashCode;
  524 | 		}
  525 | 		return hashCode;
  526 | 	}
  527 | 
  528 | 	@Nullable ConfigurationPropertyName asSystemEnvironmentLegacyName() {
  529 | 		ConfigurationPropertyName name = this.systemEnvironmentLegacyName;
  530 | 		if (name == null) {
  531 | 			name = ConfigurationPropertyName
  532 | 				.ofIfValid(buildSimpleToString('.', (i) -> getElement(i, Form.DASHED).replace('-', '.')));
  533 | 			this.systemEnvironmentLegacyName = (name != null) ? name : EMPTY;
  534 | 		}
  535 | 		return (name != EMPTY) ? name : null;
  536 | 	}
  537 | 
  538 | 	@Override
  539 | 	public String toString() {
  540 | 		return toString(ToStringFormat.DEFAULT, false);
  541 | 	}
  542 | 
  543 | 	String toString(ToStringFormat format, boolean upperCase) {
  544 | 		String string = this.string[format.ordinal()];
  545 | 		if (string == null) {
  546 | 			string = buildToString(format);
  547 | 			this.string[format.ordinal()] = string;
  548 | 		}
  549 | 		return (!upperCase) ? string : string.toUpperCase(Locale.ENGLISH);
  550 | 	}
  551 | 
  552 | 	private String buildToString(ToStringFormat format) {
  553 | 		return switch (format) {
  554 | 			case DEFAULT -> buildDefaultToString();
  555 | 			case SYSTEM_ENVIRONMENT -> buildSimpleToString('_', (i) -> getElement(i, Form.UNIFORM));
  556 | 			case LEGACY_SYSTEM_ENVIRONMENT ->
  557 | 				buildSimpleToString('_', (i) -> getElement(i, Form.ORIGINAL).replace('-', '_'));
  558 | 		};
  559 | 	}
  560 | 
  561 | 	private String buildDefaultToString() {
  562 | 		if (this.elements.canShortcutWithSource(ElementType.UNIFORM, ElementType.DASHED)) {
  563 | 			return this.elements.getSource().toString();
  564 | 		}
  565 | 		int elements = getNumberOfElements();
  566 | 		StringBuilder result = new StringBuilder(elements * 8);
  567 | 		for (int i = 0; i < elements; i++) {
  568 | 			boolean indexed = isIndexed(i);
  569 | 			if (!result.isEmpty() && !indexed) {
  570 | 				result.append('.');
  571 | 			}
  572 | 			if (indexed) {
  573 | 				result.append('[');
  574 | 				result.append(getElement(i, Form.ORIGINAL));
  575 | 				result.append(']');
  576 | 			}
  577 | 			else {
  578 | 				result.append(getElement(i, Form.DASHED));
  579 | 			}
  580 | 		}
  581 | 		return result.toString();
  582 | 	}
  583 | 
  584 | 	private String buildSimpleToString(char joinChar, IntFunction<String> elementConverter) {
  585 | 		StringBuilder result = new StringBuilder();
  586 | 		for (int i = 0; i < getNumberOfElements(); i++) {
  587 | 			if (!result.isEmpty()) {
  588 | 				result.append(joinChar);
  589 | 			}
  590 | 			result.append(elementConverter.apply(i));
  591 | 		}
  592 | 		return result.toString();
  593 | 	}
  594 | 
  595 | 	boolean hasDashedElement() {
  596 | 		Boolean hasDashedElement = this.hasDashedElement;
  597 | 		if (hasDashedElement != null) {
  598 | 			return hasDashedElement;
  599 | 		}
  600 | 		for (int i = 0; i < getNumberOfElements(); i++) {
  601 | 			if (getElement(i, Form.DASHED).indexOf('-') != -1) {
  602 | 				this.hasDashedElement = true;
  603 | 				return true;
  604 | 			}
  605 | 		}
  606 | 		this.hasDashedElement = false;
  607 | 		return false;
  608 | 	}
  609 | 
  610 | 	/**
  611 | 	 * Returns if the given name is valid. If this method returns {@code true} then the
  612 | 	 * name may be used with {@link #of(CharSequence)} without throwing an exception.
  613 | 	 * @param name the name to test
  614 | 	 * @return {@code true} if the name is valid
  615 | 	 */
  616 | 	public static boolean isValid(@Nullable CharSequence name) {
  617 | 		return of(name, true) != null;
  618 | 	}
  619 | 
  620 | 	/**
  621 | 	 * Return a {@link ConfigurationPropertyName} for the specified string.
  622 | 	 * @param name the source name
  623 | 	 * @return a {@link ConfigurationPropertyName} instance
  624 | 	 * @throws InvalidConfigurationPropertyNameException if the name is not valid
  625 | 	 */
  626 | 	public static ConfigurationPropertyName of(@Nullable CharSequence name) {
  627 | 		return of(name, false);
  628 | 	}
  629 | 
  630 | 	/**
  631 | 	 * Return a {@link ConfigurationPropertyName} for the specified string or {@code null}
  632 | 	 * if the name is not valid.
  633 | 	 * @param name the source name
  634 | 	 * @return a {@link ConfigurationPropertyName} instance
  635 | 	 * @since 2.3.1
  636 | 	 */
  637 | 	public static @Nullable ConfigurationPropertyName ofIfValid(@Nullable CharSequence name) {
  638 | 		return of(name, true);
  639 | 	}
  640 | 
  641 | 	/**
  642 | 	 * Return a {@link ConfigurationPropertyName} for the specified string.
  643 | 	 * @param name the source name
  644 | 	 * @param returnNullIfInvalid if null should be returned if the name is not valid
  645 | 	 * @return a {@link ConfigurationPropertyName} instance
  646 | 	 * @throws InvalidConfigurationPropertyNameException if the name is not valid and
  647 | 	 * {@code returnNullIfInvalid} is {@code false}
  648 | 	 */
  649 | 	@Contract("_, false -> !null")
  650 | 	static @Nullable ConfigurationPropertyName of(@Nullable CharSequence name, boolean returnNullIfInvalid) {
  651 | 		Elements elements = elementsOf(name, returnNullIfInvalid, ElementsParser.DEFAULT_CAPACITY);
  652 | 		return (elements != null) ? new ConfigurationPropertyName(elements) : null;
  653 | 	}
  654 | 
  655 | 	private static Elements probablySingleElementOf(CharSequence name) {
  656 | 		return elementsOf(name, false, 1);
  657 | 	}
  658 | 
  659 | 	@Contract("_, false, _ -> !null")
  660 | 	private static @Nullable Elements elementsOf(@Nullable CharSequence name, boolean returnNullIfInvalid,
  661 | 			int parserCapacity) {
  662 | 		if (name == null) {
  663 | 			Assert.isTrue(returnNullIfInvalid, "'name' must not be null");
  664 | 			return null;
  665 | 		}
  666 | 		if (name.isEmpty()) {
  667 | 			return Elements.EMPTY;
  668 | 		}
  669 | 		if (name.charAt(0) == '.' || name.charAt(name.length() - 1) == '.') {
  670 | 			if (returnNullIfInvalid) {
  671 | 				return null;
  672 | 			}
  673 | 			throw new InvalidConfigurationPropertyNameException(name, Collections.singletonList('.'));
  674 | 		}
  675 | 		if (name.charAt(name.length() - 1) == '[') {
  676 | 			if (returnNullIfInvalid) {
  677 | 				return null;
  678 | 			}
  679 | 			throw new InvalidConfigurationPropertyNameException(name, Collections.singletonList('['));
  680 | 		}
  681 | 		Elements elements = new ElementsParser(name, '.', parserCapacity).parse();
  682 | 		for (int i = 0; i < elements.getSize(); i++) {
  683 | 			if (elements.getType(i) == ElementType.NON_UNIFORM) {
  684 | 				if (returnNullIfInvalid) {
  685 | 					return null;
  686 | 				}
  687 | 				throw new InvalidConfigurationPropertyNameException(name, getInvalidChars(elements, i));
  688 | 			}
  689 | 		}
  690 | 		return elements;
  691 | 	}
  692 | 
  693 | 	private static List<Character> getInvalidChars(Elements elements, int index) {
  694 | 		List<Character> invalidChars = new ArrayList<>();
  695 | 		for (int charIndex = 0; charIndex < elements.getLength(index); charIndex++) {
  696 | 			char ch = elements.charAt(index, charIndex);
  697 | 			if (!ElementsParser.isValidChar(ch, charIndex)) {
  698 | 				invalidChars.add(ch);
  699 | 			}
  700 | 		}
  701 | 		return invalidChars;
  702 | 	}
  703 | 
  704 | 	/**
  705 | 	 * Create a {@link ConfigurationPropertyName} by adapting the given source. See
  706 | 	 * {@link #adapt(CharSequence, char, Function)} for details.
  707 | 	 * @param name the name to parse
  708 | 	 * @param separator the separator used to split the name
  709 | 	 * @return a {@link ConfigurationPropertyName}
  710 | 	 */
  711 | 	public static ConfigurationPropertyName adapt(CharSequence name, char separator) {
  712 | 		return adapt(name, separator, null);
  713 | 	}
  714 | 
  715 | 	/**
  716 | 	 * Create a {@link ConfigurationPropertyName} by adapting the given source. The name
  717 | 	 * is split into elements around the given {@code separator}. This method is more
  718 | 	 * lenient than {@link #of} in that it allows mixed case names and '{@code _}'
  719 | 	 * characters. Other invalid characters are stripped out during parsing.
  720 | 	 * <p>
  721 | 	 * The {@code elementValueProcessor} function may be used if additional processing is
  722 | 	 * required on the extracted element values.
  723 | 	 * @param name the name to parse
  724 | 	 * @param separator the separator used to split the name
  725 | 	 * @param elementValueProcessor a function to process element values
  726 | 	 * @return a {@link ConfigurationPropertyName}
  727 | 	 */
  728 | 	static ConfigurationPropertyName adapt(CharSequence name, char separator,
  729 | 			@Nullable Function<CharSequence, CharSequence> elementValueProcessor) {
  730 | 		Assert.notNull(name, "Name must not be null");
  731 | 		if (name.isEmpty()) {
  732 | 			return EMPTY;
  733 | 		}
  734 | 		Elements elements = new ElementsParser(name, separator).parse(elementValueProcessor);
  735 | 		if (elements.getSize() == 0) {
  736 | 			return EMPTY;
  737 | 		}
  738 | 		return new ConfigurationPropertyName(elements);
  739 | 	}
  740 | 
  741 | 	/**
  742 | 	 * The various forms that a non-indexed element value can take.
  743 | 	 */
  744 | 	public enum Form {
  745 | 
  746 | 		/**
  747 | 		 * The original form as specified when the name was created or adapted. For
  748 | 		 * example:
  749 | 		 * <ul>
  750 | 		 * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
  751 | 		 * <li>"{@code fooBar}" = "{@code fooBar}"</li>
  752 | 		 * <li>"{@code foo_bar}" = "{@code foo_bar}"</li>
  753 | 		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
  754 | 		 * </ul>
  755 | 		 */
  756 | 		ORIGINAL,
  757 | 
  758 | 		/**
  759 | 		 * The dashed configuration form (used for toString; lower-case with only
  760 | 		 * alphanumeric characters and dashes).
  761 | 		 * <ul>
  762 | 		 * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
  763 | 		 * <li>"{@code fooBar}" = "{@code foobar}"</li>
  764 | 		 * <li>"{@code foo_bar}" = "{@code foobar}"</li>
  765 | 		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
  766 | 		 * </ul>
  767 | 		 */
  768 | 		DASHED,
  769 | 
  770 | 		/**
  771 | 		 * The uniform configuration form (used for equals/hashCode; lower-case with only
  772 | 		 * alphanumeric characters).
  773 | 		 * <ul>
  774 | 		 * <li>"{@code foo-bar}" = "{@code foobar}"</li>
  775 | 		 * <li>"{@code fooBar}" = "{@code foobar}"</li>
  776 | 		 * <li>"{@code foo_bar}" = "{@code foobar}"</li>
  777 | 		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
  778 | 		 * </ul>
  779 | 		 */
  780 | 		UNIFORM
  781 | 
  782 | 	}
  783 | 
  784 | 	/**
  785 | 	 * Allows access to the individual elements that make up the name. We store the
  786 | 	 * indexes in arrays rather than a list of object in order to conserve memory.
  787 | 	 */
  788 | 	private static class Elements {
  789 | 
  790 | 		private static final int[] NO_POSITION = {};
  791 | 
  792 | 		private static final ElementType[] NO_TYPE = {};
  793 | 
  794 | 		public static final Elements EMPTY = new Elements("", 0, NO_POSITION, NO_POSITION, NO_TYPE, null, null);
  795 | 
  796 | 		private final CharSequence source;
  797 | 
  798 | 		private final int size;
  799 | 
  800 | 		private final int[] start;
  801 | 
  802 | 		private final int[] end;
  803 | 
  804 | 		private final ElementType[] type;
  805 | 
  806 | 		private final int[] hashCode;
  807 | 
  808 | 		/**
  809 | 		 * Contains any resolved elements or can be {@code null} if there aren't any.
  810 | 		 * Resolved elements allow us to modify the element values in some way (or example
  811 | 		 * when adapting with a mapping function, or when append has been called). Note
  812 | 		 * that this array is not used as a cache, in fact, when it's not null then
  813 | 		 * {@link #canShortcutWithSource} will always return false which may hurt
  814 | 		 * performance.
  815 | 		 */
  816 | 		private final @Nullable CharSequence @Nullable [] resolved;
  817 | 
  818 | 		Elements(CharSequence source, int size, int[] start, int[] end, ElementType[] type, int @Nullable [] hashCode,
  819 | 				CharSequence @Nullable [] resolved) {
  820 | 			this.source = source;
  821 | 			this.size = size;
  822 | 			this.start = start;
  823 | 			this.end = end;
  824 | 			this.type = type;
  825 | 			this.hashCode = (hashCode != null) ? hashCode : new int[size];
  826 | 			this.resolved = resolved;
  827 | 		}
  828 | 
  829 | 		Elements append(Elements additional) {
  830 | 			int size = this.size + additional.size;
  831 | 			ElementType[] type = new ElementType[size];
  832 | 			int[] hashCode = new int[size];
  833 | 			System.arraycopy(this.type, 0, type, 0, this.size);
  834 | 			System.arraycopy(additional.type, 0, type, this.size, additional.size);
  835 | 			System.arraycopy(this.hashCode, 0, hashCode, 0, this.size);
  836 | 			System.arraycopy(additional.hashCode, 0, hashCode, this.size, additional.size);
  837 | 			CharSequence[] resolved = newResolved(0, size);
  838 | 			for (int i = 0; i < additional.size; i++) {
  839 | 				resolved[this.size + i] = additional.get(i);
  840 | 			}
  841 | 			return new Elements(this.source, size, this.start, this.end, type, hashCode, resolved);
  842 | 		}
  843 | 
  844 | 		Elements chop(int size) {
  845 | 			CharSequence[] resolved = newResolved(0, size);
  846 | 			return new Elements(this.source, size, this.start, this.end, this.type, this.hashCode, resolved);
  847 | 		}
  848 | 
  849 | 		Elements subElements(int offset) {
  850 | 			int size = this.size - offset;
  851 | 			CharSequence[] resolved = newResolved(offset, size);
  852 | 			int[] start = new int[size];
  853 | 			System.arraycopy(this.start, offset, start, 0, size);
  854 | 			int[] end = new int[size];
  855 | 			System.arraycopy(this.end, offset, end, 0, size);
  856 | 			ElementType[] type = new ElementType[size];
  857 | 			System.arraycopy(this.type, offset, type, 0, size);
  858 | 			int[] hashCode = new int[size];
  859 | 			System.arraycopy(this.hashCode, offset, hashCode, 0, size);
  860 | 			return new Elements(this.source, size, start, end, type, hashCode, resolved);
  861 | 		}
  862 | 
  863 | 		private CharSequence[] newResolved(int offset, int size) {
  864 | 			CharSequence[] resolved = new CharSequence[size];
  865 | 			if (this.resolved != null) {
  866 | 				System.arraycopy(this.resolved, offset, resolved, 0, Math.min(size, this.size));
  867 | 			}
  868 | 			return resolved;
  869 | 		}
  870 | 
  871 | 		int getSize() {
  872 | 			return this.size;
  873 | 		}
  874 | 
  875 | 		CharSequence get(int index) {
  876 | 			if (this.resolved != null) {
  877 | 				CharSequence element = this.resolved[index];
  878 | 				if (element != null) {
  879 | 					return element;
  880 | 				}
  881 | 			}
  882 | 			int start = this.start[index];
  883 | 			int end = this.end[index];
  884 | 			return this.source.subSequence(start, end);
  885 | 		}
  886 | 
  887 | 		int getLength(int index) {
  888 | 			if (this.resolved != null) {
  889 | 				CharSequence element = this.resolved[index];
  890 | 				if (element != null) {
  891 | 					return element.length();
  892 | 				}
  893 | 			}
  894 | 			int start = this.start[index];
  895 | 			int end = this.end[index];
  896 | 			return end - start;
  897 | 		}
  898 | 
  899 | 		char charAt(int index, int charIndex) {
  900 | 			if (this.resolved != null) {
  901 | 				CharSequence element = this.resolved[index];
  902 | 				if (element != null) {
  903 | 					return element.charAt(charIndex);
  904 | 				}
  905 | 			}
  906 | 			int start = this.start[index];
  907 | 			return this.source.charAt(start + charIndex);
  908 | 		}
  909 | 
  910 | 		ElementType getType(int index) {
  911 | 			return this.type[index];
  912 | 		}
  913 | 
  914 | 		int hashCode(int index) {
  915 | 			int hashCode = this.hashCode[index];
  916 | 			if (hashCode == 0) {
  917 | 				boolean indexed = getType(index).isIndexed();
  918 | 				int length = getLength(index);
  919 | 				for (int i = 0; i < length; i++) {
  920 | 					char ch = charAt(index, i);
  921 | 					if (!indexed) {
  922 | 						ch = Character.toLowerCase(ch);
  923 | 					}
  924 | 					if (ElementsParser.isAlphaNumeric(ch)) {
  925 | 						hashCode = 31 * hashCode + ch;
  926 | 					}
  927 | 				}
  928 | 				this.hashCode[index] = hashCode;
  929 | 			}
  930 | 			return hashCode;
  931 | 		}
  932 | 
  933 | 		CharSequence getSource() {
  934 | 			return this.source;
  935 | 		}
  936 | 
  937 | 		/**
  938 | 		 * Returns if the element source can be used as a shortcut for an operation such
  939 | 		 * as {@code equals} or {@code toString}.
  940 | 		 * @param requiredType the required type
  941 | 		 * @return {@code true} if all elements match at least one of the types
  942 | 		 */
  943 | 		boolean canShortcutWithSource(ElementType requiredType) {
  944 | 			return canShortcutWithSource(requiredType, requiredType);
  945 | 		}
  946 | 
  947 | 		/**
  948 | 		 * Returns if the element source can be used as a shortcut for an operation such
  949 | 		 * as {@code equals} or {@code toString}.
  950 | 		 * @param requiredType the required type
  951 | 		 * @param alternativeType and alternative required type
  952 | 		 * @return {@code true} if all elements match at least one of the types
  953 | 		 */
  954 | 		boolean canShortcutWithSource(ElementType requiredType, ElementType alternativeType) {
  955 | 			if (this.resolved != null) {
  956 | 				return false;
  957 | 			}
  958 | 			for (int i = 0; i < this.size; i++) {
  959 | 				ElementType type = this.type[i];
  960 | 				if (type != requiredType && type != alternativeType) {
  961 | 					return false;
  962 | 				}
  963 | 				if (i > 0 && this.end[i - 1] + 1 != this.start[i]) {
  964 | 					return false;
  965 | 				}
  966 | 			}
  967 | 			return true;
  968 | 		}
  969 | 
  970 | 	}
  971 | 
  972 | 	/**
  973 | 	 * Main parsing logic used to convert a {@link CharSequence} to {@link Elements}.
  974 | 	 */
  975 | 	private static class ElementsParser {
  976 | 
  977 | 		private static final int DEFAULT_CAPACITY = 6;
  978 | 
  979 | 		private final CharSequence source;
  980 | 
  981 | 		private final char separator;
  982 | 
  983 | 		private int size;
  984 | 
  985 | 		private int[] start;
  986 | 
  987 | 		private int[] end;
  988 | 
  989 | 		private ElementType[] type;
  990 | 
  991 | 		private CharSequence @Nullable [] resolved;
  992 | 
  993 | 		ElementsParser(CharSequence source, char separator) {
  994 | 			this(source, separator, DEFAULT_CAPACITY);
  995 | 		}
  996 | 
  997 | 		ElementsParser(CharSequence source, char separator, int capacity) {
  998 | 			this.source = source;
  999 | 			this.separator = separator;
 1000 | 			this.start = new int[capacity];
 1001 | 			this.end = new int[capacity];
 1002 | 			this.type = new ElementType[capacity];
 1003 | 		}
 1004 | 
 1005 | 		Elements parse() {
 1006 | 			return parse(null);
 1007 | 		}
 1008 | 
 1009 | 		Elements parse(@Nullable Function<CharSequence, CharSequence> valueProcessor) {
 1010 | 			int length = this.source.length();
 1011 | 			int openBracketCount = 0;
 1012 | 			int start = 0;
 1013 | 			ElementType type = ElementType.EMPTY;
 1014 | 			for (int i = 0; i < length; i++) {
 1015 | 				char ch = this.source.charAt(i);
 1016 | 				if (ch == '[') {
 1017 | 					if (openBracketCount == 0) {
 1018 | 						add(start, i, type, valueProcessor);
 1019 | 						start = i + 1;
 1020 | 						type = ElementType.NUMERICALLY_INDEXED;
 1021 | 					}
 1022 | 					openBracketCount++;
 1023 | 				}
 1024 | 				else if (ch == ']') {
 1025 | 					openBracketCount--;
 1026 | 					if (openBracketCount == 0) {
 1027 | 						add(start, i, type, valueProcessor);
 1028 | 						start = i + 1;
 1029 | 						type = ElementType.EMPTY;
 1030 | 					}
 1031 | 				}
 1032 | 				else if (!type.isIndexed() && ch == this.separator) {
 1033 | 					add(start, i, type, valueProcessor);
 1034 | 					start = i + 1;
 1035 | 					type = ElementType.EMPTY;
 1036 | 				}
 1037 | 				else {
 1038 | 					type = updateType(type, ch, i - start);
 1039 | 				}
 1040 | 			}
 1041 | 			if (openBracketCount != 0) {
 1042 | 				type = ElementType.NON_UNIFORM;
 1043 | 			}
 1044 | 			add(start, length, type, valueProcessor);
 1045 | 			return new Elements(this.source, this.size, this.start, this.end, this.type, null, this.resolved);
 1046 | 		}
 1047 | 
 1048 | 		private ElementType updateType(ElementType existingType, char ch, int index) {
 1049 | 			if (existingType.isIndexed()) {
 1050 | 				if (existingType == ElementType.NUMERICALLY_INDEXED && !isNumeric(ch)) {
 1051 | 					return ElementType.INDEXED;
 1052 | 				}
 1053 | 				return existingType;
 1054 | 			}
 1055 | 			if (existingType == ElementType.EMPTY && isValidChar(ch, index)) {
 1056 | 				return (index == 0) ? ElementType.UNIFORM : ElementType.NON_UNIFORM;
 1057 | 			}
 1058 | 			if (existingType == ElementType.UNIFORM && ch == '-') {
 1059 | 				return ElementType.DASHED;
 1060 | 			}
 1061 | 			if (!isValidChar(ch, index)) {
 1062 | 				if (existingType == ElementType.EMPTY && !isValidChar(Character.toLowerCase(ch), index)) {
 1063 | 					return ElementType.EMPTY;
 1064 | 				}
 1065 | 				return ElementType.NON_UNIFORM;
 1066 | 			}
 1067 | 			return existingType;
 1068 | 		}
 1069 | 
 1070 | 		private void add(int start, int end, ElementType type,
 1071 | 				@Nullable Function<CharSequence, CharSequence> valueProcessor) {
 1072 | 			if ((end - start) < 1 || type == ElementType.EMPTY) {
 1073 | 				return;
 1074 | 			}
 1075 | 			if (this.start.length == this.size) {
 1076 | 				this.start = expand(this.start);
 1077 | 				this.end = expand(this.end);
 1078 | 				this.type = expand(this.type);
 1079 | 				this.resolved = expand(this.resolved);
 1080 | 			}
 1081 | 			if (valueProcessor != null) {
 1082 | 				if (this.resolved == null) {
 1083 | 					this.resolved = new CharSequence[this.start.length];
 1084 | 				}
 1085 | 				CharSequence resolved = valueProcessor.apply(this.source.subSequence(start, end));
 1086 | 				Elements resolvedElements = new ElementsParser(resolved, '.').parse();
 1087 | 				Assert.state(resolvedElements.getSize() == 1, "Resolved element must not contain multiple elements");
 1088 | 				this.resolved[this.size] = resolvedElements.get(0);
 1089 | 				type = resolvedElements.getType(0);
 1090 | 			}
 1091 | 			this.start[this.size] = start;
 1092 | 			this.end[this.size] = end;
 1093 | 			this.type[this.size] = type;
 1094 | 			this.size++;
 1095 | 		}
 1096 | 
 1097 | 		private int[] expand(int[] src) {
 1098 | 			int[] dest = new int[src.length + DEFAULT_CAPACITY];
 1099 | 			System.arraycopy(src, 0, dest, 0, src.length);
 1100 | 			return dest;
 1101 | 		}
 1102 | 
 1103 | 		private ElementType[] expand(ElementType[] src) {
 1104 | 			ElementType[] dest = new ElementType[src.length + DEFAULT_CAPACITY];
 1105 | 			System.arraycopy(src, 0, dest, 0, src.length);
 1106 | 			return dest;
 1107 | 		}
 1108 | 
 1109 | 		private CharSequence @Nullable [] expand(CharSequence @Nullable [] src) {
 1110 | 			if (src == null) {
 1111 | 				return null;
 1112 | 			}
 1113 | 			CharSequence[] dest = new CharSequence[src.length + DEFAULT_CAPACITY];
 1114 | 			System.arraycopy(src, 0, dest, 0, src.length);
 1115 | 			return dest;
 1116 | 		}
 1117 | 
 1118 | 		static boolean isValidChar(char ch, int index) {
 1119 | 			return isAlpha(ch) || isNumeric(ch) || (index != 0 && ch == '-');
 1120 | 		}
 1121 | 
 1122 | 		static boolean isAlphaNumeric(char ch) {
 1123 | 			return isAlpha(ch) || isNumeric(ch);
 1124 | 		}
 1125 | 
 1126 | 		private static boolean isAlpha(char ch) {
 1127 | 			return ch >= 'a' && ch <= 'z';
 1128 | 		}
 1129 | 
 1130 | 		private static boolean isNumeric(char ch) {
 1131 | 			return ch >= '0' && ch <= '9';
 1132 | 		}
 1133 | 
 1134 | 	}
 1135 | 
 1136 | 	/**
 1137 | 	 * The various types of element that we can detect.
 1138 | 	 */
 1139 | 	private enum ElementType {
 1140 | 
 1141 | 		/**
 1142 | 		 * The element is logically empty (contains no valid chars).
 1143 | 		 */
 1144 | 		EMPTY(false),
 1145 | 
 1146 | 		/**
 1147 | 		 * The element is a uniform name (a-z, 0-9, no dashes, lowercase).
 1148 | 		 */
 1149 | 		UNIFORM(false),
 1150 | 
 1151 | 		/**
 1152 | 		 * The element is almost uniform, but it contains (but does not start with) at
 1153 | 		 * least one dash.
 1154 | 		 */
 1155 | 		DASHED(false),
 1156 | 
 1157 | 		/**
 1158 | 		 * The element contains non-uniform characters and will need to be converted.
 1159 | 		 */
 1160 | 		NON_UNIFORM(false),
 1161 | 
 1162 | 		/**
 1163 | 		 * The element is non-numerically indexed.
 1164 | 		 */
 1165 | 		INDEXED(true),
 1166 | 
 1167 | 		/**
 1168 | 		 * The element is numerically indexed.
 1169 | 		 */
 1170 | 		NUMERICALLY_INDEXED(true);
 1171 | 
 1172 | 		private final boolean indexed;
 1173 | 
 1174 | 		ElementType(boolean indexed) {
 1175 | 			this.indexed = indexed;
 1176 | 		}
 1177 | 
 1178 | 		public boolean isIndexed() {
 1179 | 			return this.indexed;
 1180 | 		}
 1181 | 
 1182 | 		public boolean allowsFastEqualityCheck() {
 1183 | 			return this == UNIFORM || this == NUMERICALLY_INDEXED;
 1184 | 		}
 1185 | 
 1186 | 		public boolean allowsDashIgnoringEqualityCheck() {
 1187 | 			return allowsFastEqualityCheck() || this == DASHED;
 1188 | 		}
 1189 | 
 1190 | 	}
 1191 | 
 1192 | 	/**
 1193 | 	 * Predicate used to filter element chars.
 1194 | 	 */
 1195 | 	private interface ElementCharPredicate {
 1196 | 
 1197 | 		boolean test(char ch, int index);
 1198 | 
 1199 | 	}
 1200 | 
 1201 | 	/**
 1202 | 	 * Formats for {@code toString}.
 1203 | 	 */
 1204 | 	enum ToStringFormat {
 1205 | 
 1206 | 		DEFAULT, SYSTEM_ENVIRONMENT, LEGACY_SYSTEM_ENVIRONMENT
 1207 | 
 1208 | 	}
 1209 | 
 1210 | }
```
