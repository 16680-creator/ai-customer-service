<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.csp:sentinel-core:1.8.10 | path: com/alibaba/csp/sentinel/slots/block/flow/FlowRuleChecker.java -->
<!-- 正文引用：[[源码:FlowRuleChecker.java#L起-L止]] ；代码块头部：(src: FlowRuleChecker.java#L起-L止) -->

# FlowRuleChecker.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.csp:sentinel-core:1.8.10` |
| 路径 | `com/alibaba/csp/sentinel/slots/block/flow/FlowRuleChecker.java` |
| 行数 | 210 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/csp/sentinel-core/1.8.10/sentinel-core-1.8.10-sources.jar |
| 抓取时间 | 2026-11-25T15:33:29.698Z |

```java
    1 | /*
    2 |  * Copyright 1999-2018 Alibaba Group Holding Ltd.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      http://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | package com.alibaba.csp.sentinel.slots.block.flow;
   17 | 
   18 | import java.util.Collection;
   19 | 
   20 | import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
   21 | import com.alibaba.csp.sentinel.cluster.server.EmbeddedClusterTokenServerProvider;
   22 | import com.alibaba.csp.sentinel.cluster.client.TokenClientProvider;
   23 | import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
   24 | import com.alibaba.csp.sentinel.cluster.TokenResult;
   25 | import com.alibaba.csp.sentinel.cluster.TokenService;
   26 | import com.alibaba.csp.sentinel.context.Context;
   27 | import com.alibaba.csp.sentinel.log.RecordLog;
   28 | import com.alibaba.csp.sentinel.node.DefaultNode;
   29 | import com.alibaba.csp.sentinel.node.Node;
   30 | import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
   31 | import com.alibaba.csp.sentinel.slots.block.BlockException;
   32 | import com.alibaba.csp.sentinel.slots.block.RuleConstant;
   33 | import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
   34 | import com.alibaba.csp.sentinel.util.StringUtil;
   35 | import com.alibaba.csp.sentinel.util.function.Function;
   36 | 
   37 | /**
   38 |  * Rule checker for flow control rules.
   39 |  *
   40 |  * @author Eric Zhao
   41 |  */
   42 | public class FlowRuleChecker {
   43 | 
   44 |     public void checkFlow(Function<String, Collection<FlowRule>> ruleProvider, ResourceWrapper resource,
   45 |                           Context context, DefaultNode node, int count, boolean prioritized) throws BlockException {
   46 |         if (ruleProvider == null || resource == null) {
   47 |             return;
   48 |         }
   49 |         Collection<FlowRule> rules = ruleProvider.apply(resource.getName());
   50 |         if (rules != null) {
   51 |             for (FlowRule rule : rules) {
   52 |                 if (!canPassCheck(rule, context, node, count, prioritized)) {
   53 |                     throw new FlowException(rule.getLimitApp(), rule);
   54 |                 }
   55 |             }
   56 |         }
   57 |     }
   58 | 
   59 |     public boolean canPassCheck(/*@NonNull*/ FlowRule rule, Context context, DefaultNode node,
   60 |                                                     int acquireCount) {
   61 |         return canPassCheck(rule, context, node, acquireCount, false);
   62 |     }
   63 | 
   64 |     public boolean canPassCheck(/*@NonNull*/ FlowRule rule, Context context, DefaultNode node, int acquireCount,
   65 |                                                     boolean prioritized) {
   66 |         String limitApp = rule.getLimitApp();
   67 |         if (limitApp == null) {
   68 |             return true;
   69 |         }
   70 | 
   71 |         if (rule.isClusterMode()) {
   72 |             return passClusterCheck(rule, context, node, acquireCount, prioritized);
   73 |         }
   74 | 
   75 |         return passLocalCheck(rule, context, node, acquireCount, prioritized);
   76 |     }
   77 | 
   78 |     private static boolean passLocalCheck(FlowRule rule, Context context, DefaultNode node, int acquireCount,
   79 |                                           boolean prioritized) {
   80 |         Node selectedNode = selectNodeByRequesterAndStrategy(rule, context, node);
   81 |         if (selectedNode == null) {
   82 |             return true;
   83 |         }
   84 | 
   85 |         return rule.getRater().canPass(selectedNode, acquireCount, prioritized);
   86 |     }
   87 | 
   88 |     static Node selectReferenceNode(FlowRule rule, Context context, DefaultNode node) {
   89 |         String refResource = rule.getRefResource();
   90 |         int strategy = rule.getStrategy();
   91 | 
   92 |         if (StringUtil.isEmpty(refResource)) {
   93 |             return null;
   94 |         }
   95 | 
   96 |         if (strategy == RuleConstant.STRATEGY_RELATE) {
   97 |             return ClusterBuilderSlot.getClusterNode(refResource);
   98 |         }
   99 | 
  100 |         if (strategy == RuleConstant.STRATEGY_CHAIN) {
  101 |             if (!refResource.equals(context.getName())) {
  102 |                 return null;
  103 |             }
  104 |             return node;
  105 |         }
  106 |         // No node.
  107 |         return null;
  108 |     }
  109 | 
  110 |     private static boolean filterOrigin(String origin) {
  111 |         // Origin cannot be `default` or `other`.
  112 |         return !RuleConstant.LIMIT_APP_DEFAULT.equals(origin) && !RuleConstant.LIMIT_APP_OTHER.equals(origin);
  113 |     }
  114 | 
  115 |     static Node selectNodeByRequesterAndStrategy(/*@NonNull*/ FlowRule rule, Context context, DefaultNode node) {
  116 |         // The limit app should not be empty.
  117 |         String limitApp = rule.getLimitApp();
  118 |         int strategy = rule.getStrategy();
  119 |         String origin = context.getOrigin();
  120 | 
  121 |         if (limitApp.equals(origin) && filterOrigin(origin)) {
  122 |             if (strategy == RuleConstant.STRATEGY_DIRECT) {
  123 |                 // Matches limit origin, return origin statistic node.
  124 |                 return context.getOriginNode();
  125 |             }
  126 | 
  127 |             return selectReferenceNode(rule, context, node);
  128 |         } else if (RuleConstant.LIMIT_APP_DEFAULT.equals(limitApp)) {
  129 |             if (strategy == RuleConstant.STRATEGY_DIRECT) {
  130 |                 // Return the cluster node.
  131 |                 return node.getClusterNode();
  132 |             }
  133 | 
  134 |             return selectReferenceNode(rule, context, node);
  135 |         } else if (RuleConstant.LIMIT_APP_OTHER.equals(limitApp)
  136 |             && FlowRuleManager.isOtherOrigin(origin, rule.getResource())) {
  137 |             if (strategy == RuleConstant.STRATEGY_DIRECT) {
  138 |                 return context.getOriginNode();
  139 |             }
  140 | 
  141 |             return selectReferenceNode(rule, context, node);
  142 |         }
  143 | 
  144 |         return null;
  145 |     }
  146 | 
  147 |     private static boolean passClusterCheck(FlowRule rule, Context context, DefaultNode node, int acquireCount,
  148 |                                             boolean prioritized) {
  149 |         try {
  150 |             TokenService clusterService = pickClusterService();
  151 |             if (clusterService == null) {
  152 |                 return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
  153 |             }
  154 |             long flowId = rule.getClusterConfig().getFlowId();
  155 |             TokenResult result = clusterService.requestToken(flowId, acquireCount, prioritized);
  156 |             return applyTokenResult(result, rule, context, node, acquireCount, prioritized);
  157 |             // If client is absent, then fallback to local mode.
  158 |         } catch (Throwable ex) {
  159 |             RecordLog.warn("[FlowRuleChecker] Request cluster token unexpected failed", ex);
  160 |         }
  161 |         // Fallback to local flow control when token client or server for this rule is not available.
  162 |         // If fallback is not enabled, then directly pass.
  163 |         return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
  164 |     }
  165 | 
  166 |     private static boolean fallbackToLocalOrPass(FlowRule rule, Context context, DefaultNode node, int acquireCount,
  167 |                                                  boolean prioritized) {
  168 |         if (rule.getClusterConfig().isFallbackToLocalWhenFail()) {
  169 |             return passLocalCheck(rule, context, node, acquireCount, prioritized);
  170 |         } else {
  171 |             // The rule won't be activated, just pass.
  172 |             return true;
  173 |         }
  174 |     }
  175 | 
  176 |     private static TokenService pickClusterService() {
  177 |         if (ClusterStateManager.isClient()) {
  178 |             return TokenClientProvider.getClient();
  179 |         }
  180 |         if (ClusterStateManager.isServer()) {
  181 |             return EmbeddedClusterTokenServerProvider.getServer();
  182 |         }
  183 |         return null;
  184 |     }
  185 | 
  186 |     private static boolean applyTokenResult(/*@NonNull*/ TokenResult result, FlowRule rule, Context context,
  187 |                                                          DefaultNode node,
  188 |                                                          int acquireCount, boolean prioritized) {
  189 |         switch (result.getStatus()) {
  190 |             case TokenResultStatus.OK:
  191 |                 return true;
  192 |             case TokenResultStatus.SHOULD_WAIT:
  193 |                 // Wait for next tick.
  194 |                 try {
  195 |                     Thread.sleep(result.getWaitInMs());
  196 |                 } catch (InterruptedException e) {
  197 |                     e.printStackTrace();
  198 |                 }
  199 |                 return true;
  200 |             case TokenResultStatus.NO_RULE_EXISTS:
  201 |             case TokenResultStatus.BAD_REQUEST:
  202 |             case TokenResultStatus.FAIL:
  203 |             case TokenResultStatus.TOO_MANY_REQUEST:
  204 |                 return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
  205 |             case TokenResultStatus.BLOCKED:
  206 |             default:
  207 |                 return false;
  208 |         }
  209 |     }
  210 | }
```
