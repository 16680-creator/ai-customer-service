<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/parsing/XNode.java -->
<!-- 正文引用：[[源码:XNode.java#L起-L止]] ；代码块头部：(src: XNode.java#L起-L止) -->

# XNode.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/parsing/XNode.java` |
| 行数 | 362 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:32.204Z |

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
   16 | package org.apache.ibatis.parsing;
   17 | 
   18 | import java.util.ArrayList;
   19 | import java.util.List;
   20 | import java.util.Map;
   21 | import java.util.Properties;
   22 | import java.util.function.Supplier;
   23 | 
   24 | import org.w3c.dom.CharacterData;
   25 | import org.w3c.dom.Element;
   26 | import org.w3c.dom.NamedNodeMap;
   27 | import org.w3c.dom.Node;
   28 | import org.w3c.dom.NodeList;
   29 | 
   30 | /**
   31 |  * @author Clinton Begin
   32 |  */
   33 | public class XNode {
   34 | 
   35 |   private final Node node;
   36 |   private final String name;
   37 |   private final String body;
   38 |   private final Properties attributes;
   39 |   private final Properties variables;
   40 |   private final XPathParser xpathParser;
   41 | 
   42 |   public XNode(XPathParser xpathParser, Node node, Properties variables) {
   43 |     this.xpathParser = xpathParser;
   44 |     this.node = node;
   45 |     this.name = node.getNodeName();
   46 |     this.variables = variables;
   47 |     this.attributes = parseAttributes(node);
   48 |     this.body = parseBody(node);
   49 |   }
   50 | 
   51 |   public XNode newXNode(Node node) {
   52 |     return new XNode(xpathParser, node, variables);
   53 |   }
   54 | 
   55 |   public XNode getParent() {
   56 |     Node parent = node.getParentNode();
   57 |     if (!(parent instanceof Element)) {
   58 |       return null;
   59 |     }
   60 |     return new XNode(xpathParser, parent, variables);
   61 |   }
   62 | 
   63 |   public String getPath() {
   64 |     StringBuilder builder = new StringBuilder();
   65 |     Node current = node;
   66 |     while (current instanceof Element) {
   67 |       if (current != node) {
   68 |         builder.insert(0, "/");
   69 |       }
   70 |       builder.insert(0, current.getNodeName());
   71 |       current = current.getParentNode();
   72 |     }
   73 |     return builder.toString();
   74 |   }
   75 | 
   76 |   public String getValueBasedIdentifier() {
   77 |     StringBuilder builder = new StringBuilder();
   78 |     XNode current = this;
   79 |     while (current != null) {
   80 |       if (current != this) {
   81 |         builder.insert(0, "_");
   82 |       }
   83 |       String value = current.getStringAttribute("id",
   84 |           current.getStringAttribute("value", current.getStringAttribute("property", (String) null)));
   85 |       if (value != null) {
   86 |         value = value.replace('.', '_');
   87 |         builder.insert(0, "]");
   88 |         builder.insert(0, value);
   89 |         builder.insert(0, "[");
   90 |       }
   91 |       builder.insert(0, current.getName());
   92 |       current = current.getParent();
   93 |     }
   94 |     return builder.toString();
   95 |   }
   96 | 
   97 |   public String evalString(String expression) {
   98 |     return xpathParser.evalString(node, expression);
   99 |   }
  100 | 
  101 |   public Boolean evalBoolean(String expression) {
  102 |     return xpathParser.evalBoolean(node, expression);
  103 |   }
  104 | 
  105 |   public Double evalDouble(String expression) {
  106 |     return xpathParser.evalDouble(node, expression);
  107 |   }
  108 | 
  109 |   public List<XNode> evalNodes(String expression) {
  110 |     return xpathParser.evalNodes(node, expression);
  111 |   }
  112 | 
  113 |   public XNode evalNode(String expression) {
  114 |     return xpathParser.evalNode(node, expression);
  115 |   }
  116 | 
  117 |   public Node getNode() {
  118 |     return node;
  119 |   }
  120 | 
  121 |   public String getName() {
  122 |     return name;
  123 |   }
  124 | 
  125 |   public String getStringBody() {
  126 |     return getStringBody(null);
  127 |   }
  128 | 
  129 |   public String getStringBody(String def) {
  130 |     return body == null ? def : body;
  131 |   }
  132 | 
  133 |   public Boolean getBooleanBody() {
  134 |     return getBooleanBody(null);
  135 |   }
  136 | 
  137 |   public Boolean getBooleanBody(Boolean def) {
  138 |     return body == null ? def : Boolean.valueOf(body);
  139 |   }
  140 | 
  141 |   public Integer getIntBody() {
  142 |     return getIntBody(null);
  143 |   }
  144 | 
  145 |   public Integer getIntBody(Integer def) {
  146 |     return body == null ? def : Integer.valueOf(body);
  147 |   }
  148 | 
  149 |   public Long getLongBody() {
  150 |     return getLongBody(null);
  151 |   }
  152 | 
  153 |   public Long getLongBody(Long def) {
  154 |     return body == null ? def : Long.valueOf(body);
  155 |   }
  156 | 
  157 |   public Double getDoubleBody() {
  158 |     return getDoubleBody(null);
  159 |   }
  160 | 
  161 |   public Double getDoubleBody(Double def) {
  162 |     return body == null ? def : Double.valueOf(body);
  163 |   }
  164 | 
  165 |   public Float getFloatBody() {
  166 |     return getFloatBody(null);
  167 |   }
  168 | 
  169 |   public Float getFloatBody(Float def) {
  170 |     return body == null ? def : Float.valueOf(body);
  171 |   }
  172 | 
  173 |   public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name) {
  174 |     return getEnumAttribute(enumType, name, null);
  175 |   }
  176 | 
  177 |   public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name, T def) {
  178 |     String value = getStringAttribute(name);
  179 |     return value == null ? def : Enum.valueOf(enumType, value);
  180 |   }
  181 | 
  182 |   /**
  183 |    * Return a attribute value as String.
  184 |    * <p>
  185 |    * If attribute value is absent, return value that provided from supplier of default value.
  186 |    *
  187 |    * @param name
  188 |    *          attribute name
  189 |    * @param defSupplier
  190 |    *          a supplier of default value
  191 |    *
  192 |    * @return the string attribute
  193 |    *
  194 |    * @since 3.5.4
  195 |    */
  196 |   public String getStringAttribute(String name, Supplier<String> defSupplier) {
  197 |     String value = attributes.getProperty(name);
  198 |     return value == null ? defSupplier.get() : value;
  199 |   }
  200 | 
  201 |   public String getStringAttribute(String name) {
  202 |     return getStringAttribute(name, (String) null);
  203 |   }
  204 | 
  205 |   public String getStringAttribute(String name, String def) {
  206 |     String value = attributes.getProperty(name);
  207 |     return value == null ? def : value;
  208 |   }
  209 | 
  210 |   public Boolean getBooleanAttribute(String name) {
  211 |     return getBooleanAttribute(name, null);
  212 |   }
  213 | 
  214 |   public Boolean getBooleanAttribute(String name, Boolean def) {
  215 |     String value = attributes.getProperty(name);
  216 |     return value == null ? def : Boolean.valueOf(value);
  217 |   }
  218 | 
  219 |   public Integer getIntAttribute(String name) {
  220 |     return getIntAttribute(name, null);
  221 |   }
  222 | 
  223 |   public Integer getIntAttribute(String name, Integer def) {
  224 |     String value = attributes.getProperty(name);
  225 |     return value == null ? def : Integer.valueOf(value);
  226 |   }
  227 | 
  228 |   public Long getLongAttribute(String name) {
  229 |     return getLongAttribute(name, null);
  230 |   }
  231 | 
  232 |   public Long getLongAttribute(String name, Long def) {
  233 |     String value = attributes.getProperty(name);
  234 |     return value == null ? def : Long.valueOf(value);
  235 |   }
  236 | 
  237 |   public Double getDoubleAttribute(String name) {
  238 |     return getDoubleAttribute(name, null);
  239 |   }
  240 | 
  241 |   public Double getDoubleAttribute(String name, Double def) {
  242 |     String value = attributes.getProperty(name);
  243 |     return value == null ? def : Double.valueOf(value);
  244 |   }
  245 | 
  246 |   public Float getFloatAttribute(String name) {
  247 |     return getFloatAttribute(name, null);
  248 |   }
  249 | 
  250 |   public Float getFloatAttribute(String name, Float def) {
  251 |     String value = attributes.getProperty(name);
  252 |     return value == null ? def : Float.valueOf(value);
  253 |   }
  254 | 
  255 |   public List<XNode> getChildren() {
  256 |     List<XNode> children = new ArrayList<>();
  257 |     NodeList nodeList = node.getChildNodes();
  258 |     if (nodeList != null) {
  259 |       for (int i = 0, n = nodeList.getLength(); i < n; i++) {
  260 |         Node node = nodeList.item(i);
  261 |         if (node.getNodeType() == Node.ELEMENT_NODE) {
  262 |           children.add(new XNode(xpathParser, node, variables));
  263 |         }
  264 |       }
  265 |     }
  266 |     return children;
  267 |   }
  268 | 
  269 |   public Properties getChildrenAsProperties() {
  270 |     Properties properties = new Properties();
  271 |     for (XNode child : getChildren()) {
  272 |       String name = child.getStringAttribute("name");
  273 |       String value = child.getStringAttribute("value");
  274 |       if (name != null && value != null) {
  275 |         properties.setProperty(name, value);
  276 |       }
  277 |     }
  278 |     return properties;
  279 |   }
  280 | 
  281 |   @Override
  282 |   public String toString() {
  283 |     return buildToString(new StringBuilder(), 0).toString();
  284 |   }
  285 | 
  286 |   private StringBuilder buildToString(StringBuilder builder, int indentLevel) {
  287 |     indent(builder, indentLevel).append("<").append(name);
  288 |     for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
  289 |       builder.append(" ");
  290 |       builder.append(entry.getKey());
  291 |       builder.append("=\"");
  292 |       builder.append(entry.getValue());
  293 |       builder.append("\"");
  294 |     }
  295 | 
  296 |     NodeList nodeList = node.getChildNodes();
  297 |     if (nodeList == null || nodeList.getLength() == 0) {
  298 |       builder.append(" />\n");
  299 |     } else {
  300 |       builder.append(">\n");
  301 |       for (int i = 0, n = nodeList.getLength(); i < n; i++) {
  302 |         Node node = nodeList.item(i);
  303 |         short nodeType = node.getNodeType();
  304 |         if (nodeType == Node.ELEMENT_NODE) {
  305 |           new XNode(xpathParser, node, variables).buildToString(builder, indentLevel + 1);
  306 |         } else {
  307 |           String text = getBodyData(node).trim();
  308 |           if (text.length() > 0) {
  309 |             indent(builder, indentLevel + 1).append(text).append("\n");
  310 |           }
  311 |         }
  312 |       }
  313 |       indent(builder, indentLevel).append("</").append(name).append(">\n");
  314 |     }
  315 | 
  316 |     return builder;
  317 |   }
  318 | 
  319 |   private StringBuilder indent(StringBuilder builder, int level) {
  320 |     for (int i = 0; i < level; i++) {
  321 |       builder.append("  ");
  322 |     }
  323 |     return builder;
  324 |   }
  325 | 
  326 |   private Properties parseAttributes(Node n) {
  327 |     Properties attributes = new Properties();
  328 |     NamedNodeMap attributeNodes = n.getAttributes();
  329 |     if (attributeNodes != null) {
  330 |       for (int i = 0; i < attributeNodes.getLength(); i++) {
  331 |         Node attribute = attributeNodes.item(i);
  332 |         String value = PropertyParser.parse(attribute.getNodeValue(), variables);
  333 |         attributes.put(attribute.getNodeName(), value);
  334 |       }
  335 |     }
  336 |     return attributes;
  337 |   }
  338 | 
  339 |   private String parseBody(Node node) {
  340 |     String data = getBodyData(node);
  341 |     if (data == null) {
  342 |       NodeList children = node.getChildNodes();
  343 |       for (int i = 0; i < children.getLength(); i++) {
  344 |         Node child = children.item(i);
  345 |         data = getBodyData(child);
  346 |         if (data != null) {
  347 |           break;
  348 |         }
  349 |       }
  350 |     }
  351 |     return data;
  352 |   }
  353 | 
  354 |   private String getBodyData(Node child) {
  355 |     if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
  356 |       String data = ((CharacterData) child).getData();
  357 |       return PropertyParser.parse(data, variables);
  358 |     }
  359 |     return null;
  360 |   }
  361 | 
  362 | }
```
