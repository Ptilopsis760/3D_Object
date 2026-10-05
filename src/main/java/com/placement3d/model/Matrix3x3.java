package com.placement3d.model;

import java.util.Objects;

/**
 * Represents a 3x3 matrix for 3D transformations (rotation, scaling).
 * This immutable class provides matrix operations for geometric transformations.
 *
 * <p>Matrix elements are stored in row-major order.</p>
 *
 * @author 3D Placement System
 * @version 1.0
 */
public final class Matrix3x3 {

    /** Matrix elements in row-major order */
    private final double[][] elements;

    /** Epsilon for floating-point comparisons */
    private static final double EPSILON = 1e-9;

    /** Identity matrix constant */
    public static final Matrix3x3 IDENTITY = identity();

    /**
     * Constructs a new Matrix3x3 from a 2D array.
     *
     * @param elements the 3x3 array of matrix elements
     * @throws IllegalArgumentException if the array is not 3x3
     * @throws NullPointerException if elements is null
     */
    public Matrix3x3(double[][] elements) {
        Objects.requireNonNull(elements, "Matrix elements cannot be null");
        if (elements.length != 3) {
            throw new IllegalArgumentException("Matrix must be 3x3");
        }
        for (int i = 0; i < 3; i++) {
            Objects.requireNonNull(elements[i], "Matrix row " + i + " cannot be null");
            if (elements[i].length != 3) {
                throw new IllegalArgumentException("Matrix must be 3x3");
            }
        }

        // Deep copy to ensure immutability
        this.elements = new double[3][3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(elements[i], 0, this.elements[i], 0, 3);
        }
    }

    /**
     * Creates an identity matrix.
     *
     * @return a 3x3 identity matrix
     */
    public static Matrix3x3 identity() {
        return new Matrix3x3(new double[][] {
            {1, 0, 0},
            {0, 1, 0},
            {0, 0, 1}
        });
    }

    /**
     * Creates a rotation matrix around the X-axis.
     *
     * @param angleDegrees the rotation angle in degrees
     * @return a rotation matrix
     */
    public static Matrix3x3 rotationX(double angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Matrix3x3(new double[][] {
            {1, 0, 0},
            {0, cos, -sin},
            {0, sin, cos}
        });
    }

    /**
     * Creates a rotation matrix around the Y-axis.
     *
     * @param angleDegrees the rotation angle in degrees
     * @return a rotation matrix
     */
    public static Matrix3x3 rotationY(double angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Matrix3x3(new double[][] {
            {cos, 0, sin},
            {0, 1, 0},
            {-sin, 0, cos}
        });
    }

    /**
     * Creates a rotation matrix around the Z-axis.
     *
     * @param angleDegrees the rotation angle in degrees
     * @return a rotation matrix
     */
    public static Matrix3x3 rotationZ(double angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Matrix3x3(new double[][] {
            {cos, -sin, 0},
            {sin, cos, 0},
            {0, 0, 1}
        });
    }

    /**
     * Creates a composite rotation matrix from Euler angles (XYZ order).
     *
     * @param xDegrees rotation around X-axis in degrees
     * @param yDegrees rotation around Y-axis in degrees
     * @param zDegrees rotation around Z-axis in degrees
     * @return a composite rotation matrix
     */
    public static Matrix3x3 fromEulerAngles(double xDegrees, double yDegrees, double zDegrees) {
        return rotationZ(zDegrees).multiply(rotationY(yDegrees)).multiply(rotationX(xDegrees));
    }

    /**
     * Gets a matrix element at the specified position.
     *
     * @param row the row index (0-2)
     * @param col the column index (0-2)
     * @return the element value
     * @throws IndexOutOfBoundsException if indices are out of range
     */
    public double get(int row, int col) {
        if (row < 0 || row > 2 || col < 0 || col > 2) {
            throw new IndexOutOfBoundsException("Matrix indices must be between 0 and 2");
        }
        return elements[row][col];
    }

    /**
     * Multiplies this matrix by another matrix.
     *
     * @param other the other matrix
     * @return a new Matrix3x3 representing the product
     * @throws NullPointerException if other is null
     */
    public Matrix3x3 multiply(Matrix3x3 other) {
        Objects.requireNonNull(other, "Matrix cannot be null");
        double[][] result = new double[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                result[i][j] = 0;
                for (int k = 0; k < 3; k++) {
                    result[i][j] += elements[i][k] * other.elements[k][j];
                }
            }
        }

