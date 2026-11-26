<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/parsing/XPathParser.java -->
<!-- 正文引用：[[源码:XPathParser.java#L起-L止]] ；代码块头部：(src: XPathParser.java#L起-L止) -->

# XPathParser.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/parsing/XPathParser.java` |
| 行数 | 274 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:23.416Z |

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
   18 | import java.io.InputStream;
   19 | import java.io.Reader;
   20 | import java.io.StringReader;
   21 | import java.util.ArrayList;
   22 | import java.util.List;
   23 | import java.util.Properties;
   24 | 
   25 | import javax.xml.XMLConstants;
   26 | import javax.xml.namespace.QName;
   27 | import javax.xml.parsers.DocumentBuilder;
   28 | import javax.xml.parsers.DocumentBuilderFactory;
   29 | import javax.xml.xpath.XPath;
   30 | import javax.xml.xpath.XPathConstants;
   31 | import javax.xml.xpath.XPathFactory;
   32 | 
   33 | import org.apache.ibatis.builder.BuilderException;
   34 | import org.w3c.dom.Document;
   35 | import org.w3c.dom.Node;
   36 | import org.w3c.dom.NodeList;
   37 | import org.xml.sax.EntityResolver;
   38 | import org.xml.sax.ErrorHandler;
   39 | import org.xml.sax.InputSource;
   40 | import org.xml.sax.SAXException;
   41 | import org.xml.sax.SAXParseException;
   42 | 
   43 | /**
   44 |  * @author Clinton Begin
   45 |  * @author Kazuki Shimizu
   46 |  */
   47 | public class XPathParser {
   48 | 
   49 |   private final Document document;
   50 |   private boolean validation;
   51 |   private EntityResolver entityResolver;
   52 |   private Properties variables;
   53 |   private XPath xpath;
   54 | 
   55 |   public XPathParser(String xml) {
   56 |     commonConstructor(false, null, null);
   57 |     this.document = createDocument(new InputSource(new StringReader(xml)));
   58 |   }
   59 | 
   60 |   public XPathParser(Reader reader) {
   61 |     commonConstructor(false, null, null);
   62 |     this.document = createDocument(new InputSource(reader));
   63 |   }
   64 | 
   65 |   public XPathParser(InputStream inputStream) {
   66 |     commonConstructor(false, null, null);
   67 |     this.document = createDocument(new InputSource(inputStream));
   68 |   }
   69 | 
   70 |   public XPathParser(Document document) {
   71 |     commonConstructor(false, null, null);
   72 |     this.document = document;
   73 |   }
   74 | 
   75 |   public XPathParser(String xml, boolean validation) {
   76 |     commonConstructor(validation, null, null);
   77 |     this.document = createDocument(new InputSource(new StringReader(xml)));
   78 |   }
   79 | 
   80 |   public XPathParser(Reader reader, boolean validation) {
   81 |     commonConstructor(validation, null, null);
   82 |     this.document = createDocument(new InputSource(reader));
   83 |   }
   84 | 
   85 |   public XPathParser(InputStream inputStream, boolean validation) {
   86 |     commonConstructor(validation, null, null);
   87 |     this.document = createDocument(new InputSource(inputStream));
   88 |   }
   89 | 
   90 |   public XPathParser(Document document, boolean validation) {
   91 |     commonConstructor(validation, null, null);
   92 |     this.document = document;
   93 |   }
   94 | 
   95 |   public XPathParser(String xml, boolean validation, Properties variables) {
   96 |     commonConstructor(validation, variables, null);
   97 |     this.document = createDocument(new InputSource(new StringReader(xml)));
   98 |   }
   99 | 
  100 |   public XPathParser(Reader reader, boolean validation, Properties variables) {
  101 |     commonConstructor(validation, variables, null);
  102 |     this.document = createDocument(new InputSource(reader));
  103 |   }
  104 | 
  105 |   public XPathParser(InputStream inputStream, boolean validation, Properties variables) {
  106 |     commonConstructor(validation, variables, null);
  107 |     this.document = createDocument(new InputSource(inputStream));
  108 |   }
  109 | 
  110 |   public XPathParser(Document document, boolean validation, Properties variables) {
  111 |     commonConstructor(validation, variables, null);
  112 |     this.document = document;
  113 |   }
  114 | 
  115 |   public XPathParser(String xml, boolean validation, Properties variables, EntityResolver entityResolver) {
  116 |     commonConstructor(validation, variables, entityResolver);
  117 |     this.document = createDocument(new InputSource(new StringReader(xml)));
  118 |   }
  119 | 
  120 |   public XPathParser(Reader reader, boolean validation, Properties variables, EntityResolver entityResolver) {
  121 |     commonConstructor(validation, variables, entityResolver);
  122 |     this.document = createDocument(new InputSource(reader));
  123 |   }
  124 | 
  125 |   public XPathParser(InputStream inputStream, boolean validation, Properties variables, EntityResolver entityResolver) {
  126 |     commonConstructor(validation, variables, entityResolver);
  127 |     this.document = createDocument(new InputSource(inputStream));
  128 |   }
  129 | 
  130 |   public XPathParser(Document document, boolean validation, Properties variables, EntityResolver entityResolver) {
  131 |     commonConstructor(validation, variables, entityResolver);
  132 |     this.document = document;
  133 |   }
  134 | 
  135 |   public void setVariables(Properties variables) {
  136 |     this.variables = variables;
  137 |   }
  138 | 
  139 |   public String evalString(String expression) {
  140 |     return evalString(document, expression);
  141 |   }
  142 | 
  143 |   public String evalString(Object root, String expression) {
  144 |     String result = (String) evaluate(expression, root, XPathConstants.STRING);
  145 |     return PropertyParser.parse(result, variables);
  146 |   }
  147 | 
  148 |   public Boolean evalBoolean(String expression) {
  149 |     return evalBoolean(document, expression);
  150 |   }
  151 | 
  152 |   public Boolean evalBoolean(Object root, String expression) {
  153 |     return (Boolean) evaluate(expression, root, XPathConstants.BOOLEAN);
  154 |   }
  155 | 
  156 |   public Short evalShort(String expression) {
  157 |     return evalShort(document, expression);
  158 |   }
  159 | 
  160 |   public Short evalShort(Object root, String expression) {
  161 |     return Short.valueOf(evalString(root, expression));
  162 |   }
  163 | 
  164 |   public Integer evalInteger(String expression) {
  165 |     return evalInteger(document, expression);
  166 |   }
  167 | 
  168 |   public Integer evalInteger(Object root, String expression) {
  169 |     return Integer.valueOf(evalString(root, expression));
  170 |   }
  171 | 
  172 |   public Long evalLong(String expression) {
  173 |     return evalLong(document, expression);
  174 |   }
  175 | 
  176 |   public Long evalLong(Object root, String expression) {
  177 |     return Long.valueOf(evalString(root, expression));
  178 |   }
  179 | 
  180 |   public Float evalFloat(String expression) {
  181 |     return evalFloat(document, expression);
  182 |   }
  183 | 
  184 |   public Float evalFloat(Object root, String expression) {
  185 |     return Float.valueOf(evalString(root, expression));
  186 |   }
  187 | 
  188 |   public Double evalDouble(String expression) {
  189 |     return evalDouble(document, expression);
  190 |   }
  191 | 
  192 |   public Double evalDouble(Object root, String expression) {
  193 |     return (Double) evaluate(expression, root, XPathConstants.NUMBER);
  194 |   }
  195 | 
  196 |   public List<XNode> evalNodes(String expression) {
  197 |     return evalNodes(document, expression);
  198 |   }
  199 | 
  200 |   public List<XNode> evalNodes(Object root, String expression) {
  201 |     List<XNode> xnodes = new ArrayList<>();
  202 |     NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
  203 |     for (int i = 0; i < nodes.getLength(); i++) {
  204 |       xnodes.add(new XNode(this, nodes.item(i), variables));
  205 |     }
  206 |     return xnodes;
  207 |   }
  208 | 
  209 |   public XNode evalNode(String expression) {
  210 |     return evalNode(document, expression);
  211 |   }
  212 | 
  213 |   public XNode evalNode(Object root, String expression) {
  214 |     Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
  215 |     if (node == null) {
  216 |       return null;
  217 |     }
  218 |     return new XNode(this, node, variables);
  219 |   }
  220 | 
  221 |   private Object evaluate(String expression, Object root, QName returnType) {
  222 |     try {
  223 |       return xpath.evaluate(expression, root, returnType);
  224 |     } catch (Exception e) {
  225 |       throw new BuilderException("Error evaluating XPath.  Cause: " + e, e);
  226 |     }
  227 |   }
  228 | 
  229 |   private Document createDocument(InputSource inputSource) {
  230 |     // important: this must only be called AFTER common constructor
  231 |     try {
  232 |       DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
  233 |       factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
  234 |       factory.setValidating(validation);
  235 | 
  236 |       factory.setNamespaceAware(false);
  237 |       factory.setIgnoringComments(true);
  238 |       factory.setIgnoringElementContentWhitespace(false);
  239 |       factory.setCoalescing(false);
  240 |       factory.setExpandEntityReferences(false);
  241 | 
  242 |       DocumentBuilder builder = factory.newDocumentBuilder();
  243 |       builder.setEntityResolver(entityResolver);
  244 |       builder.setErrorHandler(new ErrorHandler() {
  245 |         @Override
  246 |         public void error(SAXParseException exception) throws SAXException {
  247 |           throw exception;
  248 |         }
  249 | 
  250 |         @Override
  251 |         public void fatalError(SAXParseException exception) throws SAXException {
  252 |           throw exception;
  253 |         }
  254 | 
  255 |         @Override
  256 |         public void warning(SAXParseException exception) throws SAXException {
  257 |           // NOP
  258 |         }
  259 |       });
  260 |       return builder.parse(inputSource);
  261 |     } catch (Exception e) {
  262 |       throw new BuilderException("Error creating document instance.  Cause: " + e, e);
  263 |     }
  264 |   }
  265 | 
  266 |   private void commonConstructor(boolean validation, Properties variables, EntityResolver entityResolver) {
  267 |     this.validation = validation;
  268 |     this.entityResolver = entityResolver;
  269 |     this.variables = variables;
  270 |     XPathFactory factory = XPathFactory.newInstance();
  271 |     this.xpath = factory.newXPath();
  272 |   }
  273 | 
  274 | }
```
