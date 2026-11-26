<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/bind/validation/ValidationBindHandler.java -->
<!-- 正文引用：[[源码:ValidationBindHandler.java#L起-L止]] ；代码块头部：(src: ValidationBindHandler.java#L起-L止) -->

# ValidationBindHandler.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/bind/validation/ValidationBindHandler.java` |
| 行数 | 242 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:53.680Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.bind.validation;
    6 | 
    7 | import java.util.LinkedHashMap;
    8 | import java.util.LinkedHashSet;
    9 | import java.util.Map;
   10 | import java.util.Set;
   11 | import java.util.stream.Collectors;
   12 | 
   13 | import org.jspecify.annotations.Nullable;
   14 | 
   15 | import org.springframework.beans.NotReadablePropertyException;
   16 | import org.springframework.boot.context.properties.bind.AbstractBindHandler;
   17 | import org.springframework.boot.context.properties.bind.BindContext;
   18 | import org.springframework.boot.context.properties.bind.BindHandler;
   19 | import org.springframework.boot.context.properties.bind.Bindable;
   20 | import org.springframework.boot.context.properties.bind.DataObjectPropertyName;
   21 | import org.springframework.boot.context.properties.source.ConfigurationProperty;
   22 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
   23 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
   24 | import org.springframework.core.ResolvableType;
   25 | import org.springframework.util.ObjectUtils;
   26 | import org.springframework.validation.AbstractBindingResult;
   27 | import org.springframework.validation.BeanPropertyBindingResult;
   28 | import org.springframework.validation.Validator;
   29 | 
   30 | /**
   31 |  * {@link BindHandler} to apply {@link Validator Validators} to bound results.
   32 |  *
   33 |  * @author Phillip Webb
   34 |  * @author Madhura Bhave
   35 |  * @since 2.0.0
   36 |  */
   37 | public class ValidationBindHandler extends AbstractBindHandler {
   38 | 
   39 | 	private final Validator[] validators;
   40 | 
   41 | 	private final Map<ConfigurationPropertyName, ResolvableType> boundTypes = new LinkedHashMap<>();
   42 | 
   43 | 	private final Map<ConfigurationPropertyName, Object> boundResults = new LinkedHashMap<>();
   44 | 
   45 | 	private final Set<ConfigurationProperty> boundProperties = new LinkedHashSet<>();
   46 | 
   47 | 	private @Nullable BindValidationException exception;
   48 | 
   49 | 	public ValidationBindHandler(Validator... validators) {
   50 | 		this.validators = validators;
   51 | 	}
   52 | 
   53 | 	public ValidationBindHandler(BindHandler parent, Validator... validators) {
   54 | 		super(parent);
   55 | 		this.validators = validators;
   56 | 	}
   57 | 
   58 | 	@Override
   59 | 	public <T> @Nullable Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
   60 | 		this.boundTypes.put(name, target.getType());
   61 | 		return super.onStart(name, target, context);
   62 | 	}
   63 | 
   64 | 	@Override
   65 | 	public @Nullable Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
   66 | 			Object result) {
   67 | 		this.boundResults.put(name, result);
   68 | 		if (context.getConfigurationProperty() != null) {
   69 | 			this.boundProperties.add(context.getConfigurationProperty());
   70 | 		}
   71 | 		return super.onSuccess(name, target, context, result);
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public @Nullable Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
   76 | 			Exception error) throws Exception {
   77 | 		Object result = super.onFailure(name, target, context, error);
   78 | 		if (result != null) {
   79 | 			clear();
   80 | 			this.boundResults.put(name, result);
   81 | 		}
   82 | 		validate(name, target, context, result);
   83 | 		return result;
   84 | 	}
   85 | 
   86 | 	private void clear() {
   87 | 		this.boundTypes.clear();
   88 | 		this.boundResults.clear();
   89 | 		this.boundProperties.clear();
   90 | 		this.exception = null;
   91 | 	}
   92 | 
   93 | 	@Override
   94 | 	public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
   95 | 			@Nullable Object result) throws Exception {
   96 | 		validate(name, target, context, result);
   97 | 		super.onFinish(name, target, context, result);
   98 | 	}
   99 | 
  100 | 	private void validate(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
  101 | 			@Nullable Object result) {
  102 | 		if (this.exception == null) {
  103 | 			Object validationTarget = getValidationTarget(target, context, result);
  104 | 			Class<?> validationType = target.getBoxedType().resolve();
  105 | 			if (validationTarget != null && validationType != null) {
  106 | 				validateAndPush(name, validationTarget, validationType);
  107 | 			}
  108 | 		}
  109 | 		if (context.getDepth() == 0 && this.exception != null) {
  110 | 			throw this.exception;
  111 | 		}
  112 | 	}
  113 | 
  114 | 	private @Nullable Object getValidationTarget(Bindable<?> target, BindContext context, @Nullable Object result) {
  115 | 		if (result != null) {
  116 | 			return result;
  117 | 		}
  118 | 		if (context.getDepth() == 0 && target.getValue() != null) {
  119 | 			return target.getValue().get();
  120 | 		}
  121 | 		return null;
  122 | 	}
  123 | 
  124 | 	private void validateAndPush(ConfigurationPropertyName name, Object target, Class<?> type) {
  125 | 		ValidationResult result = null;
  126 | 		for (Validator validator : this.validators) {
  127 | 			if (validator.supports(type)) {
  128 | 				result = (result != null) ? result : new ValidationResult(name, target);
  129 | 				validator.validate(target, result);
  130 | 			}
  131 | 		}
  132 | 		if (result != null && result.hasErrors()) {
  133 | 			this.exception = new BindValidationException(result.getValidationErrors());
  134 | 		}
  135 | 	}
  136 | 
  137 | 	/**
  138 | 	 * {@link AbstractBindingResult} implementation backed by the bound properties.
  139 | 	 */
  140 | 	private class ValidationResult extends BeanPropertyBindingResult {
  141 | 
  142 | 		private final ConfigurationPropertyName name;
  143 | 
  144 | 		protected ValidationResult(ConfigurationPropertyName name, Object target) {
  145 | 			super(target, "");
  146 | 			this.name = name;
  147 | 		}
  148 | 
  149 | 		@Override
  150 | 		public String getObjectName() {
  151 | 			return this.name.toString();
  152 | 		}
  153 | 
  154 | 		@Override
  155 | 		public @Nullable Class<?> getFieldType(@Nullable String field) {
  156 | 			ResolvableType type = getBoundField(ValidationBindHandler.this.boundTypes, field);
  157 | 			Class<?> resolved = (type != null) ? type.resolve() : null;
  158 | 			if (resolved != null) {
  159 | 				return resolved;
  160 | 			}
  161 | 			return super.getFieldType(field);
  162 | 		}
  163 | 
  164 | 		@Override
  165 | 		protected @Nullable Object getActualFieldValue(String field) {
  166 | 			Object boundField = getBoundField(ValidationBindHandler.this.boundResults, field);
  167 | 			if (boundField != null) {
  168 | 				return boundField;
  169 | 			}
  170 | 			try {
  171 | 				return super.getActualFieldValue(field);
  172 | 			}
  173 | 			catch (Exception ex) {
  174 | 				if (isPropertyNotReadable(ex)) {
  175 | 					return null;
  176 | 				}
  177 | 				throw ex;
  178 | 			}
  179 | 		}
  180 | 
  181 | 		private boolean isPropertyNotReadable(Throwable ex) {
  182 | 			while (ex != null) {
  183 | 				if (ex instanceof NotReadablePropertyException) {
  184 | 					return true;
  185 | 				}
  186 | 				ex = ex.getCause();
  187 | 			}
  188 | 			return false;
  189 | 		}
  190 | 
  191 | 		private <T> @Nullable T getBoundField(Map<ConfigurationPropertyName, T> boundFields, @Nullable String field) {
  192 | 			if (field == null) {
  193 | 				return null;
  194 | 			}
  195 | 			try {
  196 | 				ConfigurationPropertyName name = getName(field);
  197 | 				T bound = boundFields.get(name);
  198 | 				if (bound != null) {
  199 | 					return bound;
  200 | 				}
  201 | 				if (name.hasIndexedElement()) {
  202 | 					for (Map.Entry<ConfigurationPropertyName, T> entry : boundFields.entrySet()) {
  203 | 						if (isFieldNameMatch(entry.getKey(), name)) {
  204 | 							return entry.getValue();
  205 | 						}
  206 | 					}
  207 | 				}
  208 | 			}
  209 | 			catch (Exception ex) {
  210 | 				// Ignore
  211 | 			}
  212 | 			return null;
  213 | 		}
  214 | 
  215 | 		private boolean isFieldNameMatch(ConfigurationPropertyName name, ConfigurationPropertyName fieldName) {
  216 | 			if (name.getNumberOfElements() != fieldName.getNumberOfElements()) {
  217 | 				return false;
  218 | 			}
  219 | 			for (int i = 0; i < name.getNumberOfElements(); i++) {
  220 | 				String element = name.getElement(i, Form.ORIGINAL);
  221 | 				String fieldElement = fieldName.getElement(i, Form.ORIGINAL);
  222 | 				if (!ObjectUtils.nullSafeEquals(element, fieldElement)) {
  223 | 					return false;
  224 | 				}
  225 | 			}
  226 | 			return true;
  227 | 		}
  228 | 
  229 | 		private ConfigurationPropertyName getName(String field) {
  230 | 			return this.name.append(DataObjectPropertyName.toDashedForm(field));
  231 | 		}
  232 | 
  233 | 		ValidationErrors getValidationErrors() {
  234 | 			Set<ConfigurationProperty> boundProperties = ValidationBindHandler.this.boundProperties.stream()
  235 | 				.filter((property) -> this.name.isAncestorOf(property.getName()))
  236 | 				.collect(Collectors.toCollection(LinkedHashSet::new));
  237 | 			return new ValidationErrors(this.name, boundProperties, getAllErrors());
  238 | 		}
  239 | 
  240 | 	}
  241 | 
  242 | }
```
