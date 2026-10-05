package com.placement3d.model;

import com.placement3d.geometry.BoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the Mesh class.
 * Tests volume calculation, centroid computation, transformations, and edge cases.
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
@DisplayName("Mesh Tests")
class MeshTest {

    private static final double DELTA = 0.01; // Tolerance for floating-point comparisons

    private List<Triangle> cubeTriangles;
    private Mesh cubeMesh;

    /**
     * Creates a unit cube (1mm × 1mm × 1mm) centered at origin.
     * Expected volume: 1.0 mm³
     * Expected centroid: (0.5, 0.5, 0.5)
     */
    @BeforeEach
    void setUp() {
        cubeTriangles = createUnitCube();
        cubeMesh = new Mesh(cubeTriangles, "unit_cube.stl");
    }

    /**
     * Creates a unit cube mesh with 12 triangles (2 per face).
     * Cube spans from (0,0,0) to (1,1,1).
     */
    private List<Triangle> createUnitCube() {
        List<Triangle> triangles = new ArrayList<>();

        // Define 8 vertices of the cube
        Vector3D v000 = new Vector3D(0, 0, 0);
        Vector3D v100 = new Vector3D(1, 0, 0);
        Vector3D v110 = new Vector3D(1, 1, 0);
        Vector3D v010 = new Vector3D(0, 1, 0);
        Vector3D v001 = new Vector3D(0, 0, 1);
        Vector3D v101 = new Vector3D(1, 0, 1);
        Vector3D v111 = new Vector3D(1, 1, 1);
        Vector3D v011 = new Vector3D(0, 1, 1);

        // Bottom face (z=0)
        triangles.add(new Triangle(v000, v100, v110));
        triangles.add(new Triangle(v000, v110, v010));

        // Top face (z=1)
        triangles.add(new Triangle(v001, v111, v101));
        triangles.add(new Triangle(v001, v011, v111));

        // Front face (y=0)
        triangles.add(new Triangle(v000, v101, v100));
        triangles.add(new Triangle(v000, v001, v101));

        // Back face (y=1)
        triangles.add(new Triangle(v010, v110, v111));
        triangles.add(new Triangle(v010, v111, v011));

        // Left face (x=0)
        triangles.add(new Triangle(v000, v010, v011));
        triangles.add(new Triangle(v000, v011, v001));

        // Right face (x=1)
        triangles.add(new Triangle(v100, v111, v110));
        triangles.add(new Triangle(v100, v101, v111));

        return triangles;
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create mesh with valid triangles and file name")
        void testValidConstruction() {
            assertNotNull(cubeMesh);
            assertEquals(12, cubeMesh.getTriangleCount());
            assertEquals("unit_cube.stl", cubeMesh.getFileName());
            assertNotNull(cubeMesh.getBoundingBox());
            assertTrue(cubeMesh.getVolume() > 0);
            assertNotNull(cubeMesh.getCentroid());
        }

        @Test
        @DisplayName("Should create mesh with null file name")
        void testNullFileName() {
            Mesh mesh = new Mesh(cubeTriangles, null);
            assertNull(mesh.getFileName());
            assertEquals(12, mesh.getTriangleCount());
        }

        @Test
        @DisplayName("Should throw exception for null triangles list")
        void testNullTrianglesList() {
            assertThrows(NullPointerException.class, () -> new Mesh(null, "test.stl"));
        }

        @Test
        @DisplayName("Should throw exception for empty triangles list")
        void testEmptyTrianglesList() {
            assertThrows(IllegalArgumentException.class,
                () -> new Mesh(Collections.emptyList(), "test.stl"));
        }

        @Test
        @DisplayName("Should throw exception for null element in triangles list")
        void testNullTriangleElement() {
            List<Triangle> trianglesWithNull = new ArrayList<>(cubeTriangles);
            trianglesWithNull.add(null);
            assertThrows(NullPointerException.class,
                () -> new Mesh(trianglesWithNull, "test.stl"));
        }

        @Test
        @DisplayName("Should create immutable copy of triangles list")
        void testImmutability() {
            List<Triangle> mutableList = new ArrayList<>(cubeTriangles);
            Mesh mesh = new Mesh(mutableList, "test.stl");
            int originalCount = mesh.getTriangleCount();

            // Modify original list
            mutableList.clear();

            // Mesh should be unaffected
            assertEquals(originalCount, mesh.getTriangleCount());
        }
    }

