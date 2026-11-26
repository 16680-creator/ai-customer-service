<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/transaction/interceptor/RuleBasedTransactionAttribute.java -->
<!-- 正文引用：[[源码:RuleBasedTransactionAttribute.java#L起-L止]] ；代码块头部：(src: RuleBasedTransactionAttribute.java#L起-L止) -->

# RuleBasedTransactionAttribute.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/transaction/interceptor/RuleBasedTransactionAttribute.java` |
| 行数 | 158 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:13.051Z |

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
   17 | package org.springframework.transaction.interceptor;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.util.ArrayList;
   21 | import java.util.List;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | /**
   26 |  * TransactionAttribute implementation that works out whether a given exception
   27 |  * should cause transaction rollback by applying a number of rollback rules,
   28 |  * both positive and negative. If no custom rollback rules apply, this attribute
   29 |  * behaves like DefaultTransactionAttribute (rolling back on runtime exceptions).
   30 |  *
   31 |  * <p>{@link TransactionAttributeEditor} creates objects of this class.
   32 |  *
   33 |  * @author Rod Johnson
   34 |  * @author Juergen Hoeller
   35 |  * @since 09.04.2003
   36 |  * @see TransactionAttributeEditor
   37 |  */
   38 | @SuppressWarnings("serial")
   39 | public class RuleBasedTransactionAttribute extends DefaultTransactionAttribute implements Serializable {
   40 | 
   41 | 	/** Prefix for rollback-on-exception rules in description strings. */
   42 | 	public static final String PREFIX_ROLLBACK_RULE = "-";
   43 | 
   44 | 	/** Prefix for commit-on-exception rules in description strings. */
   45 | 	public static final String PREFIX_COMMIT_RULE = "+";
   46 | 
   47 | 
   48 | 	private @Nullable List<RollbackRuleAttribute> rollbackRules;
   49 | 
   50 | 
   51 | 	/**
   52 | 	 * Create a new RuleBasedTransactionAttribute, with default settings.
   53 | 	 * Can be modified through bean property setters.
   54 | 	 * @see #setPropagationBehavior
   55 | 	 * @see #setIsolationLevel
   56 | 	 * @see #setTimeout
   57 | 	 * @see #setReadOnly
   58 | 	 * @see #setName
   59 | 	 * @see #setRollbackRules
   60 | 	 */
   61 | 	public RuleBasedTransactionAttribute() {
   62 | 	}
   63 | 
   64 | 	/**
   65 | 	 * Copy constructor. Definition can be modified through bean property setters.
   66 | 	 * @see #setPropagationBehavior
   67 | 	 * @see #setIsolationLevel
   68 | 	 * @see #setTimeout
   69 | 	 * @see #setReadOnly
   70 | 	 * @see #setName
   71 | 	 * @see #setRollbackRules
   72 | 	 */
   73 | 	public RuleBasedTransactionAttribute(RuleBasedTransactionAttribute other) {
   74 | 		super(other);
   75 | 		this.rollbackRules = (other.rollbackRules != null ? new ArrayList<>(other.rollbackRules) : null);
   76 | 	}
   77 | 
   78 | 	/**
   79 | 	 * Create a new DefaultTransactionAttribute with the given
   80 | 	 * propagation behavior. Can be modified through bean property setters.
   81 | 	 * @param propagationBehavior one of the propagation constants in the
   82 | 	 * TransactionDefinition interface
   83 | 	 * @param rollbackRules the list of RollbackRuleAttributes to apply
   84 | 	 * @see #setIsolationLevel
   85 | 	 * @see #setTimeout
   86 | 	 * @see #setReadOnly
   87 | 	 */
   88 | 	public RuleBasedTransactionAttribute(int propagationBehavior, List<RollbackRuleAttribute> rollbackRules) {
   89 | 		super(propagationBehavior);
   90 | 		this.rollbackRules = rollbackRules;
   91 | 	}
   92 | 
   93 | 
   94 | 	/**
   95 | 	 * Set the list of {@code RollbackRuleAttribute} objects
   96 | 	 * (and/or {@code NoRollbackRuleAttribute} objects) to apply.
   97 | 	 * @see RollbackRuleAttribute
   98 | 	 * @see NoRollbackRuleAttribute
   99 | 	 */
  100 | 	public void setRollbackRules(List<RollbackRuleAttribute> rollbackRules) {
  101 | 		this.rollbackRules = rollbackRules;
  102 | 	}
  103 | 
  104 | 	/**
  105 | 	 * Return the list of {@code RollbackRuleAttribute} objects
  106 | 	 * (never {@code null}).
  107 | 	 */
  108 | 	public List<RollbackRuleAttribute> getRollbackRules() {
  109 | 		if (this.rollbackRules == null) {
  110 | 			this.rollbackRules = new ArrayList<>();
  111 | 		}
  112 | 		return this.rollbackRules;
  113 | 	}
  114 | 
  115 | 
  116 | 	/**
  117 | 	 * Winning rule is the shallowest rule (that is, the closest in the
  118 | 	 * inheritance hierarchy to the exception). If no rule applies (-1),
  119 | 	 * return {@code false}.
  120 | 	 * @see TransactionAttribute#rollbackOn(java.lang.Throwable)
  121 | 	 */
  122 | 	@Override
  123 | 	public boolean rollbackOn(Throwable ex) {
  124 | 		RollbackRuleAttribute winner = null;
  125 | 		int deepest = Integer.MAX_VALUE;
  126 | 
  127 | 		if (this.rollbackRules != null) {
  128 | 			for (RollbackRuleAttribute rule : this.rollbackRules) {
  129 | 				int depth = rule.getDepth(ex);
  130 | 				if (depth >= 0 && depth < deepest) {
  131 | 					deepest = depth;
  132 | 					winner = rule;
  133 | 				}
  134 | 			}
  135 | 		}
  136 | 
  137 | 		// User superclass behavior (rollback on unchecked) if no rule matches.
  138 | 		if (winner == null) {
  139 | 			return super.rollbackOn(ex);
  140 | 		}
  141 | 
  142 | 		return !(winner instanceof NoRollbackRuleAttribute);
  143 | 	}
  144 | 
  145 | 
  146 | 	@Override
  147 | 	public String toString() {
  148 | 		StringBuilder result = getAttributeDescription();
  149 | 		if (this.rollbackRules != null) {
  150 | 			for (RollbackRuleAttribute rule : this.rollbackRules) {
  151 | 				String sign = (rule instanceof NoRollbackRuleAttribute ? PREFIX_COMMIT_RULE : PREFIX_ROLLBACK_RULE);
  152 | 				result.append(',').append(sign).append(rule.getExceptionName());
  153 | 			}
  154 | 		}
  155 | 		return result.toString();
  156 | 	}
  157 | 
  158 | }
```
