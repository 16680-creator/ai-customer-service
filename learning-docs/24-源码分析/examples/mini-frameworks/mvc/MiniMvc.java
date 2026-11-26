package mini.mvc;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory GET dispatcher: route selection, binding, and response status.
 *
 * 迷你 MVC 分发器：只演示五件事（对应真实 Spring 的五个类/方法）——
 *   1) 注册表：启动时扫一遍控制器的 @Get 方法，算出"路径模板 + 静态段个数"（对应 RequestMappingInfo）；
 *   2) 路由选择：先精确、再模板，静态段多的优先 —— 所以 /orders/all 不会被 /orders/{id} 抢走
 *      （对应 AbstractHandlerMethodMapping#lookupHandlerMethod 的"比具体度"）；
 *   3) 参数绑定：@Path 取路径变量、@Query 取查询串（对应 HandlerMethodArgumentResolver）；
 *   4) 状态码是分水岭的产物：找不到 404、方法不对 405、绑定失败 400、正常 200；
 *   5) 查询串要 URL 解码：`Li+Lei` 必须变回 `Li Lei`（对应 Servlet 容器对 queryString 的解码）。
 *
 * 编译与运行（在 docs/learning-docs 目录下，JDK 17；Windows 命令）：
 *   javac -encoding UTF-8 -d .source-cache/mini-check/mvc examples/mini-frameworks/mvc/MiniMvc.java
 *   java -cp .source-cache/mini-check/mvc mini.mvc.MiniMvc
 *
 * 正常退出时打印 7 行 PASS，退出码 0；任何断言不成立都会抛 AssertionError。
 */
