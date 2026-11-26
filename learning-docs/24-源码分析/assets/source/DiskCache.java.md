<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/client/naming/cache/DiskCache.java -->
<!-- 正文引用：[[源码:DiskCache.java#L起-L止]] ；代码块头部：(src: DiskCache.java#L起-L止) -->

# DiskCache.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/client/naming/cache/DiskCache.java` |
| 行数 | 190 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:11.610Z |

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
   17 | package com.alibaba.nacos.client.naming.cache;
   18 | 
   19 | import com.alibaba.nacos.api.common.Constants;
   20 | import com.alibaba.nacos.api.naming.pojo.Instance;
   21 | import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
   22 | import com.alibaba.nacos.client.utils.ConcurrentDiskUtil;
   23 | import com.alibaba.nacos.common.utils.CollectionUtils;
   24 | import com.alibaba.nacos.api.utils.json.JsonUtils;
   25 | import com.alibaba.nacos.common.utils.StringUtils;
   26 | 
   27 | import java.io.BufferedReader;
   28 | import java.io.File;
   29 | import java.io.IOException;
   30 | import java.io.StringReader;
   31 | import java.io.UnsupportedEncodingException;
   32 | import java.net.URLDecoder;
   33 | import java.nio.charset.StandardCharsets;
   34 | import java.util.ArrayList;
   35 | import java.util.HashMap;
   36 | import java.util.List;
   37 | import java.util.Map;
   38 | 
   39 | import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;
   40 | 
   41 | /**
   42 |  * Disk cache.
   43 |  *
   44 |  * @author xuanyin
   45 |  */
   46 | public class DiskCache {
   47 |     
   48 |     /**
   49 |      * Write service info to dir.
   50 |      *
   51 |      * @param dom service info
   52 |      * @param dir directory
   53 |      */
   54 |     public static void write(ServiceInfo dom, String dir) {
   55 |         writeWithResult(dom, dir);
   56 |     }
   57 |     
   58 |     static boolean writeWithResult(ServiceInfo dom, String dir) {
   59 |         
   60 |         try {
   61 |             makeSureCacheDirExists(dir);
   62 |             
   63 |             File file = new File(dir, dom.getKeyEncoded());
   64 |             createFileIfAbsent(file, false);
   65 |             
   66 |             StringBuilder keyContentBuffer = new StringBuilder();
   67 |             
   68 |             String json = dom.getJsonFromServer();
   69 |             
   70 |             if (StringUtils.isEmpty(json)) {
   71 |                 json = JsonUtils.toJson(dom);
   72 |             }
   73 |             
   74 |             keyContentBuffer.append(json);
   75 |             
   76 |             //Use the concurrent API to ensure the consistency.
   77 |             ConcurrentDiskUtil.writeFileContent(file, keyContentBuffer.toString(),
   78 |                 StandardCharsets.UTF_8.name());
   79 |             return true;
   80 |         } catch (Throwable e) {
   81 |             NAMING_LOGGER.error("[NA] failed to write cache for dom:" + dom.getName(), e);
   82 |             return false;
   83 |         }
   84 |     }
   85 |     
   86 |     public static String getLineSeparator() {
   87 |         return System.getProperty("line.separator");
   88 |     }
   89 |     
   90 |     /**
   91 |      * Read service info from disk.
   92 |      *
   93 |      * @param cacheDir cache file dir
   94 |      * @return service infos
   95 |      */
   96 |     public static Map<String, ServiceInfo> read(String cacheDir) {
   97 |         Map<String, ServiceInfo> domMap = new HashMap<>(16);
   98 |         try {
   99 |             File[] files = makeSureCacheDirExists(cacheDir).listFiles();
  100 |             if (files == null || files.length == 0) {
  101 |                 return domMap;
  102 |             }
  103 |             
  104 |             for (File file : files) {
  105 |                 if (!file.isFile()) {
  106 |                     continue;
  107 |                 }
  108 |                 domMap.putAll(parseServiceInfoFromCache(file));
  109 |             }
  110 |         } catch (Throwable e) {
  111 |             NAMING_LOGGER.error("[NA] failed to read cache file", e);
  112 |         }
  113 |         
  114 |         return domMap;
  115 |     }
  116 |     
  117 |     /**
  118 |      * Parse Service info from cache file or failover file.
  119 |      *
  120 |      * @param file cache file or failover file
  121 |      * @return Service info
  122 |      * @throws UnsupportedEncodingException if the file is not encoded in UTF-8
  123 |      */
  124 |     public static Map<String, ServiceInfo> parseServiceInfoFromCache(File file)
  125 |         throws UnsupportedEncodingException {
  126 |         Map<String, ServiceInfo> result = new HashMap<>(1);
  127 |         String fileName = URLDecoder.decode(file.getName(), "UTF-8");
  128 |         if (!(fileName.endsWith(Constants.SERVICE_INFO_SPLITER + "meta") || fileName
  129 |             .endsWith(Constants.SERVICE_INFO_SPLITER + "special-url"))) {
  130 |             ServiceInfo dom = new ServiceInfo(fileName);
  131 |             List<Instance> ips = new ArrayList<>();
  132 |             dom.setHosts(ips);
  133 |             ServiceInfo newFormat = null;
  134 |             try (BufferedReader reader = new BufferedReader(
  135 |                 new StringReader(ConcurrentDiskUtil.getFileContent(file,
  136 |                     StandardCharsets.UTF_8.name())))) {
  137 |                 
  138 |                 String json;
  139 |                 while ((json = reader.readLine()) != null) {
  140 |                     try {
  141 |                         if (!json.startsWith("{")) {
  142 |                             continue;
  143 |                         }
  144 |                         
  145 |                         newFormat = JsonUtils.toObj(json, ServiceInfo.class);
  146 |                         
  147 |                         if (StringUtils.isEmpty(newFormat.getName())) {
  148 |                             ips.add(JsonUtils.toObj(json, Instance.class));
  149 |                         }
  150 |                     } catch (Throwable e) {
  151 |                         NAMING_LOGGER.error("[NA] error while parsing cache file: " + json, e);
  152 |                     }
  153 |                 }
  154 |             } catch (Exception e) {
  155 |                 NAMING_LOGGER.error("[NA] failed to read cache for dom: " + file.getName(), e);
  156 |             }
  157 |             if (newFormat != null && !StringUtils.isEmpty(newFormat.getName()) && !CollectionUtils
  158 |                 .isEmpty(newFormat.getHosts())) {
  159 |                 result.put(dom.getKey(), newFormat);
  160 |             } else if (!CollectionUtils.isEmpty(dom.getHosts())) {
  161 |                 result.put(dom.getKey(), dom);
  162 |             }
  163 |         }
  164 |         return result;
  165 |     }
  166 |     
  167 |     /**
  168 |      * Create file if absent.
  169 |      *
  170 |      * @param file  file
  171 |      * @param isDir is dir
  172 |      * @throws IOException if any io exception during create.
  173 |      */
  174 |     public static void createFileIfAbsent(File file, boolean isDir) throws IOException {
  175 |         if (file.exists()) {
  176 |             return;
  177 |         }
  178 |         boolean createResult = isDir ? file.mkdirs() : file.createNewFile();
  179 |         if (!createResult && !file.exists()) {
  180 |             throw new IllegalStateException(
  181 |                 "failed to create cache : " + (isDir ? "dir" : file) + file.getPath());
  182 |         }
  183 |     }
  184 |     
  185 |     private static File makeSureCacheDirExists(String dir) throws IOException {
  186 |         File cacheDir = new File(dir);
  187 |         createFileIfAbsent(cacheDir, true);
  188 |         return cacheDir;
  189 |     }
  190 | }
```
