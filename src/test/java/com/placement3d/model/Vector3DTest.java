package com.placement3d.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Vector3D class.
 * Tests all vector operations including construction, arithmetic, and utility methods.
 */
@DisplayName("Vector3D Tests")
class Vector3DTest {

    private static final float EPSILON = 0.0001f;

    @Test
    @DisplayName("Default constructor creates zero vector")
    void testDefaultConstructor() {
        Vector3D v = new Vector3D();
        assertEquals(0.0f, v.getX(), EPSILON);
        assertEquals(0.0f, v.getY(), EPSILON);
        assertEquals(0.0f, v.getZ(), EPSILON);
    }

    @Test
    @DisplayName("Parameterized constructor sets correct values")
    void testParameterizedConstructor() {
        Vector3D v = new Vector3D(1.5f, 2.5f, 3.5f);
        assertEquals(1.5f, v.getX(), EPSILON);
        assertEquals(2.5f, v.getY(), EPSILON);
        assertEquals(3.5f, v.getZ(), EPSILON);
    }

    @Test
    @DisplayName("Add returns new vector with sum")
    void testAdd() {
        Vector3D v1 = new Vector3D(1.0f, 2.0f, 3.0f);
        Vector3D v2 = new Vector3D(4.0f, 5.0f, 6.0f);
        Vector3D result = v1.add(v2);

        assertEquals(5.0f, result.getX(), EPSILON);
        assertEquals(7.0f, result.getY(), EPSILON);
        assertEquals(9.0f, result.getZ(), EPSILON);

        // Verify immutability
        assertEquals(1.0f, v1.getX(), EPSILON);
        assertEquals(4.0f, v2.getX(), EPSILON);
    }

    @Test
    @DisplayName("Subtract returns new vector with difference")
    void testSubtract() {
        Vector3D v1 = new Vector3D(5.0f, 7.0f, 9.0f);
        Vector3D v2 = new Vector3D(1.0f, 2.0f, 3.0f);
        Vector3D result = v1.subtract(v2);

        assertEquals(4.0f, result.getX(), EPSILON);
        assertEquals(5.0f, result.getY(), EPSILON);
        assertEquals(6.0f, result.getZ(), EPSILON);

        // Verify immutability
        assertEquals(5.0f, v1.getX(), EPSILON);
    }

    @Test
    @DisplayName("Multiply returns new vector scaled by scalar")
    void testMultiply() {
        Vector3D v = new Vector3D(2.0f, 3.0f, 4.0f);
        Vector3D result = v.multiply(2.5f);

        assertEquals(5.0f, result.getX(), EPSILON);
        assertEquals(7.5f, result.getY(), EPSILON);
        assertEquals(10.0f, result.getZ(), EPSILON);

        // Verify immutability
        assertEquals(2.0f, v.getX(), EPSILON);
    }

    @Test
    @DisplayName("Multiply by zero returns zero vector")
    void testMultiplyByZero() {
        Vector3D v = new Vector3D(5.0f, 10.0f, 15.0f);
        Vector3D result = v.multiply(0.0f);

        assertEquals(0.0f, result.getX(), EPSILON);
        assertEquals(0.0f, result.getY(), EPSILON);
        assertEquals(0.0f, result.getZ(), EPSILON);
    }

    @Test
    @DisplayName("Dot product calculates correctly")
    void testDot() {
        Vector3D v1 = new Vector3D(1.0f, 2.0f, 3.0f);
        Vector3D v2 = new Vector3D(4.0f, 5.0f, 6.0f);

        float result = v1.dot(v2);
        // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        assertEquals(32.0f, result, EPSILON);
    }

    @Test
    @DisplayName("Dot product with orthogonal vectors is zero")
    void testDotOrthogonal() {
        Vector3D v1 = new Vector3D(1.0f, 0.0f, 0.0f);
        Vector3D v2 = new Vector3D(0.0f, 1.0f, 0.0f);

        float result = v1.dot(v2);
        assertEquals(0.0f, result, EPSILON);
    }