        return new Matrix3x3(result);
    }

    /**
     * Multiplies this matrix by a vector.
     *
     * @param vector the vector to multiply
     * @return a new Vector3D representing the transformed vector
     * @throws NullPointerException if vector is null
     */
    public Vector3D multiply(Vector3D vector) {
        Objects.requireNonNull(vector, "Vector cannot be null");
        double x = elements[0][0] * vector.x + elements[0][1] * vector.y + elements[0][2] * vector.z;
        double y = elements[1][0] * vector.x + elements[1][1] * vector.y + elements[1][2] * vector.z;
        double z = elements[2][0] * vector.x + elements[2][1] * vector.y + elements[2][2] * vector.z;

        // Validate range before casting
        if (Math.abs(x) > Float.MAX_VALUE || Math.abs(y) > Float.MAX_VALUE || Math.abs(z) > Float.MAX_VALUE) {
            throw new ArithmeticException("Transformation result exceeds float range");
        }

        return new Vector3D((float)x, (float)y, (float)z);
    }

    /**
     * Computes the transpose of this matrix.
     *
     * @return a new Matrix3x3 representing the transpose
     */
    public Matrix3x3 transpose() {
        double[][] result = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                result[i][j] = elements[j][i];
            }
        }
        return new Matrix3x3(result);
    }

    /**
     * Returns a copy of the internal matrix data.
     *
     * @return a 3x3 array copy of the matrix elements
     */
    public double[][] getData() {
        double[][] copy = new double[3][3];
        for (int i = 0; i < 3; i++) {
            System.arraycopy(elements[i], 0, copy[i], 0, 3);
        }
        return copy;
    }

    /**
     * Converts this rotation matrix back to Euler angles (in degrees).
     * Assumes the matrix was created using Z*Y*X rotation order (matching fromEulerAngles).
     *
     * <p>Returns angles in the range:
     * <ul>
     *   <li>X: [-180, 180] degrees</li>
     *   <li>Y: [-90, 90] degrees</li>
     *   <li>Z: [-180, 180] degrees</li>
     * </ul>
     *
     * <p><strong>Note:</strong> Due to gimbal lock at Y = ±90°, the decomposition
     * may not be unique. In such cases, X rotation is set to 0.
     *
     * @return array [xDegrees, yDegrees, zDegrees]
     */
    public double[] toEulerAngles() {
        double[] angles = new double[3];

        // For Z*Y*X order, extract Y rotation from -sin(Y) = m[2][0]
        double sinY = -elements[2][0];

        // Clamp to avoid numerical errors from floating point
        if (sinY > 1.0) sinY = 1.0;
        if (sinY < -1.0) sinY = -1.0;

        double yRad = Math.asin(sinY);
        double cosY = Math.cos(yRad);

        // Check for gimbal lock
        if (Math.abs(cosY) > EPSILON) {
            // No gimbal lock - extract X and Z
            double xRad = Math.atan2(elements[2][1] / cosY, elements[2][2] / cosY);
            double zRad = Math.atan2(elements[1][0] / cosY, elements[0][0] / cosY);

            angles[0] = Math.toDegrees(xRad);
            angles[1] = Math.toDegrees(yRad);
            angles[2] = Math.toDegrees(zRad);
        } else {
            // Gimbal lock at Y = ±90°
            // Set X = 0 and solve for Z
            angles[0] = 0;
            angles[1] = Math.toDegrees(yRad);

            if (sinY > 0) {
                // Y = 90°
                double zRad = Math.atan2(-elements[0][1], elements[1][1]);
                angles[2] = Math.toDegrees(zRad);
            } else {
                // Y = -90°
                double zRad = Math.atan2(elements[0][1], elements[1][1]);
                angles[2] = Math.toDegrees(zRad);
            }
        }

        return angles;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Matrix3x3 other = (Matrix3x3) obj;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (Math.abs(elements[i][j] - other.elements[i][j]) > EPSILON) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                result = 31 * result + Long.hashCode(Math.round(elements[i][j] / EPSILON));
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Matrix3x3[\n");
        for (int i = 0; i < 3; i++) {
            sb.append("  [");
            for (int j = 0; j < 3; j++) {
                sb.append(String.format("%8.3f", elements[i][j]));
                if (j < 2) sb.append(", ");
            }
            sb.append("]\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
