<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/annotation/SpringTransactionAnnotationParser.java -->
<!-- 正文引用：[[源码:SpringTransactionAnnotationParser.java#L起-L止]] ；代码块头部：(src: SpringTransactionAnnotationParser.java#L起-L止) -->

# SpringTransactionAnnotationParser.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/annotation/SpringTransactionAnnotationParser.java` |
| 行数 | 117 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:13.811Z |

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
   17 | package org.springframework.transaction.annotation;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.lang.reflect.AnnotatedElement;
   21 | import java.util.ArrayList;
   22 | import java.util.List;
   23 | import java.util.Set;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.core.annotation.AnnotatedElementUtils;
   28 | import org.springframework.core.annotation.AnnotationAttributes;
   29 | import org.springframework.core.annotation.AnnotationUtils;
   30 | import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
   31 | import org.springframework.transaction.interceptor.RollbackRuleAttribute;
   32 | import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
   33 | import org.springframework.transaction.interceptor.TransactionAttribute;
   34 | import org.springframework.util.Assert;
   35 | import org.springframework.util.StringUtils;
   36 | 
   37 | /**
   38 |  * Strategy implementation for parsing Spring's {@link Transactional} annotation.
   39 |  *
   40 |  * @author Juergen Hoeller
   41 |  * @author Mark Paluch
   42 |  * @since 2.5
   43 |  * @see JtaTransactionAnnotationParser
   44 |  * @see Ejb3TransactionAnnotationParser
   45 |  */
   46 | @SuppressWarnings("serial")
   47 | public class SpringTransactionAnnotationParser implements TransactionAnnotationParser, Serializable {
   48 | 
   49 | 	@Override
   50 | 	public boolean isCandidateClass(Class<?> targetClass) {
   51 | 		return AnnotationUtils.isCandidateClass(targetClass, Transactional.class);
   52 | 	}
   53 | 
   54 | 	@Override
   55 | 	public @Nullable TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
   56 | 		AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
   57 | 				element, Transactional.class, false, false);
   58 | 		if (attributes != null) {
   59 | 			return parseTransactionAnnotation(attributes);
   60 | 		}
   61 | 		else {
   62 | 			return null;
   63 | 		}
   64 | 	}
   65 | 
   66 | 	public TransactionAttribute parseTransactionAnnotation(Transactional ann) {
   67 | 		return parseTransactionAnnotation(AnnotationUtils.getAnnotationAttributes(ann, false, false));
   68 | 	}
   69 | 
   70 | 	protected TransactionAttribute parseTransactionAnnotation(AnnotationAttributes attributes) {
   71 | 		RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
   72 | 
   73 | 		Propagation propagation = attributes.getEnum("propagation");
   74 | 		rbta.setPropagationBehavior(propagation.value());
   75 | 		Isolation isolation = attributes.getEnum("isolation");
   76 | 		rbta.setIsolationLevel(isolation.value());
   77 | 
   78 | 		rbta.setTimeout(attributes.getNumber("timeout").intValue());
   79 | 		String timeoutString = attributes.getString("timeoutString");
   80 | 		Assert.isTrue(!StringUtils.hasText(timeoutString) || rbta.getTimeout() < 0,
   81 | 				"Specify 'timeout' or 'timeoutString', not both");
   82 | 		rbta.setTimeoutString(timeoutString);
   83 | 
   84 | 		rbta.setReadOnly(attributes.getBoolean("readOnly"));
   85 | 		rbta.setQualifier(attributes.getString("value"));
   86 | 		rbta.setLabels(Set.of(attributes.getStringArray("label")));
   87 | 
   88 | 		List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();
   89 | 		for (Class<?> rbRule : attributes.getClassArray("rollbackFor")) {
   90 | 			rollbackRules.add(new RollbackRuleAttribute(rbRule));
   91 | 		}
   92 | 		for (String rbRule : attributes.getStringArray("rollbackForClassName")) {
   93 | 			rollbackRules.add(new RollbackRuleAttribute(rbRule));
   94 | 		}
   95 | 		for (Class<?> rbRule : attributes.getClassArray("noRollbackFor")) {
   96 | 			rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
   97 | 		}
   98 | 		for (String rbRule : attributes.getStringArray("noRollbackForClassName")) {
   99 | 			rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
  100 | 		}
  101 | 		rbta.setRollbackRules(rollbackRules);
  102 | 
  103 | 		return rbta;
  104 | 	}
  105 | 
  106 | 
  107 | 	@Override
  108 | 	public boolean equals(@Nullable Object other) {
  109 | 		return (other instanceof SpringTransactionAnnotationParser);
  110 | 	}
  111 | 
  112 | 	@Override
  113 | 	public int hashCode() {
  114 | 		return SpringTransactionAnnotationParser.class.hashCode();
  115 | 	}
  116 | 
  117 | }
```
