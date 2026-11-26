<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/model/tool/ToolCallDecision.java -->
<!-- 正文引用：[[源码:ToolCallDecision.java#L起-L止]] ；代码块头部：(src: ToolCallDecision.java#L起-L止) -->

# ToolCallDecision.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/model/tool/ToolCallDecision.java` |
| 行数 | 70 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:26.006Z |

```java
    1 | /*
    2 |  * Copyright 2023-present the original author or authors.
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
   17 | package org.springframework.ai.model.tool;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.util.Assert;
   22 | 
   23 | /**
   24 |  * A caller's decision on one tool call that waits for confirmation: allow it to run, or
   25 |  * deny it with a message that the model receives as the call's result.
   26 |  *
   27 |  * @param toolCallId the key of the tool call the decision is about: its id, or its
   28 |  * position key when it has no id of its own (see {@link ToolCallDecisions#toolCallKeys})
   29 |  * @param allowed whether the call may run
   30 |  * @param message for a denied call, the result the model receives; {@code null} for the
   31 |  * {@linkplain #DEFAULT_DENY_MESSAGE default}, never blank
   32 |  * @author Christian Tzolov
   33 |  * @since 2.1.0
   34 |  * @see ToolCallDecisions
   35 |  */
   36 | public record ToolCallDecision(String toolCallId, boolean allowed, @Nullable String message) {
   37 | 
   38 | 	/** The result a denied call answers the model with when the decision gives none. */
   39 | 	public static final String DEFAULT_DENY_MESSAGE = "The user did not allow this tool call.";
   40 | 
   41 | 	public ToolCallDecision {
   42 | 		Assert.hasText(toolCallId, "toolCallId must not be empty");
   43 | 		Assert.isTrue(message == null || !message.isBlank(),
   44 | 				"message must not be blank; leave it null for the default deny message");
   45 | 	}
   46 | 
   47 | 	/** Allows the tool call to run. */
   48 | 	public static ToolCallDecision allow(String toolCallId) {
   49 | 		return new ToolCallDecision(toolCallId, true, null);
   50 | 	}
   51 | 
   52 | 	/**
   53 | 	 * Denies the tool call; the model receives the {@linkplain #DEFAULT_DENY_MESSAGE
   54 | 	 * default} message.
   55 | 	 */
   56 | 	public static ToolCallDecision deny(String toolCallId) {
   57 | 		return new ToolCallDecision(toolCallId, false, null);
   58 | 	}
   59 | 
   60 | 	/** Denies the tool call; the model receives the message as the call's result. */
   61 | 	public static ToolCallDecision deny(String toolCallId, String message) {
   62 | 		return new ToolCallDecision(toolCallId, false, message);
   63 | 	}
   64 | 
   65 | 	/** The result a denied call answers the model with. */
   66 | 	public String denyMessage() {
   67 | 		return (this.message != null) ? this.message : DEFAULT_DENY_MESSAGE;
   68 | 	}
   69 | 
   70 | }
```