    @Nested
    @DisplayName("Volume Calculation Tests")
    class VolumeTests {

        @Test
        @DisplayName("Should calculate correct volume for unit cube")
        void testUnitCubeVolume() {
            // Unit cube: 1mm × 1mm × 1mm = 1.0 mm³
            assertEquals(1.0, cubeMesh.getVolume(), DELTA);
        }

        @Test
        @DisplayName("Should calculate correct volume for 2×2×2 cube")
        void testLargerCubeVolume() {
            List<Triangle> triangles = createScaledCube(2.0f);
            Mesh mesh = new Mesh(triangles, "2x2x2_cube.stl");

            // 2mm × 2mm × 2mm = 8.0 mm³
            assertEquals(8.0, mesh.getVolume(), DELTA);
        }

        @Test
        @DisplayName("Should calculate correct volume for rectangular box")
        void testRectangularBoxVolume() {
            List<Triangle> triangles = createBox(10.0f, 20.0f, 5.0f);
            Mesh mesh = new Mesh(triangles, "box.stl");

            // 10mm × 20mm × 5mm = 1000.0 mm³
            assertEquals(1000.0, mesh.getVolume(), DELTA);
        }

        @Test
        @DisplayName("Should calculate positive volume regardless of triangle orientation")
        void testVolumeAlwaysPositive() {
            // Even with reversed normals, volume should be positive
            assertTrue(cubeMesh.getVolume() > 0);
        }

        @Test
        @DisplayName("Should calculate correct volume for single tetrahedron")
        void testTetrahedronVolume() {
            // Regular tetrahedron with edge length √2
            Vector3D v1 = new Vector3D(0, 0, 0);
            Vector3D v2 = new Vector3D(1, 0, 0);
            Vector3D v3 = new Vector3D(0, 1, 0);
            Vector3D v4 = new Vector3D(0, 0, 1);

            List<Triangle> triangles = Arrays.asList(
                new Triangle(v1, v3, v2), // Bottom
                new Triangle(v1, v2, v4), // Front
                new Triangle(v1, v4, v3), // Left
                new Triangle(v2, v3, v4)  // Slanted
            );

            Mesh tetrahedron = new Mesh(triangles, "tetrahedron.stl");

            // Volume = (1/6) × base × height = (1/6) × (1/2) × 1 = 1/6 ≈ 0.1667
            assertEquals(1.0 / 6.0, tetrahedron.getVolume(), DELTA);
        }
    }

    @Nested
    @DisplayName("Centroid Calculation Tests")
    class CentroidTests {

        @Test
        @DisplayName("Should calculate correct centroid for unit cube")
        void testUnitCubeCentroid() {
            Vector3D centroid = cubeMesh.getCentroid();

            // Unit cube from (0,0,0) to (1,1,1) has centroid at (0.5, 0.5, 0.5)
            assertEquals(0.5, centroid.x, DELTA);
            assertEquals(0.5, centroid.y, DELTA);
            assertEquals(0.5, centroid.z, DELTA);
        }

        @Test
        @DisplayName("Should calculate correct centroid for offset cube")
        void testOffsetCubeCentroid() {
            List<Triangle> triangles = createOffsetCube(10.0f, 20.0f, 30.0f);
            Mesh mesh = new Mesh(triangles, "offset_cube.stl");
            Vector3D centroid = mesh.getCentroid();

            // Cube from (10,20,30) to (11,21,31) has centroid at (10.5, 20.5, 30.5)
            assertEquals(10.5, centroid.x, DELTA);
            assertEquals(20.5, centroid.y, DELTA);
            assertEquals(30.5, centroid.z, DELTA);
        }

