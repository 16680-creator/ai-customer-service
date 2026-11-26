<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/util/pattern/PathPattern.java -->
<!-- 正文引用：[[源码:PathPattern.java#L起-L止]] ；代码块头部：(src: PathPattern.java#L起-L止) -->

# PathPattern.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/util/pattern/PathPattern.java` |
| 行数 | 759 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.607Z |

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
   17 | package org.springframework.web.util.pattern;
   18 | 
   19 | import java.util.Collections;
   20 | import java.util.Comparator;
   21 | import java.util.HashMap;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.StringJoiner;
   25 | 
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.http.server.PathContainer;
   29 | import org.springframework.http.server.PathContainer.Element;
   30 | import org.springframework.http.server.PathContainer.PathSegment;
   31 | import org.springframework.http.server.PathContainer.Separator;
   32 | import org.springframework.util.CollectionUtils;
   33 | import org.springframework.util.MultiValueMap;
   34 | import org.springframework.util.StringUtils;
   35 | 
   36 | /**
   37 |  * Representation of a parsed path pattern. Includes a chain of path elements
   38 |  * for fast matching and accumulates computed state for quick comparison of
   39 |  * patterns.
   40 |  *
   41 |  * <p>{@code PathPattern} matches URL paths using the following rules:<br>
   42 |  * <ul>
   43 |  * <li>{@code ?} matches one character</li>
   44 |  * <li>{@code *} matches zero or more characters within a path segment</li>
   45 |  * <li>{@code **} matches zero or more <em>path segments</em> until the end of the path</li>
   46 |  * <li><code>{spring}</code> matches a <em>path segment</em> and captures it as a variable named "spring"</li>
   47 |  * <li><code>{spring:[a-z]+}</code> matches the regexp {@code [a-z]+} against a path segment
   48 |  * and captures it as a path variable named "spring"</li>
   49 |  * <li><code>{*spring}</code> matches zero or more <em>path segments</em> until the end of the path
   50 |  * and captures it as a variable named "spring"</li>
   51 |  * </ul>
   52 |  *
   53 |  * <p><strong>Note:</strong> In contrast to
   54 |  * {@link org.springframework.util.AntPathMatcher}, {@code **} is supported only
   55 |  * at the end of a pattern. For example {@code /pages/{**}} is valid but
   56 |  * {@code /pages/{**}/details} is not. The same applies also to the capturing
   57 |  * variant <code>{*spring}</code>. The aim is to eliminate ambiguity when
   58 |  * comparing patterns for specificity.
   59 |  *
   60 |  * <h3>Examples</h3>
   61 |  * <ul>
   62 |  * <li>{@code /pages/t?st.html} &mdash; matches {@code /pages/test.html} as well as
   63 |  * {@code /pages/tXst.html} but not {@code /pages/toast.html}</li>
   64 |  * <li>{@code /resources/*.png} &mdash; matches all {@code .png} files in the
   65 |  * {@code resources} directory</li>
   66 |  * <li><code>/resources/&#42;&#42;</code> &mdash; matches all files
   67 |  * underneath the {@code /resources/} path, including {@code /resources/image.png}
   68 |  * and {@code /resources/css/spring.css}</li>
   69 |  * <li><code>/resources/{&#42;path}</code> &mdash; matches all files
   70 |  * underneath the {@code /resources/}, as well as {@code /resources}, and captures
   71 |  * their relative path in a variable named "path"; {@code /resources/image.png}
   72 |  * will match with "path" &rarr; "/image.png", and {@code /resources/css/spring.css}
   73 |  * will match with "path" &rarr; "/css/spring.css"</li>
   74 |  * <li><code>/resources/{filename:\\w+}.dat</code> will match {@code /resources/spring.dat}
   75 |  * and assign the value {@code "spring"} to the {@code filename} variable</li>
   76 |  * </ul>
   77 |  *
   78 |  * @author Andy Clement
   79 |  * @author Rossen Stoyanchev
   80 |  * @since 5.0
   81 |  * @see PathContainer
   82 |  */
   83 | public class PathPattern implements Comparable<PathPattern> {
   84 | 
   85 | 	private static final PathContainer EMPTY_PATH = PathContainer.parsePath("");
   86 | 
   87 | 	/**
   88 | 	 * Comparator that sorts patterns by specificity as follows:
   89 | 	 * <ol>
   90 | 	 * <li>Null instances are last.
   91 | 	 * <li>Catch-all patterns are last.
   92 | 	 * <li>If both patterns are catch-all, consider the length (longer wins).
   93 | 	 * <li>Compare wildcard and captured variable count (lower wins).
   94 | 	 * <li>Consider length (longer wins)
   95 | 	 * </ol>
   96 | 	 */
   97 | 	public static final Comparator<@Nullable PathPattern> SPECIFICITY_COMPARATOR =
   98 | 			Comparator.nullsLast(
   99 | 					Comparator.<PathPattern>
  100 | 							comparingInt(p -> p.isCatchAll() ? 1 : 0)
  101 | 							.thenComparingInt(p -> p.isCatchAll() ? scoreByNormalizedLength(p) : 0)
  102 | 							.thenComparingInt(PathPattern::getScore)
  103 | 							.thenComparingInt(PathPattern::scoreByNormalizedLength)
  104 | 			);
  105 | 
  106 | 
  107 | 	/** The text of the parsed pattern. */
  108 | 	private final String patternString;
  109 | 
  110 | 	/** The parser used to construct this pattern. */
  111 | 	private final PathPatternParser parser;
  112 | 
  113 | 	/** The options to use to parse a pattern. */
  114 | 	private final PathContainer.Options pathOptions;
  115 | 
  116 | 	/** Will this match candidates in a case-sensitive way? (case sensitivity  at parse time). */
  117 | 	private final boolean caseSensitive;
  118 | 
  119 | 	/** First path element in the parsed chain of path elements for this pattern. */
  120 | 	private final @Nullable PathElement head;
  121 | 
  122 | 	/** How many variables are captured in this pattern. */
  123 | 	private int capturedVariableCount;
  124 | 
  125 | 	/**
  126 | 	 * The normalized length is trying to measure the 'active' part of the pattern. It is computed
  127 | 	 * by assuming all captured variables have a normalized length of 1. Effectively this means changing
  128 | 	 * your variable name lengths isn't going to change the length of the active part of the pattern.
  129 | 	 * Useful when comparing two patterns.
  130 | 	 */
  131 | 	private int normalizedLength;
  132 | 
  133 | 	/**
  134 | 	 * Does the pattern end with '&lt;separator&gt;'.
  135 | 	 */
  136 | 	private boolean endsWithSeparatorWildcard = false;
  137 | 
  138 | 	/**
  139 | 	 * Score is used to quickly compare patterns. Different pattern components are given different
  140 | 	 * weights. A 'lower score' is more specific. Current weights:
  141 | 	 * <ul>
  142 | 	 * <li>Captured variables are worth 1
  143 | 	 * <li>Wildcard is worth 100
  144 | 	 * </ul>
  145 | 	 */
  146 | 	private int score;
  147 | 
  148 | 	/** Does the pattern end with {*...}. */
  149 | 	private boolean catchAll = false;
  150 | 
  151 | 
  152 | 	PathPattern(String patternText, PathPatternParser parser, @Nullable PathElement head) {
  153 | 		this.patternString = patternText;
  154 | 		this.parser = parser;
  155 | 		this.pathOptions = parser.getPathOptions();
  156 | 		this.caseSensitive = parser.isCaseSensitive();
  157 | 		this.head = head;
  158 | 
  159 | 		// Compute fields for fast comparison
  160 | 		PathElement elem = head;
  161 | 		while (elem != null) {
  162 | 			this.capturedVariableCount += elem.getCaptureCount();
  163 | 			this.normalizedLength += elem.getNormalizedLength();
  164 | 			this.score += elem.getScore();
  165 | 			if (elem instanceof CaptureSegmentsPathElement || elem instanceof WildcardSegmentsPathElement) {
  166 | 				this.catchAll = true;
  167 | 			}
  168 | 			if (elem instanceof SeparatorPathElement && elem.next instanceof WildcardPathElement && elem.next.next == null) {
  169 | 				this.endsWithSeparatorWildcard = true;
  170 | 			}
  171 | 			elem = elem.next;
  172 | 		}
  173 | 	}
  174 | 
  175 | 
  176 | 	/**
  177 | 	 * Return the original String that was parsed to create this PathPattern.
  178 | 	 */
  179 | 	public String getPatternString() {
  180 | 		return this.patternString;
  181 | 	}
  182 | 
  183 | 	/**
  184 | 	 * Whether the pattern string contains pattern syntax that would require
  185 | 	 * use of {@link #matches(PathContainer)}, or if it is a regular String that
  186 | 	 * could be compared directly to others.
  187 | 	 * @since 5.2
  188 | 	 */
  189 | 	public boolean hasPatternSyntax() {
  190 | 		return (this.score > 0 || this.catchAll || this.patternString.indexOf('?') != -1);
  191 | 	}
  192 | 
  193 | 	/**
  194 | 	 * Whether this pattern matches the given path.
  195 | 	 * @param pathContainer the candidate path to attempt to match against
  196 | 	 * @return {@code true} if the path matches this pattern
  197 | 	 */
  198 | 	public boolean matches(PathContainer pathContainer) {
  199 | 		if (this.head == null) {
  200 | 			return !hasLength(pathContainer);
  201 | 		}
  202 | 		else if (!hasLength(pathContainer)) {
  203 | 			if (this.head instanceof WildcardSegmentsPathElement || this.head instanceof CaptureSegmentsPathElement) {
  204 | 				pathContainer = EMPTY_PATH; // Will allow CaptureTheRest to bind the variable to empty
  205 | 			}
  206 | 			else {
  207 | 				return false;
  208 | 			}
  209 | 		}
  210 | 		MatchingContext matchingContext = new MatchingContext(pathContainer, false);
  211 | 		return this.head.matches(0, matchingContext);
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * Match this pattern to the given URI path and return extracted URI template
  216 | 	 * variables as well as path parameters (matrix variables).
  217 | 	 * @param pathContainer the candidate path to attempt to match against
  218 | 	 * @return info object with the extracted variables, or {@code null} for no match
  219 | 	 */
  220 | 	public @Nullable PathMatchInfo matchAndExtract(PathContainer pathContainer) {
  221 | 		if (this.head == null) {
  222 | 			return (hasLength(pathContainer) && !pathContainerIsJustSeparator(pathContainer) ? null : PathMatchInfo.EMPTY);
  223 | 		}
  224 | 		else if (!hasLength(pathContainer)) {
  225 | 			if (this.head instanceof WildcardSegmentsPathElement || this.head instanceof CaptureSegmentsPathElement) {
  226 | 				pathContainer = EMPTY_PATH; // Will allow CaptureTheRest to bind the variable to empty
  227 | 			}
  228 | 			else {
  229 | 				return null;
  230 | 			}
  231 | 		}
  232 | 		MatchingContext matchingContext = new MatchingContext(pathContainer, true);
  233 | 		return this.head.matches(0, matchingContext) ? matchingContext.getPathMatchResult() : null;
  234 | 	}
  235 | 
  236 | 	/**
  237 | 	 * Match the beginning of the given path and return the remaining portion
  238 | 	 * not covered by this pattern. This is useful for matching nested routes
  239 | 	 * where the path is matched incrementally at each level.
  240 | 	 * @param pathContainer the candidate path to attempt to match against
  241 | 	 * @return info object with the match result or {@code null} for no match
  242 | 	 */
  243 | 	public @Nullable PathRemainingMatchInfo matchStartOfPath(PathContainer pathContainer) {
  244 | 		if (this.head == null) {
  245 | 			return new PathRemainingMatchInfo(EMPTY_PATH, pathContainer);
  246 | 		}
  247 | 		else if (!hasLength(pathContainer)) {
  248 | 			return null;
  249 | 		}
  250 | 
  251 | 		MatchingContext matchingContext = new MatchingContext(pathContainer, true);
  252 | 		matchingContext.setMatchAllowExtraPath();
  253 | 		boolean matches = this.head.matches(0, matchingContext);
  254 | 		if (!matches) {
  255 | 			return null;
  256 | 		}
  257 | 		else {
  258 | 			PathContainer pathMatched;
  259 | 			PathContainer pathRemaining;
  260 | 			if (matchingContext.remainingPathIndex == pathContainer.elements().size()) {
  261 | 				pathMatched = pathContainer;
  262 | 				pathRemaining = EMPTY_PATH;
  263 | 			}
  264 | 			else {
  265 | 				pathMatched = pathContainer.subPath(0, matchingContext.remainingPathIndex);
  266 | 				pathRemaining = pathContainer.subPath(matchingContext.remainingPathIndex);
  267 | 			}
  268 | 			return new PathRemainingMatchInfo(pathMatched, pathRemaining, matchingContext.getPathMatchResult());
  269 | 		}
  270 | 	}
  271 | 
  272 | 	/**
  273 | 	 * Determine the pattern-mapped part for the given path.
  274 | 	 * <p>For example: <ul>
  275 | 	 * <li>'{@code /docs/cvs/commit.html}' and '{@code /docs/cvs/commit.html}' &rarr; ''</li>
  276 | 	 * <li>'{@code /docs/*}' and '{@code /docs/cvs/commit}' &rarr; '{@code cvs/commit}'</li>
  277 | 	 * <li>'{@code /docs/cvs/*.html}' and '{@code /docs/cvs/commit.html}' &rarr; '{@code commit.html}'</li>
  278 | 	 * <li>'{@code /docs/**}' and '{@code /docs/cvs/commit}' &rarr; '{@code cvs/commit}'</li>
  279 | 	 * </ul>
  280 | 	 * <p><b>Notes:</b>
  281 | 	 * <ul>
  282 | 	 * <li>Assumes that {@link #matches} returns {@code true} for
  283 | 	 * the same path but does <strong>not</strong> enforce this.
  284 | 	 * <li>Duplicate occurrences of separators within the returned result are removed
  285 | 	 * <li>Leading and trailing separators are removed from the returned result
  286 | 	 * </ul>
  287 | 	 * @param path a path that matches this pattern
  288 | 	 * @return the subset of the path that is matched by pattern or "" if none
  289 | 	 * of it is matched by pattern elements
  290 | 	 */
  291 | 	public PathContainer extractPathWithinPattern(PathContainer path) {
  292 | 		List<Element> pathElements = path.elements();
  293 | 		int pathElementsCount = pathElements.size();
  294 | 
  295 | 		int startIndex = 0;
  296 | 		// Find first path element that is not a separator or a literal (i.e. the first pattern based element)
  297 | 		PathElement elem = this.head;
  298 | 		while (elem != null) {
  299 | 			if (!elem.isLiteral()) {
  300 | 				break;
  301 | 			}
  302 | 			elem = elem.next;
  303 | 			startIndex++;
  304 | 		}
  305 | 		if (elem == null) {
  306 | 			// There is no pattern piece
  307 | 			return PathContainer.parsePath("");
  308 | 		}
  309 | 
  310 | 		// Skip leading separators that would be in the result
  311 | 		while (startIndex < pathElementsCount && (pathElements.get(startIndex) instanceof Separator)) {
  312 | 			startIndex++;
  313 | 		}
  314 | 
  315 | 		int endIndex = pathElements.size();
  316 | 		// Skip trailing separators that would be in the result
  317 | 		while (endIndex > 0 && (pathElements.get(endIndex - 1) instanceof Separator)) {
  318 | 			endIndex--;
  319 | 		}
  320 | 
  321 | 		boolean multipleAdjacentSeparators = false;
  322 | 		for (int i = startIndex; i < (endIndex - 1); i++) {
  323 | 			if ((pathElements.get(i) instanceof Separator) && (pathElements.get(i+1) instanceof Separator)) {
  324 | 				multipleAdjacentSeparators=true;
  325 | 				break;
  326 | 			}
  327 | 		}
  328 | 
  329 | 		PathContainer resultPath = null;
  330 | 		if (multipleAdjacentSeparators) {
  331 | 			// Need to rebuild the path without the duplicate adjacent separators
  332 | 			StringBuilder sb = new StringBuilder();
  333 | 			int i = startIndex;
  334 | 			while (i < endIndex) {
  335 | 				Element e = pathElements.get(i++);
  336 | 				sb.append(e.value());
  337 | 				if (e instanceof Separator) {
  338 | 					while (i < endIndex && (pathElements.get(i) instanceof Separator)) {
  339 | 						i++;
  340 | 					}
  341 | 				}
  342 | 			}
  343 | 			resultPath = PathContainer.parsePath(sb.toString(), this.pathOptions);
  344 | 		}
  345 | 		else if (startIndex >= endIndex) {
  346 | 			resultPath = PathContainer.parsePath("");
  347 | 		}
  348 | 		else {
  349 | 			resultPath = path.subPath(startIndex, endIndex);
  350 | 		}
  351 | 		return resultPath;
  352 | 	}
  353 | 
  354 | 	/**
  355 | 	 * Compare this pattern with a supplied pattern: return -1,0,+1 if this pattern
  356 | 	 * is more specific, the same or less specific than the supplied pattern.
  357 | 	 * The aim is to sort more specific patterns first.
  358 | 	 */
  359 | 	@Override
  360 | 	public int compareTo(@Nullable PathPattern otherPattern) {
  361 | 		int result = SPECIFICITY_COMPARATOR.compare(this, otherPattern);
  362 | 		return (result == 0 && otherPattern != null ?
  363 | 				this.patternString.compareTo(otherPattern.patternString) : result);
  364 | 	}
  365 | 
  366 | 	/**
  367 | 	 * Combine this pattern with the one given as parameter, returning a new
  368 | 	 * {@code PathPattern} instance that concatenates or merges both.
  369 | 	 * This operation is not commutative, meaning {@code pattern1.combine(pattern2)}
  370 | 	 * is not equal to {@code pattern2.combine(pattern1)}.
  371 | 	 *
  372 | 	 * <p>Combining two "fixed" patterns effectively concatenates them:
  373 | 	 * <ul>
  374 | 	 *     <li><code> "/projects" + "/spring-framework" => "/projects/spring-framework"</code>
  375 |  *     </ul>
  376 | 	 * Combining a "fixed" pattern with a "matching" pattern concatenates them:
  377 | 	 * <ul>
  378 | 	 *     <li><code> "/projects" + "/{project}" => "/projects/{project}"</code>
  379 | 	 * </ul>
  380 | 	 * Combining a "matching" pattern with a "fixed" pattern merges them:
  381 | 	 * <ul>
  382 | 	 *     <li><code> "/projects/&#42;" + "/spring-framework" => "/projects/spring-framework"</code>
  383 | 	 *     <li><code> "/projects/&#42;.html" + "/spring-framework.html" => "/projects/spring-framework.html"</code>
  384 | 	 * </ul>
  385 | 	 * Combining two "matching" patterns merges them:
  386 | 	 * <ul>
  387 | 	 *     <li><code> "/projects/&#42;&#42;" + "/&#42;.html" => "/projects/&#42;.html"</code>
  388 | 	 *     <li><code> "/projects/&#42;" + "/{project}" => "/projects/{project}"</code>
  389 | 	 *     <li><code> "/projects/&#42;.html" + "/spring-framework.&#42;" => "/projects/spring-framework.html"</code>
  390 | 	 * </ul>
  391 | 	 * Note, if a pattern does not end with a "matching" segment, it is considered as a "fixed" one:
  392 | 	 * <ul>
  393 | 	 *     <li><code> "/projects/&#42;/releases" + "/{id}" => "/projects/&#42;/releases/{id}"</code>
  394 | 	 * </ul>
  395 | 	 * @param otherPattern the pattern to be combined with the current one
  396 | 	 * @return the new {@code PathPattern} that combines both patterns
  397 | 	 * @throws IllegalArgumentException if the combination is not allowed
  398 | 	 */
  399 | 	public PathPattern combine(PathPattern otherPattern) {
  400 | 		// If one of them is empty, the result is the other. If both are empty, the result is ""
  401 | 		if (!StringUtils.hasLength(this.patternString)) {
  402 | 			if (!StringUtils.hasLength(otherPattern.patternString)) {
  403 | 				return this.parser.parse("");
  404 | 			}
  405 | 			else {
  406 | 				return otherPattern;
  407 | 			}
  408 | 		}
  409 | 		else if (!StringUtils.hasLength(otherPattern.patternString)) {
  410 | 			return this;
  411 | 		}
  412 | 
  413 | 		// /* + /hotel => /hotel
  414 | 		// /*.* + /*.html => /*.html
  415 | 		// However:
  416 | 		// /usr + /user => /usr/user
  417 | 		// /{foo} + /bar => /{foo}/bar
  418 | 		if (!this.patternString.equals(otherPattern.patternString) && this.capturedVariableCount == 0 &&
  419 | 				matches(PathContainer.parsePath(otherPattern.patternString))) {
  420 | 			return otherPattern;
  421 | 		}
  422 | 
  423 | 		// /hotels/* + /booking => /hotels/booking
  424 | 		// /hotels/* + booking => /hotels/booking
  425 | 		if (this.endsWithSeparatorWildcard) {
  426 | 			String prefix = this.patternString.length() > 2 ?
  427 | 					this.patternString.substring(0, this.patternString.length() - 2) :
  428 | 					String.valueOf(getSeparator());
  429 | 			return this.parser.parse(concat(prefix, otherPattern.patternString));
  430 | 		}
  431 | 
  432 | 		// /hotels/** +  "/booking/rooms => /hotels/booking/rooms
  433 | 		if (this.catchAll) {
  434 | 			return this.parser.parse(concat(this.patternString.substring(0, this.patternString.length() - 3),
  435 | 					otherPattern.patternString));
  436 | 		}
  437 | 
  438 | 		// /hotels + /booking => /hotels/booking
  439 | 		// /hotels + booking => /hotels/booking
  440 | 		int firstStarDotPos = this.patternString.indexOf("*.");  // Are there any file prefix/suffix things to consider?
  441 | 		if (this.capturedVariableCount != 0 || firstStarDotPos == -1 || getSeparator() == '.') {
  442 | 			return this.parser.parse(concat(this.patternString, otherPattern.patternString));
  443 | 		}
  444 | 
  445 | 		// /*.html + /hotel => /hotel.html
  446 | 		// /*.html + /hotel.* => /hotel.html
  447 | 		int secondDotPos = otherPattern.patternString.indexOf('.');
  448 | 
  449 | 		String firstExtension = this.patternString.substring(firstStarDotPos + 1);  // looking for the first extension
  450 | 		String secondExtension = (secondDotPos == -1 ? "" : otherPattern.patternString.substring(secondDotPos));
  451 | 		boolean isFirstExtensionWildcard = (firstExtension.equals(".*") || firstExtension.isEmpty());
  452 | 		boolean isSecondExtensionWildcard = (secondExtension.equals(".*") || secondExtension.isEmpty());
  453 | 		if (!isFirstExtensionWildcard && !isSecondExtensionWildcard) {
  454 | 			throw new IllegalArgumentException(
  455 | 					"Cannot combine patterns: " + this.patternString + " and " + otherPattern);
  456 | 		}
  457 | 
  458 | 		String firstPath = this.patternString.substring(0, this.patternString.lastIndexOf(getSeparator()));
  459 | 		String secondPath = otherPattern.patternString.substring(0, otherPattern.patternString.lastIndexOf(getSeparator()));
  460 | 		if (!this.parser.parse(firstPath).matches(PathContainer.parsePath(secondPath))) {
  461 | 			throw new IllegalArgumentException(
  462 | 					"Cannot combine patterns: " + this.patternString + " and " + otherPattern);
  463 | 		}
  464 | 
  465 | 		String secondFile = (secondDotPos == -1 ? otherPattern.patternString : otherPattern.patternString.substring(0, secondDotPos));
  466 | 		return this.parser.parse(secondFile + (isFirstExtensionWildcard ? secondExtension : firstExtension));
  467 | 	}
  468 | 
  469 | 	@Override
  470 | 	public boolean equals(@Nullable Object other) {
  471 | 		return (this == other || (other instanceof PathPattern that &&
  472 | 				this.patternString.equals(that.getPatternString()) &&
  473 | 				getSeparator() == that.getSeparator() &&
  474 | 				this.caseSensitive == that.caseSensitive));
  475 | 	}
  476 | 
  477 | 	@Override
  478 | 	public int hashCode() {
  479 | 		return (this.patternString.hashCode() + getSeparator()) * 17 + (this.caseSensitive ? 1 : 0);
  480 | 	}
  481 | 
  482 | 	@Override
  483 | 	public String toString() {
  484 | 		return this.patternString;
  485 | 	}
  486 | 
  487 | 
  488 | 	int getScore() {
  489 | 		return this.score;
  490 | 	}
  491 | 
  492 | 	boolean isCatchAll() {
  493 | 		return this.catchAll;
  494 | 	}
  495 | 
  496 | 	/**
  497 | 	 * The normalized length is trying to measure the 'active' part of the pattern. It is computed
  498 | 	 * by assuming all capture variables have a normalized length of 1. Effectively this means changing
  499 | 	 * your variable name lengths isn't going to change the length of the active part of the pattern.
  500 | 	 * Useful when comparing two patterns.
  501 | 	 */
  502 | 	int getNormalizedLength() {
  503 | 		return this.normalizedLength;
  504 | 	}
  505 | 
  506 | 	char getSeparator() {
  507 | 		return this.pathOptions.separator();
  508 | 	}
  509 | 
  510 | 	int getCapturedVariableCount() {
  511 | 		return this.capturedVariableCount;
  512 | 	}
  513 | 
  514 | 	String toChainString() {
  515 | 		StringJoiner stringJoiner = new StringJoiner(" ");
  516 | 		PathElement pe = this.head;
  517 | 		while (pe != null) {
  518 | 			stringJoiner.add(pe.toString());
  519 | 			pe = pe.next;
  520 | 		}
  521 | 		return stringJoiner.toString();
  522 | 	}
  523 | 
  524 | 	/**
  525 | 	 * Return the string form of the pattern built from walking the path element chain.
  526 | 	 * @return the string form of the pattern
  527 | 	 */
  528 | 	String computePatternString() {
  529 | 		StringBuilder sb = new StringBuilder();
  530 | 		PathElement pe = this.head;
  531 | 		while (pe != null) {
  532 | 			sb.append(pe.getChars());
  533 | 			pe = pe.next;
  534 | 		}
  535 | 		return sb.toString();
  536 | 	}
  537 | 
  538 | 	@Nullable PathElement getHeadSection() {
  539 | 		return this.head;
  540 | 	}
  541 | 
  542 | 	/**
  543 | 	 * Join two paths together including a separator if necessary.
  544 | 	 * Extraneous separators are removed (if the first path
  545 | 	 * ends with one and the second path starts with one).
  546 | 	 * @param path1 first path
  547 | 	 * @param path2 second path
  548 | 	 * @return joined path that may include separator if necessary
  549 | 	 */
  550 | 	private String concat(String path1, String path2) {
  551 | 		boolean path1EndsWithSeparator = (path1.charAt(path1.length() - 1) == getSeparator());
  552 | 		boolean path2StartsWithSeparator = (path2.charAt(0) == getSeparator());
  553 | 		if (path1EndsWithSeparator && path2StartsWithSeparator) {
  554 | 			return path1 + path2.substring(1);
  555 | 		}
  556 | 		else if (path1EndsWithSeparator || path2StartsWithSeparator) {
  557 | 			return path1 + path2;
  558 | 		}
  559 | 		else {
  560 | 			return path1 + getSeparator() + path2;
  561 | 		}
  562 | 	}
  563 | 
  564 | 	/**
  565 | 	 * Return if the container is not null and has more than zero elements.
  566 | 	 * @param container a path container
  567 | 	 * @return {@code true} has more than zero elements
  568 | 	 */
  569 | 	private boolean hasLength(@Nullable PathContainer container) {
  570 | 		return container != null && !container.elements().isEmpty();
  571 | 	}
  572 | 
  573 | 	private static int scoreByNormalizedLength(PathPattern pattern) {
  574 | 		return -pattern.getNormalizedLength();
  575 | 	}
  576 | 
  577 | 	private boolean pathContainerIsJustSeparator(PathContainer pathContainer) {
  578 | 		return pathContainer.value().length() == 1 &&
  579 | 				pathContainer.value().charAt(0) == getSeparator();
  580 | 	}
  581 | 
  582 | 
  583 | 	/**
  584 | 	 * Holder for URI variables and path parameters (matrix variables) extracted
  585 | 	 * based on the pattern for a given matched path.
  586 | 	 */
  587 | 	public static class PathMatchInfo {
  588 | 
  589 | 		private static final PathMatchInfo EMPTY = new PathMatchInfo(Collections.emptyMap(), Collections.emptyMap());
  590 | 
  591 | 		private final Map<String, String> uriVariables;
  592 | 
  593 | 		private final Map<String, MultiValueMap<String, String>> matrixVariables;
  594 | 
  595 | 		PathMatchInfo(Map<String, String> uriVars, @Nullable Map<String, MultiValueMap<String, String>> matrixVars) {
  596 | 			this.uriVariables = Collections.unmodifiableMap(uriVars);
  597 | 			this.matrixVariables = (matrixVars != null ?
  598 | 					Collections.unmodifiableMap(matrixVars) : Collections.emptyMap());
  599 | 		}
  600 | 
  601 | 		/**
  602 | 		 * Return the extracted URI variables.
  603 | 		 */
  604 | 		public Map<String, String> getUriVariables() {
  605 | 			return this.uriVariables;
  606 | 		}
  607 | 
  608 | 		/**
  609 | 		 * Return maps of matrix variables per path segment, keyed off by URI
  610 | 		 * variable name.
  611 | 		 */
  612 | 		public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
  613 | 			return this.matrixVariables;
  614 | 		}
  615 | 
  616 | 		@Override
  617 | 		public String toString() {
  618 | 			return "PathMatchInfo[uriVariables=" + this.uriVariables + ", " +
  619 | 					"matrixVariables=" + this.matrixVariables + "]";
  620 | 		}
  621 | 	}
  622 | 
  623 | 
  624 | 	/**
  625 | 	 * Holder for the result of a match on the start of a pattern.
  626 | 	 * Provides access to the remaining path not matched to the pattern as well
  627 | 	 * as any variables bound in that first part that was matched.
  628 | 	 */
  629 | 	public static class PathRemainingMatchInfo {
  630 | 
  631 | 		private final PathContainer pathMatched;
  632 | 
  633 | 		private final PathContainer pathRemaining;
  634 | 
  635 | 		private final PathMatchInfo pathMatchInfo;
  636 | 
  637 | 		PathRemainingMatchInfo(PathContainer pathMatched, PathContainer pathRemaining) {
  638 | 			this(pathMatched, pathRemaining, PathMatchInfo.EMPTY);
  639 | 		}
  640 | 
  641 | 		PathRemainingMatchInfo(PathContainer pathMatched, PathContainer pathRemaining, PathMatchInfo pathMatchInfo) {
  642 | 			this.pathRemaining = pathRemaining;
  643 | 			this.pathMatched = pathMatched;
  644 | 			this.pathMatchInfo = pathMatchInfo;
  645 | 		}
  646 | 
  647 | 		/**
  648 | 		 * Return the part of a path that was matched by a pattern.
  649 | 		 * @since 5.3
  650 | 		 */
  651 | 		public PathContainer getPathMatched() {
  652 | 			return this.pathMatched;
  653 | 		}
  654 | 
  655 | 		/**
  656 | 		 * Return the part of a path that was not matched by a pattern.
  657 | 		 */
  658 | 		public PathContainer getPathRemaining() {
  659 | 			return this.pathRemaining;
  660 | 		}
  661 | 
  662 | 		/**
  663 | 		 * Return variables that were bound in the part of the path that was
  664 | 		 * successfully matched or an empty map.
  665 | 		 */
  666 | 		public Map<String, String> getUriVariables() {
  667 | 			return this.pathMatchInfo.getUriVariables();
  668 | 		}
  669 | 
  670 | 		/**
  671 | 		 * Return the path parameters for each bound variable.
  672 | 		 */
  673 | 		public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
  674 | 			return this.pathMatchInfo.getMatrixVariables();
  675 | 		}
  676 | 	}
  677 | 
  678 | 
  679 | 	/**
  680 | 	 * Encapsulates context when attempting a match. Includes some fixed state like the
  681 | 	 * candidate currently being considered for a match but also some accumulators for
  682 | 	 * extracted variables.
  683 | 	 */
  684 | 	static class MatchingContext {
  685 | 
  686 | 		final PathContainer candidate;
  687 | 
  688 | 		final List<Element> pathElements;
  689 | 
  690 | 		final int pathLength;
  691 | 
  692 | 		private @Nullable Map<String, String> extractedUriVariables;
  693 | 
  694 | 		private @Nullable Map<String, MultiValueMap<String, String>> extractedMatrixVariables;
  695 | 
  696 | 		boolean extractingVariables;
  697 | 
  698 | 		boolean determineRemainingPath = false;
  699 | 
  700 | 		// if determineRemaining is true, this is set to the position in
  701 | 		// the candidate where the pattern finished matching - i.e. it
  702 | 		// points to the remaining path that wasn't consumed
  703 | 		int remainingPathIndex;
  704 | 
  705 | 		public MatchingContext(PathContainer pathContainer, boolean extractVariables) {
  706 | 			this.candidate = pathContainer;
  707 | 			this.pathElements = pathContainer.elements();
  708 | 			this.pathLength = this.pathElements.size();
  709 | 			this.extractingVariables = extractVariables;
  710 | 		}
  711 | 
  712 | 		public void setMatchAllowExtraPath() {
  713 | 			this.determineRemainingPath = true;
  714 | 		}
  715 | 
  716 | 		public void set(String key, String value, MultiValueMap<String,String> parameters) {
  717 | 			if (this.extractedUriVariables == null) {
  718 | 				this.extractedUriVariables = new HashMap<>();
  719 | 			}
  720 | 			this.extractedUriVariables.put(key, value);
  721 | 
  722 | 			if (!parameters.isEmpty()) {
  723 | 				if (this.extractedMatrixVariables == null) {
  724 | 					this.extractedMatrixVariables = new HashMap<>();
  725 | 				}
  726 | 				this.extractedMatrixVariables.put(key, CollectionUtils.unmodifiableMultiValueMap(parameters));
  727 | 			}
  728 | 		}
  729 | 
  730 | 		public PathMatchInfo getPathMatchResult() {
  731 | 			if (this.extractedUriVariables == null) {
  732 | 				return PathMatchInfo.EMPTY;
  733 | 			}
  734 | 			else {
  735 | 				return new PathMatchInfo(this.extractedUriVariables, this.extractedMatrixVariables);
  736 | 			}
  737 | 		}
  738 | 
  739 | 		/**
  740 | 		 * Return if element at specified index is a separator.
  741 | 		 * @param pathIndex possible index of a separator
  742 | 		 * @return {@code true} if element is a separator
  743 | 		 */
  744 | 		boolean isSeparator(int pathIndex) {
  745 | 			return this.pathElements.get(pathIndex) instanceof Separator;
  746 | 		}
  747 | 
  748 | 		/**
  749 | 		 * Return the decoded value of the specified element.
  750 | 		 * @param pathIndex path element index
  751 | 		 * @return the decoded value
  752 | 		 */
  753 | 		String pathElementValue(int pathIndex) {
  754 | 			Element element = (pathIndex < this.pathLength) ? this.pathElements.get(pathIndex) : null;
  755 | 			return (element instanceof PathSegment pathSegment ? pathSegment.valueToMatch() : "");
  756 | 		}
  757 | 	}
  758 | 
  759 | }
```
