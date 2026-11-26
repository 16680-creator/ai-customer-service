<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/api/naming/pojo/ServiceInfo.java -->
<!-- 正文引用：[[源码:ServiceInfo.java#L起-L止]] ；代码块头部：(src: ServiceInfo.java#L起-L止) -->

# ServiceInfo.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/api/naming/pojo/ServiceInfo.java` |
| 行数 | 323 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:11.106Z |

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
   16 | 
   17 | package com.alibaba.nacos.api.naming.pojo;
   18 | 
   19 | import com.alibaba.nacos.api.common.Constants;
   20 | import com.fasterxml.jackson.annotation.JsonIgnore;
   21 | import com.fasterxml.jackson.annotation.JsonInclude;
   22 | import com.fasterxml.jackson.annotation.JsonInclude.Include;
   23 | 
   24 | import java.io.UnsupportedEncodingException;
   25 | import java.net.URLEncoder;
   26 | import java.util.ArrayList;
   27 | import java.util.HashMap;
   28 | import java.util.List;
   29 | import java.util.Map;
   30 | 
   31 | /**
   32 |  * Service Information with instances and without cluster information, used in data pushing and cached for nacos-client.
   33 |  *
   34 |  * @author nkorange
   35 |  * @author shizhengxing
   36 |  */
   37 | @JsonInclude(Include.NON_NULL)
   38 | public class ServiceInfo implements Cloneable {
   39 |     
   40 |     /**
   41 |      * file name pattern: groupName@@name@@clusters.
   42 |      */
   43 |     private static final int GROUP_POSITION = 0;
   44 |     
   45 |     private static final int SERVICE_POSITION = 1;
   46 |     
   47 |     private static final int CLUSTER_POSITION = 2;
   48 |     
   49 |     private static final int FILE_NAME_PARTS = 3;
   50 |     
   51 |     @JsonIgnore
   52 |     private String jsonFromServer = EMPTY;
   53 |     
   54 |     private static final String EMPTY = "";
   55 |     
   56 |     private static final String DEFAULT_CHARSET = "UTF-8";
   57 |     
   58 |     private String name;
   59 |     
   60 |     private String groupName;
   61 |     
   62 |     private String clusters;
   63 |     
   64 |     private long cacheMillis = 1000L;
   65 |     
   66 |     private List<Instance> hosts = new ArrayList<>();
   67 |     
   68 |     private long lastRefTime = 0L;
   69 |     
   70 |     private String checksum = "";
   71 |     
   72 |     private volatile boolean allIps = false;
   73 |     
   74 |     private volatile boolean reachProtectionThreshold = false;
   75 |     
   76 |     public ServiceInfo() {
   77 |     }
   78 |     
   79 |     public boolean isAllIps() {
   80 |         return allIps;
   81 |     }
   82 |     
   83 |     public void setAllIps(boolean allIps) {
   84 |         this.allIps = allIps;
   85 |     }
   86 |     
   87 |     /**
   88 |      * There is only one form of the key:groupName@@name@@clusters. This constructor used by DiskCache.read(String) and
   89 |      * FailoverReactor.FailoverFileReader,you should know that 'groupName' must not be null,and 'clusters' can be null.
   90 |      */
   91 |     public ServiceInfo(final String key) {
   92 |         String[] keys = key.split(Constants.SERVICE_INFO_SPLITER);
   93 |         if (keys.length >= FILE_NAME_PARTS) {
   94 |             this.groupName = keys[GROUP_POSITION];
   95 |             this.name = keys[SERVICE_POSITION];
   96 |             this.clusters = keys[CLUSTER_POSITION];
   97 |         } else if (keys.length == CLUSTER_POSITION) {
   98 |             this.groupName = keys[GROUP_POSITION];
   99 |             this.name = keys[SERVICE_POSITION];
  100 |         } else {
  101 |             //defensive programming
  102 |             throw new IllegalArgumentException(
  103 |                 "Can't parse out 'groupName',but it must not be null!");
  104 |         }
  105 |     }
  106 |     
  107 |     public ServiceInfo(String name, String clusters) {
  108 |         this.name = name;
  109 |         this.clusters = clusters;
  110 |     }
  111 |     
  112 |     public int ipCount() {
  113 |         return hosts.size();
  114 |     }
  115 |     
  116 |     public boolean expired() {
  117 |         return System.currentTimeMillis() - lastRefTime > cacheMillis;
  118 |     }
  119 |     
  120 |     public void setHosts(List<Instance> hosts) {
  121 |         this.hosts = hosts;
  122 |     }
  123 |     
  124 |     public void addHost(Instance host) {
  125 |         hosts.add(host);
  126 |     }
  127 |     
  128 |     public void addAllHosts(List<? extends Instance> hosts) {
  129 |         this.hosts.addAll(hosts);
  130 |     }
  131 |     
  132 |     public List<Instance> getHosts() {
  133 |         return new ArrayList<>(hosts);
  134 |     }
  135 |     
  136 |     public boolean isValid() {
  137 |         return hosts != null;
  138 |     }
  139 |     
  140 |     public String getName() {
  141 |         return name;
  142 |     }
  143 |     
  144 |     public void setName(String name) {
  145 |         this.name = name;
  146 |     }
  147 |     
  148 |     public String getGroupName() {
  149 |         return groupName;
  150 |     }
  151 |     
  152 |     public void setGroupName(String groupName) {
  153 |         this.groupName = groupName;
  154 |     }
  155 |     
  156 |     public void setLastRefTime(long lastRefTime) {
  157 |         this.lastRefTime = lastRefTime;
  158 |     }
  159 |     
  160 |     public long getLastRefTime() {
  161 |         return lastRefTime;
  162 |     }
  163 |     
  164 |     public String getClusters() {
  165 |         return clusters;
  166 |     }
  167 |     
  168 |     public void setClusters(String clusters) {
  169 |         this.clusters = clusters;
  170 |     }
  171 |     
  172 |     public long getCacheMillis() {
  173 |         return cacheMillis;
  174 |     }
  175 |     
  176 |     public void setCacheMillis(long cacheMillis) {
  177 |         this.cacheMillis = cacheMillis;
  178 |     }
  179 |     
  180 |     /**
  181 |      * Judge whether service info is validate.
  182 |      *
  183 |      * @return true if validate, otherwise false
  184 |      */
  185 |     public boolean validate() {
  186 |         if (isAllIps()) {
  187 |             return true;
  188 |         }
  189 |         
  190 |         if (hosts == null) {
  191 |             return false;
  192 |         }
  193 |         
  194 |         boolean existValidHosts = false;
  195 |         for (Instance host : hosts) {
  196 |             if (host.isHealthy() && host.getWeight() > 0) {
  197 |                 existValidHosts = true;
  198 |                 break;
  199 |             }
  200 |         }
  201 |         return existValidHosts;
  202 |     }
  203 |     
  204 |     @JsonIgnore
  205 |     public String getJsonFromServer() {
  206 |         return jsonFromServer;
  207 |     }
  208 |     
  209 |     public void setJsonFromServer(String jsonFromServer) {
  210 |         this.jsonFromServer = jsonFromServer;
  211 |     }
  212 |     
  213 |     @JsonIgnore
  214 |     public String getKey() {
  215 |         String serviceName = getGroupedServiceName();
  216 |         return getKey(serviceName, clusters);
  217 |     }
  218 |     
  219 |     @JsonIgnore
  220 |     public static String getKey(String name, String clusters) {
  221 |         if (!isEmpty(clusters)) {
  222 |             return name + Constants.SERVICE_INFO_SPLITER + clusters;
  223 |         }
  224 |         return name;
  225 |     }
  226 |     
  227 |     @JsonIgnore
  228 |     public String getKeyWithoutClusters() {
  229 |         return getGroupedServiceName();
  230 |     }
  231 |     
  232 |     @JsonIgnore
  233 |     public String getKeyEncoded() {
  234 |         String serviceName = getGroupedServiceName();
  235 |         try {
  236 |             serviceName = URLEncoder.encode(serviceName, DEFAULT_CHARSET);
  237 |         } catch (UnsupportedEncodingException ignored) {
  238 |         }
  239 |         return getKey(serviceName, clusters);
  240 |     }
  241 |     
  242 |     private String getGroupedServiceName() {
  243 |         String serviceName = this.name;
  244 |         if (!isEmpty(groupName) && !serviceName.contains(Constants.SERVICE_INFO_SPLITER)) {
  245 |             serviceName = groupName + Constants.SERVICE_INFO_SPLITER + serviceName;
  246 |         }
  247 |         return serviceName;
  248 |     }
  249 |     
  250 |     /**
  251 |      * Get {@link ServiceInfo} from key.
  252 |      *
  253 |      * @param key key of service info
  254 |      * @return new service info
  255 |      */
  256 |     public static ServiceInfo fromKey(final String key) {
  257 |         return new ServiceInfo(key);
  258 |     }
  259 |     
  260 |     @Override
  261 |     public String toString() {
  262 |         return getKey();
  263 |     }
  264 |     
  265 |     public String getChecksum() {
  266 |         return checksum;
  267 |     }
  268 |     
  269 |     public void setChecksum(String checksum) {
  270 |         this.checksum = checksum;
  271 |     }
  272 |     
  273 |     private static boolean isEmpty(String str) {
  274 |         return str == null || str.length() == 0;
  275 |     }
  276 |     
  277 |     public boolean isReachProtectionThreshold() {
  278 |         return reachProtectionThreshold;
  279 |     }
  280 |     
  281 |     public void setReachProtectionThreshold(boolean reachProtectionThreshold) {
  282 |         this.reachProtectionThreshold = reachProtectionThreshold;
  283 |     }
  284 |     
  285 |     @Override
  286 |     public ServiceInfo clone() {
  287 |         ServiceInfo cloned = new ServiceInfo();
  288 |         cloned.jsonFromServer = this.jsonFromServer;
  289 |         cloned.name = this.name;
  290 |         cloned.groupName = this.groupName;
  291 |         cloned.clusters = this.clusters;
  292 |         cloned.cacheMillis = this.cacheMillis;
  293 |         cloned.lastRefTime = this.lastRefTime;
  294 |         cloned.checksum = this.checksum;
  295 |         cloned.allIps = this.allIps;
  296 |         cloned.reachProtectionThreshold = this.reachProtectionThreshold;
  297 |         cloned.hosts = new ArrayList<>();
  298 |         
  299 |         if (this.hosts != null) {
  300 |             for (Instance host : this.hosts) {
  301 |                 Instance clonedHost = new Instance();
  302 |                 clonedHost.setInstanceId(host.getInstanceId());
  303 |                 clonedHost.setIp(host.getIp());
  304 |                 clonedHost.setPort(host.getPort());
  305 |                 clonedHost.setWeight(host.getWeight());
  306 |                 clonedHost.setHealthy(host.isHealthy());
  307 |                 clonedHost.setEnabled(host.isEnabled());
  308 |                 clonedHost.setEphemeral(host.isEphemeral());
  309 |                 clonedHost.setClusterName(host.getClusterName());
  310 |                 clonedHost.setServiceName(host.getServiceName());
  311 |                 
  312 |                 if (host.getMetadata() != null) {
  313 |                     Map<String, String> clonedMetadata = new HashMap<>(host.getMetadata());
  314 |                     clonedHost.setMetadata(clonedMetadata);
  315 |                 }
  316 |                 
  317 |                 cloned.hosts.add(clonedHost);
  318 |             }
  319 |         }
  320 |         
  321 |         return cloned;
  322 |     }
  323 | }
```
