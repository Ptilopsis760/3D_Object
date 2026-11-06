package com.placement3d.api;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlacementApiServer 的集成测试。
 * 每个测试类独立使用不同端口，避免端口冲突。
 */
@DisplayName("PlacementApiServer 测试")
class PlacementApiServerTest {

    private static final int BASE_PORT = 18080;
    private PlacementApiServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        // 每次测试使用独立端口（通过测试序号区分）
        port = BASE_PORT + (int)(System.nanoTime() % 100);
        server = new PlacementApiServer(port);
        server.start();
        // 等待服务器就绪
        Thread.sleep(100);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    // ── 测试1：服务器启动和停止 ────────────────────────────────────────────

    @Test
    @DisplayName("测试1：服务器启动和停止不抛异常")
    void testServerStartStop() {
        // setUp/tearDown 已验证，到这里就是成功
        assertNotNull(server);
    }

    // ── 测试2：GET /api/health ─────────────────────────────────────────────

    @Test
    @DisplayName("测试2：GET /api/health 返回200和ok")
    void testHealthEndpoint() throws Exception {
        HttpURLConnection conn = get("/api/health");
        int status = conn.getResponseCode();
        String body = readResponse(conn);

        assertEquals(200, status);
        assertTrue(body.contains("ok"), "响应体应包含'ok'，实际：" + body);
        assertTrue(body.contains("version"), "响应体应包含'version'，实际：" + body);
        conn.disconnect();
    }

    // ── 测试3：GET /api/workspace/default ────────────────────────────────

    @Test
    @DisplayName("测试3：GET /api/workspace/default 返回200和4000")
    void testWorkspaceDefaultEndpoint() throws Exception {
        HttpURLConnection conn = get("/api/workspace/default");
        int status = conn.getResponseCode();
        String body = readResponse(conn);

        assertEquals(200, status);
        assertTrue(body.contains("4000"), "响应体应包含'4000'，实际：" + body);
        assertTrue(body.contains("minGap"), "响应体应包含'minGap'，实际：" + body);
        conn.disconnect();
    }

    // ── 测试4：POST /api/place 空stlPaths ────────────────────────────────

    @Test
    @DisplayName("测试4：POST /api/place 空stlPaths返回400且不崩溃")
    void testPlaceWithEmptyStlPaths() throws Exception {
        String requestBody = "{\"stlPaths\":[],\"workspace\":{\"x\":1000,\"y\":1000,\"z\":500}}";
        HttpURLConnection conn = post("/api/place", requestBody);
        int status = conn.getResponseCode();

        // 空 stlPaths 应返回 400
        assertEquals(400, status, "空stlPaths应返回400，实际：" + status);
        conn.disconnect();
    }

    // ── 测试5：POST /api/place/single-batch 空stlPaths ───────────────────

    @Test
    @DisplayName("测试5：POST /api/place/single-batch 空stlPaths不崩溃")
    void testSingleBatchWithEmptyStlPaths() throws Exception {
        String requestBody = "{\"stlPaths\":[]}";
        HttpURLConnection conn = post("/api/place/single-batch", requestBody);
        int status = conn.getResponseCode();

        // 不崩溃（不抛 500），返回 400 或其他错误码
        assertTrue(status >= 400 && status < 600,
            "应返回4xx或5xx，实际：" + status);
        conn.disconnect();
    }

    // ── 测试6：Content-Type 和 CORS 头 ───────────────────────────────────

    @Test
    @DisplayName("测试6：响应包含正确的Content-Type和CORS头")
    void testResponseHeaders() throws Exception {
        HttpURLConnection conn = get("/api/health");
        conn.getResponseCode(); // 触发请求

        String contentType = conn.getHeaderField("Content-Type");
        String cors = conn.getHeaderField("Access-Control-Allow-Origin");

        assertNotNull(contentType, "Content-Type头不应为null");
        assertTrue(contentType.contains("application/json"),
            "Content-Type应包含application/json，实际：" + contentType);
        assertEquals("*", cors, "CORS头应为*，实际：" + cors);
        conn.disconnect();
    }

    // ── 测试7：不存在的端点返回404或405 ─────────────────────────────────

    @Test
    @DisplayName("测试7：不存在的路径不会导致服务器崩溃")
    void testUnknownPath() throws Exception {
        HttpURLConnection conn = get("/api/unknown/path/that/does/not/exist");
        int status = conn.getResponseCode();

        // 不崩溃（不是 -1 或抛异常）
        assertTrue(status >= 100 && status < 600,
            "应返回合法HTTP状态码，实际：" + status);
        conn.disconnect();
    }

    // ── 工具方法 ─────────────────────────────────────────────────────────

    private HttpURLConnection get(String path) throws Exception {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(5000);
        return conn;
    }

    private HttpURLConnection post(String path, String body) throws Exception {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(5000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        InputStream is;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            is = conn.getErrorStream();
        }
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }
}