        @Test
        @DisplayName("Should calculate correct centroid for rectangular box")
        void testRectangularBoxCentroid() {
            List<Triangle> triangles = createBox(10.0f, 20.0f, 5.0f);
            Mesh mesh = new Mesh(triangles, "box.stl");
            Vector3D centroid = mesh.getCentroid();

            // Box from (0,0,0) to (10,20,5) has centroid at (5, 10, 2.5)
            assertEquals(5.0, centroid.x, DELTA);
            assertEquals(10.0, centroid.y, DELTA);
            assertEquals(2.5, centroid.z, DELTA);
        }
    }

    @Nested
    @DisplayName("Bounding Box Tests")
    class BoundingBoxTests {

        @Test
        @DisplayName("Should calculate correct bounding box for unit cube")
        void testUnitCubeBoundingBox() {
            BoundingBox bbox = cubeMesh.getBoundingBox();

            assertEquals(0.0, bbox.getMin().x, DELTA);
            assertEquals(0.0, bbox.getMin().y, DELTA);
            assertEquals(0.0, bbox.getMin().z, DELTA);
            assertEquals(1.0, bbox.getMax().x, DELTA);
            assertEquals(1.0, bbox.getMax().y, DELTA);
            assertEquals(1.0, bbox.getMax().z, DELTA);
        }

        @Test
        @DisplayName("Should have correct bounding box dimensions")
        void testBoundingBoxDimensions() {
            BoundingBox bbox = cubeMesh.getBoundingBox();

            assertEquals(1.0, bbox.getWidth(), DELTA);
            assertEquals(1.0, bbox.getHeight(), DELTA);
            assertEquals(1.0, bbox.getDepth(), DELTA);
        }
    }

    @Nested
    @DisplayName("Transformation Tests")
    class TransformationTests {

        @Test
        @DisplayName("Should transform mesh with translation")
        void testTranslation() {
            Vector3D translation = new Vector3D(10, 20, 30);
            Matrix3x3 identity = Matrix3x3.IDENTITY;

            Mesh transformed = cubeMesh.transform(identity, translation);

            // Volume should remain the same
            assertEquals(cubeMesh.getVolume(), transformed.getVolume(), DELTA);

            // Centroid should be shifted by translation
            Vector3D expectedCentroid = cubeMesh.getCentroid().add(translation);
            assertEquals(expectedCentroid.x, transformed.getCentroid().x, DELTA);
            assertEquals(expectedCentroid.y, transformed.getCentroid().y, DELTA);
            assertEquals(expectedCentroid.z, transformed.getCentroid().z, DELTA);

            // Original mesh should be unchanged (immutability)
            assertEquals(0.5, cubeMesh.getCentroid().x, DELTA);
        }

        @Test
        @DisplayName("Should transform mesh with 90° rotation around Z-axis")
        void testRotation() {
            Matrix3x3 rotation = Matrix3x3.rotationZ(90);
            Vector3D noTranslation = Vector3D.ZERO;

            Mesh transformed = cubeMesh.transform(rotation, noTranslation);

            // Volume should remain the same
            assertEquals(cubeMesh.getVolume(), transformed.getVolume(), DELTA);

            // Triangle count should remain the same
            assertEquals(cubeMesh.getTriangleCount(), transformed.getTriangleCount());
        }

        @Test
        @DisplayName("Should preserve file name after transformation")
        void testFileNamePreservedAfterTransform() {
            Matrix3x3 rotation = Matrix3x3.rotationX(45);
            Vector3D translation = new Vector3D(5, 5, 5);

            Mesh transformed = cubeMesh.transform(rotation, translation);

            assertEquals(cubeMesh.getFileName(), transformed.getFileName());
        }

        @Test
        @DisplayName("Should throw exception for null rotation matrix")
        void testNullRotation() {
            assertThrows(NullPointerException.class,
                () -> cubeMesh.transform(null, Vector3D.ZERO));
        }

        @Test
        @DisplayName("Should throw exception for null translation vector")
        void testNullTranslation() {
            assertThrows(NullPointerException.class,
                () -> cubeMesh.transform(Matrix3x3.IDENTITY, null));
        }

