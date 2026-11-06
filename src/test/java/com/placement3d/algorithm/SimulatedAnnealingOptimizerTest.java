package com.placement3d.algorithm;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.io.STLParser;
import com.placement3d.model.Matrix3x3;
import com.placement3d.model.Mesh;
import com.placement3d.model.ModelInfo;
import com.placement3d.model.PlacedModel;
import com.placement3d.model.Triangle;
import com.placement3d.model.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimulatedAnnealingOptimizer 单元测试 + 真实 STL 效果验证。
 *
 * <p>测试分两类：</p>
 * <ul>
 *   <li>基础行为测试（用合成立方体，快速、可复现）</li>
 *   <li>效果验证测试（用真实 STL，对比贪心基线与退火优化的利用率提升）</li>
 * </ul>
 */
@DisplayName("SimulatedAnnealingOptimizer 测试")
class SimulatedAnnealingOptimizerTest {

    private static final float MIN_GAP = 5.0f;
    private static final BoundingBox WORKSPACE = new BoundingBox(
        new Vector3D(0, 0, 0),
        new Vector3D(1000, 1000, 1000)
    );

    private SimulatedAnnealingOptimizer optimizer;
    private GreedyPlacementStrategy greedy;

    @BeforeEach
    void setUp() {
        // 固定种子保证可复现；迭代次数 200 使基础测试在 1 秒内完成
        optimizer = new SimulatedAnnealingOptimizer(200, new Random(42));
        greedy    = new GreedyPlacementStrategy();
    }

    // ─────────────────────────── 基础行为测试 ────────────────────────────────

    @Test
    @DisplayName("空初始解 → 返回空列表")
    void testEmptyInitialSolution() {
        List<PlacedModel> result = optimizer.optimize(new ArrayList<>(), WORKSPACE, MIN_GAP);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("单个模型 → 结果至少 1 个模型，无碰撞")
    void testSingleModel() {
        ModelInfo model = createCube(100, 0);
        List<ModelInfo> models = List.of(model);
        List<PlacedModel> initial = greedy.place(models, WORKSPACE, MIN_GAP);
        List<PlacedModel> result  = optimizer.optimize(initial, WORKSPACE, MIN_GAP);

        assertFalse(result.isEmpty(), "至少保留 1 个模型");
        assertNoCollision(result, MIN_GAP);
        assertAllInWorkspace(result, WORKSPACE);
    }

    @Test
    @DisplayName("多个模型 → 优化后无碰撞、不越界")
    void testMultipleModels() {
        List<ModelInfo> models = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            models.add(createCube(80 + i * 10, i));
        }

        List<PlacedModel> initial = greedy.placeMultiBatch(models, WORKSPACE, MIN_GAP, 1.0f, 5);
        List<PlacedModel> result  = optimizer.optimize(initial, WORKSPACE, MIN_GAP);

        assertFalse(result.isEmpty());
        assertNoCollision(result, MIN_GAP);
        assertAllInWorkspace(result, WORKSPACE);
    }

    @Test
    @DisplayName("退火结果利用率 ≥ 初始解利用率（不得变差）")
    void testUtilizationNotWorse() {
        List<ModelInfo> models = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            models.add(createCube(60 + i * 10, i));
        }

        List<PlacedModel> initial = greedy.placeMultiBatch(models, WORKSPACE, MIN_GAP, 1.0f, 8);
        double initUtil  = aabbUtilization(initial, WORKSPACE);

        // 更多迭代以便有机会改善
        SimulatedAnnealingOptimizer sa = new SimulatedAnnealingOptimizer(500, new Random(7));
        List<PlacedModel> result  = sa.optimize(initial, WORKSPACE, MIN_GAP);
        double finalUtil = aabbUtilization(result, WORKSPACE);

        System.out.printf("[合成] 贪心利用率=%.4f  退火利用率=%.4f  Δ=%.4f%n",
            initUtil, finalUtil, finalUtil - initUtil);

