<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/scripting/xmltags/XMLScriptBuilder.java -->
<!-- 正文引用：[[源码:XMLScriptBuilder.java#L起-L止]] ；代码块头部：(src: XMLScriptBuilder.java#L起-L止) -->

# XMLScriptBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/scripting/xmltags/XMLScriptBuilder.java` |
| 行数 | 281 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:59:16.890Z |

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
   16 | package org.apache.ibatis.scripting.xmltags;
   17 | 
   18 | import java.util.ArrayList;
   19 | import java.util.HashMap;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | import java.util.concurrent.ConcurrentHashMap;
   23 | 
   24 | import org.apache.ibatis.builder.BaseBuilder;
   25 | import org.apache.ibatis.builder.BuilderException;
   26 | import org.apache.ibatis.mapping.SqlSource;
   27 | import org.apache.ibatis.parsing.XNode;
   28 | import org.apache.ibatis.reflection.ParamNameResolver;
   29 | import org.apache.ibatis.scripting.defaults.RawSqlSource;
   30 | import org.apache.ibatis.session.Configuration;
   31 | import org.w3c.dom.Node;
   32 | import org.w3c.dom.NodeList;
   33 | 
   34 | /**
   35 |  * @author Clinton Begin
   36 |  */
   37 | public class XMLScriptBuilder extends BaseBuilder {
   38 | 
   39 |   private final XNode context;
   40 |   private boolean isDynamic;
   41 |   private final Class<?> parameterType;
   42 |   private final ParamNameResolver paramNameResolver;
   43 |   private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
   44 |   private static final Map<String, SqlNode> emptyNodeCache = new ConcurrentHashMap<>();
   45 | 
   46 |   public XMLScriptBuilder(Configuration configuration, XNode context) {
   47 |     this(configuration, context, null);
   48 |   }
   49 | 
   50 |   public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType) {
   51 |     this(configuration, context, parameterType, null);
   52 |   }
   53 | 
   54 |   public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType,
   55 |       ParamNameResolver paramNameResolver) {
   56 |     super(configuration);
   57 |     this.context = context;
   58 |     this.parameterType = parameterType;
   59 |     this.paramNameResolver = paramNameResolver;
   60 |     initNodeHandlerMap();
   61 |   }
   62 | 
   63 |   private void initNodeHandlerMap() {
   64 |     nodeHandlerMap.put("trim", new TrimHandler());
   65 |     nodeHandlerMap.put("where", new WhereHandler());
   66 |     nodeHandlerMap.put("set", new SetHandler());
   67 |     nodeHandlerMap.put("foreach", new ForEachHandler());
   68 |     nodeHandlerMap.put("if", new IfHandler());
   69 |     nodeHandlerMap.put("choose", new ChooseHandler());
   70 |     nodeHandlerMap.put("when", new IfHandler());
   71 |     nodeHandlerMap.put("otherwise", new OtherwiseHandler());
   72 |     nodeHandlerMap.put("bind", new BindHandler());
   73 |   }
   74 | 
   75 |   public SqlSource parseScriptNode() {
   76 |     MixedSqlNode rootSqlNode = parseDynamicTags(context);
   77 |     SqlSource sqlSource;
   78 |     if (isDynamic) {
   79 |       sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
   80 |     } else {
   81 |       sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType, paramNameResolver);
   82 |     }
   83 |     return sqlSource;
   84 |   }
   85 | 
   86 |   protected MixedSqlNode parseDynamicTags(XNode node) {
   87 |     List<SqlNode> contents = new ArrayList<>();
   88 |     NodeList children = node.getNode().getChildNodes();
   89 |     for (int i = 0; i < children.getLength(); i++) {
   90 |       XNode child = node.newXNode(children.item(i));
   91 |       if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
   92 |         String data = child.getStringBody("");
   93 |         if (data.trim().isEmpty()) {
   94 |           contents.add(emptyNodeCache.computeIfAbsent(data, EmptySqlNode::new));
   95 |           continue;
   96 |         }
   97 |         TextSqlNode textSqlNode = new TextSqlNode(data);
   98 |         if (textSqlNode.isDynamic()) {
   99 |           contents.add(textSqlNode);
  100 |           isDynamic = true;
  101 |         } else {
  102 |           contents.add(new StaticTextSqlNode(data));
  103 |         }
  104 |       } else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) { // issue #628
  105 |         String nodeName = child.getNode().getNodeName();
  106 |         NodeHandler handler = nodeHandlerMap.get(nodeName);
  107 |         if (handler == null) {
  108 |           throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
  109 |         }
  110 |         handler.handleNode(child, contents);
  111 |         isDynamic = true;
  112 |       }
  113 |     }
  114 |     return new MixedSqlNode(contents);
  115 |   }
  116 | 
  117 |   private interface NodeHandler {
  118 |     void handleNode(XNode nodeToHandle, List<SqlNode> targetContents);
  119 |   }
  120 | 
  121 |   private static class BindHandler implements NodeHandler {
  122 |     public BindHandler() {
  123 |       // Prevent Synthetic Access
  124 |     }
  125 | 
  126 |     @Override
  127 |     public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
  128 |       final String name = nodeToHandle.getStringAttribute("name");
  129 |       final String expression = nodeToHandle.getStringAttribute("value");
  130 |       final VarDeclSqlNode node = new VarDeclSqlNode(name, expression);
  131 |       targetContents.add(node);
  132 |     }
  133 |   }
  134 | 
  135 |   private class TrimHandler implements NodeHandler {
  136 |     public TrimHandler() {
  137 |       // Prevent Synthetic Access
  138 |     }
  139 | 
  140 |     @Override
  141 |     public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
  142 |       MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
  143 |       String prefix = nodeToHandle.getStringAttribute("prefix");
  144 |       String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
  145 |       String suffix = nodeToHandle.getStringAttribute("suffix");
  146 |       String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");
  147 |       TrimSqlNode trim = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
  148 |       targetContents.add(trim);
  149 |     }
  150 |   }
  151 | 
  152 |   private class WhereHandler implements NodeHandler {
  153 |     public WhereHandler() {
  154 |       // Prevent Synthetic Access
  155 |     }
  156 | 
  157 |     @Override
  158 |     public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
  159 |       MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
  160 |       WhereSqlNode where = new WhereSqlNode(configuration, mixedSqlNode);
  161 |       targetContents.add(where);
  162 |     }
  163 |   }
  164 | 
  165 |   private class SetHandler implements NodeHandler {
  166 |     public SetHandler() {
  167 |       // Prevent Synthetic Access
  168 |     }
  169 | 
  170 |     @Override
  171 |     public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
  172 |       MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
  173 |       SetSqlNode set = new SetSqlNode(configuration, mixedSqlNode);
  174 |       targetContents.add(set);
  175 |     }
  176 |   }
  177 | 
  178 |   private class ForEachHandler implements NodeHandler {
  179 |     public ForEachHandler() {
  180 |       // Prevent Synthetic Access
  181 |     }
  182 | 
  183 |     @Override
  184 |     public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
  185 |       MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
  186 |       String collection = nodeToHandle.getStringAttribute("collection");
  187 |       Boolean nullable = nodeToHandle.getBooleanAttribute("nullable");
  188 |       String item = nodeToHandle.getStringAttribute("item");
  189 |       String index = nodeToHandle.getStringAttribute("index");
  190 |       String open = nodeToHandle.getStringAttribute("open");
  191 |       String close = nodeToHandle.getStringAttribute("close");
  192 |       String separator = nodeToHandle.getStringAttribute("separator");
  193 |       ForEachSqlNode forEachSqlNode = new ForEachSqlNode(configuration, mixedSqlNode, collection, nullable, index, item,
  194 |           open, close, separator);
  195 |       targetContents.add(forEachSqlNode);
  196 |     }
  197 |   }
  198 | 
  199 |   private class IfHandler implements NodeHandler {
  200 |     public IfHandler() {
  201 |       // Prevent Synthetic Access
  202 |     }
  203 | 
  204 |     @Override
  205 |     public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
  206 |       MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
  207 |       String test = nodeToHandle.getStringAttribute("test");
  208 |       IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
  209 |       targetContents.add(ifSqlNode);
  210 |     }
  211 |   }
  212 | 
  213 |   private class OtherwiseHandler implements NodeHandler {
  214 |     public OtherwiseHandler() {
  215 |       // Prevent Synthetic Access
  216 |     }
  217 | 
  218 |     @Override
  219 |     public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
  220 |       MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
  221 |       targetContents.add(mixedSqlNode);
  222 |     }
  223 |   }
  224 | 
  225 |   private class ChooseHandler implements NodeHandler {
  226 |     public ChooseHandler() {
  227 |       // Prevent Synthetic Access
  228 |     }
  229 | 
  230 |     @Override
  231 |     public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
  232 |       List<SqlNode> whenSqlNodes = new ArrayList<>();
  233 |       List<SqlNode> otherwiseSqlNodes = new ArrayList<>();
  234 |       handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
  235 |       SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
  236 |       ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
  237 |       targetContents.add(chooseSqlNode);
  238 |     }
  239 | 
  240 |     private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes,
  241 |         List<SqlNode> defaultSqlNodes) {
  242 |       List<XNode> children = chooseSqlNode.getChildren();
  243 |       for (XNode child : children) {
  244 |         String nodeName = child.getNode().getNodeName();
  245 |         NodeHandler handler = nodeHandlerMap.get(nodeName);
  246 |         if (handler instanceof IfHandler) {
  247 |           handler.handleNode(child, ifSqlNodes);
  248 |         } else if (handler instanceof OtherwiseHandler) {
  249 |           handler.handleNode(child, defaultSqlNodes);
  250 |         } else {
  251 |           throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement.");
  252 |         }
  253 |       }
  254 |     }
  255 | 
  256 |     private SqlNode getDefaultSqlNode(List<SqlNode> defaultSqlNodes) {
  257 |       SqlNode defaultSqlNode = null;
  258 |       if (defaultSqlNodes.size() == 1) {
  259 |         defaultSqlNode = defaultSqlNodes.get(0);
  260 |       } else if (defaultSqlNodes.size() > 1) {
  261 |         throw new BuilderException("Too many default (otherwise) elements in choose statement.");
  262 |       }
  263 |       return defaultSqlNode;
  264 |     }
  265 |   }
  266 | 
  267 |   private static class EmptySqlNode implements SqlNode {
  268 |     private final String whitespaces;
  269 | 
  270 |     public EmptySqlNode(String whitespaces) {
  271 |       super();
  272 |       this.whitespaces = whitespaces;
  273 |     }
  274 | 
  275 |     @Override
  276 |     public boolean apply(DynamicContext context) {
  277 |       context.appendSql(whitespaces);
  278 |       return true;
  279 |     }
  280 |   }
  281 | }
```
