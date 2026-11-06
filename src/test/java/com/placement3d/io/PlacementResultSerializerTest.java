package com.placement3d.io;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.Matrix3x3;
import com.placement3d.model.Mesh;
import com.placement3d.model.ModelInfo;
import com.placement3d.model.PlacedModel;
import com.placement3d.model.Triangle;
import com.placement3d.model.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlacementResultSerializer 单元测试。
 *
 * <p>测试覆盖：字段完整性、matrix4x4 平移列计算、
 * aabbUtilization 计算、workspace 字段、空列表合法性。</p>
 */
@DisplayName("PlacementResultSerializer 测试")
class PlacementResultSerializerTest {

    private BoundingBox workspace;
    private ModelInfo cubeModel;

    @BeforeEach
    void setUp() {
        workspace = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(1000, 1000, 1000)
        );
        cubeModel = createCubeModel(100.0f, "cube.stl", 0);
    }

    /**
     * 构造 12 三角形完整封闭立方体模型。
     * 立方体一角在原点，沿 +X/+Y/+Z 延伸 size。
     */
    private ModelInfo createCubeModel(float size, String fileName, int modelId) {
        Vector3D v000 = new Vector3D(0, 0, 0);
        Vector3D v100 = new Vector3D(size, 0, 0);
        Vector3D v110 = new Vector3D(size, size, 0);
        Vector3D v010 = new Vector3D(0, size, 0);
        Vector3D v001 = new Vector3D(0, 0, size);
        Vector3D v101 = new Vector3D(size, 0, size);
        Vector3D v111 = new Vector3D(size, size, size);
        Vector3D v011 = new Vector3D(0, size, size);

        List<Triangle> triangles = new ArrayList<>();
        // 底面
        triangles.add(new Triangle(v000, v100, v110));
        triangles.add(new Triangle(v000, v110, v010));
        // 顶面
        triangles.add(new Triangle(v001, v111, v101));
        triangles.add(new Triangle(v001, v011, v111));
        // 前面
        triangles.add(new Triangle(v000, v101, v100));
        triangles.add(new Triangle(v000, v001, v101));
        // 后面
        triangles.add(new Triangle(v010, v110, v111));
        triangles.add(new Triangle(v010, v111, v011));
        // 左面
        triangles.add(new Triangle(v000, v010, v011));
        triangles.add(new Triangle(v000, v011, v001));
        // 右面
        triangles.add(new Triangle(v100, v111, v110));
        triangles.add(new Triangle(v100, v101, v111));

        Mesh mesh = new Mesh(triangles, fileName);
        return new ModelInfo(fileName, mesh, modelId);
    }

    @Test
    @DisplayName("单个模型序列化包含所有必要字段")
    void testSingleModelContainsAllFields() {
        Vector3D pos = new Vector3D(200, 200, 200);
        PlacedModel placed = PlacedModel.create(cubeModel, Matrix3x3.IDENTITY, pos);
        List<PlacedModel> models = Collections.singletonList(placed);

        String json = PlacementResultSerializer.toJson(models, workspace);

        assertNotNull(json);
        // 检查所有必要字段都存在
        assertTrue(json.contains("\"fileName\""), "缺少 fileName 字段");
        assertTrue(json.contains("\"modelId\""), "缺少 modelId 字段");
        assertTrue(json.contains("\"position\""), "缺少 position 字段");
        assertTrue(json.contains("\"pivot\""), "缺少 pivot 字段");
        assertTrue(json.contains("\"euler\""), "缺少 euler 字段");
        assertTrue(json.contains("\"matrix4x4\""), "缺少 matrix4x4 字段");
        assertTrue(json.contains("\"aabb\""), "缺少 aabb 字段");
        assertTrue(json.contains("\"volume\""), "缺少 volume 字段");
    }

    @Test
    @DisplayName("matrix4x4 平移列计算正确（identity 旋转）")
    void testMatrix4x4TranslationColumnWithIdentityRotation() {
        // identity 旋转：t = position - I·pivot = position - pivot
        Vector3D pivot = cubeModel.getCenterOfMass();
        Vector3D pos = new Vector3D(500, 500, 500);
        PlacedModel placed = PlacedModel.create(cubeModel, Matrix3x3.IDENTITY, pos);

        String json = PlacementResultSerializer.toJson(placed);

        // 解析 matrix4x4 的第一行平移列 tx
        // 期望 tx = pos.x - pivot.x
        double expectedTx = pos.x - pivot.x;
        double expectedTy = pos.y - pivot.y;
        double expectedTz = pos.z - pivot.z;

        // JSON 中 matrix4x4 第一行格式: [r00, r01, r02, tx]
        // 提取第一行最后一个数值
        String txStr = extractMatrixTranslation(json, 0);
        String tyStr = extractMatrixTranslation(json, 1);
        String tzStr = extractMatrixTranslation(json, 2);

        double tx = Double.parseDouble(txStr);
        double ty = Double.parseDouble(tyStr);
        double tz = Double.parseDouble(tzStr);

        assertEquals(expectedTx, tx, 0.01, "tx 计算错误");
        assertEquals(expectedTy, ty, 0.01, "ty 计算错误");
        assertEquals(expectedTz, tz, 0.01, "tz 计算错误");
    }

    @Test
    @DisplayName("matrix4x4 旋转部分与 R 一致（90° 绕 Z）")
    void testMatrix4x4RotationPartConsistent() {
        Matrix3x3 rot = Matrix3x3.rotationZ(90);
        Vector3D pos = new Vector3D(300, 300, 300);
        PlacedModel placed = PlacedModel.create(cubeModel, rot, pos);

        String json = PlacementResultSerializer.toJson(placed);

        // 验证 4×4 矩阵左上 3×3 与 R.getData() 一致
        double[][] r = rot.getData();
        String r00 = extractMatrixElement(json, 0, 0);
        String r01 = extractMatrixElement(json, 0, 1);
        String r22 = extractMatrixElement(json, 2, 2);

        assertEquals(r[0][0], Double.parseDouble(r00), 1e-4, "r00 不匹配");
        assertEquals(r[0][1], Double.parseDouble(r01), 1e-4, "r01 不匹配");
        assertEquals(r[2][2], Double.parseDouble(r22), 1e-4, "r22 不匹配");
    }

    @Test
    @DisplayName("aabbUtilization 计算正确")
    void testAabbUtilizationCalculation() {
        // 摆 1 个 100³ 的立方体在 1000³ 空间里
        // 利用率 = 100³×1000 / (1000³×1000)... 实际是 AABB 体积 / 工作空间体积
        // AABB 体积 = 100×100×100 = 1,000,000 mm³
        // 工作空间体积 = 1000×1000×1000 = 1,000,000,000 mm³
        // 利用率 = 1,000,000 / 1,000,000,000 = 0.001
        Vector3D pos = new Vector3D(200, 200, 200);
        PlacedModel placed = PlacedModel.create(cubeModel, Matrix3x3.IDENTITY, pos);
        List<PlacedModel> models = Collections.singletonList(placed);

        String json = PlacementResultSerializer.toJson(models, workspace);

        // 提取 aabbUtilization 值
        double utilization = extractUtilization(json);
        float expectedAabbVolume = placed.getTransformedAABB().getVolume();
        float expectedUtil = expectedAabbVolume / workspace.getVolume();

        assertEquals(expectedUtil, utilization, 1e-5, "aabbUtilization 计算错误");
        assertTrue(utilization > 0, "利用率应大于 0");
    }

    @Test
    @DisplayName("workspace 字段存在且正确")
    void testWorkspaceField() {
        PlacedModel placed = PlacedModel.create(
            cubeModel, Matrix3x3.IDENTITY, new Vector3D(200, 200, 200));
        List<PlacedModel> models = Collections.singletonList(placed);

        String json = PlacementResultSerializer.toJson(models, workspace);

        assertTrue(json.contains("\"workspace\""), "缺少 workspace 字段");
        // workspace min 应为 [0, 0, 0]
        assertTrue(json.contains("\"min\""), "缺少 min 字段");
        assertTrue(json.contains("\"max\""), "缺少 max 字段");
    }

    @Test
    @DisplayName("空列表返回合法 JSON")
    void testEmptyListReturnsValidJson() {
        List<PlacedModel> emptyModels = Collections.emptyList();

        String json = PlacementResultSerializer.toJson(emptyModels, workspace);

        assertNotNull(json);
        assertTrue(json.contains("\"totalPlaced\": 0"), "空列表 totalPlaced 应为 0");
        assertTrue(json.contains("\"models\": []"), "空列表 models 应为空数组");
        assertTrue(json.contains("\"aabbUtilization\""), "空列表也应包含利用率字段");
    }

    @Test
    @DisplayName("多模型序列化结果合法")
    void testMultipleModelsSerialization() {
        ModelInfo model2 = createCubeModel(50.0f, "small.stl", 1);
        List<PlacedModel> models = new ArrayList<>();
        models.add(PlacedModel.create(cubeModel, Matrix3x3.IDENTITY, new Vector3D(200, 200, 200)));
        models.add(PlacedModel.create(model2, Matrix3x3.IDENTITY, new Vector3D(500, 200, 200)));
        models.add(PlacedModel.create(model2, Matrix3x3.rotationZ(90), new Vector3D(200, 400, 200)));

        String json = PlacementResultSerializer.toJson(models, workspace);

        assertNotNull(json);
        assertTrue(json.contains("\"totalPlaced\": 3"), "totalPlaced 应为 3");
        // 验证 JSON 基本结构合法：开头和结尾
        assertTrue(json.trim().startsWith("{"), "JSON 应以 { 开头");
        assertTrue(json.trim().endsWith("}"), "JSON 应以 } 结尾");
    }

    @Test
    @DisplayName("fileName 中的特殊字符被正确转义")
    void testFileNameEscaping() {
        // 创建带特殊字符的文件名（路径分隔符）
        ModelInfo weirdModel = createCubeModel(50.0f, "path\\to\"file.stl", 2);
        PlacedModel placed = PlacedModel.create(
            weirdModel, Matrix3x3.IDENTITY, new Vector3D(200, 200, 200));

        String json = PlacementResultSerializer.toJson(placed);

        // 转义后的引号应为 \"
        assertTrue(json.contains("\\\""), "文件名中的引号应被转义");
    }

    @Test
    @DisplayName("null 参数抛出 NullPointerException")
    void testNullParametersThrowNpe() {
        assertThrows(NullPointerException.class, () ->
            PlacementResultSerializer.toJson(null, workspace));
        assertThrows(NullPointerException.class, () ->
            PlacementResultSerializer.toJson(Collections.emptyList(), null));
        assertThrows(NullPointerException.class, () ->
            PlacementResultSerializer.toJson((PlacedModel) null));
    }

    // ───────────────────────── 辅助解析方法 ─────────────────────────

    /**
     * 从 JSON 中提取 aabbUtilization 数值。
     */
    private double extractUtilization(String json) {
        int idx = json.indexOf("\"aabbUtilization\":");
        String after = json.substring(idx + "\"aabbUtilization\":".length()).trim();
        // 读取到逗号或换行
        int end = after.indexOf(',');
        if (end < 0) end = after.indexOf('\n');
        return Double.parseDouble(after.substring(0, end).trim());
    }

    /**
     * 从单模型 JSON 中提取 matrix4x4 指定行的平移列（第 4 个元素）。
     */
    private String extractMatrixTranslation(String json, int row) {
        // matrix4x4 是 [row][col]，平移列 col=3
        return extractMatrixElement(json, row, 3);
    }

    /**
     * 从单模型 JSON 中提取 matrix4x4[row][col] 的字符串值。
     */
    private String extractMatrixElement(String json, int row, int col) {
        // 找到 "matrix4x4": [
        int matrixIdx = json.indexOf("\"matrix4x4\":");
        String after = json.substring(matrixIdx);
        // 跳过 "matrix4x4": [ 到第一个 [
        int firstBracket = after.indexOf('[');
        after = after.substring(firstBracket + 1);

        // 跳过 row 行
        for (int r = 0; r < row; r++) {
            int nextBracket = after.indexOf('[');
            after = after.substring(nextBracket + 1);
        }
        // 现在 after 的开头是 r00, r01, r02, tx]
        int nextBracket = after.indexOf('[');
        if (nextBracket >= 0) {
            after = after.substring(nextBracket + 1);
        }

        // 分割元素
        int closeBracket = after.indexOf(']');
        String rowContent = after.substring(0, closeBracket);
        String[] elements = rowContent.split(",");
        return elements[col].trim();
    }
}
