package com.placement3d.model;

import com.placement3d.geometry.BoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ModelInfo}.
 *
 * <p>Test coverage includes:
 * <ul>
 *   <li>Constructor validation (null checks, empty strings, invalid IDs)</li>
 *   <li>Getter methods</li>
 *   <li>Rotated AABB computation (identity, 90° rotations, combined rotations)</li>
 *   <li>Sort key (volume-based sorting)</li>
 *   <li>Clone operation (immutability verification)</li>
 *   <li>equals() and hashCode() contracts</li>
 *   <li>toString() format</li>
 * </ul>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
class ModelInfoTest {

    private Mesh simpleCubeMesh;
    private ModelInfo cubeModelInfo;

    @BeforeEach
    void setUp() {
        // Create a simple cube mesh: 1x1x1 mm from (0,0,0) to (1,1,1)
        // Cube with 12 triangles (2 per face)
        List<Triangle> triangles = Arrays.asList(
            // Bottom face (z=0) - 2 triangles
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(1, 0, 0), new Vector3D(1, 1, 0)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(1, 1, 0), new Vector3D(0, 1, 0)),
            // Top face (z=1) - 2 triangles
            new Triangle(new Vector3D(0, 0, 1), new Vector3D(1, 1, 1), new Vector3D(1, 0, 1)),
            new Triangle(new Vector3D(0, 0, 1), new Vector3D(0, 1, 1), new Vector3D(1, 1, 1)),
            // Front face (y=0) - 2 triangles
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(1, 0, 1), new Vector3D(1, 0, 0)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0, 0, 1), new Vector3D(1, 0, 1)),
            // Back face (y=1) - 2 triangles
            new Triangle(new Vector3D(0, 1, 0), new Vector3D(1, 1, 0), new Vector3D(1, 1, 1)),
            new Triangle(new Vector3D(0, 1, 0), new Vector3D(1, 1, 1), new Vector3D(0, 1, 1)),
            // Left face (x=0) - 2 triangles
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0, 1, 0), new Vector3D(0, 1, 1)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0, 1, 1), new Vector3D(0, 0, 1)),
            // Right face (x=1) - 2 triangles
            new Triangle(new Vector3D(1, 0, 0), new Vector3D(1, 1, 1), new Vector3D(1, 1, 0)),
            new Triangle(new Vector3D(1, 0, 0), new Vector3D(1, 0, 1), new Vector3D(1, 1, 1))
        );

        simpleCubeMesh = new Mesh(triangles, "cube.stl");
        cubeModelInfo = new ModelInfo("cube.stl", simpleCubeMesh, 0);
    }

    @Test
    @DisplayName("Constructor should accept valid parameters")
    void testConstructorValid() {
        ModelInfo model = new ModelInfo("test.stl", simpleCubeMesh, 5);

        assertNotNull(model);
        assertEquals("test.stl", model.getFileName());
        assertEquals(simpleCubeMesh, model.getMesh());
        assertEquals(5, model.getModelId());
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException when fileName is null")
    void testConstructorNullFileName() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new ModelInfo(null, simpleCubeMesh, 0)
        );
        assertTrue(exception.getMessage().contains("File name"));
    }

    @Test
    @DisplayName("Constructor should throw NullPointerException when mesh is null")
    void testConstructorNullMesh() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> new ModelInfo("test.stl", null, 0)
        );
        assertTrue(exception.getMessage().contains("Mesh"));
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException when fileName is empty")
    void testConstructorEmptyFileName() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ModelInfo("", simpleCubeMesh, 0)
        );
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException when fileName is whitespace")
    void testConstructorWhitespaceFileName() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ModelInfo("   ", simpleCubeMesh, 0)
        );
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("Constructor should throw IllegalArgumentException when modelId is negative")
    void testConstructorNegativeModelId() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ModelInfo("test.stl", simpleCubeMesh, -1)
        );
        assertTrue(exception.getMessage().contains("non-negative"));
    }

    @Test
    @DisplayName("Getter methods should return correct values")
    void testGetters() {
        assertEquals("cube.stl", cubeModelInfo.getFileName());
        assertEquals(simpleCubeMesh, cubeModelInfo.getMesh());
        assertEquals(0, cubeModelInfo.getModelId());

        // Volume of 1x1x1 cube should be approximately 1.0
        assertTrue(Math.abs(cubeModelInfo.getVolume() - 1.0f) < 0.1f,
            "Expected volume ~1.0, got " + cubeModelInfo.getVolume());

        assertNotNull(cubeModelInfo.getCenterOfMass());
        assertNotNull(cubeModelInfo.getOriginalAABB());
    }

    @Test
    @DisplayName("Original AABB should match mesh bounding box")
    void testOriginalAABB() {
        BoundingBox meshBBox = simpleCubeMesh.getBoundingBox();
        BoundingBox modelBBox = cubeModelInfo.getOriginalAABB();

        assertEquals(meshBBox, modelBBox);
    }

    @Test
    @DisplayName("computeRotatedAABB should throw NullPointerException when rotation is null")
    void testComputeRotatedAABBNull() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> cubeModelInfo.computeRotatedAABB(null)
        );
        assertTrue(exception.getMessage().contains("Rotation"));
    }

    @Test
    @DisplayName("computeRotatedAABB with identity rotation should return same AABB")
    void testComputeRotatedAABBIdentity() {
        Matrix3x3 identity = Matrix3x3.IDENTITY;
        BoundingBox rotated = cubeModelInfo.computeRotatedAABB(identity);
        BoundingBox original = cubeModelInfo.getOriginalAABB();

        // Should be very close (allow small floating-point error)
        assertEquals(original.getMin().getX(), rotated.getMin().getX(), 0.001f);
        assertEquals(original.getMin().getY(), rotated.getMin().getY(), 0.001f);
        assertEquals(original.getMin().getZ(), rotated.getMin().getZ(), 0.001f);
        assertEquals(original.getMax().getX(), rotated.getMax().getX(), 0.001f);
        assertEquals(original.getMax().getY(), rotated.getMax().getY(), 0.001f);
        assertEquals(original.getMax().getZ(), rotated.getMax().getZ(), 0.001f);
    }

    @Test
    @DisplayName("computeRotatedAABB with 90° Z-rotation should swap X and Y dimensions")
    void testComputeRotatedAABB90DegreeZ() {
        Matrix3x3 rotationZ90 = Matrix3x3.rotationZ(90);
        BoundingBox original = cubeModelInfo.getOriginalAABB();
        BoundingBox rotated = cubeModelInfo.computeRotatedAABB(rotationZ90);

        // For a cube, 90° rotation around Z should swap X and Y extents
        // Original: X:[0,1], Y:[0,1], Z:[0,1]
        // After Z-90°: X:[-1,0], Y:[0,1], Z:[0,1] (approximately)
        double originalWidth = original.getWidth();
        double originalHeight = original.getHeight();
        double rotatedWidth = rotated.getWidth();
        double rotatedHeight = rotated.getHeight();

        // Width and height should remain similar (cube symmetry)
        assertEquals(originalWidth, rotatedWidth, 0.1, "Width should remain similar after Z rotation");
        assertEquals(originalHeight, rotatedHeight, 0.1, "Height should remain similar after Z rotation");
    }

    @Test
    @DisplayName("computeRotatedAABB with 90° X-rotation should swap Y and Z dimensions")
    void testComputeRotatedAABB90DegreeX() {
        Matrix3x3 rotationX90 = Matrix3x3.rotationX(90);
        BoundingBox original = cubeModelInfo.getOriginalAABB();
        BoundingBox rotated = cubeModelInfo.computeRotatedAABB(rotationX90);

        // For a cube, rotation should preserve dimensions (cube symmetry)
        double originalHeight = original.getHeight();
        double originalDepth = original.getDepth();
        double rotatedHeight = rotated.getHeight();
        double rotatedDepth = rotated.getDepth();

        assertEquals(originalHeight, rotatedDepth, 0.1, "Original height should match rotated depth");
        assertEquals(originalDepth, rotatedHeight, 0.1, "Original depth should match rotated height");
    }

    @Test
    @DisplayName("computeRotatedAABB with 45° Z-rotation should expand AABB")
    void testComputeRotatedAABB45Degree() {
        Matrix3x3 rotationZ45 = Matrix3x3.rotationZ(45);
        BoundingBox original = cubeModelInfo.getOriginalAABB();
        BoundingBox rotated = cubeModelInfo.computeRotatedAABB(rotationZ45);

        // 45° rotation of a square should expand the AABB
        // Diagonal of unit square = sqrt(2) ≈ 1.414
        double originalWidth = original.getWidth();
        double rotatedWidth = rotated.getWidth();

        assertTrue(rotatedWidth > originalWidth,
            "45° rotation should expand AABB width. Original: " + originalWidth + ", Rotated: " + rotatedWidth);
    }

    @Test
    @DisplayName("computeRotatedAABB with combined rotations should produce valid AABB")
    void testComputeRotatedAABBCombined() {
        Matrix3x3 rotation = Matrix3x3.fromEulerAngles(30, 45, 60);
        BoundingBox rotated = cubeModelInfo.computeRotatedAABB(rotation);

        assertNotNull(rotated);
        assertTrue(rotated.getWidth() > 0, "Width should be positive");
        assertTrue(rotated.getHeight() > 0, "Height should be positive");
        assertTrue(rotated.getDepth() > 0, "Depth should be positive");
    }

    @Test
    @DisplayName("getSortKey should return volume")
    void testGetSortKey() {
        float sortKey = cubeModelInfo.getSortKey();
        float volume = cubeModelInfo.getVolume();

        assertEquals(volume, sortKey, 0.001f);
    }

    @Test
    @DisplayName("getSortKey should enable volume-based sorting")
    void testSortKeyComparison() {
        // Create models with different volumes
        List<Triangle> smallTriangles = Arrays.asList(
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0.5f, 0, 0), new Vector3D(0.5f, 0.5f, 0)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(0.5f, 0.5f, 0), new Vector3D(0, 0.5f, 0))
        );
        Mesh smallMesh = new Mesh(smallTriangles, "small.stl");
        ModelInfo smallModel = new ModelInfo("small.stl", smallMesh, 1);

        assertTrue(cubeModelInfo.getSortKey() > smallModel.getSortKey(),
            "Larger model should have higher sort key");
    }

    @Test
    @DisplayName("clone should create independent copy")
    void testClone() {
        ModelInfo cloned = cubeModelInfo.clone();

        assertNotNull(cloned);
        assertNotSame(cubeModelInfo, cloned, "Clone should be a different object");
        assertEquals(cubeModelInfo, cloned, "Clone should be equal to original");
        assertEquals(cubeModelInfo.getFileName(), cloned.getFileName());
        assertEquals(cubeModelInfo.getMesh(), cloned.getMesh());
        assertEquals(cubeModelInfo.getModelId(), cloned.getModelId());
        assertEquals(cubeModelInfo.getVolume(), cloned.getVolume());
    }

    @Test
    @DisplayName("equals should return true for same object")
    void testEqualsSameObject() {
        assertEquals(cubeModelInfo, cubeModelInfo);
    }

    @Test
    @DisplayName("equals should return false for null")
    void testEqualsNull() {
        assertNotEquals(null, cubeModelInfo);
    }

    @Test
    @DisplayName("equals should return false for different class")
    void testEqualsDifferentClass() {
        assertNotEquals(cubeModelInfo, "not a ModelInfo");
    }

    @Test
    @DisplayName("equals should return true for identical content")
    void testEqualsIdenticalContent() {
        ModelInfo other = new ModelInfo("cube.stl", simpleCubeMesh, 0);
        assertEquals(cubeModelInfo, other);
    }

    @Test
    @DisplayName("equals should return false for different fileName")
    void testEqualsDifferentFileName() {
        ModelInfo other = new ModelInfo("different.stl", simpleCubeMesh, 0);
        assertNotEquals(cubeModelInfo, other);
    }

    @Test
    @DisplayName("equals should return false for different modelId")
    void testEqualsDifferentModelId() {
        ModelInfo other = new ModelInfo("cube.stl", simpleCubeMesh, 1);
        assertNotEquals(cubeModelInfo, other);
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void testHashCode() {
        ModelInfo other = new ModelInfo("cube.stl", simpleCubeMesh, 0);

        assertEquals(cubeModelInfo.hashCode(), other.hashCode(),
            "Equal objects should have equal hash codes");
    }

    @Test
    @DisplayName("hashCode should be stable across multiple calls")
    void testHashCodeStability() {
        int hash1 = cubeModelInfo.hashCode();
        int hash2 = cubeModelInfo.hashCode();

        assertEquals(hash1, hash2, "Hash code should be stable");
    }

    @Test
    @DisplayName("toString should contain key information")
    void testToString() {
        String str = cubeModelInfo.toString();

        assertNotNull(str);
        assertTrue(str.contains("ModelInfo"), "Should contain class name");
        assertTrue(str.contains("cube.stl"), "Should contain file name");
        assertTrue(str.contains("id=0"), "Should contain model ID");
        assertTrue(str.contains("volume"), "Should contain volume label");
    }

    @Test
    @DisplayName("toString should not throw exception")
    void testToStringNoException() {
        assertDoesNotThrow(() -> cubeModelInfo.toString());
    }

    @Test
    @DisplayName("Computed volume should match mesh volume")
    void testVolumeConsistency() {
        float modelVolume = cubeModelInfo.getVolume();
        float meshVolume = simpleCubeMesh.getVolume();

        assertEquals(meshVolume, modelVolume, 0.001f,
            "Model volume should match mesh volume");
    }

    @Test
    @DisplayName("Computed center of mass should match mesh center")
    void testCenterOfMassConsistency() {
        Vector3D modelCenter = cubeModelInfo.getCenterOfMass();
        Vector3D meshCenter = simpleCubeMesh.getCentroid();

        assertEquals(meshCenter.getX(), modelCenter.getX(), 0.001f);
        assertEquals(meshCenter.getY(), modelCenter.getY(), 0.001f);
        assertEquals(meshCenter.getZ(), modelCenter.getZ(), 0.001f);
    }

    @Test
    @DisplayName("Multiple rotations should preserve volume (AABB volume may change)")
    void testRotationPreservesOriginalData() {
        Matrix3x3 rotation1 = Matrix3x3.rotationZ(30);
        Matrix3x3 rotation2 = Matrix3x3.rotationX(45);

        cubeModelInfo.computeRotatedAABB(rotation1);
        cubeModelInfo.computeRotatedAABB(rotation2);

        // Original data should be unchanged (immutability)
        assertEquals(simpleCubeMesh.getBoundingBox(), cubeModelInfo.getOriginalAABB(),
            "Original AABB should remain unchanged after rotations");
    }

    @Test
    @DisplayName("Large model should have larger sort key than small model")
    void testSortKeyOrdering() {
        // Create a larger mesh (2x2x2 cube)
        List<Triangle> largeTriangles = Arrays.asList(
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(2, 0, 0), new Vector3D(2, 2, 0)),
            new Triangle(new Vector3D(0, 0, 0), new Vector3D(2, 2, 0), new Vector3D(0, 2, 0)),
            new Triangle(new Vector3D(0, 0, 2), new Vector3D(2, 2, 2), new Vector3D(2, 0, 2)),
            new Triangle(new Vector3D(0, 0, 2), new Vector3D(0, 2, 2), new Vector3D(2, 2, 2))
        );

        Mesh largeMesh = new Mesh(largeTriangles, "large.stl");
        ModelInfo largeModel = new ModelInfo("large.stl", largeMesh, 2);

        assertTrue(largeModel.getSortKey() > cubeModelInfo.getSortKey(),
            "Larger model (2x2x2) should have higher sort key than smaller model (1x1x1)");
    }
}
