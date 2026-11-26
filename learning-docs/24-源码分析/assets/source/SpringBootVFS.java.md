<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis.spring.boot:mybatis-spring-boot-autoconfigure:4.1.0 | path: org/mybatis/spring/boot/autoconfigure/SpringBootVFS.java -->
<!-- 正文引用：[[源码:SpringBootVFS.java#L起-L止]] ；代码块头部：(src: SpringBootVFS.java#L起-L止) -->

# SpringBootVFS.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis.spring.boot:mybatis-spring-boot-autoconfigure:4.1.0` |
| 路径 | `org/mybatis/spring/boot/autoconfigure/SpringBootVFS.java` |
| 行数 | 111 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/spring/boot/mybatis-spring-boot-autoconfigure/4.1.0/mybatis-spring-boot-autoconfigure-4.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:01:44.571Z |

```java
    1 | /*
    2 |  *    Copyright 2015-2023 the original author or authors.
    3 |  *
    4 |  *    Licensed under the Apache License, Version 2.0 (the "License");
    5 |  *    you may not use this file except in compliance with the License.
    6 |  *    You may obtain a copy of the License at
    7 |  *
    8 |  *       https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  *    Unless required by applicable law or agreed to in writing, software
   11 |  *    distributed under the License is distributed on an "AS IS" BASIS,
   12 |  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  *    See the License for the specific language governing permissions and
   14 |  *    limitations under the License.
   15 |  */
   16 | package org.mybatis.spring.boot.autoconfigure;
   17 | 
   18 | import java.io.IOException;
   19 | import java.io.UncheckedIOException;
   20 | import java.net.URL;
   21 | import java.net.URLDecoder;
   22 | import java.nio.charset.Charset;
   23 | import java.text.Normalizer;
   24 | import java.util.List;
   25 | import java.util.function.Supplier;
   26 | import java.util.stream.Collectors;
   27 | import java.util.stream.Stream;
   28 | 
   29 | import org.apache.ibatis.io.VFS;
   30 | import org.springframework.core.io.Resource;
   31 | import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
   32 | import org.springframework.core.io.support.ResourcePatternResolver;
   33 | import org.springframework.util.ClassUtils;
   34 | 
   35 | /**
   36 |  * @author Hans Westerbeek
   37 |  * @author Eddú Meléndez
   38 |  * @author Kazuki Shimizu
   39 |  */
   40 | public class SpringBootVFS extends VFS {
   41 | 
   42 |   private static Charset urlDecodingCharset;
   43 |   private static Supplier<ClassLoader> classLoaderSupplier;
   44 |   private final ResourcePatternResolver resourceResolver;
   45 | 
   46 |   static {
   47 |     setUrlDecodingCharset(Charset.defaultCharset());
   48 |     setClassLoaderSupplier(ClassUtils::getDefaultClassLoader);
   49 |   }
   50 | 
   51 |   public SpringBootVFS() {
   52 |     this.resourceResolver = new PathMatchingResourcePatternResolver(classLoaderSupplier.get());
   53 |   }
   54 | 
   55 |   @Override
   56 |   public boolean isValid() {
   57 |     return true;
   58 |   }
   59 | 
   60 |   @Override
   61 |   protected List<String> list(URL url, String path) throws IOException {
   62 |     String urlString = URLDecoder.decode(url.toString(), urlDecodingCharset);
   63 |     String baseUrlString = urlString.endsWith("/") ? urlString : urlString.concat("/");
   64 |     Resource[] resources = resourceResolver.getResources(baseUrlString + "**/*.class");
   65 |     return Stream.of(resources).map(resource -> preserveSubpackageName(baseUrlString, resource, path))
   66 |         .collect(Collectors.toList());
   67 |   }
   68 | 
   69 |   /**
   70 |    * Set the charset for decoding an encoded URL string.
   71 |    * <p>
   72 |    * Default is system default charset.
   73 |    * </p>
   74 |    *
   75 |    * @param charset
   76 |    *          the charset for decoding an encoded URL string
   77 |    *
   78 |    * @since 2.3.0
   79 |    */
   80 |   public static void setUrlDecodingCharset(Charset charset) {
   81 |     urlDecodingCharset = charset;
   82 |   }
   83 | 
   84 |   /**
   85 |    * Set the supplier for providing {@link ClassLoader} to used.
   86 |    * <p>
   87 |    * Default is a returned instance from {@link ClassUtils#getDefaultClassLoader()}.
   88 |    * </p>
   89 |    *
   90 |    * @param supplier
   91 |    *          the supplier for providing {@link ClassLoader} to used
   92 |    *
   93 |    * @since 3.0.2
   94 |    */
   95 |   public static void setClassLoaderSupplier(Supplier<ClassLoader> supplier) {
   96 |     classLoaderSupplier = supplier;
   97 |   }
   98 | 
   99 |   private static String preserveSubpackageName(final String baseUrlString, final Resource resource,
  100 |       final String rootPath) {
  101 |     try {
  102 |       return rootPath + (rootPath.endsWith("/") ? "" : "/")
  103 |           + Normalizer
  104 |               .normalize(URLDecoder.decode(resource.getURL().toString(), urlDecodingCharset), Normalizer.Form.NFC)
  105 |               .substring(baseUrlString.length());
  106 |     } catch (IOException e) {
  107 |       throw new UncheckedIOException(e);
  108 |     }
  109 |   }
  110 | 
  111 | }
```
