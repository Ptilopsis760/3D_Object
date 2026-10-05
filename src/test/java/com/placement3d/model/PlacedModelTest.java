package com.placement3d.model;

import com.placement3d.geometry.BoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PlacedModel类的单元测试。
 *
 * <p>测试覆盖范围包括：
 * <ul>
 *   <li>工厂方法创建（正常场景、边界情况）</li>
 *   <li>空值验证（model、rotation、position为null）</li>
 *   <li>变换后AABB的正确性（无旋转、有旋转、平移）</li>
 *   <li>Getter方法</li>
 *   <li>equals()和hashCode()契约</li>
 *   <li>toString()格式</li>
 *   <li>不可变性验证</li>
 *   <li>复杂变换场景（旋转+平移）</li>
 * </ul>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
@DisplayName("PlacedModel单元测试")
class PlacedModelTest {

    private ModelInfo cubeModel;
    private ModelInfo rectangleModel;

    @BeforeEach
    void setUp() {
        // 创建一个简单的立方体模型：1x1x1 mm，从(0,0,0)到(1,1,1)
        // 质心在(0.5, 0.5, 0.5)
        List<Triangle> cubeTriangles = Arrays.asList(
            // 底面 (z=0) - 2个三角形
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(1, 0, 0), new Vector3D(1, 1, 0)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(1, 1, 0), new Vector3D(0, 1, 0)),
            // 顶面 (z=1) - 2个三角形
            new Triangle(new Vector3D(0, 0, 1), new Vector3D(1, 1, 1), new Vector3D(1, 0, 1)),
            new Triangle(new Vector3D(0, 0, 1), new Vector3D(0, 1, 1), new Vector3D(1, 1, 1)),
            // 前面 (y=0) - 2个三角形
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(1, 0, 1), new Vector3D(1, 0, 0)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0, 0, 1), new Vector3D(1, 0, 1)),
            // 后面 (y=1) - 2个三角形
            new Triangle(new Vector3D(0, 1, 0), new Vector3D(1, 1, 0), new Vector3D(1, 1, 1)),
            new Triangle(new Vector3D(0, 1, 0), new Vector3D(1, 1, 1), new Vector3D(0, 1, 1)),
            // 左面 (x=0) - 2个三角形
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0, 1, 0), new Vector3D(0, 1, 1)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0, 1, 1), new Vector3D(0, 0, 1)),
            // 右面 (x=1) - 2个三角形
            new Triangle(new Vector3D(1, 0, 0), new Vector3D(1, 1, 1), new Vector3D(1, 1, 0)),
            new Triangle(new Vector3D(1, 0, 0), new Vector3D(1, 0, 1), new Vector3D(1, 1, 1))
        );
        Mesh cubeMesh = new Mesh(cubeTriangles, "cube.stl");
        cubeModel = new ModelInfo("cube.stl", cubeMesh, 0);

        // 创建一个长方体模型：2x1x1 mm，从(0,0,0)到(2,1,1)
        // 质心在(1.0, 0.5, 0.5)
        List<Triangle> rectangleTriangles = Arrays.asList(
            // 底面 (z=0)
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(2, 0, 0), new Vector3D(2, 1, 0)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(2, 1, 0), new Vector3D(0, 1, 0)),
            // 顶面 (z=1)
            new Triangle(new Vector3D(0, 0, 1), new Vector3D(2, 1, 1), new Vector3D(2, 0, 1)),
            new Triangle(new Vector3D(0, 0, 1), new Vector3D(0, 1, 1), new Vector3D(2, 1, 1)),
            // 前面 (y=0)
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(2, 0, 1), new Vector3D(2, 0, 0)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0, 0, 1), new Vector3D(2, 0, 1)),
            // 后面 (y=1)
            new Triangle(new Vector3D(0, 1, 0), new Vector3D(2, 1, 0), new Vector3D(2, 1, 1)),
            new Triangle(new Vector3D(0, 1, 0), new Vector3D(2, 1, 1), new Vector3D(0, 1, 1)),
            // 左面 (x=0)
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0, 1, 0), new Vector3D(0, 1, 1)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0, 1, 1), new Vector3D(0, 0, 1)),
            // 右面 (x=2)
            new Triangle(new Vector3D(2, 0, 0), new Vector3D(2, 1, 1), new Vector3D(2, 1, 0)),
            new Triangle(new Vector3D(2, 0, 0), new Vector3D(2, 0, 1), new Vector3D(2, 1, 1))
        );
        Mesh rectangleMesh = new Mesh(rectangleTriangles, "rectangle.stl");
        rectangleModel = new ModelInfo("rectangle.stl", rectangleMesh, 1);
    }

    // ==================== 测试1-3：工厂方法创建 ====================

    @Test
    @DisplayName("测试1：使用单位旋转矩阵和零位置创建PlacedModel")
    void testCreateWithIdentityRotationAndZeroPosition() {
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            Vector3D.ZERO
        );

        assertNotNull(placed);
        assertEquals(cubeModel, placed.model);
        assertEquals(Matrix3x3.IDENTITY, placed.rotationMatrix);
        assertEquals(Vector3D.ZERO, placed.position);
        assertNotNull(placed.transformedAABB);
    }

    @Test
    @DisplayName("测试2：使用单位旋转矩阵和非零位置创建PlacedModel")
    void testCreateWithIdentityRotationAndNonZeroPosition() {
        Vector3D position = new Vector3D(100, 200, 50);
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            position
        );

        assertNotNull(placed);
        assertEquals(position, placed.position);

        // 验证transformedAABB的位置正确
        // 立方体质心在(0.5, 0.5, 0.5)，移到(100, 200, 50)
        // 偏移量 = (100, 200, 50) - (0.5, 0.5, 0.5) = (99.5, 199.5, 49.5)
        // 原始AABB: min(0, 0, 0), max(1, 1, 1)
        // 变换后AABB: min(99.5, 199.5, 49.5), max(100.5, 200.5, 50.5)
        Vector3D expectedMin = new Vector3D(99.5f, 199.5f, 49.5f);
        Vector3D expectedMax = new Vector3D(100.5f, 200.5f, 50.5f);

        assertEquals(expectedMin, placed.transformedAABB.getMin());
        assertEquals(expectedMax, placed.transformedAABB.getMax());
    }

    @Test
    @DisplayName("测试3：使用非单位旋转矩阵创建PlacedModel")
    void testCreateWithRotation() {
        Matrix3x3 rotation = Matrix3x3.rotationZ(90); // 绕Z轴旋转90度
        Vector3D position = new Vector3D(10, 20, 30);

        PlacedModel placed = PlacedModel.create(cubeModel, rotation, position);

        assertNotNull(placed);
        assertEquals(rotation, placed.rotationMatrix);
        assertEquals(position, placed.position);
        assertNotNull(placed.transformedAABB);
    }

    // ==================== 测试4-6：空值验证 ====================

    @Test
    @DisplayName("测试4：model为null时抛出NullPointerException")
    void testCreateWithNullModel() {
        assertThrows(NullPointerException.class, () -> {
            PlacedModel.create(null, Matrix3x3.IDENTITY, Vector3D.ZERO);
        });
    }

    @Test
    @DisplayName("测试5：rotation为null时抛出NullPointerException")
    void testCreateWithNullRotation() {
        assertThrows(NullPointerException.class, () -> {
            PlacedModel.create(cubeModel, null, Vector3D.ZERO);
        });
    }

    @Test
    @DisplayName("测试6：position为null时抛出NullPointerException")
    void testCreateWithNullPosition() {
        assertThrows(NullPointerException.class, () -> {
            PlacedModel.create(cubeModel, Matrix3x3.IDENTITY, null);
        });
    }

    // ==================== 测试7-10：Getter方法 ====================

    @Test
    @DisplayName("测试7：getModel()返回正确的ModelInfo")
    void testGetModel() {
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            Vector3D.ZERO
        );

        assertEquals(cubeModel, placed.getModel());
        assertSame(cubeModel, placed.model); // 验证直接字段访问
    }

    @Test
    @DisplayName("测试8：getRotationMatrix()返回正确的旋转矩阵")
    void testGetRotationMatrix() {
        Matrix3x3 rotation = Matrix3x3.rotationX(45);
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            rotation,
            Vector3D.ZERO
        );

        assertEquals(rotation, placed.getRotationMatrix());
        assertSame(rotation, placed.rotationMatrix);
    }

    @Test
    @DisplayName("测试9：getPosition()返回正确的位置")
    void testGetPosition() {
        Vector3D position = new Vector3D(50, 75, 100);
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            position
        );

        assertEquals(position, placed.getPosition());
        assertSame(position, placed.position);
    }

    @Test
    @DisplayName("测试10：getTransformedAABB()返回正确的变换后AABB")
    void testGetTransformedAABB() {
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            Vector3D.ZERO
        );

        assertNotNull(placed.getTransformedAABB());
        assertSame(placed.transformedAABB, placed.getTransformedAABB());
    }

    // ==================== 测试11-13：transformedAABB计算正确性 ====================

    @Test
    @DisplayName("测试11：无旋转无平移时，transformedAABB与原始AABB一致")
    void testTransformedAABBWithNoTransformation() {
        // 将模型放在其质心位置（相当于没有移动）
        Vector3D centerOfMass = cubeModel.getCenterOfMass();
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            centerOfMass
        );

        // transformedAABB应该与原始AABB相同
        BoundingBox originalAABB = cubeModel.getOriginalAABB();
        BoundingBox transformedAABB = placed.transformedAABB;

        assertEquals(originalAABB.getMin(), transformedAABB.getMin());
        assertEquals(originalAABB.getMax(), transformedAABB.getMax());
    }

    @Test
    @DisplayName("测试12：纯平移后，AABB正确平移")
    void testTransformedAABBWithPureTranslation() {
        // 立方体：AABB从(0,0,0)到(1,1,1)，质心在(0.5, 0.5, 0.5)
        // 移动到位置(100, 200, 300)
        Vector3D position = new Vector3D(100, 200, 300);
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            position
        );

        // 偏移量 = (100, 200, 300) - (0.5, 0.5, 0.5) = (99.5, 199.5, 299.5)
        // 新AABB: min(99.5, 199.5, 299.5), max(100.5, 200.5, 300.5)
        Vector3D expectedMin = new Vector3D(99.5f, 199.5f, 299.5f);
        Vector3D expectedMax = new Vector3D(100.5f, 200.5f, 300.5f);

        assertEquals(expectedMin, placed.transformedAABB.getMin());
        assertEquals(expectedMax, placed.transformedAABB.getMax());
    }

    @Test
    @DisplayName("测试13：旋转90度后，AABB尺寸发生变化（长方体）")
    void testTransformedAABBWithRotation() {
        // 长方体：2x1x1 mm，原始AABB从(0,0,0)到(2,1,1)
        // 绕Z轴旋转90度后，宽度和高度交换
        Matrix3x3 rotation = Matrix3x3.rotationZ(90);
        Vector3D centerOfMass = rectangleModel.getCenterOfMass(); // (1.0, 0.5, 0.5)

        PlacedModel placed = PlacedModel.create(
            rectangleModel,
            rotation,
            centerOfMass // 放在原位置
        );

        BoundingBox transformedAABB = placed.transformedAABB;

        // 旋转90度后，X方向的2mm变成Y方向，Y方向的1mm变成X方向
        // 由于是轴对齐包围盒，旋转后尺寸应该是1x2x1（宽x高x深）
        float width = transformedAABB.getWidth();
        float height = transformedAABB.getHeight();
        float depth = transformedAABB.getDepth();

        // 验证旋转后尺寸发生了变化
        assertEquals(1.0f, width, 0.01f);
        assertEquals(2.0f, height, 0.01f);
        assertEquals(1.0f, depth, 0.01f);
    }

    // ==================== 测试14-17：equals和hashCode ====================

    @Test
    @DisplayName("测试14：相同对象equals返回true")
    void testEqualsSameObject() {
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            Vector3D.ZERO
        );

        assertEquals(placed, placed);
    }

    @Test
    @DisplayName("测试15：相同内容的不同对象equals返回true")
    void testEqualsEqualContent() {
        Matrix3x3 rotation = Matrix3x3.rotationY(45);
        Vector3D position = new Vector3D(10, 20, 30);

        PlacedModel placed1 = PlacedModel.create(cubeModel, rotation, position);
        PlacedModel placed2 = PlacedModel.create(cubeModel, rotation, position);

        assertEquals(placed1, placed2);
        assertEquals(placed1.hashCode(), placed2.hashCode());
    }

    @Test
    @DisplayName("测试16：不同内容的对象equals返回false")
    void testEqualsUnequalContent() {
        PlacedModel placed1 = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            new Vector3D(10, 20, 30)
        );

        PlacedModel placed2 = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            new Vector3D(40, 50, 60) // 不同位置
        );

        assertNotEquals(placed1, placed2);
    }

    @Test
    @DisplayName("测试17：equals与null和不同类型对象比较")
    void testEqualsWithNullAndDifferentType() {
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            Vector3D.ZERO
        );

        assertNotEquals(placed, null);
        assertNotEquals(placed, "not a PlacedModel");
        assertNotEquals(placed, new Object());
    }

    // ==================== 测试18-20：toString和不可变性 ====================

    @Test
    @DisplayName("测试18：toString包含关键信息")
    void testToString() {
        Vector3D position = new Vector3D(100, 200, 300);
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            position
        );

        String str = placed.toString();

        assertNotNull(str);
        assertTrue(str.contains("PlacedModel"));
        assertTrue(str.contains("cube.stl"));
        assertTrue(str.contains("100.00")); // 位置信息
    }

    @Test
    @DisplayName("测试19：不可变性验证 - 字段是final的")
    void testImmutability() {
        PlacedModel placed = PlacedModel.create(
            cubeModel,
            Matrix3x3.IDENTITY,
            Vector3D.ZERO
        );

        // 验证所有字段都是public final（通过反射）
        try {
            java.lang.reflect.Field modelField = PlacedModel.class.getField("model");
            java.lang.reflect.Field rotationField = PlacedModel.class.getField("rotationMatrix");
            java.lang.reflect.Field positionField = PlacedModel.class.getField("position");
            java.lang.reflect.Field aabbField = PlacedModel.class.getField("transformedAABB");

            assertTrue(java.lang.reflect.Modifier.isFinal(modelField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(rotationField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(positionField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isFinal(aabbField.getModifiers()));

            assertTrue(java.lang.reflect.Modifier.isPublic(modelField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isPublic(rotationField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isPublic(positionField.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isPublic(aabbField.getModifiers()));
        } catch (NoSuchFieldException e) {
            fail("应该有public final字段: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试20：复杂变换场景 - 旋转+平移")
    void testComplexTransformation() {
        // 绕Y轴旋转45度，然后移到(500, 600, 700)
        Matrix3x3 rotation = Matrix3x3.rotationY(45);
        Vector3D position = new Vector3D(500, 600, 700);

        PlacedModel placed = PlacedModel.create(rectangleModel, rotation, position);

        assertNotNull(placed.transformedAABB);

        // 验证AABB中心大致在目标位置附近
        Vector3D aabbCenter = placed.transformedAABB.getCenter();

        // 中心应该接近目标位置
        assertEquals(500.0f, aabbCenter.getX(), 5.0f);
        assertEquals(600.0f, aabbCenter.getY(), 5.0f);
        assertEquals(700.0f, aabbCenter.getZ(), 5.0f);
    }

    // ==================== 测试21-22：额外测试 ====================

    @Test
    @DisplayName("测试21：多个旋转组合")
    void testMultipleRotations() {
        // 组合旋转：X轴30度，Y轴45度，Z轴60度
        Matrix3x3 rotation = Matrix3x3.fromEulerAngles(30, 45, 60);
        Vector3D position = new Vector3D(100, 100, 100);

        PlacedModel placed = PlacedModel.create(cubeModel, rotation, position);

        assertNotNull(placed);
        assertNotNull(placed.transformedAABB);

        // 验证旋转后AABB仍然有效（min < max）
        assertTrue(placed.transformedAABB.getMin().getX() < placed.transformedAABB.getMax().getX());
        assertTrue(placed.transformedAABB.getMin().getY() < placed.transformedAABB.getMax().getY());
        assertTrue(placed.transformedAABB.getMin().getZ() < placed.transformedAABB.getMax().getZ());
    }

    @Test
    @DisplayName("测试22：不同模型的PlacedModel不相等")
    void testEqualsWithDifferentModels() {
        Vector3D position = new Vector3D(10, 20, 30);
        Matrix3x3 rotation = Matrix3x3.IDENTITY;

        PlacedModel placed1 = PlacedModel.create(cubeModel, rotation, position);
        PlacedModel placed2 = PlacedModel.create(rectangleModel, rotation, position);

        assertNotEquals(placed1, placed2);
    }
}
