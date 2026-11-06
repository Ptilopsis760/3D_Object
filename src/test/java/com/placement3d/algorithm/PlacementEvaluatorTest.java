package com.placement3d.algorithm;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlacementEvaluator的单元测试。
 *
 * <p>测试覆盖：
 * <ul>
 *   <li>空场景评分</li>
 *   <li>单个模型评分</li>
 *   <li>多个模型评分</li>
 *   <li>边界情况（极端位置）</li>
 *   <li>权重调整影响</li>
 * </ul>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
@DisplayName("摆放评分函数测试")
class PlacementEvaluatorTest {

    private BoundingBox workspace;
    private PlacementEvaluator evaluator;
    private ModelInfo testModel;

    @BeforeEach
    void setUp() {
        // 创建工作空间：4000×2500×1500mm
        workspace = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(4000, 2500, 1500)
        );

        // 创建评分器（默认权重）
        evaluator = new PlacementEvaluator(workspace);

        // 创建测试模型（100×100×100mm立方体）
        List<Triangle> triangles = createCubeTriangles(100);
        Mesh mesh = new Mesh(triangles, "test_cube.stl");
        testModel = new ModelInfo("test_cube.stl", mesh, 0);
    }

    @Test
    @DisplayName("测试1：空场景评分")
    void testEmptySceneEvaluation() {
        // 空场景 - 没有已摆放的模型
        List<PlacedModel> placedModels = new ArrayList<>();

        // 候选位置在工作空间底部中心
        Vector3D candidatePos = new Vector3D(2000, 1250, 50);
        PlacementCandidate candidate = new PlacementCandidate(testModel, candidatePos);

        // 评估
        double score = evaluator.evaluate(candidate, placedModels);

        // 验证评分在有效范围内
        assertTrue(score >= 0.0 && score <= 1.0, "评分应在[0.0, 1.0]范围内");

        // 空场景时：
        // - 利用率低（接近0）
        // - 紧凑度应为1.0（无已摆放模型）
        // - 稳定性高（Z=50很低）
        System.out.printf("空场景评分: %.4f%n", score);

        // 紧凑度权重0.3 + 稳定性权重0.2应该占主导
        // 稳定性 = 1 - 50/1500 ≈ 0.967
        // 紧凑度 = 1.0（空场景）
        // 预期评分 ≈ 0.3 * 1.0 + 0.2 * 0.967 ≈ 0.49
        assertTrue(score > 0.4, "空场景底部位置评分应较高");
    }

    @Test
    @DisplayName("测试2：单个模型评分")
    void testSingleModelEvaluation() {
        // 已摆放一个模型在(1000, 1000, 100)
        List<PlacedModel> placedModels = new ArrayList<>();
        PlacedModel placed = new PlacedModel(
            testModel,
            Matrix3x3.IDENTITY,
            new Vector3D(1000, 1000, 100)
        );
        placedModels.add(placed);

        // 候选位置在已摆放模型附近（紧凑）
        Vector3D candidatePos = new Vector3D(1200, 1000, 100);
        PlacementCandidate candidate = new PlacementCandidate(testModel, candidatePos);

        // 评估
        double score = evaluator.evaluate(candidate, placedModels);

        // 验证评分在有效范围内
        assertTrue(score >= 0.0 && score <= 1.0, "评分应在[0.0, 1.0]范围内");

        System.out.printf("单模型场景评分: %.4f%n", score);

        // 候选位置靠近已摆放模型，紧凑度应该高
        assertTrue(score > 0.3, "靠近已摆放模型的位置评分应较高");
    }

    @Test
    @DisplayName("测试3：多个模型评分")
    void testMultipleModelsEvaluation() {
        // 已摆放多个模型
        List<PlacedModel> placedModels = new ArrayList<>();

        // 模型1：(1000, 1000, 100)
        placedModels.add(new PlacedModel(
            testModel,
            Matrix3x3.IDENTITY,
            new Vector3D(1000, 1000, 100)
        ));

        // 模型2：(1500, 1000, 100)
        placedModels.add(new PlacedModel(
            testModel,
            Matrix3x3.IDENTITY,
            new Vector3D(1500, 1000, 100)
        ));

        // 模型3：(1000, 1500, 100)
        placedModels.add(new PlacedModel(
            testModel,
            Matrix3x3.IDENTITY,
            new Vector3D(1000, 1500, 100)
        ));

        // 候选位置在质心附近（紧凑）
        Vector3D candidatePos = new Vector3D(1250, 1250, 100);
        PlacementCandidate candidate = new PlacementCandidate(testModel, candidatePos);

        // 评估
        double score = evaluator.evaluate(candidate, placedModels);

        // 验证评分在有效范围内
        assertTrue(score >= 0.0 && score <= 1.0, "评分应在[0.0, 1.0]范围内");

        System.out.printf("多模型场景评分: %.4f%n", score);

        // 质心位置应该有高评分
        assertTrue(score > 0.3, "靠近质心的位置评分应较高");
    }

    @Test
    @DisplayName("测试4：边界情况 - 底部位置")
    void testBottomPositionEvaluation() {
        List<PlacedModel> placedModels = new ArrayList<>();

        // 候选位置在工作空间底部（Z=0）
        Vector3D candidatePos = new Vector3D(2000, 1250, 0);
        PlacementCandidate candidate = new PlacementCandidate(testModel, candidatePos);

        // 评估
        double score = evaluator.evaluate(candidate, placedModels);

        System.out.printf("底部位置评分: %.4f%n", score);

        // 底部位置稳定性应该最高
        assertTrue(score > 0.0, "底部位置应有正评分");
    }

    @Test
    @DisplayName("测试5：边界情况 - 顶部位置")
    void testTopPositionEvaluation() {
        List<PlacedModel> placedModels = new ArrayList<>();

        // 候选位置在工作空间顶部（Z=1500）
        Vector3D candidatePos = new Vector3D(2000, 1250, 1500);
        PlacementCandidate candidate = new PlacementCandidate(testModel, candidatePos);

        // 评估
        double score = evaluator.evaluate(candidate, placedModels);

        System.out.printf("顶部位置评分: %.4f%n", score);

        // 顶部位置稳定性应该最低
        assertTrue(score >= 0.0 && score <= 1.0, "顶部位置评分应在有效范围内");
    }

    @Test
    @DisplayName("测试6：边界情况 - 远离质心")
    void testFarFromCentroidEvaluation() {
        // 已摆放一个模型在(1000, 1000, 100)
        List<PlacedModel> placedModels = new ArrayList<>();
        placedModels.add(new PlacedModel(
            testModel,
            Matrix3x3.IDENTITY,
            new Vector3D(1000, 1000, 100)
        ));

        // 候选位置远离质心
        Vector3D candidatePos = new Vector3D(3900, 2400, 100);
        PlacementCandidate candidate = new PlacementCandidate(testModel, candidatePos);

        // 评估
        double score = evaluator.evaluate(candidate, placedModels);

        System.out.printf("远离质心位置评分: %.4f%n", score);

        // 远离质心的位置紧凑度应该低
        assertTrue(score >= 0.0 && score < 1.0, "远离质心的位置评分应较低");
    }

    @Test
    @DisplayName("测试7：权重调整 - 高利用率权重")
    void testHighUtilizationWeight() {
        // 创建高利用率权重的评分器
        PlacementEvaluator highUtilEvaluator = new PlacementEvaluator(
            workspace,
            0.8f,  // 利用率权重
            0.1f,  // 紧凑度权重
            0.1f   // 稳定性权重
        );

        // 场景：已摆放多个模型（高利用率）
        List<PlacedModel> placedModels = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            placedModels.add(new PlacedModel(
                testModel,
                Matrix3x3.IDENTITY,
                new Vector3D(500 + i * 200, 500, 100)
            ));
        }

        Vector3D candidatePos = new Vector3D(2000, 1000, 100);
        PlacementCandidate candidate = new PlacementCandidate(testModel, candidatePos);

        // 评估
        double score = highUtilEvaluator.evaluate(candidate, placedModels);

        System.out.printf("高利用率权重评分: %.4f%n", score);

        // 由于已摆放多个模型，利用率会影响评分
        assertTrue(score > 0.0, "高利用率场景应有正评分");
    }

    @Test
    @DisplayName("测试8：权重调整 - 高紧凑度权重")
    void testHighCompactnessWeight() {
        // 创建高紧凑度权重的评分器
        PlacementEvaluator highCompactEvaluator = new PlacementEvaluator(
            workspace,
            0.1f,  // 利用率权重
            0.8f,  // 紧凑度权重
            0.1f   // 稳定性权重
        );

        // 已摆放一个模型
        List<PlacedModel> placedModels = new ArrayList<>();
        placedModels.add(new PlacedModel(
            testModel,
            Matrix3x3.IDENTITY,
            new Vector3D(1000, 1000, 100)
        ));

        // 候选位置1：靠近质心
        Vector3D nearPos = new Vector3D(1100, 1000, 100);
        PlacementCandidate nearCandidate = new PlacementCandidate(testModel, nearPos);
        double nearScore = highCompactEvaluator.evaluate(nearCandidate, placedModels);

        // 候选位置2：远离质心
        Vector3D farPos = new Vector3D(3000, 2000, 100);
        PlacementCandidate farCandidate = new PlacementCandidate(testModel, farPos);
        double farScore = highCompactEvaluator.evaluate(farCandidate, placedModels);

        System.out.printf("靠近质心评分: %.4f, 远离质心评分: %.4f%n", nearScore, farScore);

        // 高紧凑度权重下，靠近质心的位置评分应明显高于远离的位置
        assertTrue(nearScore > farScore, "高紧凑度权重下，靠近质心的位置评分应更高");
    }

    @Test
    @DisplayName("测试9：权重调整 - 高稳定性权重")
    void testHighStabilityWeight() {
        // 创建高稳定性权重的评分器
        PlacementEvaluator highStableEvaluator = new PlacementEvaluator(
            workspace,
            0.1f,  // 利用率权重
            0.1f,  // 紧凑度权重
            0.8f   // 稳定性权重
        );

        List<PlacedModel> placedModels = new ArrayList<>();

        // 候选位置1：低层（高稳定性）
        Vector3D lowPos = new Vector3D(2000, 1250, 100);
        PlacementCandidate lowCandidate = new PlacementCandidate(testModel, lowPos);
        double lowScore = highStableEvaluator.evaluate(lowCandidate, placedModels);

        // 候选位置2：高层（低稳定性）
        Vector3D highPos = new Vector3D(2000, 1250, 1400);
        PlacementCandidate highCandidate = new PlacementCandidate(testModel, highPos);
        double highScore = highStableEvaluator.evaluate(highCandidate, placedModels);

        System.out.printf("低层评分: %.4f, 高层评分: %.4f%n", lowScore, highScore);

        // 高稳定性权重下，低层位置评分应明显高于高层位置
        assertTrue(lowScore > highScore, "高稳定性权重下，低层位置评分应更高");
    }

    @Test
    @DisplayName("测试10：评分一致性")
    void testScoreConsistency() {
        List<PlacedModel> placedModels = new ArrayList<>();
        placedModels.add(new PlacedModel(
            testModel,
            Matrix3x3.IDENTITY,
            new Vector3D(1000, 1000, 100)
        ));

        Vector3D candidatePos = new Vector3D(1500, 1000, 100);
        PlacementCandidate candidate = new PlacementCandidate(testModel, candidatePos);

        // 多次评估同一候选，评分应一致
        double score1 = evaluator.evaluate(candidate, placedModels);
        double score2 = evaluator.evaluate(candidate, placedModels);
        double score3 = evaluator.evaluate(candidate, placedModels);

        assertEquals(score1, score2, 1e-6, "重复评估应得到相同结果");
        assertEquals(score2, score3, 1e-6, "重复评估应得到相同结果");
    }

    @Test
    @DisplayName("测试11：非法权重 - 负值")
    void testInvalidWeightsNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PlacementEvaluator(workspace, -0.1f, 0.6f, 0.5f);
        }, "负权重应抛出异常");
    }

    @Test
    @DisplayName("测试12：非法权重 - 和不为1")
    void testInvalidWeightsSum() {
        assertThrows(IllegalArgumentException.class, () -> {
            new PlacementEvaluator(workspace, 0.3f, 0.3f, 0.3f);
        }, "权重和不为1应抛出异常");
    }

    @Test
    @DisplayName("测试13：空指针检查")
    void testNullPointerChecks() {
        assertThrows(NullPointerException.class, () -> {
            new PlacementEvaluator(null);
        }, "null workspace应抛出异常");

        List<PlacedModel> placedModels = new ArrayList<>();
        PlacementCandidate candidate = new PlacementCandidate(
            testModel,
            new Vector3D(1000, 1000, 100)
        );

        assertThrows(NullPointerException.class, () -> {
            evaluator.evaluate(null, placedModels);
        }, "null candidate应抛出异常");

        assertThrows(NullPointerException.class, () -> {
            evaluator.evaluate(candidate, null);
        }, "null placedModels应抛出异常");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建立方体三角形（用于测试）。
     *
     * @param size 立方体边长
     * @return 三角形列表
     */
    private List<Triangle> createCubeTriangles(float size) {
        List<Triangle> triangles = new ArrayList<>();
        float half = size / 2;

        // 6个面，每面2个三角形，共12个三角形
        // 前面 (Y = half)
        triangles.add(new Triangle(
            new Vector3D(-half, half, -half),
            new Vector3D(half, half, -half),
            new Vector3D(half, half, half)
        ));
        triangles.add(new Triangle(
            new Vector3D(-half, half, -half),
            new Vector3D(half, half, half),
            new Vector3D(-half, half, half)
        ));

        // 后面 (Y = -half)
        triangles.add(new Triangle(
            new Vector3D(-half, -half, -half),
            new Vector3D(half, -half, half),
            new Vector3D(half, -half, -half)
        ));
        triangles.add(new Triangle(
            new Vector3D(-half, -half, -half),
            new Vector3D(-half, -half, half),
            new Vector3D(half, -half, half)
        ));

        // 右面 (X = half)
        triangles.add(new Triangle(
            new Vector3D(half, -half, -half),
            new Vector3D(half, half, half),
            new Vector3D(half, half, -half)
        ));
        triangles.add(new Triangle(
            new Vector3D(half, -half, -half),
            new Vector3D(half, -half, half),
            new Vector3D(half, half, half)
        ));

        // 左面 (X = -half)
        triangles.add(new Triangle(
            new Vector3D(-half, -half, -half),
            new Vector3D(-half, half, -half),
            new Vector3D(-half, half, half)
        ));
        triangles.add(new Triangle(
            new Vector3D(-half, -half, -half),
            new Vector3D(-half, half, half),
            new Vector3D(-half, -half, half)
        ));

        // 顶面 (Z = half)
        triangles.add(new Triangle(
            new Vector3D(-half, -half, half),
            new Vector3D(-half, half, half),
            new Vector3D(half, half, half)
        ));
        triangles.add(new Triangle(
            new Vector3D(-half, -half, half),
            new Vector3D(half, half, half),
            new Vector3D(half, -half, half)
        ));

        // 底面 (Z = -half)
        triangles.add(new Triangle(
            new Vector3D(-half, -half, -half),
            new Vector3D(half, -half, -half),
            new Vector3D(half, half, -half)
        ));
        triangles.add(new Triangle(
            new Vector3D(-half, -half, -half),
            new Vector3D(half, half, -half),
            new Vector3D(-half, half, -half)
        ));

        return triangles;
    }
}
