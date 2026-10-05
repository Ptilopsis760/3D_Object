package com.placement3d.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Matrix3x3 class.
 * Tests rotation matrices, Euler angle conversions, and matrix operations.
 */
class Matrix3x3Test {

    private static final double EPSILON = 1e-5;

    @Test
    void testConstructor() {
        double[][] data = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        Matrix3x3 m = new Matrix3x3(data);

        assertEquals(1, m.get(0, 0), EPSILON);
        assertEquals(5, m.get(1, 1), EPSILON);
        assertEquals(9, m.get(2, 2), EPSILON);
    }

    @Test
    void testConstructorInvalidData() {
        assertThrows(NullPointerException.class, () -> new Matrix3x3(null));
        assertThrows(IllegalArgumentException.class, () -> new Matrix3x3(new double[2][2]));
        assertThrows(IllegalArgumentException.class, () -> new Matrix3x3(new double[3][2]));
    }

    @Test
    void testGetOutOfBounds() {
        Matrix3x3 m = Matrix3x3.identity();
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(3, 0));
    }

    @Test
    void testGetData() {
        double[][] original = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        Matrix3x3 m = new Matrix3x3(original);
        double[][] copy = m.getData();

        // Verify values are correct
        assertArrayEquals(original[0], copy[0], EPSILON);
        assertArrayEquals(original[1], copy[1], EPSILON);
        assertArrayEquals(original[2], copy[2], EPSILON);

        // Verify it's a copy, not the original
        copy[0][0] = 999;
        assertEquals(1, m.get(0, 0), EPSILON);
    }

    @Test
    void testIdentity() {
        Matrix3x3 identity = Matrix3x3.identity();

        // Diagonal should be 1
        assertEquals(1, identity.get(0, 0), EPSILON);
        assertEquals(1, identity.get(1, 1), EPSILON);
        assertEquals(1, identity.get(2, 2), EPSILON);

        // Off-diagonal should be 0
        assertEquals(0, identity.get(0, 1), EPSILON);
        assertEquals(0, identity.get(0, 2), EPSILON);
        assertEquals(0, identity.get(1, 0), EPSILON);
        assertEquals(0, identity.get(1, 2), EPSILON);
        assertEquals(0, identity.get(2, 0), EPSILON);
        assertEquals(0, identity.get(2, 1), EPSILON);
    }

    @Test
    void testRotationX90Degrees() {
        Matrix3x3 rx = Matrix3x3.rotationX(90);
        Vector3D yAxis = new Vector3D(0, 1, 0);
        Vector3D result = rx.multiply(yAxis);

        // Y-axis rotated 90° around X should give Z-axis
        assertEquals(0, result.getX(), EPSILON);
        assertEquals(0, result.getY(), EPSILON);
        assertEquals(1, result.getZ(), EPSILON);
    }

    @Test
    void testRotationY90Degrees() {
        Matrix3x3 ry = Matrix3x3.rotationY(90);
        Vector3D zAxis = new Vector3D(0, 0, 1);
        Vector3D result = ry.multiply(zAxis);

        // Z-axis rotated 90° around Y should give X-axis
        assertEquals(1, result.getX(), EPSILON);
        assertEquals(0, result.getY(), EPSILON);
        assertEquals(0, result.getZ(), EPSILON);
    }

    @Test
    void testRotationZ90Degrees() {
        Matrix3x3 rz = Matrix3x3.rotationZ(90);
        Vector3D xAxis = new Vector3D(1, 0, 0);
        Vector3D result = rz.multiply(xAxis);

        // X-axis rotated 90° around Z should give Y-axis
        assertEquals(0, result.getX(), EPSILON);
        assertEquals(1, result.getY(), EPSILON);
        assertEquals(0, result.getZ(), EPSILON);
    }

    @Test
    void testFromEulerAngles() {
        // Test with 90° rotation around X axis
        Matrix3x3 m = Matrix3x3.fromEulerAngles(90, 0, 0);
        Vector3D yAxis = new Vector3D(0, 1, 0);
        Vector3D result = m.multiply(yAxis);

        assertEquals(0, result.getX(), EPSILON);
        assertEquals(0, result.getY(), EPSILON);
        assertEquals(1, result.getZ(), EPSILON);
    }

    @Test
    void testFromEulerAnglesCombined() {
        // Test combined rotation: 45° around X, 30° around Y, 60° around Z
        Matrix3x3 m = Matrix3x3.fromEulerAngles(45, 30, 60);

        // Verify it's a valid rotation matrix: M * M^T should be close to I
        Matrix3x3 transpose = m.transpose();
        Matrix3x3 product = m.multiply(transpose);

        // Should be close to identity
        assertEquals(1, product.get(0, 0), 0.01);
        assertEquals(1, product.get(1, 1), 0.01);
        assertEquals(1, product.get(2, 2), 0.01);
        assertEquals(0, product.get(0, 1), 0.01);
        assertEquals(0, product.get(0, 2), 0.01);
        assertEquals(0, product.get(1, 2), 0.01);
    }

    @Test
    void testMatrixMultiply() {
        Matrix3x3 m1 = new Matrix3x3(new double[][] {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        });
        Matrix3x3 identity = Matrix3x3.identity();
        Matrix3x3 result = m1.multiply(identity);

        // Multiplying by identity should give the same matrix
        assertEquals(m1, result);
    }

    @Test
    void testVectorMultiply() {
        Matrix3x3 identity = Matrix3x3.identity();
        Vector3D v = new Vector3D(1, 2, 3);
        Vector3D result = identity.multiply(v);

        // Identity matrix should not change the vector
        assertEquals(v, result);
    }

    @Test
    void testTranspose() {
        Matrix3x3 m = new Matrix3x3(new double[][] {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        });
        Matrix3x3 t = m.transpose();

        assertEquals(1, t.get(0, 0), EPSILON);
        assertEquals(4, t.get(0, 1), EPSILON);
        assertEquals(7, t.get(0, 2), EPSILON);
        assertEquals(2, t.get(1, 0), EPSILON);
        assertEquals(5, t.get(1, 1), EPSILON);
        assertEquals(8, t.get(1, 2), EPSILON);
        assertEquals(3, t.get(2, 0), EPSILON);
        assertEquals(6, t.get(2, 1), EPSILON);
        assertEquals(9, t.get(2, 2), EPSILON);
    }

    @Test
    void testToEulerAnglesSimple() {
        // Test round-trip conversion for simple angles
        double[] originalAngles = {30, 45, 60};
        Matrix3x3 m = Matrix3x3.fromEulerAngles(originalAngles[0], originalAngles[1], originalAngles[2]);
        double[] recoveredAngles = m.toEulerAngles();

        assertEquals(originalAngles[0], recoveredAngles[0], 0.01);
        assertEquals(originalAngles[1], recoveredAngles[1], 0.01);
        assertEquals(originalAngles[2], recoveredAngles[2], 0.01);
    }

    @Test
    void testToEulerAnglesRoundTrip90Degrees() {
        // Test round-trip for 90° rotations around each axis
        Matrix3x3 mx = Matrix3x3.rotationX(90);
        double[] anglesX = mx.toEulerAngles();
        Matrix3x3 mxRecovered = Matrix3x3.fromEulerAngles(anglesX[0], anglesX[1], anglesX[2]);

        // The matrices should be equivalent (rotate same vectors to same results)
        Vector3D testVec = new Vector3D(1, 2, 3);
        Vector3D result1 = mx.multiply(testVec);
        Vector3D result2 = mxRecovered.multiply(testVec);

        assertEquals(result1.getX(), result2.getX(), 0.01);
        assertEquals(result1.getY(), result2.getY(), 0.01);
        assertEquals(result1.getZ(), result2.getZ(), 0.01);
    }

    @Test
    void testToEulerAnglesIdentity() {
        Matrix3x3 identity = Matrix3x3.identity();
        double[] angles = identity.toEulerAngles();

        // Identity matrix should give zero angles
        assertEquals(0, angles[0], EPSILON);
        assertEquals(0, angles[1], EPSILON);
        assertEquals(0, angles[2], EPSILON);
    }

    @Test
    void testToEulerAnglesGimbalLock() {
        // Test gimbal lock case (Y = 90°)
        Matrix3x3 m = Matrix3x3.fromEulerAngles(30, 90, 45);
        double[] angles = m.toEulerAngles();

        // Y should be 90°
        assertEquals(90, angles[1], 0.01);

        // In gimbal lock, X is set to 0 by convention (not Z)
        // The recovered matrix should still transform vectors correctly
        Matrix3x3 recovered = Matrix3x3.fromEulerAngles(angles[0], angles[1], angles[2]);
        Vector3D testVec = new Vector3D(1, 2, 3);
        Vector3D result1 = m.multiply(testVec);
        Vector3D result2 = recovered.multiply(testVec);

        // The transformations should produce the same results
        assertEquals(result1.getX(), result2.getX(), 0.01);
        assertEquals(result1.getY(), result2.getY(), 0.01);
        assertEquals(result1.getZ(), result2.getZ(), 0.01);
    }

    @Test
    void testEquals() {
        Matrix3x3 m1 = Matrix3x3.rotationX(45);
        Matrix3x3 m2 = Matrix3x3.rotationX(45);
        Matrix3x3 m3 = Matrix3x3.rotationX(46);

        assertEquals(m1, m2);
        assertNotEquals(m1, m3);
        assertNotEquals(m1, null);
        assertNotEquals(m1, "not a matrix");
    }

    @Test
    void testHashCode() {
        Matrix3x3 m1 = Matrix3x3.rotationX(45);
        Matrix3x3 m2 = Matrix3x3.rotationX(45);

        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void testToString() {
        Matrix3x3 identity = Matrix3x3.identity();
        String str = identity.toString();

        assertTrue(str.contains("Matrix3x3"));
        assertTrue(str.contains("1.000"));
    }

    @Test
    void testRotationOrderXYZ() {
        // Verify that rotations are applied in XYZ order
        // Rotate 90° around X, then 90° around Y
        Matrix3x3 combined = Matrix3x3.fromEulerAngles(90, 90, 0);

        // Apply the same rotations manually in order
        Matrix3x3 rx = Matrix3x3.rotationX(90);
        Matrix3x3 ry = Matrix3x3.rotationY(90);
        Matrix3x3 manual = ry.multiply(rx);  // Y after X (since fromEulerAngles does Z*Y*X)

        // Test with a vector
        Vector3D testVec = new Vector3D(1, 2, 3);
        Vector3D result1 = combined.multiply(testVec);
        Vector3D result2 = manual.multiply(testVec);

        assertEquals(result1.getX(), result2.getX(), 0.01);
        assertEquals(result1.getY(), result2.getY(), 0.01);
        assertEquals(result1.getZ(), result2.getZ(), 0.01);
    }

    @Test
    void testImmutability() {
        double[][] data = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        Matrix3x3 m = new Matrix3x3(data);

        // Modify original data
        data[0][0] = 999;

        // Matrix should be unchanged
        assertEquals(1, m.get(0, 0), EPSILON);
    }
}
