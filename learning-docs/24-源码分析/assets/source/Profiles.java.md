<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/config/Profiles.java -->
<!-- 正文引用：[[源码:Profiles.java#L起-L止]] ；代码块头部：(src: Profiles.java#L起-L止) -->

# Profiles.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/config/Profiles.java` |
| 行数 | 257 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:54.885Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.config;
    6 | 
    7 | import java.util.ArrayDeque;
    8 | import java.util.ArrayList;
    9 | import java.util.Arrays;
   10 | import java.util.Collection;
   11 | import java.util.Collections;
   12 | import java.util.Deque;
   13 | import java.util.Iterator;
   14 | import java.util.LinkedHashSet;
   15 | import java.util.List;
   16 | import java.util.Set;
   17 | import java.util.function.Function;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.boot.context.properties.bind.BindResult;
   22 | import org.springframework.boot.context.properties.bind.Bindable;
   23 | import org.springframework.boot.context.properties.bind.Binder;
   24 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
   25 | import org.springframework.core.ResolvableType;
   26 | import org.springframework.core.env.AbstractEnvironment;
   27 | import org.springframework.core.env.Environment;
   28 | import org.springframework.core.style.ToStringCreator;
   29 | import org.springframework.util.CollectionUtils;
   30 | import org.springframework.util.LinkedMultiValueMap;
   31 | import org.springframework.util.MultiValueMap;
   32 | import org.springframework.util.StringUtils;
   33 | 
   34 | /**
   35 |  * Provides access to environment profiles that have either been set directly on the
   36 |  * {@link Environment} or will be set based on configuration data property values.
   37 |  *
   38 |  * @author Phillip Webb
   39 |  * @author Madhura Bhave
   40 |  * @since 2.4.0
   41 |  */
   42 | public class Profiles implements Iterable<String> {
   43 | 
   44 | 	/**
   45 | 	 * Name of property to set to specify additionally included active profiles.
   46 | 	 */
   47 | 	public static final String INCLUDE_PROFILES_PROPERTY_NAME = "spring.profiles.include";
   48 | 
   49 | 	static final ConfigurationPropertyName INCLUDE_PROFILES = ConfigurationPropertyName
   50 | 		.of(Profiles.INCLUDE_PROFILES_PROPERTY_NAME);
   51 | 
   52 | 	private static final Bindable<MultiValueMap<String, String>> STRING_STRINGS_MAP = Bindable
   53 | 		.of(ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class));
   54 | 
   55 | 	private static final Bindable<Set<String>> STRING_SET = Bindable.setOf(String.class);
   56 | 
   57 | 	private final MultiValueMap<String, String> groups;
   58 | 
   59 | 	private final List<String> activeProfiles;
   60 | 
   61 | 	private final List<String> defaultProfiles;
   62 | 
   63 | 	/**
   64 | 	 * Create a new {@link Profiles} instance based on the {@link Environment} and
   65 | 	 * {@link Binder}.
   66 | 	 * @param environment the source environment
   67 | 	 * @param binder the binder for profile properties
   68 | 	 * @param additionalProfiles any additional active profiles
   69 | 	 */
   70 | 	Profiles(Environment environment, Binder binder, @Nullable Collection<String> additionalProfiles) {
   71 | 		ProfilesValidator validator = ProfilesValidator.get(binder);
   72 | 		if (additionalProfiles != null) {
   73 | 			validator.validate(additionalProfiles, () -> "Invalid profile property value found in additional profiles");
   74 | 		}
   75 | 		this.groups = binder.bind("spring.profiles.group", STRING_STRINGS_MAP, validator)
   76 | 			.orElseGet(LinkedMultiValueMap::new);
   77 | 		this.activeProfiles = expandProfiles(getActivatedProfiles(environment, binder, validator, additionalProfiles));
   78 | 		this.defaultProfiles = expandProfiles(getDefaultProfiles(environment, binder, validator));
   79 | 	}
   80 | 
   81 | 	private List<String> getActivatedProfiles(Environment environment, Binder binder, ProfilesValidator validator,
   82 | 			@Nullable Collection<String> additionalProfiles) {
   83 | 		return asUniqueItemList(getProfiles(environment, binder, validator, Type.ACTIVE), additionalProfiles);
   84 | 	}
   85 | 
   86 | 	private List<String> getDefaultProfiles(Environment environment, Binder binder, ProfilesValidator validator) {
   87 | 		return asUniqueItemList(getProfiles(environment, binder, validator, Type.DEFAULT));
   88 | 	}
   89 | 
   90 | 	private Collection<String> getProfiles(Environment environment, Binder binder, ProfilesValidator validator,
   91 | 			Type type) {
   92 | 		String environmentPropertyValue = environment.getProperty(type.getName());
   93 | 		Set<String> environmentPropertyProfiles = (!StringUtils.hasLength(environmentPropertyValue))
   94 | 				? Collections.emptySet()
   95 | 				: StringUtils.commaDelimitedListToSet(StringUtils.trimAllWhitespace(environmentPropertyValue));
   96 | 		validator.validate(environmentPropertyProfiles,
   97 | 				() -> "Invalid profile property value found in Environment under '%s'".formatted(type.getName()));
   98 | 		Set<String> environmentProfiles = new LinkedHashSet<>(Arrays.asList(type.get(environment)));
   99 | 		BindResult<Set<String>> boundProfiles = binder.bind(type.getName(), STRING_SET, validator);
  100 | 		if (hasProgrammaticallySetProfiles(type, environmentPropertyValue, environmentPropertyProfiles,
  101 | 				environmentProfiles)) {
  102 | 			if (!type.isMergeWithEnvironmentProfiles() || !boundProfiles.isBound()) {
  103 | 				return environmentProfiles;
  104 | 			}
  105 | 			return boundProfiles.map((bound) -> merge(environmentProfiles, bound)).get();
  106 | 		}
  107 | 		return boundProfiles.orElse(type.getDefaultValue());
  108 | 	}
  109 | 
  110 | 	private boolean hasProgrammaticallySetProfiles(Type type, @Nullable String environmentPropertyValue,
  111 | 			Set<String> environmentPropertyProfiles, Set<String> environmentProfiles) {
  112 | 		if (!StringUtils.hasLength(environmentPropertyValue)) {
  113 | 			return !type.getDefaultValue().equals(environmentProfiles);
  114 | 		}
  115 | 		if (type.getDefaultValue().equals(environmentProfiles)) {
  116 | 			return false;
  117 | 		}
  118 | 		return !environmentPropertyProfiles.equals(environmentProfiles);
  119 | 	}
  120 | 
  121 | 	private Set<String> merge(Set<String> environmentProfiles, Set<String> bound) {
  122 | 		Set<String> result = new LinkedHashSet<>(environmentProfiles);
  123 | 		result.addAll(bound);
  124 | 		return result;
  125 | 	}
  126 | 
  127 | 	private List<String> expandProfiles(@Nullable List<String> profiles) {
  128 | 		Deque<String> stack = new ArrayDeque<>();
  129 | 		asReversedList(profiles).forEach(stack::push);
  130 | 		Set<String> expandedProfiles = new LinkedHashSet<>();
  131 | 		while (!stack.isEmpty()) {
  132 | 			String current = stack.pop();
  133 | 			if (expandedProfiles.add(current)) {
  134 | 				asReversedList(this.groups.get(current)).forEach(stack::push);
  135 | 			}
  136 | 		}
  137 | 		return asUniqueItemList(expandedProfiles);
  138 | 	}
  139 | 
  140 | 	private List<String> asReversedList(@Nullable List<String> list) {
  141 | 		if (CollectionUtils.isEmpty(list)) {
  142 | 			return Collections.emptyList();
  143 | 		}
  144 | 		List<String> reversed = new ArrayList<>(list);
  145 | 		Collections.reverse(reversed);
  146 | 		return reversed;
  147 | 	}
  148 | 
  149 | 	private List<String> asUniqueItemList(Collection<String> profiles) {
  150 | 		return asUniqueItemList(profiles, null);
  151 | 	}
  152 | 
  153 | 	private List<String> asUniqueItemList(Collection<String> profiles, @Nullable Collection<String> additional) {
  154 | 		LinkedHashSet<String> uniqueItems = new LinkedHashSet<>();
  155 | 		if (!CollectionUtils.isEmpty(additional)) {
  156 | 			uniqueItems.addAll(additional);
  157 | 		}
  158 | 		uniqueItems.addAll(profiles);
  159 | 		return Collections.unmodifiableList(new ArrayList<>(uniqueItems));
  160 | 	}
  161 | 
  162 | 	/**
  163 | 	 * Return an iterator for all {@link #getAccepted() accepted profiles}.
  164 | 	 */
  165 | 	@Override
  166 | 	public Iterator<String> iterator() {
  167 | 		return getAccepted().iterator();
  168 | 	}
  169 | 
  170 | 	/**
  171 | 	 * Return the active profiles.
  172 | 	 * @return the active profiles
  173 | 	 */
  174 | 	public List<String> getActive() {
  175 | 		return this.activeProfiles;
  176 | 	}
  177 | 
  178 | 	/**
  179 | 	 * Return the default profiles.
  180 | 	 * @return the active profiles
  181 | 	 */
  182 | 	public List<String> getDefault() {
  183 | 		return this.defaultProfiles;
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Return the accepted profiles.
  188 | 	 * @return the accepted profiles
  189 | 	 */
  190 | 	public List<String> getAccepted() {
  191 | 		return (!this.activeProfiles.isEmpty()) ? this.activeProfiles : this.defaultProfiles;
  192 | 	}
  193 | 
  194 | 	/**
  195 | 	 * Return if the given profile is active.
  196 | 	 * @param profile the profile to test
  197 | 	 * @return if the profile is active
  198 | 	 */
  199 | 	public boolean isAccepted(String profile) {
  200 | 		return getAccepted().contains(profile);
  201 | 	}
  202 | 
  203 | 	@Override
  204 | 	public String toString() {
  205 | 		ToStringCreator creator = new ToStringCreator(this);
  206 | 		creator.append("active", getActive().toString());
  207 | 		creator.append("default", getDefault().toString());
  208 | 		creator.append("accepted", getAccepted().toString());
  209 | 		return creator.toString();
  210 | 	}
  211 | 
  212 | 	/**
  213 | 	 * A profiles type that can be obtained.
  214 | 	 */
  215 | 	private enum Type {
  216 | 
  217 | 		ACTIVE(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, Environment::getActiveProfiles, true,
  218 | 				Collections.emptySet()),
  219 | 
  220 | 		DEFAULT(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, Environment::getDefaultProfiles, false,
  221 | 				Collections.singleton("default"));
  222 | 
  223 | 		private final Function<Environment, String[]> getter;
  224 | 
  225 | 		private final boolean mergeWithEnvironmentProfiles;
  226 | 
  227 | 		private final String name;
  228 | 
  229 | 		private final Set<String> defaultValue;
  230 | 
  231 | 		Type(String name, Function<Environment, String[]> getter, boolean mergeWithEnvironmentProfiles,
  232 | 				Set<String> defaultValue) {
  233 | 			this.name = name;
  234 | 			this.getter = getter;
  235 | 			this.mergeWithEnvironmentProfiles = mergeWithEnvironmentProfiles;
  236 | 			this.defaultValue = defaultValue;
  237 | 		}
  238 | 
  239 | 		String getName() {
  240 | 			return this.name;
  241 | 		}
  242 | 
  243 | 		String[] get(Environment environment) {
  244 | 			return this.getter.apply(environment);
  245 | 		}
  246 | 
  247 | 		Set<String> getDefaultValue() {
  248 | 			return this.defaultValue;
  249 | 		}
  250 | 
  251 | 		boolean isMergeWithEnvironmentProfiles() {
  252 | 			return this.mergeWithEnvironmentProfiles;
  253 | 		}
  254 | 
  255 | 	}
  256 | 
  257 | }
```
