<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/DefaultAroundAdvisorChain.java -->
<!-- 正文引用：[[源码:DefaultAroundAdvisorChain.java#L起-L止]] ；代码块头部：(src: DefaultAroundAdvisorChain.java#L起-L止) -->

# DefaultAroundAdvisorChain.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/DefaultAroundAdvisorChain.java` |
| 行数 | 296 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:55.180Z |

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
   17 | package org.springframework.ai.chat.client.advisor;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Deque;
   21 | import java.util.LinkedHashSet;
   22 | import java.util.List;
   23 | import java.util.concurrent.ConcurrentLinkedDeque;
   24 | 
   25 | import io.micrometer.observation.Observation;
   26 | import io.micrometer.observation.ObservationRegistry;
   27 | import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
   28 | import org.jspecify.annotations.Nullable;
   29 | import reactor.core.publisher.Flux;
   30 | 
   31 | import org.springframework.ai.chat.client.ChatClient;
   32 | import org.springframework.ai.chat.client.ChatClientMessageAggregator;
   33 | import org.springframework.ai.chat.client.ChatClientRequest;
   34 | import org.springframework.ai.chat.client.ChatClientResponse;
   35 | import org.springframework.ai.chat.client.advisor.api.Advisor;
   36 | import org.springframework.ai.chat.client.advisor.api.BaseAdvisorChain;
   37 | import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
   38 | import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
   39 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
   40 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
   41 | import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
   42 | import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
   43 | import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationDocumentation;
   44 | import org.springframework.ai.chat.client.advisor.observation.DefaultAdvisorObservationConvention;
   45 | import org.springframework.core.OrderComparator;
   46 | import org.springframework.util.Assert;
   47 | import org.springframework.util.CollectionUtils;
   48 | 
   49 | /**
   50 |  * Default implementation for the {@link BaseAdvisorChain}. Used by the {@link ChatClient}
   51 |  * to delegate the call to the next {@link CallAdvisor} or {@link StreamAdvisor} in the
   52 |  * chain.
   53 |  *
   54 |  * @author Christian Tzolov
   55 |  * @author Dariusz Jedrzejczyk
   56 |  * @author Thomas Vitale
   57 |  * @since 1.0.0
   58 |  */
   59 | public class DefaultAroundAdvisorChain implements BaseAdvisorChain {
   60 | 
   61 | 	public static final AdvisorObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultAdvisorObservationConvention();
   62 | 
   63 | 	private static final ChatClientMessageAggregator CHAT_CLIENT_MESSAGE_AGGREGATOR = new ChatClientMessageAggregator();
   64 | 
   65 | 	private final List<CallAdvisor> originalCallAdvisors;
   66 | 
   67 | 	private final List<StreamAdvisor> originalStreamAdvisors;
   68 | 
   69 | 	private final Deque<CallAdvisor> callAdvisors;
   70 | 
   71 | 	private final Deque<StreamAdvisor> streamAdvisors;
   72 | 
   73 | 	private final ObservationRegistry observationRegistry;
   74 | 
   75 | 	private final AdvisorObservationConvention observationConvention;
   76 | 
   77 | 	DefaultAroundAdvisorChain(ObservationRegistry observationRegistry, Deque<CallAdvisor> callAdvisors,
   78 | 			Deque<StreamAdvisor> streamAdvisors, @Nullable AdvisorObservationConvention observationConvention) {
   79 | 
   80 | 		Assert.notNull(observationRegistry, "the observationRegistry must be non-null");
   81 | 		Assert.notNull(callAdvisors, "the callAdvisors must be non-null");
   82 | 		Assert.notNull(streamAdvisors, "the streamAdvisors must be non-null");
   83 | 
   84 | 		this.observationRegistry = observationRegistry;
   85 | 		this.callAdvisors = callAdvisors;
   86 | 		this.streamAdvisors = streamAdvisors;
   87 | 		this.originalCallAdvisors = List.copyOf(callAdvisors);
   88 | 		this.originalStreamAdvisors = List.copyOf(streamAdvisors);
   89 | 		this.observationConvention = observationConvention != null ? observationConvention
   90 | 				: DEFAULT_OBSERVATION_CONVENTION;
   91 | 	}
   92 | 
   93 | 	public static Builder builder(ObservationRegistry observationRegistry) {
   94 | 		return new Builder(observationRegistry);
   95 | 	}
   96 | 
   97 | 	@Override
   98 | 	public ChatClientResponse nextCall(ChatClientRequest chatClientRequest) {
   99 | 		Assert.notNull(chatClientRequest, "the chatClientRequest cannot be null");
  100 | 
  101 | 		if (this.callAdvisors.isEmpty()) {
  102 | 			throw new IllegalStateException("No CallAdvisors available to execute");
  103 | 		}
  104 | 
  105 | 		var advisor = this.callAdvisors.pop();
  106 | 
  107 | 		var observationContext = AdvisorObservationContext.builder()
  108 | 			.advisorName(advisor.getName())
  109 | 			.chatClientRequest(chatClientRequest)
  110 | 			.order(advisor.getOrder())
  111 | 			.build();
  112 | 
  113 | 		return AdvisorObservationDocumentation.AI_ADVISOR
  114 | 			.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
  115 | 					this.observationRegistry)
  116 | 			.observe(() -> {
  117 | 				var chatClientResponse = advisor.adviseCall(chatClientRequest, this);
  118 | 				observationContext.setChatClientResponse(chatClientResponse);
  119 | 				return chatClientResponse;
  120 | 			});
  121 | 	}
  122 | 
  123 | 	@Override
  124 | 	public Flux<ChatClientResponse> nextStream(ChatClientRequest chatClientRequest) {
  125 | 		Assert.notNull(chatClientRequest, "the chatClientRequest cannot be null");
  126 | 
  127 | 		return Flux.deferContextual(contextView -> {
  128 | 			if (this.streamAdvisors.isEmpty()) {
  129 | 				return Flux.error(new IllegalStateException("No StreamAdvisors available to execute"));
  130 | 			}
  131 | 
  132 | 			var advisor = this.streamAdvisors.pop();
  133 | 
  134 | 			AdvisorObservationContext observationContext = AdvisorObservationContext.builder()
  135 | 				.advisorName(advisor.getName())
  136 | 				.chatClientRequest(chatClientRequest)
  137 | 				.order(advisor.getOrder())
  138 | 				.build();
  139 | 
  140 | 			var observation = AdvisorObservationDocumentation.AI_ADVISOR.observation(this.observationConvention,
  141 | 					DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry);
  142 | 
  143 | 			Observation parentObservation = contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
  144 | 			observation.parentObservation(parentObservation);
  145 | 			// Briefly make the parent observation current while starting this one, so
  146 | 			// Micrometer tracing derives the span's parent from the parent observation
  147 | 			// rather than from whatever scope happens to be open on the current thread
  148 | 			// (e.g.
  149 | 			// the servlet HTTP span). This keeps span parenting correct without relying
  150 | 			// on
  151 | 			// automatic context propagation.
  152 | 			try (Observation.Scope ignored = parentObservation != null ? parentObservation.openScope()
  153 | 					: Observation.Scope.NOOP) {
  154 | 				observation.start();
  155 | 			}
  156 | 
  157 | 			// @formatter:off
  158 | 			Flux<ChatClientResponse> chatClientResponse = Flux.defer(() -> advisor.adviseStream(chatClientRequest, this)
  159 | 						.doOnError(observation::error)
  160 | 						.doFinally(s -> observation.stop())
  161 | 						.contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation)));
  162 | 			// @formatter:on
  163 | 			return CHAT_CLIENT_MESSAGE_AGGREGATOR.aggregateChatClientResponse(chatClientResponse,
  164 | 					observationContext::setChatClientResponse);
  165 | 		});
  166 | 	}
  167 | 
  168 | 	@Override
  169 | 	public CallAdvisorChain copy(CallAdvisor after) {
  170 | 		return this.copyAdvisorsAfter(this.getCallAdvisors(), after);
  171 | 	}
  172 | 
  173 | 	@Override
  174 | 	public StreamAdvisorChain copy(StreamAdvisor after) {
  175 | 		return this.copyAdvisorsAfter(this.getStreamAdvisors(), after);
  176 | 	}
  177 | 
  178 | 	private DefaultAroundAdvisorChain copyAdvisorsAfter(List<? extends Advisor> advisors, Advisor after) {
  179 | 
  180 | 		Assert.notNull(after, "The after advisor must not be null");
  181 | 		Assert.notNull(advisors, "The advisors must not be null");
  182 | 
  183 | 		int afterAdvisorIndex = advisors.indexOf(after);
  184 | 
  185 | 		if (afterAdvisorIndex < 0) {
  186 | 			throw new IllegalArgumentException("The specified advisor is not part of the chain: " + after.getName());
  187 | 		}
  188 | 
  189 | 		var remainingStreamAdvisors = advisors.subList(afterAdvisorIndex + 1, advisors.size());
  190 | 
  191 | 		return DefaultAroundAdvisorChain.builder(this.getObservationRegistry())
  192 | 			.observationConvention(this.observationConvention)
  193 | 			.pushAll(remainingStreamAdvisors)
  194 | 			.build();
  195 | 	}
  196 | 
  197 | 	@Override
  198 | 	public List<CallAdvisor> getCallAdvisors() {
  199 | 		return this.originalCallAdvisors;
  200 | 	}
  201 | 
  202 | 	@Override
  203 | 	public List<StreamAdvisor> getStreamAdvisors() {
  204 | 		return this.originalStreamAdvisors;
  205 | 	}
  206 | 
  207 | 	@Override
  208 | 	public ObservationRegistry getObservationRegistry() {
  209 | 		return this.observationRegistry;
  210 | 	}
  211 | 
  212 | 	@Override
  213 | 	public Builder mutate() {
  214 | 		LinkedHashSet<Advisor> all = new LinkedHashSet<>(this.originalCallAdvisors);
  215 | 		all.addAll(this.originalStreamAdvisors);
  216 | 		return DefaultAroundAdvisorChain.builder(this.observationRegistry)
  217 | 			.observationConvention(this.observationConvention)
  218 | 			.pushAll(new ArrayList<>(all));
  219 | 	}
  220 | 
  221 | 	public static final class Builder implements BaseAdvisorChain.Builder<Builder> {
  222 | 
  223 | 		private final ObservationRegistry observationRegistry;
  224 | 
  225 | 		private final Deque<CallAdvisor> callAdvisors;
  226 | 
  227 | 		private final Deque<StreamAdvisor> streamAdvisors;
  228 | 
  229 | 		private @Nullable AdvisorObservationConvention observationConvention;
  230 | 
  231 | 		public Builder(ObservationRegistry observationRegistry) {
  232 | 			this.observationRegistry = observationRegistry;
  233 | 			this.callAdvisors = new ConcurrentLinkedDeque<>();
  234 | 			this.streamAdvisors = new ConcurrentLinkedDeque<>();
  235 | 		}
  236 | 
  237 | 		public Builder observationConvention(@Nullable AdvisorObservationConvention observationConvention) {
  238 | 			this.observationConvention = observationConvention;
  239 | 			return this;
  240 | 		}
  241 | 
  242 | 		public Builder push(Advisor advisor) {
  243 | 			Assert.notNull(advisor, "the advisor must be non-null");
  244 | 			return this.pushAll(List.of(advisor));
  245 | 		}
  246 | 
  247 | 		public Builder pushAll(List<? extends Advisor> advisors) {
  248 | 			Assert.notNull(advisors, "the advisors must be non-null");
  249 | 			Assert.noNullElements(advisors, "the advisors must not contain null elements");
  250 | 			if (!CollectionUtils.isEmpty(advisors)) {
  251 | 				List<CallAdvisor> callAroundAdvisorList = advisors.stream()
  252 | 					.filter(a -> a instanceof CallAdvisor)
  253 | 					.map(a -> (CallAdvisor) a)
  254 | 					.toList();
  255 | 
  256 | 				if (!CollectionUtils.isEmpty(callAroundAdvisorList)) {
  257 | 					callAroundAdvisorList.forEach(this.callAdvisors::push);
  258 | 				}
  259 | 
  260 | 				List<StreamAdvisor> streamAroundAdvisorList = advisors.stream()
  261 | 					.filter(a -> a instanceof StreamAdvisor)
  262 | 					.map(a -> (StreamAdvisor) a)
  263 | 					.toList();
  264 | 
  265 | 				if (!CollectionUtils.isEmpty(streamAroundAdvisorList)) {
  266 | 					streamAroundAdvisorList.forEach(this.streamAdvisors::push);
  267 | 				}
  268 | 
  269 | 				this.reOrder();
  270 | 			}
  271 | 			return this;
  272 | 		}
  273 | 
  274 | 		/**
  275 | 		 * (Re)orders the advisors in priority order based on their Ordered attribute.
  276 | 		 */
  277 | 		private void reOrder() {
  278 | 			ArrayList<CallAdvisor> callAdvisors = new ArrayList<>(this.callAdvisors);
  279 | 			OrderComparator.sort(callAdvisors);
  280 | 			this.callAdvisors.clear();
  281 | 			callAdvisors.forEach(this.callAdvisors::addLast);
  282 | 
  283 | 			ArrayList<StreamAdvisor> streamAdvisors = new ArrayList<>(this.streamAdvisors);
  284 | 			OrderComparator.sort(streamAdvisors);
  285 | 			this.streamAdvisors.clear();
  286 | 			streamAdvisors.forEach(this.streamAdvisors::addLast);
  287 | 		}
  288 | 
  289 | 		public DefaultAroundAdvisorChain build() {
  290 | 			return new DefaultAroundAdvisorChain(this.observationRegistry, this.callAdvisors, this.streamAdvisors,
  291 | 					this.observationConvention);
  292 | 		}
  293 | 
  294 | 	}
  295 | 
  296 | }
```