    @Test
    @DisplayName("Cross product calculates correctly")
    void testCross() {
        Vector3D v1 = new Vector3D(1.0f, 0.0f, 0.0f);
        Vector3D v2 = new Vector3D(0.0f, 1.0f, 0.0f);
        Vector3D result = v1.cross(v2);

        // X cross Y = Z
        assertEquals(0.0f, result.getX(), EPSILON);
        assertEquals(0.0f, result.getY(), EPSILON);
        assertEquals(1.0f, result.getZ(), EPSILON);
    }

    @Test
    @DisplayName("Cross product is anti-commutative")
    void testCrossAntiCommutative() {
        Vector3D v1 = new Vector3D(2.0f, 3.0f, 4.0f);
        Vector3D v2 = new Vector3D(5.0f, 6.0f, 7.0f);

        Vector3D cross12 = v1.cross(v2);
        Vector3D cross21 = v2.cross(v1);

        assertEquals(-cross12.getX(), cross21.getX(), EPSILON);
        assertEquals(-cross12.getY(), cross21.getY(), EPSILON);
        assertEquals(-cross12.getZ(), cross21.getZ(), EPSILON);
    }

    @Test
    @DisplayName("Length calculates correctly")
    void testLength() {
        Vector3D v = new Vector3D(3.0f, 4.0f, 0.0f);
        float length = v.length();

        // sqrt(9 + 16 + 0) = 5
        assertEquals(5.0f, length, EPSILON);
    }

    @Test
    @DisplayName("Length of zero vector is zero")
    void testLengthZeroVector() {
        Vector3D v = new Vector3D(0.0f, 0.0f, 0.0f);
        assertEquals(0.0f, v.length(), EPSILON);
    }

    @Test
    @DisplayName("Length of 3D vector calculates correctly")
    void testLength3D() {
        Vector3D v = new Vector3D(1.0f, 2.0f, 2.0f);
        float length = v.length();

        // sqrt(1 + 4 + 4) = 3
        assertEquals(3.0f, length, EPSILON);
    }

    @Test
    @DisplayName("Normalize returns unit vector")
    void testNormalize() {
        Vector3D v = new Vector3D(3.0f, 4.0f, 0.0f);
        Vector3D normalized = v.normalize();

        assertEquals(0.6f, normalized.getX(), EPSILON);
        assertEquals(0.8f, normalized.getY(), EPSILON);
        assertEquals(0.0f, normalized.getZ(), EPSILON);
        assertEquals(1.0f, normalized.length(), EPSILON);

        // Verify immutability
        assertEquals(3.0f, v.getX(), EPSILON);
    }

    @Test
    @DisplayName("Normalize zero vector returns zero vector")
    void testNormalizeZeroVector() {
        Vector3D v = new Vector3D(0.0f, 0.0f, 0.0f);
        Vector3D normalized = v.normalize();

        assertEquals(0.0f, normalized.getX(), EPSILON);
        assertEquals(0.0f, normalized.getY(), EPSILON);
        assertEquals(0.0f, normalized.getZ(), EPSILON);
    }

    @Test
    @DisplayName("Distance between two points calculates correctly")
    void testDistance() {
        Vector3D v1 = new Vector3D(1.0f, 2.0f, 3.0f);
        Vector3D v2 = new Vector3D(4.0f, 6.0f, 3.0f);

        float distance = v1.distance(v2);
        // sqrt((4-1)^2 + (6-2)^2 + (3-3)^2) = sqrt(9 + 16 + 0) = 5
        assertEquals(5.0f, distance, EPSILON);
    }

    @Test
    @DisplayName("Distance from point to itself is zero")
    void testDistanceToSelf() {
        Vector3D v = new Vector3D(5.0f, 10.0f, 15.0f);
        assertEquals(0.0f, v.distance(v), EPSILON);
    }

