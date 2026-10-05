package com.placement3d.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Triangle class.
 * Verifies normal vector computation, area calculation, centroid computation,
 * and transformation operations.
 *
 * @author 3D Placement System
 * @version 1.0
 */
class TriangleTest {

    private static final double EPSILON = 1e-6; // Adjusted for float precision

    @Test
    @DisplayName("Constructor with three vertices should auto-compute normal")
    void testConstructorAutoComputesNormal() {
        // Arrange: Create a right triangle in XY plane
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 0);
        Vector3D v3 = new Vector3D(0, 1, 0);

        // Act
        Triangle triangle = new Triangle(v1, v2, v3);

        // Assert: Normal should point in +Z direction (right-hand rule)
        Vector3D expectedNormal = new Vector3D(0, 0, 1);
        assertEquals(expectedNormal.x, triangle.getNormal().x, EPSILON);
        assertEquals(expectedNormal.y, triangle.getNormal().y, EPSILON);
        assertEquals(expectedNormal.z, triangle.getNormal().z, EPSILON);
    }

    @Test
    @DisplayName("Constructor with explicit normal should use provided normal")
    void testConstructorWithExplicitNormal() {
        // Arrange
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 0);
        Vector3D v3 = new Vector3D(0, 1, 0);
        Vector3D customNormal = new Vector3D(0, 0, -1); // Opposite direction

        // Act
        Triangle triangle = new Triangle(v1, v2, v3, customNormal);

        // Assert
        assertEquals(customNormal, triangle.getNormal());
    }

    @Test
    @DisplayName("Constructor should throw exception for collinear vertices")
    void testConstructorThrowsExceptionForCollinearVertices() {
        // Arrange: Three collinear points on X-axis
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 0);
        Vector3D v3 = new Vector3D(2, 0, 0);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Triangle(v1, v2, v3)
        );
        assertTrue(exception.getMessage().contains("collinear"));
    }

    @Test
    @DisplayName("Constructor should throw exception for null vertices")
    void testConstructorThrowsExceptionForNullVertices() {
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 0);

        assertThrows(NullPointerException.class, () -> new Triangle(null, v2, v1));
        assertThrows(NullPointerException.class, () -> new Triangle(v1, null, v1));
        assertThrows(NullPointerException.class, () -> new Triangle(v1, v2, null));
    }

    @Test
    @DisplayName("Constructor should throw exception for zero normal vector")
    void testConstructorThrowsExceptionForZeroNormal() {
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 0);
        Vector3D v3 = new Vector3D(0, 1, 0);
        Vector3D zeroNormal = new Vector3D(0, 0, 0);

        assertThrows(IllegalArgumentException.class,
            () -> new Triangle(v1, v2, v3, zeroNormal));
    }

    @Test
    @DisplayName("getArea should calculate correct area for right triangle")
    void testGetAreaForRightTriangle() {
        // Arrange: Right triangle with legs of length 3 and 4
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(3, 0, 0);
        Vector3D v3 = new Vector3D(0, 4, 0);
        Triangle triangle = new Triangle(v1, v2, v3);

        // Act
        double area = triangle.getArea();

        // Assert: Area should be 0.5 * base * height = 0.5 * 3 * 4 = 6
        assertEquals(6.0, area, EPSILON);
    }

    @Test
    @DisplayName("getArea should calculate correct area for equilateral triangle")
    void testGetAreaForEquilateralTriangle() {
        // Arrange: Equilateral triangle with side length 2
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(2, 0, 0);
        Vector3D v3 = new Vector3D(1, (float)Math.sqrt(3), 0);
        Triangle triangle = new Triangle(v1, v2, v3);

        // Act
        double area = triangle.getArea();

        // Assert: Area = (sqrt(3)/4) * side^2 = sqrt(3)
        assertEquals(Math.sqrt(3), area, EPSILON);
    }

    @Test
    @DisplayName("getArea should handle 3D triangles correctly")
    void testGetAreaFor3DTriangle() {
        // Arrange: Triangle in 3D space
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 1);
        Vector3D v3 = new Vector3D(0, 1, 1);
        Triangle triangle = new Triangle(v1, v2, v3);

        // Act
        double area = triangle.getArea();

        // Assert: Area calculation using cross product
        // edge1 = (1,0,1), edge2 = (0,1,1)
        // cross = (-1, -1, 1), magnitude = sqrt(3)
        // area = sqrt(3) / 2
        assertEquals(Math.sqrt(3) / 2, area, EPSILON);
    }

    @Test
    @DisplayName("getCentroid should calculate correct centroid")
    void testGetCentroid() {
        // Arrange
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(3, 0, 0);
        Vector3D v3 = new Vector3D(0, 3, 0);
        Triangle triangle = new Triangle(v1, v2, v3);

        // Act
        Vector3D centroid = triangle.getCentroid();

        // Assert: Centroid should be at (1, 1, 0)
        assertEquals(1.0, centroid.x, EPSILON);
        assertEquals(1.0, centroid.y, EPSILON);
        assertEquals(0.0, centroid.z, EPSILON);
    }

    @Test
    @DisplayName("getCentroid should handle 3D triangles")
    void testGetCentroidFor3DTriangle() {
        // Arrange: Use non-collinear points
        Vector3D v1 = new Vector3D(1, 2, 3);
        Vector3D v2 = new Vector3D(4, 5, 6);
        Vector3D v3 = new Vector3D(1, 5, 9);  // Changed to make non-collinear
        Triangle triangle = new Triangle(v1, v2, v3);

        // Act
        Vector3D centroid = triangle.getCentroid();

        // Assert: Centroid should be at average of vertices
        assertEquals(2.0, centroid.x, EPSILON);
        assertEquals(4.0, centroid.y, EPSILON);
        assertEquals(6.0, centroid.z, EPSILON);
    }

    @Test
    @DisplayName("transform should correctly apply rotation and translation")
    void testTransform() {
        // Arrange: Triangle in XY plane
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 0);
        Vector3D v3 = new Vector3D(0, 1, 0);
        Triangle triangle = new Triangle(v1, v2, v3);

        // Rotate 90° around Z-axis, then translate by (10, 20, 30)
        Matrix3x3 rotation = Matrix3x3.rotationZ(90);
        Vector3D translation = new Vector3D(10, 20, 30);

        // Act
        Triangle transformed = triangle.transform(rotation, translation);

        // Assert: After 90° Z-rotation: (1,0,0) -> (0,1,0), (0,1,0) -> (-1,0,0)
        // After translation: add (10, 20, 30)
        assertEquals(10.0, transformed.getV1().x, EPSILON);
        assertEquals(20.0, transformed.getV1().y, EPSILON);
        assertEquals(30.0, transformed.getV1().z, EPSILON);

        assertEquals(10.0, transformed.getV2().x, 0.01); // Small tolerance for rotation
        assertEquals(21.0, transformed.getV2().y, 0.01);
        assertEquals(30.0, transformed.getV2().z, EPSILON);

        assertEquals(9.0, transformed.getV3().x, 0.01);
        assertEquals(20.0, transformed.getV3().y, 0.01);
        assertEquals(30.0, transformed.getV3().z, EPSILON);
    }

    @Test
    @DisplayName("transform should rotate normal vector correctly")
    void testTransformRotatesNormal() {
        // Arrange: Triangle with normal in +Z direction
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 0);
        Vector3D v3 = new Vector3D(0, 1, 0);
        Triangle triangle = new Triangle(v1, v2, v3);

        // Rotate 90° around X-axis (Z becomes -Y)
        Matrix3x3 rotation = Matrix3x3.rotationX(90);
        Vector3D translation = new Vector3D(0, 0, 0);

        // Act
        Triangle transformed = triangle.transform(rotation, translation);

        // Assert: Normal (0,0,1) should become (0,-1,0)
        assertEquals(0.0, transformed.getNormal().x, 0.01);
        assertEquals(-1.0, transformed.getNormal().y, 0.01);
        assertEquals(0.0, transformed.getNormal().z, 0.01);
    }

    @Test
    @DisplayName("transform should throw exception for null parameters")
    void testTransformThrowsExceptionForNullParameters() {
        Triangle triangle = new Triangle(
            new Vector3D(0, 0, 0),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 1, 0)
        );

        Matrix3x3 rotation = Matrix3x3.identity();
        Vector3D translation = new Vector3D(0, 0, 0);

        assertThrows(NullPointerException.class,
            () -> triangle.transform(null, translation));
        assertThrows(NullPointerException.class,
            () -> triangle.transform(rotation, null));
    }

    @Test
    @DisplayName("Normal computation should follow right-hand rule")
    void testNormalFollowsRightHandRule() {
        // Arrange: Counter-clockwise vertices when viewed from +Z
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 0);
        Vector3D v3 = new Vector3D(0, 1, 0);
        Triangle triangle1 = new Triangle(v1, v2, v3);

        // Arrange: Clockwise vertices (reversed order)
        Triangle triangle2 = new Triangle(v1, v3, v2);

        // Assert: Normals should point in opposite directions
        Vector3D normal1 = triangle1.getNormal();
        Vector3D normal2 = triangle2.getNormal();

        assertEquals(normal1.getX(), -normal2.getX(), EPSILON);
        assertEquals(normal1.getY(), -normal2.getY(), EPSILON);
        assertEquals(normal1.getZ(), -normal2.getZ(), EPSILON);
    }

    @Test
    @DisplayName("equals should return true for identical triangles")
    void testEquals() {
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 0);
        Vector3D v3 = new Vector3D(0, 1, 0);

        Triangle triangle1 = new Triangle(v1, v2, v3);
        Triangle triangle2 = new Triangle(v1, v2, v3);

        assertEquals(triangle1, triangle2);
        assertEquals(triangle1.hashCode(), triangle2.hashCode());
    }

    @Test
    @DisplayName("equals should return false for different triangles")
    void testNotEquals() {
        Triangle triangle1 = new Triangle(
            new Vector3D(0, 0, 0),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 1, 0)
        );

        Triangle triangle2 = new Triangle(
            new Vector3D(0, 0, 0),
            new Vector3D(2, 0, 0),
            new Vector3D(0, 2, 0)
        );

        assertNotEquals(triangle1, triangle2);
    }

    @Test
    @DisplayName("toString should provide readable representation")
    void testToString() {
        Triangle triangle = new Triangle(
            new Vector3D(0, 0, 0),
            new Vector3D(1, 0, 0),
            new Vector3D(0, 1, 0)
        );

        String str = triangle.toString();

        assertTrue(str.contains("Triangle"));
        assertTrue(str.contains("v1"));
        assertTrue(str.contains("v2"));
        assertTrue(str.contains("v3"));
        assertTrue(str.contains("normal"));
        assertTrue(str.contains("area"));
    }

    @Test
    @DisplayName("Getters should return correct vertices and normal")
    void testGetters() {
        Vector3D v1 = new Vector3D(1, 2, 3);
        Vector3D v2 = new Vector3D(4, 5, 6);
        Vector3D v3 = new Vector3D(7, 8, 9);
        Vector3D normal = new Vector3D(0, 0, 1);

        Triangle triangle = new Triangle(v1, v2, v3, normal);

        assertEquals(v1, triangle.getV1());
        assertEquals(v2, triangle.getV2());
        assertEquals(v3, triangle.getV3());
        assertEquals(normal, triangle.getNormal());
    }

    @Test
    @DisplayName("Triangle should remain immutable after transformation")
    void testImmutability() {
        // Arrange
        Vector3D v1 = new Vector3D(0, 0, 0);
        Vector3D v2 = new Vector3D(1, 0, 0);
        Vector3D v3 = new Vector3D(0, 1, 0);
        Triangle original = new Triangle(v1, v2, v3);

        Vector3D originalV1 = original.getV1();
        Vector3D originalNormal = original.getNormal();

        // Act: Transform the triangle
        Matrix3x3 rotation = Matrix3x3.rotationZ(45);
        Vector3D translation = new Vector3D(10, 10, 10);
        Triangle transformed = original.transform(rotation, translation);

        // Assert: Original should be unchanged
        assertEquals(originalV1, original.getV1());
        assertEquals(originalNormal, original.getNormal());
        assertNotEquals(original, transformed);
    }
}
