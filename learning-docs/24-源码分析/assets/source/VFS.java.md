<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/io/VFS.java -->
<!-- 正文引用：[[源码:VFS.java#L起-L止]] ；代码块头部：(src: VFS.java#L起-L止) -->

# VFS.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/io/VFS.java` |
| 行数 | 244 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:23.826Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2024 the original author or authors.
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
   16 | package org.apache.ibatis.io;
   17 | 
   18 | import java.io.IOException;
   19 | import java.lang.reflect.InvocationTargetException;
   20 | import java.lang.reflect.Method;
   21 | import java.net.URL;
   22 | import java.util.ArrayList;
   23 | import java.util.Arrays;
   24 | import java.util.Collections;
   25 | import java.util.List;
   26 | 
   27 | import org.apache.ibatis.logging.Log;
   28 | import org.apache.ibatis.logging.LogFactory;
   29 | 
   30 | /**
   31 |  * Provides a very simple API for accessing resources within an application server.
   32 |  *
   33 |  * @author Ben Gunter
   34 |  */
   35 | public abstract class VFS {
   36 |   private static final Log log = LogFactory.getLog(VFS.class);
   37 | 
   38 |   /** The built-in implementations. */
   39 |   private static final Class<?>[] IMPLEMENTATIONS = { JBoss6VFS.class, DefaultVFS.class };
   40 | 
   41 |   /**
   42 |    * The list to which implementations are added by {@link #addImplClass(Class)}.
   43 |    */
   44 |   private static final List<Class<? extends VFS>> USER_IMPLEMENTATIONS = new ArrayList<>();
   45 | 
   46 |   /** Singleton instance holder. */
   47 |   private static class VFSHolder {
   48 |     static final VFS INSTANCE = createVFS();
   49 | 
   50 |     @SuppressWarnings("unchecked")
   51 |     static VFS createVFS() {
   52 |       // Try the user implementations first, then the built-ins
   53 |       List<Class<? extends VFS>> impls = new ArrayList<>(USER_IMPLEMENTATIONS);
   54 |       impls.addAll(Arrays.asList((Class<? extends VFS>[]) IMPLEMENTATIONS));
   55 | 
   56 |       // Try each implementation class until a valid one is found
   57 |       VFS vfs = null;
   58 |       for (int i = 0; vfs == null || !vfs.isValid(); i++) {
   59 |         Class<? extends VFS> impl = impls.get(i);
   60 |         try {
   61 |           vfs = impl.getDeclaredConstructor().newInstance();
   62 |           if (!vfs.isValid() && log.isDebugEnabled()) {
   63 |             log.debug("VFS implementation " + impl.getName() + " is not valid in this environment.");
   64 |           }
   65 |         } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
   66 |             | InvocationTargetException e) {
   67 |           log.error("Failed to instantiate " + impl, e);
   68 |           return null;
   69 |         }
   70 |       }
   71 | 
   72 |       if (log.isDebugEnabled()) {
   73 |         log.debug("Using VFS adapter " + vfs.getClass().getName());
   74 |       }
   75 | 
   76 |       return vfs;
   77 |     }
   78 | 
   79 |     private VFSHolder() {
   80 |     }
   81 |   }
   82 | 
   83 |   /**
   84 |    * Get the singleton {@link VFS} instance. If no {@link VFS} implementation can be found for the current environment,
   85 |    * then this method returns null.
   86 |    *
   87 |    * @return single instance of VFS
   88 |    */
   89 |   public static VFS getInstance() {
   90 |     return VFSHolder.INSTANCE;
   91 |   }
   92 | 
   93 |   /**
   94 |    * Adds the specified class to the list of {@link VFS} implementations. Classes added in this manner are tried in the
   95 |    * order they are added and before any of the built-in implementations.
   96 |    *
   97 |    * @param clazz
   98 |    *          The {@link VFS} implementation class to add.
   99 |    */
  100 |   public static void addImplClass(Class<? extends VFS> clazz) {
  101 |     if (clazz != null) {
  102 |       USER_IMPLEMENTATIONS.add(clazz);
  103 |     }
  104 |   }
  105 | 
  106 |   /**
  107 |    * Get a class by name. If the class is not found then return null.
  108 |    *
  109 |    * @param className
  110 |    *          the class name
  111 |    *
  112 |    * @return the class
  113 |    */
  114 |   protected static Class<?> getClass(String className) {
  115 |     try {
  116 |       return Thread.currentThread().getContextClassLoader().loadClass(className);
  117 |       // return ReflectUtil.findClass(className);
  118 |     } catch (ClassNotFoundException e) {
  119 |       if (log.isDebugEnabled()) {
  120 |         log.debug("Class not found: " + className);
  121 |       }
  122 |       return null;
  123 |     }
  124 |   }
  125 | 
  126 |   /**
  127 |    * Get a method by name and parameter types. If the method is not found then return null.
  128 |    *
  129 |    * @param clazz
  130 |    *          The class to which the method belongs.
  131 |    * @param methodName
  132 |    *          The name of the method.
  133 |    * @param parameterTypes
  134 |    *          The types of the parameters accepted by the method.
  135 |    *
  136 |    * @return the method
  137 |    */
  138 |   protected static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
  139 |     if (clazz == null) {
  140 |       return null;
  141 |     }
  142 |     try {
  143 |       return clazz.getMethod(methodName, parameterTypes);
  144 |     } catch (SecurityException e) {
  145 |       log.error("Security exception looking for method " + clazz.getName() + "." + methodName + ".  Cause: " + e);
  146 |       return null;
  147 |     } catch (NoSuchMethodException e) {
  148 |       log.error("Method not found " + clazz.getName() + "." + methodName + ".  Cause: " + e);
  149 |       return null;
  150 |     }
  151 |   }
  152 | 
  153 |   /**
  154 |    * Invoke a method on an object and return whatever it returns.
  155 |    *
  156 |    * @param <T>
  157 |    *          the generic type
  158 |    * @param method
  159 |    *          The method to invoke.
  160 |    * @param object
  161 |    *          The instance or class (for static methods) on which to invoke the method.
  162 |    * @param parameters
  163 |    *          The parameters to pass to the method.
  164 |    *
  165 |    * @return Whatever the method returns.
  166 |    *
  167 |    * @throws IOException
  168 |    *           If I/O errors occur
  169 |    * @throws RuntimeException
  170 |    *           If anything else goes wrong
  171 |    */
  172 |   @SuppressWarnings("unchecked")
  173 |   protected static <T> T invoke(Method method, Object object, Object... parameters)
  174 |       throws IOException, RuntimeException {
  175 |     try {
  176 |       return (T) method.invoke(object, parameters);
  177 |     } catch (IllegalArgumentException | IllegalAccessException e) {
  178 |       throw new RuntimeException(e);
  179 |     } catch (InvocationTargetException e) {
  180 |       if (e.getTargetException() instanceof IOException) {
  181 |         throw (IOException) e.getTargetException();
  182 |       }
  183 |       throw new RuntimeException(e);
  184 |     }
  185 |   }
  186 | 
  187 |   /**
  188 |    * Get a list of {@link URL}s from the context classloader for all the resources found at the specified path.
  189 |    *
  190 |    * @param path
  191 |    *          The resource path.
  192 |    *
  193 |    * @return A list of {@link URL}s, as returned by {@link ClassLoader#getResources(String)}.
  194 |    *
  195 |    * @throws IOException
  196 |    *           If I/O errors occur
  197 |    */
  198 |   protected static List<URL> getResources(String path) throws IOException {
  199 |     return Collections.list(Thread.currentThread().getContextClassLoader().getResources(path));
  200 |   }
  201 | 
  202 |   /**
  203 |    * Return true if the {@link VFS} implementation is valid for the current environment.
  204 |    *
  205 |    * @return true, if is valid
  206 |    */
  207 |   public abstract boolean isValid();
  208 | 
  209 |   /**
  210 |    * Recursively list the full resource path of all the resources that are children of the resource identified by a URL.
  211 |    *
  212 |    * @param url
  213 |    *          The URL that identifies the resource to list.
  214 |    * @param forPath
  215 |    *          The path to the resource that is identified by the URL. Generally, this is the value passed to
  216 |    *          {@link #getResources(String)} to get the resource URL.
  217 |    *
  218 |    * @return A list containing the names of the child resources.
  219 |    *
  220 |    * @throws IOException
  221 |    *           If I/O errors occur
  222 |    */
  223 |   protected abstract List<String> list(URL url, String forPath) throws IOException;
  224 | 
  225 |   /**
  226 |    * Recursively list the full resource path of all the resources that are children of all the resources found at the
  227 |    * specified path.
  228 |    *
  229 |    * @param path
  230 |    *          The path of the resource(s) to list.
  231 |    *
  232 |    * @return A list containing the names of the child resources.
  233 |    *
  234 |    * @throws IOException
  235 |    *           If I/O errors occur
  236 |    */
  237 |   public List<String> list(String path) throws IOException {
  238 |     List<String> names = new ArrayList<>();
  239 |     for (URL url : getResources(path)) {
  240 |       names.addAll(list(url, path));
  241 |     }
  242 |     return names;
  243 |   }
  244 | }
```