        // 退火保证"不变差"——bestSolution 只在改善时更新
        assertTrue(finalUtil >= initUtil - 1e-6,
            "退火利用率不得低于贪心基线，init=" + initUtil + " final=" + finalUtil);
    }

    @Test
    @DisplayName("非法参数 → 抛出异常")
    void testInvalidArgs() {
        assertThrows(NullPointerException.class, () ->
            optimizer.optimize(null, WORKSPACE, MIN_GAP));
        assertThrows(NullPointerException.class, () ->
            optimizer.optimize(new ArrayList<>(), null, MIN_GAP));
        assertThrows(IllegalArgumentException.class, () ->
            optimizer.optimize(new ArrayList<>(), WORKSPACE, -1.0f));
        assertThrows(IllegalArgumentException.class, () ->
            new SimulatedAnnealingOptimizer(0, new Random()));
    }

    @Test
    @DisplayName("toString 包含关键参数")
    void testToString() {
        String s = optimizer.toString();
        assertTrue(s.contains("T"), "包含温度符号");
        assertTrue(s.contains("0.95"), "包含降温系数");
    }

    // ─────────────────────────── 真实 STL 效果验证 ──────────────────────────

    /**
     * 用项目中的真实 STL 文件对比贪心 vs 退火，打印详细数据报告。
     * 若 STL 目录不存在则跳过（CI 环境）。
     */
    @Test
    @DisplayName("【效果验证】真实 STL — 贪心 vs 退火利用率对比")
    void testRealStlUtilizationComparison() throws Exception {
        String stlDir = "D:/CS/JAVA/3D_Object/STLmodels";
        File dir = new File(stlDir);
        if (!dir.exists()) {
            System.out.println("STL 目录不存在，跳过真实 STL 测试：" + stlDir);
            return;
        }

        // 工作空间与项目真实场景一致
        BoundingBox ws = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(4000, 2500, 1500)
        );

        // 加载全部 STL（跳过 Merge part，单独处理）
        STLParser parser = new STLParser();
        List<ModelInfo> models = new ArrayList<>();
        File[] stlFiles = dir.listFiles((d, name) ->
            name.endsWith(".stl") && !name.equals("Merge part.stl"));
        if (stlFiles == null || stlFiles.length == 0) {
            System.out.println("未找到 STL 文件，跳过。");
            return;
        }
        java.util.Arrays.sort(stlFiles);
        for (int i = 0; i < stlFiles.length; i++) {
            Mesh mesh = parser.parse(stlFiles[i].getAbsolutePath());
            models.add(new ModelInfo(stlFiles[i].getName(), mesh, i));
        }

        System.out.println("\n========================================");
        System.out.println("  真实 STL 模拟退火效果验证");
        System.out.printf("  工作空间：4000×2500×1500 mm%n");
        System.out.printf("  模型类型数：%d%n", models.size());
        System.out.println("========================================\n");

        // ── 阶段一：贪心多批次（基线）──────────────────────────────────────
        long t0 = System.currentTimeMillis();
        List<PlacedModel> greedySol = greedy.placeMultiBatch(
            models, ws, MIN_GAP, 1.0f, 20);
        long greedyMs = System.currentTimeMillis() - t0;

        double greedyUtil = aabbUtilization(greedySol, ws);
        System.out.printf("【贪心基线】%n");
        System.out.printf("  摆放数量  : %d 件%n", greedySol.size());
        System.out.printf("  AABB 利用率: %.4f  (%.2f%%)%n", greedyUtil, greedyUtil * 100);
        System.out.printf("  耗时       : %d ms%n%n", greedyMs);

        // ── 阶段二：模拟退火优化 ─────────────────────────────────────────────
        // 500 次迭代，固定种子可复现
        SimulatedAnnealingOptimizer sa = new SimulatedAnnealingOptimizer(500, new Random(2024));
        long t1 = System.currentTimeMillis();
        List<PlacedModel> saSol = sa.optimize(greedySol, ws, MIN_GAP);
        long saMs = System.currentTimeMillis() - t1;

        double saUtil = aabbUtilization(saSol, ws);
        double delta  = saUtil - greedyUtil;
        System.out.printf("【退火优化】%n");
        System.out.printf("  摆放数量  : %d 件%n", saSol.size());
        System.out.printf("  AABB 利用率: %.4f  (%.2f%%)%n", saUtil, saUtil * 100);
        System.out.printf("  耗时       : %d ms%n", saMs);
        System.out.printf("  Δ 利用率   : %+.4f  (%+.2f%%)%n%n", delta, delta * 100);

        // ── 间隙合规检查 ────────────────────────────────────────────────────
        int violations = countGapViolations(saSol, MIN_GAP);
        System.out.printf("【合规检查】%n");
        System.out.printf("  5mm 间隙违规对数: %d%n", violations);
        System.out.println("========================================\n");

        // 断言：退火不变差，无间隙违规
        assertTrue(saUtil >= greedyUtil - 1e-6, "退火利用率不得低于贪心基线");
        assertEquals(0, violations, "退火结果不得有间隙违规");
    }

    // ─────────────────────────── 辅助方法 ────────────────────────────────────

    private static double aabbUtilization(List<PlacedModel> placed, BoundingBox ws) {
        double total = 0;
        for (PlacedModel pm : placed) {
            total += pm.getTransformedAABB().getVolume();
        }
        return total / ws.getVolume();
    }

    private static void assertNoCollision(List<PlacedModel> placed, float minGap) {
        for (int i = 0; i < placed.size(); i++) {
            for (int j = i + 1; j < placed.size(); j++) {
                BoundingBox b1 = placed.get(i).getTransformedAABB();
                BoundingBox b2 = placed.get(j).getTransformedAABB();
                assertFalse(b1.expand(minGap).intersects(b2),
                    "模型 " + i + " 与模型 " + j + " 间隙不足 " + minGap + "mm");
            }
        }
    }

    private static void assertAllInWorkspace(List<PlacedModel> placed, BoundingBox ws) {
        for (PlacedModel pm : placed) {
            BoundingBox aabb = pm.getTransformedAABB();
            assertTrue(aabb.getMin().getX() >= ws.getMin().getX() - 1e-3f, pm.getFileName() + " X min 越界");
            assertTrue(aabb.getMin().getY() >= ws.getMin().getY() - 1e-3f, pm.getFileName() + " Y min 越界");
            assertTrue(aabb.getMin().getZ() >= ws.getMin().getZ() - 1e-3f, pm.getFileName() + " Z min 越界");
            assertTrue(aabb.getMax().getX() <= ws.getMax().getX() + 1e-3f, pm.getFileName() + " X max 越界");
            assertTrue(aabb.getMax().getY() <= ws.getMax().getY() + 1e-3f, pm.getFileName() + " Y max 越界");
            assertTrue(aabb.getMax().getZ() <= ws.getMax().getZ() + 1e-3f, pm.getFileName() + " Z max 越界");
        }
    }

    private static int countGapViolations(List<PlacedModel> placed, float minGap) {
        int count = 0;
        for (int i = 0; i < placed.size(); i++) {
            for (int j = i + 1; j < placed.size(); j++) {
                BoundingBox b1 = placed.get(i).getTransformedAABB();
                BoundingBox b2 = placed.get(j).getTransformedAABB();
                if (b1.expand(minGap).intersects(b2)) count++;
            }
        }
        return count;
    }

    private static ModelInfo createCube(float size, int id) {
        Vector3D v000 = new Vector3D(0, 0, 0);
        Vector3D v100 = new Vector3D(size, 0, 0);
        Vector3D v110 = new Vector3D(size, size, 0);
        Vector3D v010 = new Vector3D(0, size, 0);
        Vector3D v001 = new Vector3D(0, 0, size);
        Vector3D v101 = new Vector3D(size, 0, size);
        Vector3D v111 = new Vector3D(size, size, size);
        Vector3D v011 = new Vector3D(0, size, size);
        List<Triangle> tris = new ArrayList<>();
        tris.add(new Triangle(v000, v100, v110)); tris.add(new Triangle(v000, v110, v010));
        tris.add(new Triangle(v001, v111, v101)); tris.add(new Triangle(v001, v011, v111));
        tris.add(new Triangle(v000, v101, v100)); tris.add(new Triangle(v000, v001, v101));
        tris.add(new Triangle(v010, v110, v111)); tris.add(new Triangle(v010, v111, v011));
        tris.add(new Triangle(v000, v010, v011)); tris.add(new Triangle(v000, v011, v001));
        tris.add(new Triangle(v100, v111, v110)); tris.add(new Triangle(v100, v101, v111));
        Mesh mesh = new Mesh(tris, "cube_" + size + ".stl");
        return new ModelInfo("cube_" + size + ".stl", mesh, id);
    }
}
