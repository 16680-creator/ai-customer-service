<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/support/DefaultLifecycleProcessor.java -->
<!-- 正文引用：[[源码:DefaultLifecycleProcessor.java#L起-L止]] ；代码块头部：(src: DefaultLifecycleProcessor.java#L起-L止) -->

# DefaultLifecycleProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/support/DefaultLifecycleProcessor.java` |
| 行数 | 786 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-26T09:21:03.812Z |

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
   17 | package org.springframework.context.support;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collections;
   21 | import java.util.Comparator;
   22 | import java.util.HashSet;
   23 | import java.util.LinkedHashMap;
   24 | import java.util.LinkedHashSet;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.Set;
   28 | import java.util.TreeMap;
   29 | import java.util.concurrent.CompletableFuture;
   30 | import java.util.concurrent.ConcurrentHashMap;
   31 | import java.util.concurrent.CountDownLatch;
   32 | import java.util.concurrent.CyclicBarrier;
   33 | import java.util.concurrent.ExecutionException;
   34 | import java.util.concurrent.Executor;
   35 | import java.util.concurrent.TimeUnit;
   36 | 
   37 | import org.apache.commons.logging.Log;
   38 | import org.apache.commons.logging.LogFactory;
   39 | import org.crac.CheckpointException;
   40 | import org.crac.Core;
   41 | import org.crac.RestoreException;
   42 | import org.crac.management.CRaCMXBean;
   43 | import org.jspecify.annotations.Nullable;
   44 | 
   45 | import org.springframework.beans.factory.BeanFactory;
   46 | import org.springframework.beans.factory.BeanFactoryAware;
   47 | import org.springframework.beans.factory.BeanFactoryUtils;
   48 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   49 | import org.springframework.context.ApplicationContextException;
   50 | import org.springframework.context.Lifecycle;
   51 | import org.springframework.context.LifecycleProcessor;
   52 | import org.springframework.context.Phased;
   53 | import org.springframework.context.SmartLifecycle;
   54 | import org.springframework.core.NativeDetector;
   55 | import org.springframework.core.SpringProperties;
   56 | import org.springframework.util.Assert;
   57 | import org.springframework.util.ClassUtils;
   58 | import org.springframework.util.CollectionUtils;
   59 | 
   60 | /**
   61 |  * Spring's default implementation of the {@link LifecycleProcessor} strategy.
   62 |  *
   63 |  * <p>Provides interaction with {@link Lifecycle} and {@link SmartLifecycle} beans in
   64 |  * groups for specific phases, on startup/shutdown as well as for explicit start/stop
   65 |  * interactions on a {@link org.springframework.context.ConfigurableApplicationContext}.
   66 |  *
   67 |  * <p>As of 6.1, this also includes support for JVM checkpoint/restore (Project CRaC)
   68 |  * when the {@code org.crac:crac} dependency is on the classpath. All running beans
   69 |  * will get stopped and restarted according to the CRaC checkpoint/restore callbacks.
   70 |  *
   71 |  * <p>As of 6.2, this processor can be configured with custom timeouts for specific
   72 |  * shutdown phases, applied to {@link SmartLifecycle#stop(Runnable)} implementations.
   73 |  * As of 6.2.6, there is also support for the concurrent startup of specific phases
   74 |  * with individual timeouts, triggering the {@link SmartLifecycle#start()} callbacks
   75 |  * of all associated beans asynchronously and then waiting for all of them to return,
   76 |  * as an alternative to the default sequential startup of beans without a timeout.
   77 |  *
   78 |  * @author Mark Fisher
   79 |  * @author Juergen Hoeller
   80 |  * @author Sebastien Deleuze
   81 |  * @since 3.0
   82 |  * @see SmartLifecycle#getPhase()
   83 |  * @see #setConcurrentStartupForPhase
   84 |  * @see #setTimeoutForShutdownPhase
   85 |  */
   86 | public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {
   87 | 
   88 | 	/**
   89 | 	 * Property name for a common context checkpoint: {@value}.
   90 | 	 * @since 6.1
   91 | 	 * @see #ON_REFRESH_VALUE
   92 | 	 * @see org.crac.Core#checkpointRestore()
   93 | 	 */
   94 | 	public static final String CHECKPOINT_PROPERTY_NAME = "spring.context.checkpoint";
   95 | 
   96 | 	/**
   97 | 	 * Property name for terminating the JVM when the context reaches a specific phase: {@value}.
   98 | 	 * @since 6.1
   99 | 	 * @see #ON_REFRESH_VALUE
  100 | 	 */
  101 | 	public static final String EXIT_PROPERTY_NAME = "spring.context.exit";
  102 | 
  103 | 	/**
  104 | 	 * Recognized value for the context checkpoint and exit properties: {@value}.
  105 | 	 * @since 6.1
  106 | 	 * @see #CHECKPOINT_PROPERTY_NAME
  107 | 	 * @see #EXIT_PROPERTY_NAME
  108 | 	 */
  109 | 	public static final String ON_REFRESH_VALUE = "onRefresh";
  110 | 
  111 | 
  112 | 	private static boolean checkpointOnRefresh =
  113 | 			ON_REFRESH_VALUE.equalsIgnoreCase(SpringProperties.getProperty(CHECKPOINT_PROPERTY_NAME));
  114 | 
  115 | 	private static final boolean exitOnRefresh =
  116 | 			ON_REFRESH_VALUE.equalsIgnoreCase(SpringProperties.getProperty(EXIT_PROPERTY_NAME));
  117 | 
  118 | 	private final Log logger = LogFactory.getLog(getClass());
  119 | 
  120 | 	private final Map<Integer, Long> concurrentStartupForPhases = new ConcurrentHashMap<>();
  121 | 
  122 | 	private final Map<Integer, Long> timeoutsForShutdownPhases = new ConcurrentHashMap<>();
  123 | 
  124 | 	private volatile long timeoutPerShutdownPhase = 10000;
  125 | 
  126 | 	private volatile boolean running;
  127 | 
  128 | 	private volatile @Nullable ConfigurableListableBeanFactory beanFactory;
  129 | 
  130 | 	private volatile @Nullable Set<String> stoppedBeans;
  131 | 
  132 | 	// Just for keeping a strong reference to the registered CRaC Resource, if any
  133 | 	private @Nullable Object cracResource;
  134 | 
  135 | 
  136 | 	public DefaultLifecycleProcessor() {
  137 | 		if (!NativeDetector.inNativeImage() && ClassUtils.isPresent("org.crac.Core", getClass().getClassLoader())) {
  138 | 			this.cracResource = new CracDelegate().registerResource();
  139 | 		}
  140 | 		else if (checkpointOnRefresh) {
  141 | 			throw new IllegalStateException(
  142 | 					"Checkpoint on refresh requires a CRaC-enabled JVM and 'org.crac:crac' on the classpath");
  143 | 		}
  144 | 	}
  145 | 
  146 | 
  147 | 	/**
  148 | 	 * Switch to concurrent startup for each given phase (group of {@link SmartLifecycle}
  149 | 	 * beans with the same 'phase' value) with corresponding timeouts.
  150 | 	 * <p><b>Note: By default, the startup for every phase will be sequential without
  151 | 	 * a timeout. Calling this setter with timeouts for the given phases switches to a
  152 | 	 * mode where the beans in these phases will be started concurrently, cancelling
  153 | 	 * the startup if the corresponding timeout is not met for any of these phases.</b>
  154 | 	 * <p>For an actual concurrent startup, a bootstrap {@code Executor} needs to be
  155 | 	 * set for the application context, typically through a "bootstrapExecutor" bean.
  156 | 	 * @param phasesWithTimeouts a map of phase values (matching
  157 | 	 * {@link SmartLifecycle#getPhase()}) and corresponding timeout values
  158 | 	 * (in milliseconds)
  159 | 	 * @since 6.2.6
  160 | 	 * @see SmartLifecycle#getPhase()
  161 | 	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBootstrapExecutor()
  162 | 	 */
  163 | 	public void setConcurrentStartupForPhases(Map<Integer, Long> phasesWithTimeouts) {
  164 | 		this.concurrentStartupForPhases.putAll(phasesWithTimeouts);
  165 | 	}
  166 | 
  167 | 	/**
  168 | 	 * Switch to concurrent startup for a specific phase (group of {@link SmartLifecycle}
  169 | 	 * beans with the same 'phase' value) with a corresponding timeout.
  170 | 	 * <p><b>Note: By default, the startup for every phase will be sequential without
  171 | 	 * a timeout. Calling this setter with a timeout for the given phase switches to a
  172 | 	 * mode where the beans in this phase will be started concurrently, cancelling
  173 | 	 * the startup if the corresponding timeout is not met for this phase.</b>
  174 | 	 * <p>For an actual concurrent startup, a bootstrap {@code Executor} needs to be
  175 | 	 * set for the application context, typically through a "bootstrapExecutor" bean.
  176 | 	 * @param phase the phase value (matching {@link SmartLifecycle#getPhase()})
  177 | 	 * @param timeout the corresponding timeout value (in milliseconds)
  178 | 	 * @since 6.2.6
  179 | 	 * @see SmartLifecycle#getPhase()
  180 | 	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#getBootstrapExecutor()
  181 | 	 */
  182 | 	public void setConcurrentStartupForPhase(int phase, long timeout) {
  183 | 		this.concurrentStartupForPhases.put(phase, timeout);
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Specify the maximum time allotted for the shutdown of each given phase
  188 | 	 * (group of {@link SmartLifecycle} beans with the same 'phase' value).
  189 | 	 * <p>In case of no specific timeout configured, the default timeout per
  190 | 	 * shutdown phase will apply: 10000 milliseconds (10 seconds) as of 6.2.
  191 | 	 * @param phasesWithTimeouts a map of phase values (matching
  192 | 	 * {@link SmartLifecycle#getPhase()}) and corresponding timeout values
  193 | 	 * (in milliseconds)
  194 | 	 * @since 6.2
  195 | 	 * @see SmartLifecycle#getPhase()
  196 | 	 * @see #setTimeoutPerShutdownPhase
  197 | 	 */
  198 | 	public void setTimeoutsForShutdownPhases(Map<Integer, Long> phasesWithTimeouts) {
  199 | 		this.timeoutsForShutdownPhases.putAll(phasesWithTimeouts);
  200 | 	}
  201 | 
  202 | 	/**
  203 | 	 * Specify the maximum time allotted for the shutdown of a specific phase
  204 | 	 * (group of {@link SmartLifecycle} beans with the same 'phase' value).
  205 | 	 * <p>In case of no specific timeout configured, the default timeout per
  206 | 	 * shutdown phase will apply: 10000 milliseconds (10 seconds) as of 6.2.
  207 | 	 * @param phase the phase value (matching {@link SmartLifecycle#getPhase()})
  208 | 	 * @param timeout the corresponding timeout value (in milliseconds)
  209 | 	 * @since 6.2
  210 | 	 * @see SmartLifecycle#getPhase()
  211 | 	 * @see #setTimeoutPerShutdownPhase
  212 | 	 */
  213 | 	public void setTimeoutForShutdownPhase(int phase, long timeout) {
  214 | 		this.timeoutsForShutdownPhases.put(phase, timeout);
  215 | 	}
  216 | 
  217 | 	/**
  218 | 	 * Specify the maximum time allotted in milliseconds for the shutdown of any
  219 | 	 * phase (group of {@link SmartLifecycle} beans with the same 'phase' value).
  220 | 	 * <p>The default value is 10000 milliseconds (10 seconds) as of 6.2.
  221 | 	 * @see SmartLifecycle#getPhase()
  222 | 	 */
  223 | 	public void setTimeoutPerShutdownPhase(long timeoutPerShutdownPhase) {
  224 | 		this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
  225 | 	}
  226 | 
  227 | 	@Override
  228 | 	public void setBeanFactory(BeanFactory beanFactory) {
  229 | 		if (!(beanFactory instanceof ConfigurableListableBeanFactory clbf)) {
  230 | 			throw new IllegalArgumentException(
  231 | 					"DefaultLifecycleProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
  232 | 		}
  233 | 		if (!this.concurrentStartupForPhases.isEmpty() && clbf.getBootstrapExecutor() == null) {
  234 | 			throw new IllegalStateException("'bootstrapExecutor' needs to be configured for concurrent startup");
  235 | 		}
  236 | 		this.beanFactory = clbf;
  237 | 	}
  238 | 
  239 | 	private ConfigurableListableBeanFactory getBeanFactory() {
  240 | 		ConfigurableListableBeanFactory beanFactory = this.beanFactory;
  241 | 		Assert.state(beanFactory != null, "No BeanFactory available");
  242 | 		return beanFactory;
  243 | 	}
  244 | 
  245 | 	private Executor getBootstrapExecutor() {
  246 | 		Executor executor = getBeanFactory().getBootstrapExecutor();
  247 | 		Assert.state(executor != null, "No 'bootstrapExecutor' available");
  248 | 		return executor;
  249 | 	}
  250 | 
  251 | 	private @Nullable Long determineConcurrentStartup(int phase) {
  252 | 		return this.concurrentStartupForPhases.get(phase);
  253 | 	}
  254 | 
  255 | 	private long determineShutdownTimeout(int phase) {
  256 | 		Long timeout = this.timeoutsForShutdownPhases.get(phase);
  257 | 		return (timeout != null ? timeout : this.timeoutPerShutdownPhase);
  258 | 	}
  259 | 
  260 | 
  261 | 	// Lifecycle implementation
  262 | 
  263 | 	/**
  264 | 	 * Start all registered beans that implement {@link Lifecycle} and are <i>not</i>
  265 | 	 * already running. Any bean that implements {@link SmartLifecycle} will be
  266 | 	 * started within its 'phase', and all phases will be ordered from lowest to
  267 | 	 * highest value. All beans that do not implement {@link SmartLifecycle} will be
  268 | 	 * started in the default phase 0. A bean declared as a dependency of another bean
  269 | 	 * will be started before the dependent bean regardless of the declared phase.
  270 | 	 */
  271 | 	@Override
  272 | 	public void start() {
  273 | 		this.stoppedBeans = null;
  274 | 		startBeans(StartMode.ALL);
  275 | 		// If any bean failed to explicitly start, the exception propagates here.
  276 | 		// The caller may choose to subsequently call stop() if appropriate.
  277 | 		this.running = true;
  278 | 	}
  279 | 
  280 | 	/**
  281 | 	 * Stop all registered beans that implement {@link Lifecycle} and <i>are</i>
  282 | 	 * currently running. Any bean that implements {@link SmartLifecycle} will be
  283 | 	 * stopped within its 'phase', and all phases will be ordered from highest to
  284 | 	 * lowest value. All beans that do not implement {@link SmartLifecycle} will be
  285 | 	 * stopped in the default phase 0. A bean declared as dependent on another bean
  286 | 	 * will be stopped before the dependency bean regardless of the declared phase.
  287 | 	 */
  288 | 	@Override
  289 | 	public void stop() {
  290 | 		stopBeans(StopMode.ALL);
  291 | 		this.running = false;
  292 | 	}
  293 | 
  294 | 	@Override
  295 | 	public void onRefresh() {
  296 | 		if (checkpointOnRefresh) {
  297 | 			checkpointOnRefresh = false;
  298 | 			new CracDelegate().checkpointRestore();
  299 | 		}
  300 | 		if (exitOnRefresh) {
  301 | 			Runtime.getRuntime().halt(0);
  302 | 		}
  303 | 
  304 | 		this.stoppedBeans = null;
  305 | 		try {
  306 | 			startBeans(StartMode.REFRESH);
  307 | 		}
  308 | 		catch (ApplicationContextException ex) {
  309 | 			// Some bean failed to auto-start within context refresh:
  310 | 			// stop already started beans on context refresh failure.
  311 | 			stopBeans(StopMode.ALL);
  312 | 			throw ex;
  313 | 		}
  314 | 		this.running = true;
  315 | 	}
  316 | 
  317 | 	@Override
  318 | 	public void onRestart() {
  319 | 		this.stoppedBeans = null;
  320 | 		if (this.running) {
  321 | 			stopBeans(StopMode.PAUSE);
  322 | 		}
  323 | 		startBeans(StartMode.RESTART);
  324 | 		this.running = true;
  325 | 	}
  326 | 
  327 | 	@Override
  328 | 	public void onPause() {
  329 | 		if (this.running) {
  330 | 			stopBeans(StopMode.PAUSE);
  331 | 			this.running = false;
  332 | 		}
  333 | 	}
  334 | 
  335 | 	@Override
  336 | 	public void onClose() {
  337 | 		stopBeans(StopMode.ALL);
  338 | 		this.running = false;
  339 | 	}
  340 | 
  341 | 	@Override
  342 | 	public boolean isRunning() {
  343 | 		return this.running;
  344 | 	}
  345 | 
  346 | 
  347 | 	// Internal helpers
  348 | 
  349 | 	void stopForRestart() {
  350 | 		if (this.running) {
  351 | 			this.stoppedBeans = ConcurrentHashMap.newKeySet();
  352 | 			stopBeans(StopMode.ALL);
  353 | 			this.running = false;
  354 | 		}
  355 | 	}
  356 | 
  357 | 	void restartAfterStop() {
  358 | 		if (this.stoppedBeans != null) {
  359 | 			startBeans(StartMode.RESTART);
  360 | 			this.stoppedBeans = null;
  361 | 			this.running = true;
  362 | 		}
  363 | 	}
  364 | 
  365 | 	private void startBeans(StartMode startMode) {
  366 | 		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
  367 | 		Map<Integer, LifecycleGroup> phases = new TreeMap<>();
  368 | 
  369 | 		lifecycleBeans.forEach((beanName, bean) -> {
  370 | 			if (startMode == StartMode.ALL || isAutoStartupCandidate(beanName, bean)) {
  371 | 				int startupPhase = getPhase(bean);
  372 | 				phases.computeIfAbsent(
  373 | 						startupPhase, phase -> new LifecycleGroup(phase, lifecycleBeans, startMode))
  374 | 							.add(beanName, bean);
  375 | 			}
  376 | 		});
  377 | 
  378 | 		if (!phases.isEmpty()) {
  379 | 			phases.values().forEach(LifecycleGroup::start);
  380 | 		}
  381 | 	}
  382 | 
  383 | 	private boolean isAutoStartupCandidate(String beanName, Lifecycle bean) {
  384 | 		Set<String> stoppedBeans = this.stoppedBeans;
  385 | 		return (stoppedBeans != null ? stoppedBeans.contains(beanName) :
  386 | 				(bean instanceof SmartLifecycle smartLifecycle && smartLifecycle.isAutoStartup()));
  387 | 	}
  388 | 
  389 | 	/**
  390 | 	 * Start the specified bean as part of the given set of Lifecycle beans,
  391 | 	 * making sure that any beans that it depends on are started first.
  392 | 	 * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
  393 | 	 * @param beanName the name of the bean to start
  394 | 	 */
  395 | 	private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName,
  396 | 			StartMode startMode, @Nullable List<CompletableFuture<?>> futures) {
  397 | 
  398 | 		Lifecycle bean = lifecycleBeans.remove(beanName);
  399 | 		if (bean != null && bean != this) {
  400 | 			String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);
  401 | 			for (String dependency : dependenciesForBean) {
  402 | 				doStart(lifecycleBeans, dependency, startMode, futures);
  403 | 			}
  404 | 			if (!bean.isRunning() && (startMode == StartMode.ALL || toBeStarted(beanName, bean))) {
  405 | 				if (futures != null) {
  406 | 					futures.add(CompletableFuture.runAsync(() -> doStart(beanName, bean, startMode), getBootstrapExecutor()));
  407 | 				}
  408 | 				else {
  409 | 					doStart(beanName, bean, startMode);
  410 | 				}
  411 | 			}
  412 | 		}
  413 | 	}
  414 | 
  415 | 	private void doStart(String beanName, Lifecycle bean, StartMode startMode) {
  416 | 		if (logger.isTraceEnabled()) {
  417 | 			logger.trace("Starting bean '" + beanName + "' of type [" + bean.getClass().getName() + "]");
  418 | 		}
  419 | 		try {
  420 | 			if (startMode == StartMode.RESTART && bean instanceof SmartLifecycle smartLifecycle) {
  421 | 				smartLifecycle.restart();
  422 | 			}
  423 | 			else {
  424 | 				bean.start();
  425 | 			}
  426 | 		}
  427 | 		catch (Throwable ex) {
  428 | 			throw new ApplicationContextException("Failed to start bean '" + beanName + "'", ex);
  429 | 		}
  430 | 		if (logger.isDebugEnabled()) {
  431 | 			logger.debug("Successfully started bean '" + beanName + "'");
  432 | 		}
  433 | 	}
  434 | 
  435 | 	private boolean toBeStarted(String beanName, Lifecycle bean) {
  436 | 		Set<String> stoppedBeans = this.stoppedBeans;
  437 | 		return (stoppedBeans != null ? stoppedBeans.contains(beanName) :
  438 | 				(!(bean instanceof SmartLifecycle smartLifecycle) || smartLifecycle.isAutoStartup()));
  439 | 	}
  440 | 
  441 | 	private void stopBeans(StopMode stopMode) {
  442 | 		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
  443 | 		Map<Integer, LifecycleGroup> phases = new TreeMap<>(Comparator.reverseOrder());
  444 | 
  445 | 		lifecycleBeans.forEach((beanName, bean) -> {
  446 | 			int shutdownPhase = getPhase(bean);
  447 | 			phases.computeIfAbsent(
  448 | 					shutdownPhase, phase -> new LifecycleGroup(phase, lifecycleBeans, stopMode))
  449 | 						.add(beanName, bean);
  450 | 		});
  451 | 
  452 | 		if (!phases.isEmpty()) {
  453 | 			phases.values().forEach(LifecycleGroup::stop);
  454 | 		}
  455 | 	}
  456 | 
  457 | 	/**
  458 | 	 * Stop the specified bean as part of the given set of Lifecycle beans,
  459 | 	 * making sure that any beans that depends on it are stopped first.
  460 | 	 * @param lifecycleBeans a Map with bean name as key and Lifecycle instance as value
  461 | 	 * @param beanName the name of the bean to stop
  462 | 	 */
  463 | 	private void doStop(Map<String, ? extends Lifecycle> lifecycleBeans, final String beanName,
  464 | 			StopMode stopMode, final CountDownLatch latch, final Set<String> countDownBeanNames) {
  465 | 
  466 | 		Lifecycle bean = lifecycleBeans.remove(beanName);
  467 | 		if (bean != null) {
  468 | 			String[] dependentBeans = getBeanFactory().getDependentBeans(beanName);
  469 | 			for (String dependentBean : dependentBeans) {
  470 | 				doStop(lifecycleBeans, dependentBean, stopMode, latch, countDownBeanNames);
  471 | 			}
  472 | 			try {
  473 | 				if (bean.isRunning()) {
  474 | 					Set<String> stoppedBeans = this.stoppedBeans;
  475 | 					if (stoppedBeans != null) {
  476 | 						stoppedBeans.add(beanName);
  477 | 					}
  478 | 					if (bean instanceof SmartLifecycle smartLifecycle) {
  479 | 						if (stopMode == StopMode.ALL || smartLifecycle.isPauseable()) {
  480 | 							if (logger.isTraceEnabled()) {
  481 | 								logger.trace("Asking bean '" + beanName + "' of type [" +
  482 | 										bean.getClass().getName() + "] to stop");
  483 | 							}
  484 | 							countDownBeanNames.add(beanName);
  485 | 							Runnable callback = () -> {
  486 | 								latch.countDown();
  487 | 								countDownBeanNames.remove(beanName);
  488 | 								if (logger.isDebugEnabled()) {
  489 | 									logger.debug("Bean '" + beanName + "' completed its stop procedure");
  490 | 								}
  491 | 							};
  492 | 							if (stopMode == StopMode.PAUSE) {
  493 | 								smartLifecycle.pause(callback);
  494 | 							}
  495 | 							else {
  496 | 								smartLifecycle.stop(callback);
  497 | 							}
  498 | 						}
  499 | 						else {
  500 | 							// Don't wait for beans that aren't pauseable...
  501 | 							latch.countDown();
  502 | 						}
  503 | 					}
  504 | 					else if (stopMode == StopMode.ALL) {
  505 | 						if (logger.isTraceEnabled()) {
  506 | 							logger.trace("Stopping bean '" + beanName + "' of type [" +
  507 | 									bean.getClass().getName() + "]");
  508 | 						}
  509 | 						bean.stop();
  510 | 						if (logger.isDebugEnabled()) {
  511 | 							logger.debug("Successfully stopped bean '" + beanName + "'");
  512 | 						}
  513 | 					}
  514 | 				}
  515 | 				else if (bean instanceof SmartLifecycle) {
  516 | 					// Don't wait for beans that aren't running...
  517 | 					latch.countDown();
  518 | 				}
  519 | 			}
  520 | 			catch (Throwable ex) {
  521 | 				if (logger.isWarnEnabled()) {
  522 | 					logger.warn("Failed to stop bean '" + beanName + "'", ex);
  523 | 				}
  524 | 				if (bean instanceof SmartLifecycle) {
  525 | 					latch.countDown();
  526 | 				}
  527 | 			}
  528 | 		}
  529 | 	}
  530 | 
  531 | 
  532 | 	// Overridable hooks
  533 | 
  534 | 	/**
  535 | 	 * Retrieve all applicable Lifecycle beans: all singletons that have already been created,
  536 | 	 * as well as all SmartLifecycle beans (even if they are marked as lazy-init).
  537 | 	 * @return the Map of applicable beans, with bean names as keys and bean instances as values
  538 | 	 */
  539 | 	protected Map<String, Lifecycle> getLifecycleBeans() {
  540 | 		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
  541 | 		Map<String, Lifecycle> beans = new LinkedHashMap<>();
  542 | 		String[] beanNames = beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
  543 | 		for (String beanName : beanNames) {
  544 | 			String beanNameToRegister = BeanFactoryUtils.transformedBeanName(beanName);
  545 | 			boolean isFactoryBean = beanFactory.isFactoryBean(beanNameToRegister);
  546 | 			String beanNameToCheck = (isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
  547 | 			if ((beanFactory.containsSingleton(beanNameToRegister) &&
  548 | 					(!isFactoryBean || matchesBeanType(Lifecycle.class, beanNameToCheck, beanFactory))) ||
  549 | 					matchesBeanType(SmartLifecycle.class, beanNameToCheck, beanFactory)) {
  550 | 				Object bean = beanFactory.getBean(beanNameToCheck);
  551 | 				if (bean != this && bean instanceof Lifecycle lifecycle) {
  552 | 					beans.put(beanNameToRegister, lifecycle);
  553 | 				}
  554 | 			}
  555 | 		}
  556 | 		return beans;
  557 | 	}
  558 | 
  559 | 	private boolean matchesBeanType(Class<?> targetType, String beanName, BeanFactory beanFactory) {
  560 | 		Class<?> beanType = beanFactory.getType(beanName);
  561 | 		return (beanType != null && targetType.isAssignableFrom(beanType));
  562 | 	}
  563 | 
  564 | 	/**
  565 | 	 * Determine the lifecycle phase of the given bean.
  566 | 	 * <p>The default implementation checks for the {@link Phased} interface, using
  567 | 	 * a default of 0 otherwise. Can be overridden to apply other/further policies.
  568 | 	 * @param bean the bean to introspect
  569 | 	 * @return the phase (an integer value)
  570 | 	 * @see Phased#getPhase()
  571 | 	 * @see SmartLifecycle
  572 | 	 */
  573 | 	protected int getPhase(Lifecycle bean) {
  574 | 		return (bean instanceof Phased phased ? phased.getPhase() : 0);
  575 | 	}
  576 | 
  577 | 
  578 | 	// Enums for internal start/stop delegation.
  579 | 
  580 | 	private enum StartMode { ALL, REFRESH, RESTART }
  581 | 
  582 | 	private enum StopMode { ALL, PAUSE }
  583 | 
  584 | 
  585 | 	/**
  586 | 	 * Helper class for maintaining a group of Lifecycle beans that should be started
  587 | 	 * and stopped together based on their 'phase' value (or the default value of 0).
  588 | 	 * The group is expected to be created in an ad-hoc fashion and group members are
  589 | 	 * expected to always have the same 'phase' value.
  590 | 	 */
  591 | 	private class LifecycleGroup {
  592 | 
  593 | 		private final int phase;
  594 | 
  595 | 		private final Map<String, ? extends Lifecycle> lifecycleBeans;
  596 | 
  597 | 		private final StartMode startMode;
  598 | 
  599 | 		private final StopMode stopMode;
  600 | 
  601 | 		private final List<LifecycleGroupMember> members = new ArrayList<>();
  602 | 
  603 | 		private int smartMemberCount;
  604 | 
  605 | 		public LifecycleGroup(int phase, Map<String, ? extends Lifecycle> lifecycleBeans, StartMode startMode) {
  606 | 			this.phase = phase;
  607 | 			this.lifecycleBeans = lifecycleBeans;
  608 | 			this.startMode = startMode;
  609 | 			this.stopMode = StopMode.ALL;
  610 | 		}
  611 | 
  612 | 		public LifecycleGroup(int phase, Map<String, ? extends Lifecycle> lifecycleBeans, StopMode stopMode) {
  613 | 			this.phase = phase;
  614 | 			this.lifecycleBeans = lifecycleBeans;
  615 | 			this.startMode = StartMode.ALL;
  616 | 			this.stopMode = stopMode;
  617 | 		}
  618 | 
  619 | 		public void add(String name, Lifecycle bean) {
  620 | 			this.members.add(new LifecycleGroupMember(name, bean));
  621 | 			if (bean instanceof SmartLifecycle) {
  622 | 				this.smartMemberCount++;
  623 | 			}
  624 | 		}
  625 | 
  626 | 		public void start() {
  627 | 			if (this.members.isEmpty()) {
  628 | 				return;
  629 | 			}
  630 | 			if (logger.isDebugEnabled()) {
  631 | 				logger.debug("Starting beans in phase " + this.phase);
  632 | 			}
  633 | 			Long concurrentStartup = determineConcurrentStartup(this.phase);
  634 | 			List<CompletableFuture<?>> futures = (concurrentStartup != null ? new ArrayList<>() : null);
  635 | 			for (LifecycleGroupMember member : this.members) {
  636 | 				doStart(this.lifecycleBeans, member.name, this.startMode, futures);
  637 | 			}
  638 | 			if (concurrentStartup != null && !CollectionUtils.isEmpty(futures)) {
  639 | 				try {
  640 | 					CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
  641 | 							.get(concurrentStartup, TimeUnit.MILLISECONDS);
  642 | 				}
  643 | 				catch (Exception ex) {
  644 | 					if (ex instanceof ExecutionException exEx) {
  645 | 						Throwable cause = exEx.getCause();
  646 | 						if (cause instanceof ApplicationContextException acEx) {
  647 | 							throw acEx;
  648 | 						}
  649 | 					}
  650 | 					throw new ApplicationContextException("Failed to start beans in phase " + this.phase +
  651 | 							" within timeout of " + concurrentStartup + "ms", ex);
  652 | 				}
  653 | 			}
  654 | 		}
  655 | 
  656 | 		public void stop() {
  657 | 			if (this.members.isEmpty()) {
  658 | 				return;
  659 | 			}
  660 | 			if (logger.isDebugEnabled()) {
  661 | 				logger.debug("Stopping beans in phase " + this.phase);
  662 | 			}
  663 | 			CountDownLatch latch = new CountDownLatch(this.smartMemberCount);
  664 | 			Set<String> countDownBeanNames = Collections.synchronizedSet(new LinkedHashSet<>());
  665 | 			Set<String> lifecycleBeanNames = new HashSet<>(this.lifecycleBeans.keySet());
  666 | 			for (LifecycleGroupMember member : this.members) {
  667 | 				if (lifecycleBeanNames.contains(member.name)) {
  668 | 					doStop(this.lifecycleBeans, member.name, this.stopMode, latch, countDownBeanNames);
  669 | 				}
  670 | 				else if (member.bean instanceof SmartLifecycle) {
  671 | 					// Already removed: must have been a dependent bean from another phase
  672 | 					latch.countDown();
  673 | 				}
  674 | 			}
  675 | 			try {
  676 | 				long shutdownTimeout = determineShutdownTimeout(this.phase);
  677 | 				if (!latch.await(shutdownTimeout, TimeUnit.MILLISECONDS)) {
  678 | 					// Count is still >0 after timeout
  679 | 					if (!countDownBeanNames.isEmpty() && logger.isInfoEnabled()) {
  680 | 						logger.info("Shutdown phase " + this.phase + " ends with " + countDownBeanNames.size() +
  681 | 								" bean" + (countDownBeanNames.size() > 1 ? "s" : "") +
  682 | 								" still running after timeout of " + shutdownTimeout + "ms: " + countDownBeanNames);
  683 | 					}
  684 | 				}
  685 | 			}
  686 | 			catch (InterruptedException ex) {
  687 | 				Thread.currentThread().interrupt();
  688 | 			}
  689 | 		}
  690 | 	}
  691 | 
  692 | 
  693 | 	/**
  694 | 	 * A simple record of a LifecycleGroup member.
  695 | 	 */
  696 | 	private record LifecycleGroupMember(String name, Lifecycle bean) {}
  697 | 
  698 | 
  699 | 	/**
  700 | 	 * Inner class to avoid a hard dependency on Project CRaC at runtime.
  701 | 	 * @since 6.1
  702 | 	 * @see org.crac.Core
  703 | 	 */
  704 | 	private class CracDelegate {
  705 | 
  706 | 		public Object registerResource() {
  707 | 			logger.debug("Registering JVM checkpoint/restore callback for Spring-managed lifecycle beans");
  708 | 			CracResourceAdapter resourceAdapter = new CracResourceAdapter();
  709 | 			org.crac.Core.getGlobalContext().register(resourceAdapter);
  710 | 			return resourceAdapter;
  711 | 		}
  712 | 
  713 | 		public void checkpointRestore() {
  714 | 			logger.info("Triggering JVM checkpoint/restore");
  715 | 			try {
  716 | 				Core.checkpointRestore();
  717 | 			}
  718 | 			catch (UnsupportedOperationException ex) {
  719 | 				throw new ApplicationContextException("CRaC checkpoint not supported on current JVM", ex);
  720 | 			}
  721 | 			catch (CheckpointException ex) {
  722 | 				throw new ApplicationContextException("Failed to take CRaC checkpoint on refresh", ex);
  723 | 			}
  724 | 			catch (RestoreException ex) {
  725 | 				throw new ApplicationContextException("Failed to restore CRaC checkpoint on refresh", ex);
  726 | 			}
  727 | 		}
  728 | 	}
  729 | 
  730 | 
  731 | 	/**
  732 | 	 * Resource adapter for Project CRaC, triggering a stop-and-restart cycle
  733 | 	 * for Spring-managed lifecycle beans around a JVM checkpoint/restore.
  734 | 	 * @since 6.1
  735 | 	 * @see #stopForRestart()
  736 | 	 * @see #restartAfterStop()
  737 | 	 */
  738 | 	private class CracResourceAdapter implements org.crac.Resource {
  739 | 
  740 | 		private final CyclicBarrier beforeCheckpointBarrier = new CyclicBarrier(2);
  741 | 		private final CyclicBarrier afterRestoreBarrier = new CyclicBarrier(2);
  742 | 
  743 | 		@Override
  744 | 		public void beforeCheckpoint(org.crac.Context<? extends org.crac.Resource> context) {
  745 | 			Thread thread = new Thread(this::preventShutdown, "prevent-shutdown");
  746 | 			thread.setDaemon(false);
  747 | 			thread.start();
  748 | 
  749 | 			logger.debug("Stopping Spring-managed lifecycle beans before JVM checkpoint");
  750 | 			stopForRestart();
  751 | 		}
  752 | 
  753 | 		private void preventShutdown() {
  754 | 			awaitBarrier(this.beforeCheckpointBarrier);
  755 | 			// Checkpoint happens here
  756 | 			awaitBarrier(this.afterRestoreBarrier);
  757 | 		}
  758 | 
  759 | 		@Override
  760 | 		public void afterRestore(org.crac.Context<? extends org.crac.Resource> context) {
  761 | 			// Unlock barrier for beforeCheckpoint
  762 | 			awaitBarrier(this.beforeCheckpointBarrier);
  763 | 
  764 | 			logger.info("Restarting Spring-managed lifecycle beans after JVM restore");
  765 | 			restartAfterStop();
  766 | 
  767 | 			// Unlock barrier for afterRestore to shutdown "prevent-shutdown" thread
  768 | 			awaitBarrier(this.afterRestoreBarrier);
  769 | 
  770 | 			if (!checkpointOnRefresh) {
  771 | 				logger.info("Spring-managed lifecycle restart completed (restored JVM running for " +
  772 | 						CRaCMXBean.getCRaCMXBean().getUptimeSinceRestore() + " ms)");
  773 | 			}
  774 | 		}
  775 | 
  776 | 		private void awaitBarrier(CyclicBarrier barrier) {
  777 | 			try {
  778 | 				barrier.await();
  779 | 			}
  780 | 			catch (Exception ex) {
  781 | 				logger.trace("Exception from barrier", ex);
  782 | 			}
  783 | 		}
  784 | 	}
  785 | 
  786 | }
```
