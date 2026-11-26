<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.csp:sentinel-core:1.8.10 | path: com/alibaba/csp/sentinel/node/DefaultNode.java -->
<!-- 正文引用：[[源码:DefaultNode.java#L起-L止]] ；代码块头部：(src: DefaultNode.java#L起-L止) -->

# DefaultNode.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.csp:sentinel-core:1.8.10` |
| 路径 | `com/alibaba/csp/sentinel/node/DefaultNode.java` |
| 行数 | 170 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/csp/sentinel-core/1.8.10/sentinel-core-1.8.10-sources.jar |
| 抓取时间 | 2026-11-25T15:33:29.977Z |

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
   16 | package com.alibaba.csp.sentinel.node;
   17 | 
   18 | import java.util.HashSet;
   19 | import java.util.Set;
   20 | 
   21 | import com.alibaba.csp.sentinel.log.RecordLog;
   22 | import com.alibaba.csp.sentinel.SphO;
   23 | import com.alibaba.csp.sentinel.SphU;
   24 | import com.alibaba.csp.sentinel.context.Context;
   25 | import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
   26 | import com.alibaba.csp.sentinel.slots.nodeselector.NodeSelectorSlot;
   27 | 
   28 | /**
   29 |  * <p>
   30 |  * A {@link Node} used to hold statistics for specific resource name in the specific context.
   31 |  * Each distinct resource in each distinct {@link Context} will corresponding to a {@link DefaultNode}.
   32 |  * </p>
   33 |  * <p>
   34 |  * This class may have a list of sub {@link DefaultNode}s. Child nodes will be created when
   35 |  * calling {@link SphU}#entry() or {@link SphO}@entry() multiple times in the same {@link Context}.
   36 |  * </p>
   37 |  *
   38 |  * @author qinan.qn
   39 |  * @see NodeSelectorSlot
   40 |  */
   41 | public class DefaultNode extends StatisticNode {
   42 | 
   43 |     /**
   44 |      * The resource associated with the node.
   45 |      */
   46 |     private ResourceWrapper id;
   47 | 
   48 |     /**
   49 |      * The list of all child nodes.
   50 |      */
   51 |     private volatile Set<Node> childList = new HashSet<>();
   52 | 
   53 |     /**
   54 |      * Associated cluster node.
   55 |      */
   56 |     private ClusterNode clusterNode;
   57 | 
   58 |     public DefaultNode(ResourceWrapper id, ClusterNode clusterNode) {
   59 |         this.id = id;
   60 |         this.clusterNode = clusterNode;
   61 |     }
   62 | 
   63 |     public ResourceWrapper getId() {
   64 |         return id;
   65 |     }
   66 | 
   67 |     public ClusterNode getClusterNode() {
   68 |         return clusterNode;
   69 |     }
   70 | 
   71 |     public void setClusterNode(ClusterNode clusterNode) {
   72 |         this.clusterNode = clusterNode;
   73 |     }
   74 | 
   75 |     /**
   76 |      * Add child node to current node.
   77 |      *
   78 |      * @param node valid child node
   79 |      */
   80 |     public void addChild(Node node) {
   81 |         if (node == null) {
   82 |             RecordLog.warn("Trying to add null child to node <{}>, ignored", id.getName());
   83 |             return;
   84 |         }
   85 |         if (!childList.contains(node)) {
   86 |             synchronized (this) {
   87 |                 if (!childList.contains(node)) {
   88 |                     Set<Node> newSet = new HashSet<>(childList.size() + 1);
   89 |                     newSet.addAll(childList);
   90 |                     newSet.add(node);
   91 |                     childList = newSet;
   92 |                 }
   93 |             }
   94 |             RecordLog.info("Add child <{}> to node <{}>", ((DefaultNode)node).id.getName(), id.getName());
   95 |         }
   96 |     }
   97 | 
   98 |     /**
   99 |      * Reset the child node list.
  100 |      */
  101 |     public void removeChildList() {
  102 |         this.childList = new HashSet<>();
  103 |     }
  104 | 
  105 |     public Set<Node> getChildList() {
  106 |         return childList;
  107 |     }
  108 | 
  109 |     @Override
  110 |     public void increaseBlockQps(int count) {
  111 |         super.increaseBlockQps(count);
  112 |         this.clusterNode.increaseBlockQps(count);
  113 |     }
  114 | 
  115 |     @Override
  116 |     public void increaseExceptionQps(int count) {
  117 |         super.increaseExceptionQps(count);
  118 |         this.clusterNode.increaseExceptionQps(count);
  119 |     }
  120 | 
  121 |     @Override
  122 |     public void addRtAndSuccess(long rt, int successCount) {
  123 |         super.addRtAndSuccess(rt, successCount);
  124 |         this.clusterNode.addRtAndSuccess(rt, successCount);
  125 |     }
  126 | 
  127 |     @Override
  128 |     public void increaseThreadNum() {
  129 |         super.increaseThreadNum();
  130 |         this.clusterNode.increaseThreadNum();
  131 |     }
  132 | 
  133 |     @Override
  134 |     public void decreaseThreadNum() {
  135 |         super.decreaseThreadNum();
  136 |         this.clusterNode.decreaseThreadNum();
  137 |     }
  138 | 
  139 |     @Override
  140 |     public void addPassRequest(int count) {
  141 |         super.addPassRequest(count);
  142 |         this.clusterNode.addPassRequest(count);
  143 |     }
  144 | 
  145 |     public void printDefaultNode() {
  146 |         visitTree(0, this);
  147 |     }
  148 | 
  149 |     private void visitTree(int level, DefaultNode node) {
  150 |         for (int i = 0; i < level; ++i) {
  151 |             System.out.print("-");
  152 |         }
  153 |         if (!(node instanceof EntranceNode)) {
  154 |             System.out.println(
  155 |                 String.format("%s(thread:%s pq:%s bq:%s tq:%s rt:%s 1mp:%s 1mb:%s 1mt:%s)", node.id.getShowName(),
  156 |                     node.curThreadNum(), node.passQps(), node.blockQps(), node.totalQps(), node.avgRt(),
  157 |                     node.totalRequest() - node.blockRequest(), node.blockRequest(), node.totalRequest()));
  158 |         } else {
  159 |             System.out.println(
  160 |                 String.format("Entry-%s(t:%s pq:%s bq:%s tq:%s rt:%s 1mp:%s 1mb:%s 1mt:%s)", node.id.getShowName(),
  161 |                     node.curThreadNum(), node.passQps(), node.blockQps(), node.totalQps(), node.avgRt(),
  162 |                     node.totalRequest() - node.blockRequest(), node.blockRequest(), node.totalRequest()));
  163 |         }
  164 |         for (Node n : node.getChildList()) {
  165 |             DefaultNode dn = (DefaultNode)n;
  166 |             visitTree(level + 1, dn);
  167 |         }
  168 |     }
  169 | 
  170 | }
```