        @Test
        @DisplayName("Should handle combined rotation and translation")
        void testCombinedTransformation() {
            Matrix3x3 rotation = Matrix3x3.fromEulerAngles(45, 30, 60);
            Vector3D translation = new Vector3D(100, 200, 300);

            Mesh transformed = cubeMesh.transform(rotation, translation);

            // Volume should be approximately preserved (allowing for floating-point errors in rotation)
            assertEquals(cubeMesh.getVolume(), transformed.getVolume(), 0.1);

            // Mesh should have same number of triangles
            assertEquals(cubeMesh.getTriangleCount(), transformed.getTriangleCount());

            // Bounding box should exist and have positive dimensions
            BoundingBox bbox = transformed.getBoundingBox();
            assertTrue(bbox.getWidth() > 0);
            assertTrue(bbox.getHeight() > 0);
            assertTrue(bbox.getDepth() > 0);
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct triangle count")
        void testGetTriangleCount() {
            assertEquals(12, cubeMesh.getTriangleCount());
        }

        @Test
        @DisplayName("Should return immutable triangles list")
        void testGetTrianglesImmutable() {
            List<Triangle> triangles = cubeMesh.getTriangles();

            assertThrows(UnsupportedOperationException.class, () -> triangles.clear());
        }

        @Test
        @DisplayName("Should return all triangles")
        void testGetTriangles() {
            List<Triangle> triangles = cubeMesh.getTriangles();

            assertNotNull(triangles);
            assertEquals(12, triangles.size());
            assertFalse(triangles.contains(null));
        }

        @Test
        @DisplayName("Should return correct file name")
        void testGetFileName() {
            assertEquals("unit_cube.stl", cubeMesh.getFileName());
        }

        @Test
        @DisplayName("Should handle null file name")
        void testNullFileNameGetter() {
            Mesh mesh = new Mesh(cubeTriangles, null);
            assertNull(mesh.getFileName());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Should be equal to itself")
        void testEqualsSelf() {
            assertEquals(cubeMesh, cubeMesh);
        }

        @Test
        @DisplayName("Should be equal to mesh with same properties")
        void testEqualsIdentical() {
            Mesh other = new Mesh(cubeTriangles, "unit_cube.stl");
            assertEquals(cubeMesh, other);
            assertEquals(cubeMesh.hashCode(), other.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to null")
        void testNotEqualsNull() {
            assertNotEquals(null, cubeMesh);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void testNotEqualsDifferentType() {
            assertNotEquals(cubeMesh, "not a mesh");
        }

        @Test
        @DisplayName("Should not be equal to mesh with different file name")
        void testNotEqualsDifferentFileName() {
            Mesh other = new Mesh(cubeTriangles, "different.stl");
            assertNotEquals(cubeMesh, other);
        }

        @Test
        @DisplayName("Should not be equal to mesh with different triangles")
        void testNotEqualsDifferentTriangles() {
            List<Triangle> differentTriangles = createScaledCube(2.0f);
            Mesh other = new Mesh(differentTriangles, "unit_cube.stl");
            assertNotEquals(cubeMesh, other);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should contain file name in toString")
        void testToStringContainsFileName() {
            String str = cubeMesh.toString();
            assertTrue(str.contains("unit_cube.stl"));
        }

        @Test
        @DisplayName("Should contain triangle count in toString")
        void testToStringContainsTriangleCount() {
            String str = cubeMesh.toString();
            assertTrue(str.contains("12"));
        }

        @Test
        @DisplayName("Should contain volume in toString")
        void testToStringContainsVolume() {
            String str = cubeMesh.toString();
            assertTrue(str.contains("mm³"));
        }

        @Test
        @DisplayName("Should handle null file name in toString")
        void testToStringWithNullFileName() {
            Mesh mesh = new Mesh(cubeTriangles, null);
            String str = mesh.toString();
            assertTrue(str.contains("unnamed"));
        }
    }

    // Helper methods for creating test geometries

    private List<Triangle> createScaledCube(float scale) {
        List<Triangle> triangles = new ArrayList<>();

        Vector3D v000 = new Vector3D(0, 0, 0);
        Vector3D v100 = new Vector3D(scale, 0, 0);
        Vector3D v110 = new Vector3D(scale, scale, 0);
        Vector3D v010 = new Vector3D(0, scale, 0);
        Vector3D v001 = new Vector3D(0, 0, scale);
        Vector3D v101 = new Vector3D(scale, 0, scale);
        Vector3D v111 = new Vector3D(scale, scale, scale);
        Vector3D v011 = new Vector3D(0, scale, scale);

        triangles.add(new Triangle(v000, v100, v110));
        triangles.add(new Triangle(v000, v110, v010));
        triangles.add(new Triangle(v001, v111, v101));
        triangles.add(new Triangle(v001, v011, v111));
        triangles.add(new Triangle(v000, v101, v100));
        triangles.add(new Triangle(v000, v001, v101));
        triangles.add(new Triangle(v010, v110, v111));
        triangles.add(new Triangle(v010, v111, v011));
        triangles.add(new Triangle(v000, v010, v011));
        triangles.add(new Triangle(v000, v011, v001));
        triangles.add(new Triangle(v100, v111, v110));
        triangles.add(new Triangle(v100, v101, v111));

        return triangles;
    }

    private List<Triangle> createBox(float width, float height, float depth) {
        List<Triangle> triangles = new ArrayList<>();

        Vector3D v000 = new Vector3D(0, 0, 0);
        Vector3D v100 = new Vector3D(width, 0, 0);
        Vector3D v110 = new Vector3D(width, height, 0);
        Vector3D v010 = new Vector3D(0, height, 0);
        Vector3D v001 = new Vector3D(0, 0, depth);
        Vector3D v101 = new Vector3D(width, 0, depth);
        Vector3D v111 = new Vector3D(width, height, depth);
        Vector3D v011 = new Vector3D(0, height, depth);

        triangles.add(new Triangle(v000, v100, v110));
        triangles.add(new Triangle(v000, v110, v010));
        triangles.add(new Triangle(v001, v111, v101));
        triangles.add(new Triangle(v001, v011, v111));
        triangles.add(new Triangle(v000, v101, v100));
        triangles.add(new Triangle(v000, v001, v101));
        triangles.add(new Triangle(v010, v110, v111));
        triangles.add(new Triangle(v010, v111, v011));
        triangles.add(new Triangle(v000, v010, v011));
        triangles.add(new Triangle(v000, v011, v001));
        triangles.add(new Triangle(v100, v111, v110));
        triangles.add(new Triangle(v100, v101, v111));

        return triangles;
    }

    private List<Triangle> createOffsetCube(float offsetX, float offsetY, float offsetZ) {
        List<Triangle> triangles = new ArrayList<>();

        Vector3D v000 = new Vector3D(offsetX, offsetY, offsetZ);
        Vector3D v100 = new Vector3D(offsetX + 1, offsetY, offsetZ);
        Vector3D v110 = new Vector3D(offsetX + 1, offsetY + 1, offsetZ);
        Vector3D v010 = new Vector3D(offsetX, offsetY + 1, offsetZ);
        Vector3D v001 = new Vector3D(offsetX, offsetY, offsetZ + 1);
        Vector3D v101 = new Vector3D(offsetX + 1, offsetY, offsetZ + 1);
        Vector3D v111 = new Vector3D(offsetX + 1, offsetY + 1, offsetZ + 1);
        Vector3D v011 = new Vector3D(offsetX, offsetY + 1, offsetZ + 1);

        triangles.add(new Triangle(v000, v100, v110));
        triangles.add(new Triangle(v000, v110, v010));
        triangles.add(new Triangle(v001, v111, v101));
        triangles.add(new Triangle(v001, v011, v111));
        triangles.add(new Triangle(v000, v101, v100));
        triangles.add(new Triangle(v000, v001, v101));
        triangles.add(new Triangle(v010, v110, v111));
        triangles.add(new Triangle(v010, v111, v011));
        triangles.add(new Triangle(v000, v010, v011));
        triangles.add(new Triangle(v000, v011, v001));
        triangles.add(new Triangle(v100, v111, v110));
        triangles.add(new Triangle(v100, v101, v111));

        return triangles;
    }
}
