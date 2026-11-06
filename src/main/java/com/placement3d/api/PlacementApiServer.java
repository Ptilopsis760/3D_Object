package com.placement3d.api;

import com.placement3d.algorithm.GreedyPlacementStrategy;
import com.placement3d.geometry.BoundingBox;
import com.placement3d.io.PlacementResultSerializer;
import com.placement3d.io.STLParser;
import com.placement3d.model.ModelInfo;
import com.placement3d.model.Mesh;
import com.placement3d.model.PlacedModel;
import com.placement3d.model.Vector3D;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量 REST API 服务器，基于 JDK 内置 HttpServer，无第三方框架依赖。
 *
 * <p><b>端点列表：</b></p>
 * <ul>
 *   <li>GET  /api/health               — 健康检查</li>
 *   <li>GET  /api/workspace/default    — 默认工作空间参数</li>
 *   <li>POST /api/place                — 多批次摆放（placeMultiBatch）</li>
 *   <li>POST /api/place/single-batch   — 单批次摆放（place）</li>
 * </ul>
 *
 * <p><b>启动示例：</b></p>
 * <pre>{@code
 * PlacementApiServer server = new PlacementApiServer(8080);
 * server.start();
 * // ...
 * server.stop();
 * }</pre>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
public class PlacementApiServer {

    // ── 默认参数 ──────────────────────────────────────────────────────────
    private static final float  DEFAULT_WORKSPACE_X   = 4000f;
    private static final float  DEFAULT_WORKSPACE_Y   = 2500f;
    private static final float  DEFAULT_WORKSPACE_Z   = 1500f;
    private static final float  DEFAULT_MIN_GAP       = 5.0f;
    private static final float  DEFAULT_UTILIZATION   = 0.55f;
    private static final int    DEFAULT_MAX_COPIES    = 10;
    private static final int    DEFAULT_PORT          = 8080;

    // ── 正则：从 JSON 字符串中提取 stlPaths 数组 ──────────────────────────
    private static final Pattern STL_PATHS_PATTERN =
        Pattern.compile("\"stlPaths\"\\s*:\\s*\\[([^\\]]*)]");
    private static final Pattern STRING_VALUE_PATTERN =
        Pattern.compile("\"([^\"\\\\]*)\"");
    private static final Pattern NUM_PATTERN =
        Pattern.compile("\"(%s)\"\\s*:\\s*([0-9.eE+\\-]+)");

    private final int port;
    private HttpServer httpServer;

    /**
     * 使用默认端口 8080 创建服务器。
     */
    public PlacementApiServer() {
        this(DEFAULT_PORT);
    }

    /**
     * 使用指定端口创建服务器。
     *
     * @param port 监听端口
     */
    public PlacementApiServer(int port) {
        this.port = port;
    }

