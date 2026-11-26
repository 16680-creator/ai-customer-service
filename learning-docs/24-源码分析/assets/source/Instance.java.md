<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/api/naming/pojo/Instance.java -->
<!-- 正文引用：[[源码:Instance.java#L起-L止]] ；代码块头部：(src: Instance.java#L起-L止) -->

# Instance.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/api/naming/pojo/Instance.java` |
| 行数 | 295 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:10.224Z |

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
   20 | import com.alibaba.nacos.api.exception.NacosException;
   21 | import com.alibaba.nacos.api.exception.api.NacosApiException;
   22 | import com.alibaba.nacos.api.model.NacosForm;
   23 | import com.alibaba.nacos.api.model.v2.ErrorCode;
   24 | import com.alibaba.nacos.api.naming.PreservedMetadataKeys;
   25 | import com.alibaba.nacos.api.naming.utils.NamingUtils;
   26 | import com.alibaba.nacos.api.utils.StringUtils;
   27 | import com.fasterxml.jackson.annotation.JsonInclude;
   28 | import com.fasterxml.jackson.annotation.JsonInclude.Include;
   29 | 
   30 | import java.util.HashMap;
   31 | import java.util.Map;
   32 | import java.util.Objects;
   33 | 
   34 | /**
   35 |  * Instance.
   36 |  *
   37 |  * @author nkorange
   38 |  */
   39 | @JsonInclude(Include.NON_NULL)
   40 | public class Instance implements NacosForm {
   41 |     
   42 |     private static final long serialVersionUID = -742906310567291979L;
   43 |     
   44 |     private static final int MAX_PORT = 65535;
   45 |     
   46 |     /**
   47 |      * unique id of this instance.
   48 |      */
   49 |     private String instanceId;
   50 |     
   51 |     /**
   52 |      * instance ip.
   53 |      */
   54 |     private String ip;
   55 |     
   56 |     /**
   57 |      * instance port.
   58 |      */
   59 |     private int port;
   60 |     
   61 |     /**
   62 |      * instance weight.
   63 |      */
   64 |     private double weight = 1.0D;
   65 |     
   66 |     /**
   67 |      * instance health status.
   68 |      */
   69 |     private boolean healthy = true;
   70 |     
   71 |     /**
   72 |      * If instance is enabled to accept request.
   73 |      */
   74 |     private boolean enabled = true;
   75 |     
   76 |     /**
   77 |      * If instance is ephemeral.
   78 |      *
   79 |      * @since 1.0.0
   80 |      */
   81 |     private boolean ephemeral = true;
   82 |     
   83 |     /**
   84 |      * cluster information of instance.
   85 |      */
   86 |     private String clusterName;
   87 |     
   88 |     /**
   89 |      * Service information of instance.
   90 |      */
   91 |     private String serviceName;
   92 |     
   93 |     /**
   94 |      * user extended attributes.
   95 |      */
   96 |     private Map<String, String> metadata = new HashMap<>();
   97 |     
   98 |     public String getInstanceId() {
   99 |         return this.instanceId;
  100 |     }
  101 |     
  102 |     public void setInstanceId(final String instanceId) {
  103 |         this.instanceId = instanceId;
  104 |     }
  105 |     
  106 |     public String getIp() {
  107 |         return this.ip;
  108 |     }
  109 |     
  110 |     public void setIp(final String ip) {
  111 |         this.ip = ip;
  112 |     }
  113 |     
  114 |     public int getPort() {
  115 |         return this.port;
  116 |     }
  117 |     
  118 |     public void setPort(final int port) {
  119 |         this.port = port;
  120 |     }
  121 |     
  122 |     public double getWeight() {
  123 |         return this.weight;
  124 |     }
  125 |     
  126 |     public void setWeight(final double weight) {
  127 |         this.weight = weight;
  128 |     }
  129 |     
  130 |     public boolean isHealthy() {
  131 |         return this.healthy;
  132 |     }
  133 |     
  134 |     public void setHealthy(final boolean healthy) {
  135 |         this.healthy = healthy;
  136 |     }
  137 |     
  138 |     public String getClusterName() {
  139 |         return this.clusterName;
  140 |     }
  141 |     
  142 |     public void setClusterName(final String clusterName) {
  143 |         this.clusterName = clusterName;
  144 |     }
  145 |     
  146 |     public String getServiceName() {
  147 |         return this.serviceName;
  148 |     }
  149 |     
  150 |     public void setServiceName(final String serviceName) {
  151 |         this.serviceName = serviceName;
  152 |     }
  153 |     
  154 |     public Map<String, String> getMetadata() {
  155 |         return this.metadata;
  156 |     }
  157 |     
  158 |     public void setMetadata(final Map<String, String> metadata) {
  159 |         this.metadata = metadata;
  160 |     }
  161 |     
  162 |     /**
  163 |      * add meta data.
  164 |      *
  165 |      * @param key   meta data key
  166 |      * @param value meta data value
  167 |      */
  168 |     public void addMetadata(final String key, final String value) {
  169 |         if (metadata == null) {
  170 |             metadata = new HashMap<>(4);
  171 |         }
  172 |         metadata.put(key, value);
  173 |     }
  174 |     
  175 |     public boolean isEnabled() {
  176 |         return this.enabled;
  177 |     }
  178 |     
  179 |     public void setEnabled(final boolean enabled) {
  180 |         this.enabled = enabled;
  181 |     }
  182 |     
  183 |     public boolean isEphemeral() {
  184 |         return this.ephemeral;
  185 |     }
  186 |     
  187 |     public void setEphemeral(final boolean ephemeral) {
  188 |         this.ephemeral = ephemeral;
  189 |     }
  190 |     
  191 |     @Override
  192 |     public void validate() throws NacosApiException {
  193 |         fillDefaultValue();
  194 |         if (StringUtils.isBlank(ip)) {
  195 |             throw new NacosApiException(NacosException.INVALID_PARAM, ErrorCode.PARAMETER_MISSING,
  196 |                 "Required parameter 'ip' type String is not present");
  197 |         }
  198 |         if (port < 0 || port > MAX_PORT) {
  199 |             throw new NacosApiException(NacosException.INVALID_PARAM,
  200 |                 ErrorCode.PARAMETER_VALIDATE_ERROR,
  201 |                 "Required parameter 'port' type int is require 0 ~ 65535");
  202 |         }
  203 |     }
  204 |     
  205 |     private void fillDefaultValue() {
  206 |         if (StringUtils.isBlank(clusterName)) {
  207 |             clusterName = Constants.DEFAULT_CLUSTER_NAME;
  208 |         }
  209 |     }
  210 |     
  211 |     @Override
  212 |     public String toString() {
  213 |         return "Instance{" + "instanceId='" + instanceId + '\'' + ", ip='" + ip + '\'' + ", port="
  214 |             + port + ", weight="
  215 |             + weight + ", healthy=" + healthy + ", enabled=" + enabled + ", ephemeral=" + ephemeral
  216 |             + ", clusterName='" + clusterName + '\'' + ", serviceName='" + serviceName + '\''
  217 |             + ", metadata="
  218 |             + metadata + '}';
  219 |     }
  220 |     
  221 |     public String toInetAddr() {
  222 |         return ip + ":" + port;
  223 |     }
  224 |     
  225 |     @Override
  226 |     public boolean equals(final Object obj) {
  227 |         if (!(obj instanceof Instance)) {
  228 |             return false;
  229 |         }
  230 |         
  231 |         final Instance host = (Instance) obj;
  232 |         return Instance.strEquals(host.toString(), toString());
  233 |     }
  234 |     
  235 |     @Override
  236 |     public int hashCode() {
  237 |         return toString().hashCode();
  238 |     }
  239 |     
  240 |     private static boolean strEquals(final String str1, final String str2) {
  241 |         return Objects.equals(str1, str2);
  242 |     }
  243 |     
  244 |     public long getInstanceHeartBeatInterval() {
  245 |         return getMetaDataByKeyWithDefault(PreservedMetadataKeys.HEART_BEAT_INTERVAL,
  246 |             Constants.DEFAULT_HEART_BEAT_INTERVAL);
  247 |     }
  248 |     
  249 |     public long getInstanceHeartBeatTimeOut() {
  250 |         return getMetaDataByKeyWithDefault(PreservedMetadataKeys.HEART_BEAT_TIMEOUT,
  251 |             Constants.DEFAULT_HEART_BEAT_TIMEOUT);
  252 |     }
  253 |     
  254 |     public long getIpDeleteTimeout() {
  255 |         return getMetaDataByKeyWithDefault(PreservedMetadataKeys.IP_DELETE_TIMEOUT,
  256 |             Constants.DEFAULT_IP_DELETE_TIMEOUT);
  257 |     }
  258 |     
  259 |     public String getInstanceIdGenerator() {
  260 |         return getMetaDataByKeyWithDefault(PreservedMetadataKeys.INSTANCE_ID_GENERATOR,
  261 |             Constants.DEFAULT_INSTANCE_ID_GENERATOR);
  262 |     }
  263 |     
  264 |     /**
  265 |      * Returns {@code true} if this metadata contains the specified key.
  266 |      *
  267 |      * @param key metadata key
  268 |      * @return {@code true} if this metadata contains the specified key
  269 |      */
  270 |     public boolean containsMetadata(final String key) {
  271 |         if (getMetadata() == null || getMetadata().isEmpty()) {
  272 |             return false;
  273 |         }
  274 |         return getMetadata().containsKey(key);
  275 |     }
  276 |     
  277 |     private long getMetaDataByKeyWithDefault(final String key, final long defaultValue) {
  278 |         if (getMetadata() == null || getMetadata().isEmpty()) {
  279 |             return defaultValue;
  280 |         }
  281 |         final String value = getMetadata().get(key);
  282 |         if (NamingUtils.isNumber(value)) {
  283 |             return Long.parseLong(value);
  284 |         }
  285 |         return defaultValue;
  286 |     }
  287 |     
  288 |     private String getMetaDataByKeyWithDefault(final String key, final String defaultValue) {
  289 |         if (getMetadata() == null || getMetadata().isEmpty()) {
  290 |             return defaultValue;
  291 |         }
  292 |         return getMetadata().get(key);
  293 |     }
  294 |     
  295 | }
```
