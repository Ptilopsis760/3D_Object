package com.placement3d.space;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SpaceManager类的单元测试。
 */
@DisplayName("SpaceManager 测试")
class SpaceManagerTest {

    private SpaceManager spaceManager;
    private BoundingBox workspace;
    private ModelInfo cubeModel;
    private ModelInfo smallCubeModel;

    @BeforeEach
    void setUp() {
        // 1000x1000x1000mm工作空间
        workspace = new BoundingBox(new Vector3D(0, 0, 0), new Vector3D(1000, 1000, 1000));
        spaceManager = new SpaceManager(workspace, 5.0f);

        // 创建100x100x100mm立方体模型
        List<Vector3D> vertices = new ArrayList<>();
        vertices.add(new Vector3D(0, 0, 0));
        vertices.add(new Vector3D(100, 0, 0));
        vertices.add(new Vector3D(100, 100, 0));
        vertices.add(new Vector3D(0, 100, 0));
        vertices.add(new Vector3D(0, 0, 100));
        vertices.add(new Vector3D(100, 0, 100));
        vertices.add(new Vector3D(100, 100, 100));
        vertices.add(new Vector3D(0, 100, 100));

        List<Triangle> triangles = new ArrayList<>();
        triangles.add(new Triangle(vertices.get(0), vertices.get(1), vertices.get(2)));

        Mesh mesh = new Mesh(triangles, "cube.stl");
        cubeModel = new ModelInfo("cube.stl", mesh, 0);

        // 创建50x50x50mm小立方体模型
        List<Vector3D> smallVertices = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            smallVertices.add(vertices.get(i).multiply(0.5f));
        }
        Mesh smallMesh = new Mesh(triangles, "small_cube.stl");
        smallCubeModel = new ModelInfo("small_cube.stl", smallMesh, 1);
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("正常创建SpaceManager")
        void testNormalConstruction() {
            SpaceManager manager = new SpaceManager(workspace, 5.0f);

            assertNotNull(manager);
            assertEquals(0, manager.getPlacedCount());
            assertEquals(0.0f, manager.getUtilizationRate());
        }

        @Test
        @DisplayName("使用默认最小间隙创建")
        void testDefaultMinGapConstruction() {
            SpaceManager manager = new SpaceManager(workspace);

            assertNotNull(manager);
            assertEquals(CollisionDetector.DEFAULT_MIN_GAP, manager.getMinGap());
        }

        @Test
        @DisplayName("null工作空间应抛出NullPointerException")
        void testNullWorkspace() {
            assertThrows(NullPointerException.class, () -> new SpaceManager(null, 5.0f));
        }

