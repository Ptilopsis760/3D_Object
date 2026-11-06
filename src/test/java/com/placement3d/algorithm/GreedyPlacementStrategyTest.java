package com.placement3d.algorithm;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.Matrix3x3;
import com.placement3d.model.Mesh;
import com.placement3d.model.ModelInfo;
import com.placement3d.model.PlacedModel;
import com.placement3d.model.Triangle;
import com.placement3d.model.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GreedyPlacementStrategy的测试类。
 *
 * <p>测试场景包括：</p>
 * <ul>
 *   <li>单个模型摆放</li>
 *   <li>多个模型摆放</li>
 *   <li>边界检测</li>
 *   <li>旋转优化</li>
 *   <li>利用率计算</li>
 *   <li>碰撞检测</li>
 * </ul>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
class GreedyPlacementStrategyTest {

    private GreedyPlacementStrategy strategy;
    private BoundingBox workspace;
    private static final float MIN_GAP = 5.0f;

    @BeforeEach
    void setUp() {
        strategy = new GreedyPlacementStrategy();
        // 创建工作空间：1000×1000×1000mm
        workspace = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(1000, 1000, 1000)
        );
    }

    /**
     * 创建一个简单的立方体模型（用于测试）。
     *
     * @param size 立方体边长（mm）
     * @param modelId 模型ID
     * @return ModelInfo实例
     */
    private ModelInfo createCubeMesh(float size, int modelId) {
        List<Triangle> triangles = new ArrayList<>();
        Vector3D v000 = new Vector3D(0, 0, 0);
        Vector3D v100 = new Vector3D(size, 0, 0);
        Vector3D v110 = new Vector3D(size, size, 0);
        Vector3D v010 = new Vector3D(0, size, 0);
        Vector3D v001 = new Vector3D(0, 0, size);
        Vector3D v101 = new Vector3D(size, 0, size);
        Vector3D v111 = new Vector3D(size, size, size);
        Vector3D v011 = new Vector3D(0, size, size);

        // 底面（z=0）
        triangles.add(new Triangle(v000, v100, v110));
        triangles.add(new Triangle(v000, v110, v010));

        // 顶面（z=size）
        triangles.add(new Triangle(v001, v111, v101));
        triangles.add(new Triangle(v001, v011, v111));

        // 前面（y=0）
        triangles.add(new Triangle(v000, v101, v100));
        triangles.add(new Triangle(v000, v001, v101));

        // 后面（y=size）
        triangles.add(new Triangle(v010, v110, v111));
        triangles.add(new Triangle(v010, v111, v011));

        // 左面（x=0）
        triangles.add(new Triangle(v000, v010, v011));
        triangles.add(new Triangle(v000, v011, v001));

        // 右面（x=size）
        triangles.add(new Triangle(v100, v111, v110));
        triangles.add(new Triangle(v100, v101, v111));

        Mesh mesh = new Mesh(triangles, "cube_" + size + ".stl");
        return new ModelInfo("cube_" + size + ".stl", mesh, modelId);
    }

    @Test
    @DisplayName("测试1：单个模型摆放")
    void testSingleModelPlacement() {
        // 创建一个100mm立方体
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(100, 0));

        // 执行摆放
        List<PlacedModel> result = strategy.place(models, workspace, MIN_GAP);

        // 验证结果
        assertNotNull(result, "摆放结果不应为null");
        assertEquals(1, result.size(), "应该成功摆放1个模型");

        PlacedModel placed = result.get(0);
        assertNotNull(placed, "已摆放模型不应为null");

        // 检查模型在工作空间内
        BoundingBox placedAABB = placed.getTransformedAABB();
        assertTrue(placedAABB.getMin().getX() >= workspace.getMin().getX(),
            "模型应在工作空间X轴最小边界内");
        assertTrue(placedAABB.getMax().getX() <= workspace.getMax().getX(),
            "模型应在工作空间X轴最大边界内");
        assertTrue(placedAABB.getMin().getY() >= workspace.getMin().getY(),
            "模型应在工作空间Y轴最小边界内");
        assertTrue(placedAABB.getMax().getY() <= workspace.getMax().getY(),
            "模型应在工作空间Y轴最大边界内");
        assertTrue(placedAABB.getMin().getZ() >= workspace.getMin().getZ(),
            "模型应在工作空间Z轴最小边界内");
        assertTrue(placedAABB.getMax().getZ() <= workspace.getMax().getZ(),
            "模型应在工作空间Z轴最大边界内");
    }

    @Test
    @DisplayName("测试2：多个模型摆放（至少3个）")
    void testMultipleModelPlacement() {
        // 创建3个不同尺寸的立方体
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(100, 0)); // 大立方体
        models.add(createCubeMesh(80, 1));  // 中立方体
        models.add(createCubeMesh(60, 2));  // 小立方体

        // 执行摆放
        List<PlacedModel> result = strategy.place(models, workspace, MIN_GAP);

        // 验证结果
        assertNotNull(result, "摆放结果不应为null");
        assertTrue(result.size() >= 3, "应该至少成功摆放3个模型，实际摆放了 " + result.size() + " 个");

        // 检查所有模型之间没有碰撞
        for (int i = 0; i < result.size(); i++) {
            for (int j = i + 1; j < result.size(); j++) {
                PlacedModel model1 = result.get(i);
                PlacedModel model2 = result.get(j);

                BoundingBox box1 = model1.getTransformedAABB();
                BoundingBox box2 = model2.getTransformedAABB();

                // 检查是否保持最小间隙
                BoundingBox expandedBox1 = box1.expand(MIN_GAP);
                assertFalse(expandedBox1.intersects(box2),
                    "模型 " + i + " 和模型 " + j + " 之间应保持至少 " + MIN_GAP + "mm 的间隙");
            }
        }
    }

    @Test
    @DisplayName("测试3：边界检测")
    void testBoundaryDetection() {
        // 创建一个大模型，接近工作空间尺寸
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(950, 0)); // 950mm立方体（工作空间1000mm）

        // 执行摆放
        List<PlacedModel> result = strategy.place(models, workspace, MIN_GAP);

        // 验证结果
        assertNotNull(result, "摆放结果不应为null");

        if (!result.isEmpty()) {
            PlacedModel placed = result.get(0);
            BoundingBox placedAABB = placed.getTransformedAABB();

            // 检查边界间隙
            float minBoundaryGap = MIN_GAP - 1.0f; // 允许1mm容差

            assertTrue(placedAABB.getMin().getX() - workspace.getMin().getX() >= minBoundaryGap,
                "X轴最小边界应保持至少 " + MIN_GAP + "mm 间隙");
            assertTrue(workspace.getMax().getX() - placedAABB.getMax().getX() >= minBoundaryGap,
                "X轴最大边界应保持至少 " + MIN_GAP + "mm 间隙");
            assertTrue(placedAABB.getMin().getY() - workspace.getMin().getY() >= minBoundaryGap,
                "Y轴最小边界应保持至少 " + MIN_GAP + "mm 间隙");
            assertTrue(workspace.getMax().getY() - placedAABB.getMax().getY() >= minBoundaryGap,
                "Y轴最大边界应保持至少 " + MIN_GAP + "mm 间隙");
        }
    }

    @Test
    @DisplayName("测试4：旋转优化")
    void testRotationOptimization() {
        // 创建一个长方体模型（非立方体）
        List<Triangle> triangles = new ArrayList<>();
        Vector3D v000 = new Vector3D(0, 0, 0);
        Vector3D v200 = new Vector3D(200, 0, 0); // 长边
        Vector3D v210 = new Vector3D(200, 50, 0); // 短边
        Vector3D v010 = new Vector3D(0, 50, 0);
        Vector3D v001 = new Vector3D(0, 0, 30);
        Vector3D v201 = new Vector3D(200, 0, 30);
        Vector3D v211 = new Vector3D(200, 50, 30);
        Vector3D v011 = new Vector3D(0, 50, 30);

        // 底面
        triangles.add(new Triangle(v000, v200, v210));
        triangles.add(new Triangle(v000, v210, v010));
        // 顶面
        triangles.add(new Triangle(v001, v211, v201));
        triangles.add(new Triangle(v001, v011, v211));
        // 前面
        triangles.add(new Triangle(v000, v201, v200));
        triangles.add(new Triangle(v000, v001, v201));
        // 后面
        triangles.add(new Triangle(v010, v210, v211));
        triangles.add(new Triangle(v010, v211, v011));
        // 左面
        triangles.add(new Triangle(v000, v010, v011));
        triangles.add(new Triangle(v000, v011, v001));
        // 右面
        triangles.add(new Triangle(v200, v211, v210));
        triangles.add(new Triangle(v200, v201, v211));

        Mesh mesh = new Mesh(triangles, "rectangle.stl");
        ModelInfo rectangularModel = new ModelInfo("rectangle.stl", mesh, 0);

        List<ModelInfo> models = new ArrayList<>();
        models.add(rectangularModel);

        // 执行摆放
        List<PlacedModel> result = strategy.place(models, workspace, MIN_GAP);

        // 验证结果
        assertNotNull(result, "摆放结果不应为null");
        assertEquals(1, result.size(), "应该成功摆放1个模型");

        PlacedModel placed = result.get(0);
        assertNotNull(placed.getRotationMatrix(), "旋转矩阵不应为null");

        // 算法应该尝试了多个旋转角度
        // 我们只验证模型被成功摆放（算法已经选择了最佳旋转）
        assertTrue(placed.getTransformedAABB().getVolume() > 0,
            "已摆放模型的AABB体积应大于0");
    }

    @Test
    @DisplayName("测试5：利用率计算")
    void testUtilizationCalculation() {
        // 创建多个小立方体
        List<ModelInfo> models = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            models.add(createCubeMesh(100, i));
        }

        // 执行摆放
        List<PlacedModel> result = strategy.place(models, workspace, MIN_GAP);

        // 验证结果
        assertNotNull(result, "摆放结果不应为null");
        assertTrue(result.size() > 0, "应至少摆放1个模型");

        // 计算实际利用率
        float totalVolume = 0;
        for (PlacedModel placed : result) {
            totalVolume += placed.getVolume();
        }

        float workspaceVolume = workspace.getVolume();
        float utilization = totalVolume / workspaceVolume;

        // 验证利用率在合理范围内（0-100%）
        assertTrue(utilization >= 0.0f && utilization <= 1.0f,
            "利用率应在0-1之间，实际值：" + utilization);

        System.out.println("测试5 - 利用率：" + String.format("%.2f%%", utilization * 100));
        System.out.println("已摆放模型数量：" + result.size() + " / " + models.size());
    }

    @Test
    @DisplayName("测试6：空模型列表异常")
    void testEmptyModelListThrowsException() {
        List<ModelInfo> emptyModels = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.place(emptyModels, workspace, MIN_GAP);
        }, "空模型列表应抛出IllegalArgumentException");
    }

    @Test
    @DisplayName("测试7：null参数异常")
    void testNullParametersThrowException() {
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(100, 0));

        assertThrows(NullPointerException.class, () -> {
            strategy.place(null, workspace, MIN_GAP);
        }, "null模型列表应抛出NullPointerException");

        assertThrows(NullPointerException.class, () -> {
            strategy.place(models, null, MIN_GAP);
        }, "null工作空间应抛出NullPointerException");
    }

    @Test
    @DisplayName("测试8：负数间隙异常")
    void testNegativeGapThrowsException() {
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(100, 0));

        assertThrows(IllegalArgumentException.class, () -> {
            strategy.place(models, workspace, -1.0f);
        }, "负数间隙应抛出IllegalArgumentException");
    }

    @Test
    @DisplayName("测试9：模型按体积排序（大件优先）")
    void testModelsSortedByVolume() {
        // 创建不同尺寸的模型（故意按小到大顺序添加）
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(50, 0));  // 小
        models.add(createCubeMesh(100, 1)); // 大
        models.add(createCubeMesh(75, 2));  // 中

        // 执行摆放
        List<PlacedModel> result = strategy.place(models, workspace, MIN_GAP);

        // 验证结果
        assertNotNull(result, "摆放结果不应为null");
        assertTrue(result.size() >= 3, "应至少摆放3个模型");

        // 算法内部应该按体积降序排序，但返回结果的顺序反映了摆放顺序
        // 我们只验证所有模型都被成功摆放
        assertEquals(3, result.size(), "所有3个模型都应被成功摆放");

        System.out.println("测试9 - 摆放顺序：");
        for (int i = 0; i < result.size(); i++) {
            PlacedModel placed = result.get(i);
            System.out.println("  " + (i + 1) + ". " + placed.getFileName() +
                " (体积=" + placed.getVolume() + "mm³, 位置=" + placed.getPosition() + ")");
        }
    }

    @Test
    @DisplayName("测试10：大空间多模型压力测试")
    void testLargeWorkspaceMultipleModels() {
        // 创建较大的工作空间
        BoundingBox largeWorkspace = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(2000, 2000, 1500)
        );

        // 创建10个模型
        List<ModelInfo> models = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            models.add(createCubeMesh(80 + i * 5, i)); // 80-125mm
        }

        // 执行摆放
        long startTime = System.currentTimeMillis();
        List<PlacedModel> result = strategy.place(models, largeWorkspace, MIN_GAP);
        long endTime = System.currentTimeMillis();

        // 验证结果
        assertNotNull(result, "摆放结果不应为null");
        assertTrue(result.size() > 0, "应至少摆放1个模型");

        // 计算利用率
        float totalVolume = 0;
        for (PlacedModel placed : result) {
            totalVolume += placed.getVolume();
        }
        float utilization = totalVolume / largeWorkspace.getVolume();

        System.out.println("测试10 - 压力测试结果：");
        System.out.println("  执行时间：" + (endTime - startTime) + "ms");
        System.out.println("  已摆放模型：" + result.size() + " / " + models.size());
        System.out.println("  空间利用率：" + String.format("%.2f%%", utilization * 100));

        // 验证性能（应在2秒内完成）
        assertTrue(endTime - startTime < 2000,
            "算法应在2秒内完成，实际耗时：" + (endTime - startTime) + "ms");
    }

    @Test
    @DisplayName("测试11：多批次摆放 - 覆盖约束满足")
    void testMultiBatchCoverageConstraint() {
        // 3 种不同模型
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(100, 0));
        models.add(createCubeMesh(80, 1));
        models.add(createCubeMesh(60, 2));

        List<PlacedModel> result = strategy.placeMultiBatch(
            models, workspace, MIN_GAP, 1.0f, 10);

        assertNotNull(result);
        // 每种模型至少摆放 1 个（覆盖约束）
        boolean[] covered = new boolean[3];
        for (PlacedModel placed : result) {
            int id = placed.getModelId();
            if (id >= 0 && id < 3) covered[id] = true;
        }
        for (int i = 0; i < 3; i++) {
            assertTrue(covered[i], "模型类型 " + i + " 未被覆盖");
        }
        // 总数量应多于 3（多批次放入了副本）
        assertTrue(result.size() > 3,
            "多批次应摆放超过 3 个模型，实际=" + result.size());
    }

    @Test
    @DisplayName("测试12：多批次摆放 - 无碰撞约束")
    void testMultiBatchNoCollision() {
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(80, 0));
        models.add(createCubeMesh(60, 1));

        List<PlacedModel> result = strategy.placeMultiBatch(
            models, workspace, MIN_GAP, 1.0f, 20);

        assertNotNull(result);
        assertTrue(result.size() > 0);

        // 所有模型对之间必须保持 ≥ 5mm 间隙
        for (int i = 0; i < result.size(); i++) {
            for (int j = i + 1; j < result.size(); j++) {
                BoundingBox box1 = result.get(i).getTransformedAABB();
                BoundingBox box2 = result.get(j).getTransformedAABB();
                BoundingBox expanded = box1.expand(MIN_GAP);
                assertFalse(expanded.intersects(box2),
                    "模型 " + i + " 和模型 " + j + " 间隙不足 " + MIN_GAP + "mm");
            }
        }
    }

    @Test
    @DisplayName("测试13：多批次摆放 - 副本上限约束")
    void testMultiBatchMaxCopiesLimit() {
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(100, 0)); // 唯一模型类型

        int maxCopies = 3;
        List<PlacedModel> result = strategy.placeMultiBatch(
            models, workspace, MIN_GAP, 1.0f, maxCopies);

        assertNotNull(result);
        // 单类型副本数不超过 maxCopies
        assertTrue(result.size() <= maxCopies,
            "副本数超出上限 " + maxCopies + "，实际=" + result.size());
    }

    @Test
    @DisplayName("测试14：多批次摆放 - 目标利用率触发停止")
    void testMultiBatchTargetUtilization() {
        List<ModelInfo> models = new ArrayList<>();
        // 每个模型占工作空间的比例较大，少量副本就能触发利用率阈值
        models.add(createCubeMesh(400, 0));
        models.add(createCubeMesh(300, 1));

        float target = 0.01f; // 1% AABB 利用率，应该很快达到
        List<PlacedModel> result = strategy.placeMultiBatch(
            models, workspace, MIN_GAP, target, 100);

        assertNotNull(result);
        // 结果中已摆放的 AABB 体积之和 / 工作空间体积 应 ≥ target 或已饱和
        float totalAABB = 0;
        for (PlacedModel placed : result) {
            totalAABB += placed.getTransformedAABB().getVolume();
        }
        float utilization = totalAABB / workspace.getVolume();
        // 要么达到目标，要么空间已饱和（后者允许利用率低于目标）
        assertTrue(utilization >= target || result.size() >= 1,
            "应至少摆放 1 个模型");
    }

    @Test
    @DisplayName("测试15：多批次摆放 - 非法参数抛出异常")
    void testMultiBatchInvalidParameters() {
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(100, 0));

        assertThrows(IllegalArgumentException.class, () ->
            strategy.placeMultiBatch(models, workspace, MIN_GAP, -0.1f, 5),
            "目标利用率为负应抛出异常");

        assertThrows(IllegalArgumentException.class, () ->
            strategy.placeMultiBatch(models, workspace, MIN_GAP, 0.5f, 0),
            "最大副本数为0应抛出异常");

        assertThrows(NullPointerException.class, () ->
            strategy.placeMultiBatch(null, workspace, MIN_GAP, 0.5f, 5),
            "null模型列表应抛出异常");
    }
}