    /**
     * 启动 HTTP 服务器，注册所有路由。
     *
     * @throws IOException 如果端口绑定失败
     */
    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/api/health",            this::handleHealth);
        httpServer.createContext("/api/workspace/default", this::handleWorkspaceDefault);
        httpServer.createContext("/api/place/single-batch",this::handlePlaceSingleBatch);
        httpServer.createContext("/api/place",             this::handlePlaceMultiBatch);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.start();
        System.out.println("[PlacementApiServer] 监听端口 " + port);
    }

    /**
     * 停止 HTTP 服务器（等待进行中的请求最多 1 秒后强制关闭）。
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(1);
            System.out.println("[PlacementApiServer] 已停止");
        }
    }

    // ── 路由处理器 ─────────────────────────────────────────────────────────

    /** GET /api/health */
    private void handleHealth(HttpExchange exchange) throws IOException {
        long t = System.currentTimeMillis();
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            log("GET", "/api/health", 405, t);
            return;
        }
        sendResponse(exchange, 200, "{\"status\":\"ok\",\"version\":\"1.0\"}");
        log("GET", "/api/health", 200, t);
    }

    /** GET /api/workspace/default */
    private void handleWorkspaceDefault(HttpExchange exchange) throws IOException {
        long t = System.currentTimeMillis();
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            log("GET", "/api/workspace/default", 405, t);
            return;
        }
        String body = String.format(
            java.util.Locale.ROOT,
            "{\"x\":%.1f,\"y\":%.1f,\"z\":%.1f,\"minGap\":%.1f}",
            DEFAULT_WORKSPACE_X, DEFAULT_WORKSPACE_Y, DEFAULT_WORKSPACE_Z, DEFAULT_MIN_GAP
        );
        sendResponse(exchange, 200, body);
        log("GET", "/api/workspace/default", 200, t);
    }

    /** POST /api/place  — 多批次摆放 */
    private void handlePlaceMultiBatch(HttpExchange exchange) throws IOException {
        long t = System.currentTimeMillis();
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            log("POST", "/api/place", 405, t);
            return;
        }
        try {
            String json = readBody(exchange);
            PlaceRequest req = parsePlaceRequest(json);

            if (req.stlPaths.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"stlPaths 不能为空\"}");
                log("POST", "/api/place", 400, t);
                return;
            }

            List<ModelInfo> models = loadModels(req.stlPaths);
            BoundingBox workspace = buildWorkspace(req);
            GreedyPlacementStrategy strategy = new GreedyPlacementStrategy();
            List<PlacedModel> result = strategy.placeMultiBatch(
                models, workspace, req.minGap, req.targetUtilization, req.maxCopiesPerType);

            String responseJson = PlacementResultSerializer.toJson(result, workspace);
            sendResponse(exchange, 200, responseJson);
            log("POST", "/api/place", 200, t);

        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, errorJson(e.getMessage()));
            log("POST", "/api/place", 400, t);
        } catch (Exception e) {
            sendResponse(exchange, 500, errorJson("内部错误：" + e.getMessage()));
            log("POST", "/api/place", 500, t);
        }
    }

    /** POST /api/place/single-batch  — 单批次摆放 */
    private void handlePlaceSingleBatch(HttpExchange exchange) throws IOException {
        long t = System.currentTimeMillis();
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            log("POST", "/api/place/single-batch", 405, t);
            return;
        }
        try {
            String json = readBody(exchange);
            PlaceRequest req = parsePlaceRequest(json);

            if (req.stlPaths.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"stlPaths 不能为空\"}");
                log("POST", "/api/place/single-batch", 400, t);
                return;
            }

            List<ModelInfo> models = loadModels(req.stlPaths);
            BoundingBox workspace = buildWorkspace(req);
            GreedyPlacementStrategy strategy = new GreedyPlacementStrategy();
            List<PlacedModel> result = strategy.place(models, workspace, req.minGap);

            String responseJson = PlacementResultSerializer.toJson(result, workspace);
            sendResponse(exchange, 200, responseJson);
            log("POST", "/api/place/single-batch", 200, t);

        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, errorJson(e.getMessage()));
            log("POST", "/api/place/single-batch", 400, t);
        } catch (Exception e) {
            sendResponse(exchange, 500, errorJson("内部错误：" + e.getMessage()));
            log("POST", "/api/place/single-batch", 500, t);
        }
    }

    // ── 工具方法 ───────────────────────────────────────────────────────────

    /**
     * 读取请求体为 UTF-8 字符串。
     */
    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 发送 JSON 响应，带 CORS 头。
     */
    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * 解析摆放请求的 JSON 字符串。缺失字段使用默认值。
     */
    private PlaceRequest parsePlaceRequest(String json) {
        PlaceRequest req = new PlaceRequest();

        // stlPaths 数组
        Matcher arrayMatcher = STL_PATHS_PATTERN.matcher(json);
        if (arrayMatcher.find()) {
            String arrayContent = arrayMatcher.group(1);
            Matcher strMatcher = STRING_VALUE_PATTERN.matcher(arrayContent);
            while (strMatcher.find()) {
                String path = strMatcher.group(1);
                if (!path.isBlank()) {
                    req.stlPaths.add(path);
                }
            }
        }

        // workspace.x / y / z
        req.workspaceX = parseDouble(json, "x",   DEFAULT_WORKSPACE_X);
        req.workspaceY = parseDouble(json, "y",   DEFAULT_WORKSPACE_Y);
        req.workspaceZ = parseDouble(json, "z",   DEFAULT_WORKSPACE_Z);

        // 根级数值字段
        req.minGap            = (float) parseDouble(json, "minGap",            DEFAULT_MIN_GAP);
        req.targetUtilization = (float) parseDouble(json, "targetUtilization", DEFAULT_UTILIZATION);
        req.maxCopiesPerType  = (int)   parseDouble(json, "maxCopiesPerType",  DEFAULT_MAX_COPIES);

        return req;
    }

    /**
     * 从 JSON 字符串中提取指定数值字段，找不到时返回 defaultValue。
     */
    private double parseDouble(String json, String key, double defaultValue) {
        Pattern p = Pattern.compile(
            "\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9.eE+\\-]+)"
        );
        Matcher m = p.matcher(json);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return defaultValue;
    }

    /**
     * 按绝对路径列表解析 STL 文件，返回 ModelInfo 列表。
     *
     * @throws IOException               文件不存在或读取失败
     * @throws IllegalArgumentException  STL 格式不合法
     */
    private List<ModelInfo> loadModels(List<String> paths) throws IOException {
        STLParser parser = new STLParser();
        List<ModelInfo> models = new ArrayList<>(paths.size());
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            Mesh mesh = parser.parse(path);
            // 取文件名（去掉目录部分）作为 fileName
            String fileName = java.nio.file.Paths.get(path).getFileName().toString();
            models.add(new ModelInfo(fileName, mesh, i));
        }
        return models;
    }

    /**
     * 根据请求参数构建工作空间 BoundingBox。
     */
    private BoundingBox buildWorkspace(PlaceRequest req) {
        return new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D((float) req.workspaceX, (float) req.workspaceY, (float) req.workspaceZ)
        );
    }

    /**
     * 构造错误响应 JSON。
     */
    private String errorJson(String message) {
        if (message == null) message = "未知错误";
        // 简单转义双引号和反斜杠
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"error\":\"" + escaped + "\"}";
    }

    /**
     * 打印请求日志。
     */
    private void log(String method, String path, int status, long startMs) {
        System.out.printf("[%s] %s -> %d  %dms%n",
            method, path, status, System.currentTimeMillis() - startMs);
    }

    // ── 内部数据类 ─────────────────────────────────────────────────────────

    /** 摆放请求参数容器（全部使用默认值初始化）。 */
    private static class PlaceRequest {
        List<String> stlPaths        = new ArrayList<>();
        double workspaceX            = DEFAULT_WORKSPACE_X;
        double workspaceY            = DEFAULT_WORKSPACE_Y;
        double workspaceZ            = DEFAULT_WORKSPACE_Z;
        float  minGap                = DEFAULT_MIN_GAP;
        float  targetUtilization     = DEFAULT_UTILIZATION;
        int    maxCopiesPerType      = DEFAULT_MAX_COPIES;
    }
}