        @Test
        @DisplayName("负最小间隙应抛出IllegalArgumentException")
        void testNegativeMinGap() {
            assertThrows(IllegalArgumentException.class, () -> new SpaceManager(workspace, -1.0f));
        }
    }

    @Nested
    @DisplayName("canPlace测试")
    class CanPlaceTests {

        @Test
        @DisplayName("工作空间内的模型应可摆放")
        void testCanPlaceInside() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));

            assertTrue(spaceManager.canPlace(model));
        }

        @Test
        @DisplayName("超出边界的模型不可摆放")
        void testCannotPlaceOutside() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(950, 100, 100));

            assertFalse(spaceManager.canPlace(model));
        }

        @Test
        @DisplayName("与已摆放模型碰撞的模型不可摆放")
        void testCannotPlaceCollision() {
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model1);

            // model2与model1碰撞
            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(150, 150, 150));

            assertFalse(spaceManager.canPlace(model2));
        }

        @Test
        @DisplayName("间隙充足的模型应可摆放")
        void testCanPlaceWithSufficientGap() {
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model1);

            // model2距离model1有足够间隙（>5mm）
            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(250, 100, 100));

            assertTrue(spaceManager.canPlace(model2));
        }

        @Test
        @DisplayName("间隙不足的模型不可摆放")
        void testCannotPlaceInsufficientGap() {
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model1);

            // model2距离model1间隙不足（<5mm）
            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(203, 100, 100));

            assertFalse(spaceManager.canPlace(model2));
        }

        @Test
        @DisplayName("null模型应抛出NullPointerException")
        void testCanPlaceNull() {
            assertThrows(NullPointerException.class, () -> spaceManager.canPlace(null));
        }

        @Test
        @DisplayName("边界间隙检查")
        void testCanPlaceBoundaryGap() {
            // 距离边界只有2mm，小于5mm
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(2, 100, 100));

            assertFalse(spaceManager.canPlace(model));
        }
    }

    @Nested
    @DisplayName("place测试")
    class PlaceTests {

        @Test
        @DisplayName("摆放合法模型应成功")
        void testPlaceValid() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));

            assertDoesNotThrow(() -> spaceManager.place(model));
            assertEquals(1, spaceManager.getPlacedCount());
        }

        @Test
        @DisplayName("摆放非法模型应抛出IllegalStateException")
        void testPlaceInvalid() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(2000, 2000, 2000));

            assertThrows(IllegalStateException.class, () -> spaceManager.place(model));
        }

        @Test
        @DisplayName("摆放模型应更新占用体积")
        void testPlaceUpdatesVolume() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            float expectedVolume = model.getVolume();

            spaceManager.place(model);

            assertEquals(expectedVolume, spaceManager.getOccupiedVolume(), 0.01f);
        }

        @Test
        @DisplayName("摆放多个模型应累加体积")
        void testPlaceMultipleUpdatesVolume() {
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(300, 100, 100));

            spaceManager.place(model1);
            spaceManager.place(model2);

            float expectedVolume = model1.getVolume() + model2.getVolume();
            assertEquals(expectedVolume, spaceManager.getOccupiedVolume(), 0.01f);
        }

        @Test
        @DisplayName("摆放null模型应抛出NullPointerException")
        void testPlaceNull() {
            assertThrows(NullPointerException.class, () -> spaceManager.place(null));
        }

        @Test
        @DisplayName("摆放后模型应出现在列表中")
        void testPlaceAddsToList() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));

            spaceManager.place(model);

            assertTrue(spaceManager.getPlacedModels().contains(model));
        }
    }

    @Nested
    @DisplayName("remove测试")
    class RemoveTests {

        @Test
        @DisplayName("移除已摆放的模型应成功")
        void testRemoveExisting() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);

            boolean result = spaceManager.remove(model);

            assertTrue(result);
            assertEquals(0, spaceManager.getPlacedCount());
        }

        @Test
        @DisplayName("移除不存在的模型应返回false")
        void testRemoveNonExisting() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));

            boolean result = spaceManager.remove(model);

            assertFalse(result);
        }

        @Test
        @DisplayName("移除模型应更新占用体积")
        void testRemoveUpdatesVolume() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);

            spaceManager.remove(model);

            assertEquals(0.0f, spaceManager.getOccupiedVolume(), 0.01f);
        }

        @Test
        @DisplayName("移除一个模型不影响其他模型")
        void testRemoveOneOfMany() {
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(300, 100, 100));
            spaceManager.place(model1);
            spaceManager.place(model2);

            spaceManager.remove(model1);

            assertEquals(1, spaceManager.getPlacedCount());
            assertEquals(model2.getVolume(), spaceManager.getOccupiedVolume(), 0.01f);
        }

        @Test
        @DisplayName("移除null模型应抛出NullPointerException")
        void testRemoveNull() {
            assertThrows(NullPointerException.class, () -> spaceManager.remove(null));
        }

        @Test
        @DisplayName("移除后可以在相同位置重新摆放")
        void testRemoveThenPlace() {
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model1);
            spaceManager.remove(model1);

            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));

            assertTrue(spaceManager.canPlace(model2));
            assertDoesNotThrow(() -> spaceManager.place(model2));
        }
    }

    @Nested
    @DisplayName("getPlacedModels测试")
    class GetPlacedModelsTests {

        @Test
        @DisplayName("应返回不可修改列表")
        void testGetPlacedModelsUnmodifiable() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);

            List<PlacedModel> models = spaceManager.getPlacedModels();

            assertThrows(UnsupportedOperationException.class, () -> models.add(model));
        }

        @Test
        @DisplayName("应返回正确的模型列表")
        void testGetPlacedModelsContent() {
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(300, 100, 100));

            spaceManager.place(model1);
            spaceManager.place(model2);

            List<PlacedModel> models = spaceManager.getPlacedModels();

            assertEquals(2, models.size());
            assertTrue(models.contains(model1));
            assertTrue(models.contains(model2));
        }

        @Test
        @DisplayName("空SpaceManager应返回空列表")
        void testGetPlacedModelsEmpty() {
            List<PlacedModel> models = spaceManager.getPlacedModels();

            assertNotNull(models);
            assertEquals(0, models.size());
        }
    }

    @Nested
    @DisplayName("利用率测试")
    class UtilizationTests {

        @Test
        @DisplayName("初始利用率应为0")
        void testInitialUtilization() {
            assertEquals(0.0f, spaceManager.getUtilizationRate());
        }

        @Test
        @DisplayName("摆放模型应增加利用率")
        void testUtilizationIncrease() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);

            float expectedRate = model.getVolume() / workspace.getVolume();
            assertEquals(expectedRate, spaceManager.getUtilizationRate(), 0.0001f);
        }

        @Test
        @DisplayName("移除模型应降低利用率")
        void testUtilizationDecrease() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);
            spaceManager.remove(model);

            assertEquals(0.0f, spaceManager.getUtilizationRate());
        }

        @Test
        @DisplayName("利用率应在0到1之间")
        void testUtilizationRange() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);

            float rate = spaceManager.getUtilizationRate();
            assertTrue(rate >= 0.0f && rate <= 1.0f);
        }
    }

    @Nested
    @DisplayName("clear测试")
    class ClearTests {

        @Test
        @DisplayName("清空后count应为0")
        void testClearCount() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);

            spaceManager.clear();

            assertEquals(0, spaceManager.getPlacedCount());
        }

        @Test
        @DisplayName("清空后体积应为0")
        void testClearVolume() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);

            spaceManager.clear();

            assertEquals(0.0f, spaceManager.getOccupiedVolume());
        }

        @Test
        @DisplayName("清空后利用率应为0")
        void testClearUtilization() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);

            spaceManager.clear();

            assertEquals(0.0f, spaceManager.getUtilizationRate());
        }

        @Test
        @DisplayName("清空后可以重新摆放")
        void testClearThenPlace() {
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model1);
            spaceManager.clear();

            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));

            assertTrue(spaceManager.canPlace(model2));
            assertDoesNotThrow(() -> spaceManager.place(model2));
        }
    }

    @Nested
    @DisplayName("Getter测试")
    class GetterTests {

        @Test
        @DisplayName("getWorkspace应返回工作空间")
        void testGetWorkspace() {
            assertEquals(workspace, spaceManager.getWorkspace());
        }

        @Test
        @DisplayName("getMinGap应返回最小间隙")
        void testGetMinGap() {
            assertEquals(5.0f, spaceManager.getMinGap());
        }

        @Test
        @DisplayName("getOctree应返回八叉树实例")
        void testGetOctree() {
            assertNotNull(spaceManager.getOctree());
        }

        @Test
        @DisplayName("getCollisionDetector应返回检测器实例")
        void testGetCollisionDetector() {
            assertNotNull(spaceManager.getCollisionDetector());
        }
    }

    @Nested
    @DisplayName("一致性验证测试")
    class ConsistencyTests {

        @Test
        @DisplayName("空SpaceManager应通过一致性检查")
        void testEmptyConsistency() {
            assertTrue(spaceManager.verifyConsistency());
        }

        @Test
        @DisplayName("摆放模型后应通过一致性检查")
        void testPlaceConsistency() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);

            assertTrue(spaceManager.verifyConsistency());
        }

        @Test
        @DisplayName("移除模型后应通过一致性检查")
        void testRemoveConsistency() {
            PlacedModel model = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            spaceManager.place(model);
            spaceManager.remove(model);

            assertTrue(spaceManager.verifyConsistency());
        }

        @Test
        @DisplayName("多次操作后应通过一致性检查")
        void testMultipleOperationsConsistency() {
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(300, 100, 100));
            PlacedModel model3 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(500, 100, 100));

            spaceManager.place(model1);
            spaceManager.place(model2);
            spaceManager.place(model3);
            spaceManager.remove(model2);

            assertTrue(spaceManager.verifyConsistency());
        }
    }

    @Nested
    @DisplayName("集成测试")
    class IntegrationTests {

        @Test
        @DisplayName("完整的摆放-移除-重摆放流程")
        void testFullPlacementFlow() {
            // 摆放3个模型
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(300, 100, 100));
            PlacedModel model3 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(500, 100, 100));

            spaceManager.place(model1);
            spaceManager.place(model2);
            spaceManager.place(model3);

            assertEquals(3, spaceManager.getPlacedCount());

            // 移除中间的模型
            spaceManager.remove(model2);
            assertEquals(2, spaceManager.getPlacedCount());

            // 在原位置重新摆放
            PlacedModel model4 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(300, 100, 100));
            assertTrue(spaceManager.canPlace(model4));
            spaceManager.place(model4);

            assertEquals(3, spaceManager.getPlacedCount());
            assertTrue(spaceManager.verifyConsistency());
        }

        @Test
        @DisplayName("模拟贪心算法摆放流程")
        void testGreedyPlacementSimulation() {
            List<PlacedModel> placedModels = new ArrayList<>();

            // 按网格摆放模型
            for (int x = 0; x < 5; x++) {
                for (int y = 0; y < 5; y++) {
                    PlacedModel model = new PlacedModel(
                        smallCubeModel,
                        Matrix3x3.IDENTITY,
                        new Vector3D(x * 60, y * 60, 10)
                    );

                    if (spaceManager.canPlace(model)) {
                        spaceManager.place(model);
                        placedModels.add(model);
                    }
                }
            }

            assertTrue(placedModels.size() > 0);
            assertTrue(spaceManager.getUtilizationRate() > 0);
            assertTrue(spaceManager.verifyConsistency());
        }

        @Test
        @DisplayName("模拟模拟退火SWAP操作")
        void testSimulatedAnnealingSwap() {
            PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
            PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(300, 100, 100));

            spaceManager.place(model1);
            spaceManager.place(model2);

            // SWAP操作：交换两个模型的位置
            spaceManager.remove(model1);
            spaceManager.remove(model2);

            PlacedModel newModel1 = PlacedModel.create(
                model1.getModel(),
                model1.getRotationMatrix(),
                new Vector3D(300, 100, 100)
            );
            PlacedModel newModel2 = PlacedModel.create(
                model2.getModel(),
                model2.getRotationMatrix(),
                new Vector3D(100, 100, 100)
            );

            spaceManager.place(newModel1);
            spaceManager.place(newModel2);

            assertEquals(2, spaceManager.getPlacedCount());
            assertTrue(spaceManager.verifyConsistency());
        }

        @Test
        @DisplayName("大规模摆放测试")
        void testLargeScalePlacement() {
            int placedCount = 0;

            // 尝试摆放多个小模型
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    for (int z = 0; z < 5; z++) {
                        PlacedModel model = new PlacedModel(
                            smallCubeModel,
                            Matrix3x3.IDENTITY,
                            new Vector3D(x * 60, y * 60, z * 60)
                        );

                        if (spaceManager.canPlace(model)) {
                            spaceManager.place(model);
                            placedCount++;
                        }
                    }
                }
            }

            assertTrue(placedCount > 50, "应该能摆放至少50个小模型");
            assertTrue(spaceManager.verifyConsistency());
        }
    }

    @Nested
    @DisplayName("toString测试")
    class ToStringTests {

        @Test
        @DisplayName("toString应包含关键信息")
        void testToString() {
            String str = spaceManager.toString();

            assertNotNull(str);
            assertTrue(str.contains("SpaceManager"));
            assertTrue(str.contains("workspace"));
        }

        @Test
        @DisplayName("toString不应抛出异常")
        void testToStringNoException() {
            assertDoesNotThrow(() -> spaceManager.toString());
        }
    }
}
