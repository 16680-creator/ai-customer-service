<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-session-advisor:2.1.0 | path: org/springframework/ai/session/advisor/SessionAdvisors.java -->
<!-- 正文引用：[[源码:SessionAdvisors.java#L起-L止]] ；代码块头部：(src: SessionAdvisors.java#L起-L止) -->

# SessionAdvisors.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-session-advisor:2.1.0` |
| 路径 | `org/springframework/ai/session/advisor/SessionAdvisors.java` |
| 行数 | 169 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-session-advisor/2.1.0/spring-ai-session-advisor-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:58:22.048Z |

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
   17 | package org.springframework.ai.session.advisor;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.function.Consumer;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | import reactor.core.scheduler.Scheduler;
   25 | 
   26 | import org.springframework.ai.chat.client.ChatClient;
   27 | import org.springframework.ai.chat.client.advisor.api.Advisor;
   28 | import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
   29 | import org.springframework.ai.session.SessionService;
   30 | import org.springframework.ai.session.compaction.CompactionStrategy;
   31 | import org.springframework.ai.session.compaction.CompactionTrigger;
   32 | import org.springframework.util.Assert;
   33 | 
   34 | /**
   35 |  * The session advisors of a {@code ChatClient}, configured together: the
   36 |  * {@link SessionTurnAdvisor} (the session, compaction) and the
   37 |  * {@link SessionMemoryAdvisor} (the history, the messages), each at its default order
   38 |  * around the tool-calling loop the {@code ChatClient} registers itself:
   39 |  * <pre class="code">
   40 |  * ChatClient chatClient = ChatClient.builder(chatModel)
   41 |  *     .defaultAdvisors(SessionAdvisors.builder(sessionService)
   42 |  *         .compaction(new TurnCountTrigger(20), WindowCompactionStrategy.builder().lastTurns(10).build())
   43 |  *         .build())
   44 |  *     .build();
   45 |  * chatClient.prompt().user("Hi").advisors(SessionAdvisors.session(sessionId)).call().content();
   46 |  * </pre>
   47 |  * <p>
   48 |  * The memory advisor is a {@code MemoryAdvisor}, so the tool-calling loop the
   49 |  * {@code ChatClient} registers runs with its own history off: each round's prompt is
   50 |  * rebuilt from the session.
   51 |  * <p>
   52 |  * The advisors can still be built one by one, for an order of your own.
   53 |  *
   54 |  * @author Christian Tzolov
   55 |  * @since 2.1.0
   56 |  */
   57 | public final class SessionAdvisors {
   58 | 
   59 | 	private SessionAdvisors() {
   60 | 	}
   61 | 
   62 | 	public static Builder builder(SessionService sessionService) {
   63 | 		return new Builder(sessionService);
   64 | 	}
   65 | 
   66 | 	/**
   67 | 	 * The request runs on the session:
   68 | 	 * {@code .advisors(SessionAdvisors.session(sessionId))}.
   69 | 	 */
   70 | 	public static Consumer<ChatClient.AdvisorSpec> session(String sessionId) {
   71 | 		Assert.hasText(sessionId, "sessionId must not be empty");
   72 | 		return advisors -> advisors.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId);
   73 | 	}
   74 | 
   75 | 	public static final class Builder {
   76 | 
   77 | 		private final SessionService sessionService;
   78 | 
   79 | 		@Nullable private Scheduler scheduler;
   80 | 
   81 | 		@Nullable private CompactionTrigger compactionTrigger;
   82 | 
   83 | 		@Nullable private CompactionStrategy compactionStrategy;
   84 | 
   85 | 		private boolean asyncCompaction;
   86 | 
   87 | 		private boolean sessionIdInToolContext;
   88 | 
   89 | 		private Consumer<SessionMemoryAdvisor.Builder> memory = memory -> {
   90 | 		};
   91 | 
   92 | 		private Builder(SessionService sessionService) {
   93 | 			Assert.notNull(sessionService, "sessionService must not be null");
   94 | 			this.sessionService = sessionService;
   95 | 		}
   96 | 
   97 | 		/** The scheduler the session writes of a streamed call run on. */
   98 | 		public Builder scheduler(Scheduler scheduler) {
   99 | 			Assert.notNull(scheduler, "scheduler must not be null");
  100 | 			this.scheduler = scheduler;
  101 | 			return this;
  102 | 		}
  103 | 
  104 | 		/** Compacts the session once a turn has ended, when the trigger fires. */
  105 | 		public Builder compaction(CompactionTrigger trigger, CompactionStrategy strategy) {
  106 | 			Assert.notNull(trigger, "trigger must not be null");
  107 | 			Assert.notNull(strategy, "strategy must not be null");
  108 | 			this.compactionTrigger = trigger;
  109 | 			this.compactionStrategy = strategy;
  110 | 			return this;
  111 | 		}
  112 | 
  113 | 		/**
  114 | 		 * Whether compaction runs after the call returns, on the scheduler; see
  115 | 		 * {@link SessionTurnAdvisor.Builder#asyncCompaction(boolean)}.
  116 | 		 */
  117 | 		public Builder asyncCompaction(boolean asyncCompaction) {
  118 | 			this.asyncCompaction = asyncCompaction;
  119 | 			return this;
  120 | 		}
  121 | 
  122 | 		/**
  123 | 		 * Whether the session id also goes into the tool context of each call, so that
  124 | 		 * the session tools find it without {@code toolContext(..)} on the call;
  125 | 		 * {@code false} by default. Every tool of the call then sees the id; see
  126 | 		 * {@link SessionTurnAdvisor.Builder#sessionIdInToolContext(boolean)} for what
  127 | 		 * that means.
  128 | 		 */
  129 | 		public Builder sessionIdInToolContext(boolean sessionIdInToolContext) {
  130 | 			this.sessionIdInToolContext = sessionIdInToolContext;
  131 | 			return this;
  132 | 		}
  133 | 
  134 | 		/**
  135 | 		 * Customizes the {@link SessionMemoryAdvisor}, its filters for example.
  136 | 		 * Customizers run in the order they are added, after the shared settings, so they
  137 | 		 * can override them.
  138 | 		 */
  139 | 		public Builder memory(Consumer<SessionMemoryAdvisor.Builder> memory) {
  140 | 			Assert.notNull(memory, "memory must not be null");
  141 | 			this.memory = this.memory.andThen(memory);
  142 | 			return this;
  143 | 		}
  144 | 
  145 | 		/** The advisors, in their order: the turn, then the memory. */
  146 | 		public List<Advisor> build() {
  147 | 			Scheduler scheduler = (this.scheduler != null) ? this.scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
  148 | 
  149 | 			SessionTurnAdvisor.Builder turn = SessionTurnAdvisor.builder(this.sessionService)
  150 | 				.scheduler(scheduler)
  151 | 				.asyncCompaction(this.asyncCompaction)
  152 | 				.sessionIdInToolContext(this.sessionIdInToolContext);
  153 | 			if (this.compactionTrigger != null && this.compactionStrategy != null) {
  154 | 				turn.compactionTrigger(this.compactionTrigger).compactionStrategy(this.compactionStrategy);
  155 | 			}
  156 | 
  157 | 			SessionMemoryAdvisor.Builder memory = SessionMemoryAdvisor.builder(this.sessionService)
  158 | 				.scheduler(scheduler);
  159 | 			this.memory.accept(memory);
  160 | 
  161 | 			List<Advisor> advisors = new ArrayList<>();
  162 | 			advisors.add(turn.build());
  163 | 			advisors.add(memory.build());
  164 | 			return List.copyOf(advisors);
  165 | 		}
  166 | 
  167 | 	}
  168 | 
  169 | }
```
