<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/reflection/MetaObject.java -->
<!-- 正文引用：[[源码:MetaObject.java#L起-L止]] ；代码块头部：(src: MetaObject.java#L起-L止) -->

# MetaObject.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/reflection/MetaObject.java` |
| 行数 | 153 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:32.074Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2025 the original author or authors.
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
   16 | package org.apache.ibatis.reflection;
   17 | 
   18 | import java.lang.reflect.Type;
   19 | import java.util.Collection;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | import java.util.Map.Entry;
   23 | 
   24 | import org.apache.ibatis.reflection.factory.ObjectFactory;
   25 | import org.apache.ibatis.reflection.property.PropertyTokenizer;
   26 | import org.apache.ibatis.reflection.wrapper.BeanWrapper;
   27 | import org.apache.ibatis.reflection.wrapper.CollectionWrapper;
   28 | import org.apache.ibatis.reflection.wrapper.MapWrapper;
   29 | import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
   30 | import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
   31 | 
   32 | /**
   33 |  * @author Clinton Begin
   34 |  */
   35 | public class MetaObject {
   36 | 
   37 |   private final Object originalObject;
   38 |   private final ObjectWrapper objectWrapper;
   39 |   private final ObjectFactory objectFactory;
   40 |   private final ObjectWrapperFactory objectWrapperFactory;
   41 |   private final ReflectorFactory reflectorFactory;
   42 | 
   43 |   private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory,
   44 |       ReflectorFactory reflectorFactory) {
   45 |     this.originalObject = object;
   46 |     this.objectFactory = objectFactory;
   47 |     this.objectWrapperFactory = objectWrapperFactory;
   48 |     this.reflectorFactory = reflectorFactory;
   49 | 
   50 |     if (object instanceof ObjectWrapper) {
   51 |       this.objectWrapper = (ObjectWrapper) object;
   52 |     } else if (objectWrapperFactory.hasWrapperFor(object)) {
   53 |       this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);
   54 |     } else if (object instanceof Map) {
   55 |       this.objectWrapper = new MapWrapper(this, (Map) object);
   56 |     } else if (object instanceof Collection) {
   57 |       this.objectWrapper = new CollectionWrapper(this, (Collection) object);
   58 |     } else {
   59 |       this.objectWrapper = new BeanWrapper(this, object);
   60 |     }
   61 |   }
   62 | 
   63 |   public static MetaObject forObject(Object object, ObjectFactory objectFactory,
   64 |       ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
   65 |     if (object == null) {
   66 |       return SystemMetaObject.NULL_META_OBJECT;
   67 |     }
   68 |     return new MetaObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
   69 |   }
   70 | 
   71 |   public ObjectFactory getObjectFactory() {
   72 |     return objectFactory;
   73 |   }
   74 | 
   75 |   public ObjectWrapperFactory getObjectWrapperFactory() {
   76 |     return objectWrapperFactory;
   77 |   }
   78 | 
   79 |   public ReflectorFactory getReflectorFactory() {
   80 |     return reflectorFactory;
   81 |   }
   82 | 
   83 |   public Object getOriginalObject() {
   84 |     return originalObject;
   85 |   }
   86 | 
   87 |   public String findProperty(String propName, boolean useCamelCaseMapping) {
   88 |     return objectWrapper.findProperty(propName, useCamelCaseMapping);
   89 |   }
   90 | 
   91 |   public String[] getGetterNames() {
   92 |     return objectWrapper.getGetterNames();
   93 |   }
   94 | 
   95 |   public String[] getSetterNames() {
   96 |     return objectWrapper.getSetterNames();
   97 |   }
   98 | 
   99 |   public Class<?> getSetterType(String name) {
  100 |     return objectWrapper.getSetterType(name);
  101 |   }
  102 | 
  103 |   public Entry<Type, Class<?>> getGenericSetterType(String name) {
  104 |     return objectWrapper.getGenericSetterType(name);
  105 |   }
  106 | 
  107 |   public Class<?> getGetterType(String name) {
  108 |     return objectWrapper.getGetterType(name);
  109 |   }
  110 | 
  111 |   public Entry<Type, Class<?>> getGenericGetterType(String name) {
  112 |     return objectWrapper.getGenericGetterType(name);
  113 |   }
  114 | 
  115 |   public boolean hasSetter(String name) {
  116 |     return objectWrapper.hasSetter(name);
  117 |   }
  118 | 
  119 |   public boolean hasGetter(String name) {
  120 |     return objectWrapper.hasGetter(name);
  121 |   }
  122 | 
  123 |   public Object getValue(String name) {
  124 |     PropertyTokenizer prop = new PropertyTokenizer(name);
  125 |     return objectWrapper.get(prop);
  126 |   }
  127 | 
  128 |   public void setValue(String name, Object value) {
  129 |     objectWrapper.set(new PropertyTokenizer(name), value);
  130 |   }
  131 | 
  132 |   public MetaObject metaObjectForProperty(String name) {
  133 |     Object value = getValue(name);
  134 |     return MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
  135 |   }
  136 | 
  137 |   public ObjectWrapper getObjectWrapper() {
  138 |     return objectWrapper;
  139 |   }
  140 | 
  141 |   public boolean isCollection() {
  142 |     return objectWrapper.isCollection();
  143 |   }
  144 | 
  145 |   public void add(Object element) {
  146 |     objectWrapper.add(element);
  147 |   }
  148 | 
  149 |   public <E> void addAll(List<E> list) {
  150 |     objectWrapper.addAll(list);
  151 |   }
  152 | 
  153 | }
```