public final class MiniMvc {
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
    public @interface Get { String value(); }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
    public @interface Path { String value(); }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.PARAMETER)
    public @interface Query { String value(); }
    public static final class Response {
        public final int status;
        public final String body;
        Response(int status, String body) { this.status = status; this.body = body; }
        public String toString() { return status + " " + body; }
    }
    public static final class Controller {
        @Get("/orders/{id}") public String detail(@Path("id") long id) { return "order:" + id; }
        @Get("/orders/all") public String all() { return "all-orders"; }
        @Get("/hello") public String hello(@Query("name") String name) { return "hello:" + name; }
    }
    public static final class Dispatcher {
        /** 注册表：一条 @Get 方法 = 一条路由（对应 HandlerMapping 里的 Map<RequestMappingInfo, HandlerMethod>） */
        private final List<Route> routes = new ArrayList<Route>();

        public Dispatcher(Object controller) {
            for (Method method : controller.getClass().getMethods()) {
                Get mapping = method.getAnnotation(Get.class);
                if (mapping == null) {
                    continue;
                }
                routes.add(new Route(controller, method, mapping.value()));
            }
            // 越具体的路由排越前：静态段多的优先。这样 /orders/all 先被匹配到，
            // 不会落到 /orders/{id} 上再因为 "all" 转不成 long 而报 400。
            routes.sort((a, b) -> Integer.compare(b.literalCount, a.literalCount));
        }

        public Response dispatch(String method, String target) {
            String path = target;
            String queryString = "";
            int questionMark = target.indexOf('?');
            if (questionMark >= 0) {
                path = target.substring(0, questionMark);
                queryString = target.substring(questionMark + 1);
            }
            String[] requested = splitPath(path);
            Map<String, String> query = parseQuery(queryString);

            Route matched = null;
            Map<String, String> pathVariables = null;
            for (Route route : routes) {
                Map<String, String> variables = route.match(requested);
                if (variables != null) {
                    matched = route;
                    pathVariables = variables;
                    break;                     // 已按"具体度"排好序，第一个匹配的就是最优解
                }
            }
            if (matched == null) {
                return new Response(404, "Not Found");          // 没有任何路由认领这个路径
            }
            if (!"GET".equals(method)) {
                return new Response(405, "Method Not Allowed"); // 路径有人认领，但动词不对
            }
            Object[] args;
            try {
                args = bindArguments(matched, pathVariables, query);
            } catch (IllegalArgumentException ex) {
                return new Response(400, ex.getMessage());      // 绑定失败统一是 400
            }
            try {
                return new Response(200, String.valueOf(matched.method.invoke(matched.controller, args)));
            } catch (InvocationTargetException ex) {
                return new Response(500, String.valueOf(ex.getCause()));
            } catch (ReflectiveOperationException ex) {
                return new Response(500, String.valueOf(ex));
            }
        }

        /** 参数绑定：每个参数先看注解决定"从哪儿取"，再做类型转换（对应 HandlerMethodArgumentResolver） */
        private Object[] bindArguments(Route route, Map<String, String> pathVariables,
                Map<String, String> query) {
            Class<?>[] types = route.method.getParameterTypes();
            Annotation[][] annotations = route.method.getParameterAnnotations();
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                String name = null;
                String raw = null;
                for (Annotation annotation : annotations[i]) {
                    if (annotation instanceof Path) {
                        name = ((Path) annotation).value();
                        raw = pathVariables.get(name);
                    } else if (annotation instanceof Query) {
                        name = ((Query) annotation).value();
                        raw = query.get(name);
                    }
                }
                if (name == null) {
                    throw new IllegalArgumentException("Unsupported parameter at index " + i);
                }
                if (raw == null) {
                    throw new IllegalArgumentException("Missing parameter: " + name);
                }
                args[i] = convert(types[i], name, raw);
            }
            return args;
        }

        private Object convert(Class<?> type, String name, String raw) {
            if (type == String.class) {
                return raw;
            }
            if (type == long.class || type == Long.class
                    || type == int.class || type == Integer.class) {
                try {
                    long value = Long.parseLong(raw);
                    return type == int.class || type == Integer.class ? (Object) (int) value : (Object) value;
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid number: " + name);
                }
            }
            throw new IllegalArgumentException("Unsupported parameter type: " + type.getSimpleName());
        }
    }

    /** 一条路由：路径模板 + 对应的控制器方法 + 静态段个数（"具体度"） */
    private static final class Route {
        final Object controller;
        final Method method;
        final String[] segments;
        final int literalCount;

        Route(Object controller, Method method, String template) {
            this.controller = controller;
            this.method = method;
            this.segments = splitPath(template);
            int literals = 0;
            for (String segment : segments) {
                if (!segment.startsWith("{")) {
                    literals++;
                }
            }
            this.literalCount = literals;
        }

        /** 返回路径变量表；不匹配返回 null */
        Map<String, String> match(String[] requested) {
            if (requested.length != segments.length) {
                return null;
            }
            Map<String, String> variables = new LinkedHashMap<String, String>();
            for (int i = 0; i < segments.length; i++) {
                String declared = segments[i];
                if (declared.startsWith("{") && declared.endsWith("}")) {
                    variables.put(declared.substring(1, declared.length() - 1), requested[i]);
                } else if (!declared.equals(requested[i])) {
                    return null;
                }
            }
            return variables;
        }
    }

    private static String[] splitPath(String path) {
        String trimmed = path;
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? new String[0] : trimmed.split("/");
    }

    /** `a=1&b=Li+Lei` → {a=1, b=Li Lei}；`+` 和 %XX 都按 UTF-8 解码 */
    private static Map<String, String> parseQuery(String queryString) {
        Map<String, String> query = new LinkedHashMap<String, String>();
        if (queryString.isEmpty()) {
            return query;
        }
        for (String pair : queryString.split("&")) {
            int equals = pair.indexOf('=');
            String key = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            query.put(decode(key), decode(value));
        }
        return query;
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 must be supported", ex);
        }
    }

    private static void expect(Dispatcher dispatcher, String method, String target, int status, String body) {
        Response response = dispatcher.dispatch(method, target);
        if (response.status != status || !response.body.equals(body)) {
            throw new AssertionError(method + " " + target + ": expected " + status + " " + body + ", got " + response);
        }
        System.out.println("PASS mvc: " + method + " " + target + " -> " + response);
    }
    public static void main(String[] args) {
        Dispatcher dispatcher = new Dispatcher(new Controller());
        expect(dispatcher, "GET", "/orders/42", 200, "order:42");
        expect(dispatcher, "GET", "/orders/all", 200, "all-orders");
        expect(dispatcher, "GET", "/missing", 404, "Not Found");
        expect(dispatcher, "GET", "/orders/abc", 400, "Invalid number: id");
        expect(dispatcher, "GET", "/hello", 400, "Missing parameter: name");
        expect(dispatcher, "GET", "/hello?name=Li+Lei", 200, "hello:Li Lei");
        expect(dispatcher, "POST", "/orders/42", 405, "Method Not Allowed");
    }
}
