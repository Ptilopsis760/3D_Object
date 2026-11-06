package com.placement3d.space;

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
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 八叉树空间索引的全面测试套件。
 *
 * <p>测试覆盖：</p>
 * <ul>
 *   <li>基本操作：插入、查询、移除、清空</li>
 *   <li>边界情况：空树、单个模型、超出边界</li>
 *   <li>分割逻辑：跨空间模型、深度限制</li>
 *   <li>性能场景：大量模型、重叠查询</li>
 * </ul>
 */
@DisplayName("八叉树测试")
class OctreeTest {

    private BoundingBox workspace;
    private Octree octree;

    @BeforeEach
    void setUp() {
        // 创建4000×2500×1500mm的工作空间
        workspace = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(4000, 2500, 1500)
        );
        octree = new Octree(workspace);
    }

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("创建八叉树")
        void testConstructor() {
            Octree tree = new Octree(workspace);
            assertEquals(0, tree.size());
        }

        @Test
        @DisplayName("边界为null时抛出异常")
        void testNullBounds() {
            assertThrows(NullPointerException.class, () -> new Octree(null));
        }
    }

    @Nested
    @DisplayName("插入操作测试")
    class InsertTests {

        @Test
        @DisplayName("插入单个模型")
        void testInsertSingleModel() {
            PlacedModel model = createModel("M1", 100, 100, 100, 200, 200, 200);
            assertTrue(octree.insert(model));
            assertEquals(1, octree.size());
        }

        @Test
        @DisplayName("插入多个模型")
        void testInsertMultipleModels() {
            PlacedModel m1 = createModel("M1", 100, 100, 100, 200, 200, 200);
            PlacedModel m2 = createModel("M2", 300, 300, 300, 400, 400, 400);
            PlacedModel m3 = createModel("M3", 500, 500, 500, 600, 600, 600);

            assertTrue(octree.insert(m1));
            assertTrue(octree.insert(m2));
            assertTrue(octree.insert(m3));

            assertEquals(3, octree.size());
        }

        @Test
        @DisplayName("插入null模型时抛出异常")
        void testInsertNull() {
            assertThrows(NullPointerException.class, () -> octree.insert(null));
        }

        @Test
        @DisplayName("插入完全在工作空间内的模型")
        void testInsertModelWithinBounds() {
            PlacedModel model = createModel("M1", 1000, 1000, 500, 1500, 1500, 1000);
            assertTrue(octree.insert(model));

            assertEquals(1, octree.size());
            List<PlacedModel> results = octree.query(model.getTransformedAABB());
            assertEquals(1, results.size());
            assertEquals(model, results.get(0));
        }

        @Test
        @DisplayName("插入跨越多个子空间的大模型")
        void testInsertLargeModelSpanningOctants() {
            PlacedModel largeModel = createModel("Large", 1500, 1000, 500, 2500, 1500, 1000);
            assertTrue(octree.insert(largeModel));

            assertTrue(octree.size() > 0);
            List<PlacedModel> results = octree.query(largeModel.getTransformedAABB());
            assertTrue(results.contains(largeModel));
        }

        @Test
        @DisplayName("插入多个模型触发空间分割")
        void testInsertTriggeringSubdivision() {
            for (int i = 0; i < 15; i++) {
                PlacedModel model = createModel("M" + i, 100 + i * 10, 100 + i * 10, 100,
                                                 150 + i * 10, 150 + i * 10, 150);
                octree.insert(model);
            }

            assertEquals(15, octree.size());
            assertTrue(octree.getActualDepth() > 0);
        }

        @Test
        @DisplayName("插入超出边界的模型")
        void testInsertOutOfBounds() {
            PlacedModel outsideModel = createModel("Outside", 5000, 5000, 5000, 6000, 6000, 6000);
            assertFalse(octree.insert(outsideModel));
            assertEquals(0, octree.size());
        }
    }

    @Nested
    @DisplayName("查询操作测试")
    class QueryTests {

        @Test
        @DisplayName("在空树中查询返回空列表")
        void testQueryEmptyTree() {
            BoundingBox queryRegion = new BoundingBox(
                new Vector3D(0, 0, 0),
                new Vector3D(100, 100, 100)
            );
            List<PlacedModel> results = octree.query(queryRegion);

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("查询null区域时抛出异常")
        void testQueryNull() {
            assertThrows(NullPointerException.class, () -> octree.query(null));
        }

        @Test
        @DisplayName("查询单个模型")
        void testQuerySingleModel() {
            PlacedModel model = createModel("M1", 100, 100, 100, 200, 200, 200);
            octree.insert(model);

            BoundingBox queryRegion = new BoundingBox(
                new Vector3D(150, 150, 150),
                new Vector3D(250, 250, 250)
            );
            List<PlacedModel> results = octree.query(queryRegion);

            assertEquals(1, results.size());
            assertEquals(model, results.get(0));
        }

        @Test
        @DisplayName("查询不相交的区域返回空列表")
        void testQueryNonIntersectingRegion() {
            PlacedModel model = createModel("M1", 100, 100, 100, 200, 200, 200);
            octree.insert(model);

            BoundingBox queryRegion = new BoundingBox(
                new Vector3D(300, 300, 300),
                new Vector3D(400, 400, 400)
            );
            List<PlacedModel> results = octree.query(queryRegion);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("查询多个相交的模型")
        void testQueryMultipleIntersectingModels() {
            PlacedModel m1 = createModel("M1", 100, 100, 100, 300, 300, 300);
            PlacedModel m2 = createModel("M2", 200, 200, 200, 400, 400, 400);
            PlacedModel m3 = createModel("M3", 500, 500, 500, 600, 600, 600);

            octree.insert(m1);
            octree.insert(m2);
            octree.insert(m3);

            BoundingBox queryRegion = new BoundingBox(
                new Vector3D(150, 150, 150),
                new Vector3D(350, 350, 350)
            );
            List<PlacedModel> results = octree.query(queryRegion);

            assertEquals(2, results.size());
            assertTrue(results.contains(m1));
            assertTrue(results.contains(m2));
            assertFalse(results.contains(m3));
        }

        @Test
        @DisplayName("查询整个工作空间返回所有模型")
        void testQueryEntireWorkspace() {
            PlacedModel m1 = createModel("M1", 100, 100, 100, 200, 200, 200);
            PlacedModel m2 = createModel("M2", 300, 300, 300, 400, 400, 400);
            PlacedModel m3 = createModel("M3", 500, 500, 500, 600, 600, 600);

            octree.insert(m1);
            octree.insert(m2);
            octree.insert(m3);

            List<PlacedModel> results = octree.query(workspace);

            assertEquals(3, results.size());
            assertTrue(results.contains(m1));
            assertTrue(results.contains(m2));
            assertTrue(results.contains(m3));
        }

        @Test
        @DisplayName("使用queryCandidates方法")
        void testQueryCandidates() {
            PlacedModel m1 = createModel("M1", 100, 100, 100, 200, 200, 200);
            PlacedModel m2 = createModel("M2", 150, 150, 150, 250, 250, 250);

            octree.insert(m1);
            octree.insert(m2);

            List<PlacedModel> candidates = octree.queryCandidates(m1);
            assertTrue(candidates.size() >= 1);
        }
    }

    @Nested
    @DisplayName("移除操作测试")
    class RemoveTests {

        @Test
        @DisplayName("从树中移除存在的模型")
        void testRemoveExistingModel() {
            PlacedModel model = createModel("M1", 100, 100, 100, 200, 200, 200);
            octree.insert(model);

            assertTrue(octree.remove(model));
            assertEquals(0, octree.size());
        }

        @Test
        @DisplayName("移除不存在的模型返回false")
        void testRemoveNonExistentModel() {
            PlacedModel model = createModel("M1", 100, 100, 100, 200, 200, 200);

            assertFalse(octree.remove(model));
            assertEquals(0, octree.size());
        }

        @Test
        @DisplayName("移除null模型时抛出异常")
        void testRemoveNull() {
            assertThrows(NullPointerException.class, () -> octree.remove(null));
        }

        @Test
        @DisplayName("移除多个模型中的一个")
        void testRemoveOneOfMultipleModels() {
            PlacedModel m1 = createModel("M1", 100, 100, 100, 200, 200, 200);
            PlacedModel m2 = createModel("M2", 300, 300, 300, 400, 400, 400);
            PlacedModel m3 = createModel("M3", 500, 500, 500, 600, 600, 600);

            octree.insert(m1);
            octree.insert(m2);
            octree.insert(m3);

            assertTrue(octree.remove(m2));
            assertEquals(2, octree.size());

            List<PlacedModel> results = octree.query(workspace);
            assertEquals(2, results.size());
            assertTrue(results.contains(m1));
            assertFalse(results.contains(m2));
            assertTrue(results.contains(m3));
        }

        @Test
        @DisplayName("重复移除同一模型")
        void testRemoveSameModelTwice() {
            PlacedModel model = createModel("M1", 100, 100, 100, 200, 200, 200);
            octree.insert(model);

            assertTrue(octree.remove(model));
            assertFalse(octree.remove(model));
            assertEquals(0, octree.size());
        }
    }

    @Nested
    @DisplayName("清空操作测试")
    class ClearTests {

        @Test
        @DisplayName("清空空树")
        void testClearEmptyTree() {
            octree.clear();
            assertEquals(0, octree.size());
        }

        @Test
        @DisplayName("清空包含多个模型的树")
        void testClearTreeWithModels() {
            octree.insert(createModel("M1", 100, 100, 100, 200, 200, 200));
            octree.insert(createModel("M2", 300, 300, 300, 400, 400, 400));
            octree.insert(createModel("M3", 500, 500, 500, 600, 600, 600));

            octree.clear();

            assertEquals(0, octree.size());
            List<PlacedModel> results = octree.query(workspace);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("清空后可以重新插入模型")
        void testInsertAfterClear() {
            octree.insert(createModel("M1", 100, 100, 100, 200, 200, 200));
            octree.clear();

            PlacedModel newModel = createModel("M2", 300, 300, 300, 400, 400, 400);
            octree.insert(newModel);

            assertEquals(1, octree.size());
            List<PlacedModel> results = octree.query(workspace);
            assertEquals(1, results.size());
            assertEquals(newModel, results.get(0));
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("插入体积为零的模型（点）")
        void testInsertZeroVolumeModel() {
            PlacedModel pointModel = createModel("Point", 100, 100, 100, 100, 100, 100);
            octree.insert(pointModel);

            assertEquals(1, octree.size());
        }

        @Test
        @DisplayName("测试最大深度限制")
        void testMaxDepthLimit() {
            for (int i = 0; i < 20; i++) {
                PlacedModel model = createModel("M" + i, 100, 100, 100, 150, 150, 150);
                octree.insert(model);
            }

            assertEquals(20, octree.size());
            assertTrue(octree.getActualDepth() <= Octree.MAX_DEPTH);
        }

        @Test
        @DisplayName("查询完全超出工作空间的区域")
        void testQueryOutsideWorkspace() {
            octree.insert(createModel("M1", 100, 100, 100, 200, 200, 200));

            BoundingBox outsideRegion = new BoundingBox(
                new Vector3D(5000, 5000, 5000),
                new Vector3D(6000, 6000, 6000)
            );
            List<PlacedModel> results = octree.query(outsideRegion);

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("性能和压力测试")
    class PerformanceTests {

        @Test
        @DisplayName("插入大量模型（100个）")
        void testInsertManyModels() {
            for (int i = 0; i < 100; i++) {
                float x = (i % 10) * 400;
                float y = ((i / 10) % 10) * 250;
                float z = (i / 100) * 150;
                PlacedModel model = createModel("M" + i, x, y, z, x + 100, y + 100, z + 100);
                octree.insert(model);
            }

            assertEquals(100, octree.size());
        }

        @Test
        @DisplayName("在大量模型中进行精确查询")
        void testQueryInDenseTree() {
            PlacedModel target = null;
            for (int i = 0; i < 50; i++) {
                float x = (i % 10) * 400;
                float y = ((i / 10) % 5) * 500;
                float z = (i / 50) * 300;
                PlacedModel model = createModel("M" + i, x, y, z, x + 100, y + 100, z + 100);
                octree.insert(model);
                if (i == 25) {
                    target = model;
                }
            }

            assertNotNull(target);
            List<PlacedModel> results = octree.query(target.getTransformedAABB());
            assertTrue(results.contains(target));
        }

        @Test
        @DisplayName("重叠模型的查询")
        void testQueryOverlappingModels() {
            PlacedModel m1 = createModel("M1", 100, 100, 100, 300, 300, 300);
            PlacedModel m2 = createModel("M2", 200, 200, 200, 400, 400, 400);
            PlacedModel m3 = createModel("M3", 250, 250, 250, 450, 450, 450);

            octree.insert(m1);
            octree.insert(m2);
            octree.insert(m3);

            BoundingBox queryRegion = new BoundingBox(
                new Vector3D(250, 250, 250),
                new Vector3D(300, 300, 300)
            );
            List<PlacedModel> results = octree.query(queryRegion);

            assertEquals(3, results.size());
            assertTrue(results.contains(m1));
            assertTrue(results.contains(m2));
            assertTrue(results.contains(m3));
        }
    }

    @Nested
    @DisplayName("辅助方法测试")
    class UtilityMethodTests {

        @Test
        @DisplayName("getAllModels返回所有模型")
        void testGetAllModels() {
            PlacedModel m1 = createModel("M1", 100, 100, 100, 200, 200, 200);
            PlacedModel m2 = createModel("M2", 300, 300, 300, 400, 400, 400);
            PlacedModel m3 = createModel("M3", 500, 500, 500, 600, 600, 600);

            octree.insert(m1);
            octree.insert(m2);
            octree.insert(m3);

            List<PlacedModel> allModels = octree.getAllModels();

            assertEquals(3, allModels.size());
            assertTrue(allModels.contains(m1));
            assertTrue(allModels.contains(m2));
            assertTrue(allModels.contains(m3));
        }

        @Test
        @DisplayName("getAllModels返回不可修改列表")
        void testGetAllModelsUnmodifiable() {
            octree.insert(createModel("M1", 100, 100, 100, 200, 200, 200));

            List<PlacedModel> allModels = octree.getAllModels();

            assertThrows(UnsupportedOperationException.class, () -> {
                allModels.add(createModel("M2", 300, 300, 300, 400, 400, 400));
            });
        }

        @Test
        @DisplayName("toString返回正确格式")
        void testToString() {
            String result = octree.toString();

            assertNotNull(result);
            assertTrue(result.contains("Octree"));
            assertTrue(result.contains("depth"));
        }
    }

    /**
     * 辅助方法：创建测试用的PlacedModel。
     *
     * <p>构建一个顶点从(0,0,0)到(dx,dy,dz)的完整封闭立方体网格（12个三角形），
     * 质心固定在(dx/2, dy/2, dz/2)，然后以目标AABB中心为position放置，
     * 使transformedAABB精确落在(minX,minY,minZ)~(maxX,maxY,maxZ)。
     * 原先只用一个三角形的退化夹具会导致质心退化到原点、z厚度为0，
     * 导致AABB偏移错误，大量insert超出工作空间边界而失败。</p>
     */
    private PlacedModel createModel(String id, float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ) {
        float dx = Math.max(maxX - minX, 1.0f);
        float dy = Math.max(maxY - minY, 1.0f);
        float dz = Math.max(maxZ - minZ, 1.0f);

        // 原点对齐的完整立方体（12个三角形，质心 = (dx/2, dy/2, dz/2)）
        Vector3D v000 = new Vector3D(0,  0,  0);
        Vector3D v100 = new Vector3D(dx, 0,  0);
        Vector3D v110 = new Vector3D(dx, dy, 0);
        Vector3D v010 = new Vector3D(0,  dy, 0);
        Vector3D v001 = new Vector3D(0,  0,  dz);
        Vector3D v101 = new Vector3D(dx, 0,  dz);
        Vector3D v111 = new Vector3D(dx, dy, dz);
        Vector3D v011 = new Vector3D(0,  dy, dz);

        List<Triangle> triangles = new java.util.ArrayList<>();
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

        Mesh mesh = new Mesh(triangles, id + ".stl");
        ModelInfo modelInfo = new ModelInfo(id + ".stl", mesh, 0);

        // 目标AABB中心作为position，使transformedAABB = (minX..maxX, minY..maxY, minZ..maxZ)
        Vector3D center = new Vector3D(minX + dx / 2f, minY + dy / 2f, minZ + dz / 2f);
        return PlacedModel.create(modelInfo, Matrix3x3.IDENTITY, center);
    }
}