    @Test
    @DisplayName("Equals returns true for same values")
    void testEquals() {
        Vector3D v1 = new Vector3D(1.0f, 2.0f, 3.0f);
        Vector3D v2 = new Vector3D(1.0f, 2.0f, 3.0f);

        assertEquals(v1, v2);
        assertEquals(v2, v1);
    }

    @Test
    @DisplayName("Equals returns false for different values")
    void testNotEquals() {
        Vector3D v1 = new Vector3D(1.0f, 2.0f, 3.0f);
        Vector3D v2 = new Vector3D(1.0f, 2.0f, 4.0f);

        assertNotEquals(v1, v2);
    }

    @Test
    @DisplayName("Equals with null returns false")
    void testEqualsNull() {
        Vector3D v = new Vector3D(1.0f, 2.0f, 3.0f);
        assertNotEquals(null, v);
    }

    @Test
    @DisplayName("Equals with different class returns false")
    void testEqualsDifferentClass() {
        Vector3D v = new Vector3D(1.0f, 2.0f, 3.0f);
        assertNotEquals(v, "not a vector");
    }

    @Test
    @DisplayName("HashCode is consistent")
    void testHashCode() {
        Vector3D v1 = new Vector3D(1.0f, 2.0f, 3.0f);
        Vector3D v2 = new Vector3D(1.0f, 2.0f, 3.0f);

        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    @DisplayName("ToString contains all components")
    void testToString() {
        Vector3D v = new Vector3D(1.5f, 2.5f, 3.5f);
        String str = v.toString();

        assertTrue(str.contains("1.5"));
        assertTrue(str.contains("2.5"));
        assertTrue(str.contains("3.5"));
    }

    @Test
    @DisplayName("Negative values are handled correctly")
    void testNegativeValues() {
        Vector3D v = new Vector3D(-1.0f, -2.0f, -3.0f);

        assertEquals(-1.0f, v.getX(), EPSILON);
        assertEquals(-2.0f, v.getY(), EPSILON);
        assertEquals(-3.0f, v.getZ(), EPSILON);
    }

    @Test
    @DisplayName("Add with negative values")
    void testAddNegative() {
        Vector3D v1 = new Vector3D(5.0f, 3.0f, 2.0f);
        Vector3D v2 = new Vector3D(-2.0f, -1.0f, -1.0f);
        Vector3D result = v1.add(v2);

        assertEquals(3.0f, result.getX(), EPSILON);
        assertEquals(2.0f, result.getY(), EPSILON);
        assertEquals(1.0f, result.getZ(), EPSILON);
    }

    @Test
    @DisplayName("Multiply by negative scalar")
    void testMultiplyNegative() {
        Vector3D v = new Vector3D(2.0f, 3.0f, 4.0f);
        Vector3D result = v.multiply(-1.0f);

        assertEquals(-2.0f, result.getX(), EPSILON);
        assertEquals(-3.0f, result.getY(), EPSILON);
        assertEquals(-4.0f, result.getZ(), EPSILON);
    }

    @Test
    @DisplayName("Large values are handled correctly")
    void testLargeValues() {
        Vector3D v = new Vector3D(1000.0f, 2000.0f, 3000.0f);

        assertEquals(1000.0f, v.getX(), EPSILON);
        assertEquals(2000.0f, v.getY(), EPSILON);
        assertEquals(3000.0f, v.getZ(), EPSILON);
    }

    @Test
    @DisplayName("Distance with large coordinates")
    void testDistanceLargeValues() {
        // Test with values typical for 3D printing space (millimeters)
        Vector3D v1 = new Vector3D(0.0f, 0.0f, 0.0f);
        Vector3D v2 = new Vector3D(4000.0f, 0.0f, 0.0f); // 4000mm = 4m

        assertEquals(4000.0f, v1.distance(v2), 0.01f);
    }
}
