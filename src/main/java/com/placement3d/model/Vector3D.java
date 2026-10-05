package com.placement3d.model;

import java.util.Objects;

/**
 * Represents a three-dimensional vector in 3D space.
 *
 * <p>This class is used to represent both points and direction vectors in the
 * 3D model placement system. All coordinates are measured in millimeters (mm)
 * relative to the world coordinate system with origin at the bottom-left corner
 * of the printing platform.</p>
 *
 * <p><b>Coordinate System:</b></p>
 * <ul>
 *   <li>X-axis: Right (positive direction)</li>
 *   <li>Y-axis: Forward (positive direction)</li>
 *   <li>Z-axis: Up (positive direction)</li>
 *   <li>Origin: (0, 0, 0) at bottom-left corner of platform</li>
 *   <li>Unit: millimeters (mm)</li>
 * </ul>
 *
 * <p><b>Immutability:</b> This class follows the immutable pattern. All operations
 * return new Vector3D instances rather than modifying existing ones, preventing
 * unintended side effects and enabling safe sharing across multiple threads.</p>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
public class Vector3D {

    /** X-coordinate in millimeters */
    public final float x;

    /** Y-coordinate in millimeters */
    public final float y;

    /** Z-coordinate in millimeters */
    public final float z;

    /** Zero vector constant (0, 0, 0) */
    public static final Vector3D ZERO = new Vector3D(0.0f, 0.0f, 0.0f);

    /**
     * Creates a zero vector at the origin (0, 0, 0).
     */
    public Vector3D() {
        this.x = 0.0f;
        this.y = 0.0f;
        this.z = 0.0f;
    }

    /**
     * Creates a vector with specified coordinates.
     *
     * @param x the x-coordinate in millimeters
     * @param y the y-coordinate in millimeters
     * @param z the z-coordinate in millimeters
     */
    public Vector3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Returns the x-coordinate.
     *
     * @return the x-coordinate in millimeters
     */
    public float getX() {
        return x;
    }

    /**
     * Returns the y-coordinate.
     *
     * @return the y-coordinate in millimeters
     */
    public float getY() {
        return y;
    }

    /**
     * Returns the z-coordinate.
     *
     * @return the z-coordinate in millimeters
     */
    public float getZ() {
        return z;
    }

    /**
     * Adds this vector to another vector and returns the result as a new vector.
     *
     * <p>Vector addition is performed component-wise:</p>
     * <pre>
     * result = (x1 + x2, y1 + y2, z1 + z2)
     * </pre>
     *
     * @param other the vector to add to this vector
     * @return a new vector representing the sum
     * @throws NullPointerException if other is null
     */
    public Vector3D add(Vector3D other) {
        Objects.requireNonNull(other, "Cannot add null vector");
        return new Vector3D(
            this.x + other.x,
            this.y + other.y,
            this.z + other.z
        );
    }

    /**
     * Subtracts another vector from this vector and returns the result as a new vector.
     *
     * <p>Vector subtraction is performed component-wise:</p>
     * <pre>
     * result = (x1 - x2, y1 - y2, z1 - z2)
     * </pre>
     *
     * @param other the vector to subtract from this vector
     * @return a new vector representing the difference
     * @throws NullPointerException if other is null
     */
    public Vector3D subtract(Vector3D other) {
        Objects.requireNonNull(other, "Cannot subtract null vector");
        return new Vector3D(
            this.x - other.x,
            this.y - other.y,
            this.z - other.z
        );
    }

    /**
     * Multiplies this vector by a scalar value and returns the result as a new vector.
     *
     * <p>Scalar multiplication scales the vector's magnitude:</p>
     * <pre>
     * result = (x * scalar, y * scalar, z * scalar)
     * </pre>
     *
     * @param scalar the scalar value to multiply by
     * @return a new vector representing the scaled result
     */
    public Vector3D multiply(float scalar) {
        return new Vector3D(
            this.x * scalar,
            this.y * scalar,
            this.z * scalar
        );
    }

    /**
     * Computes the dot product (scalar product) of this vector with another vector.
     *
     * <p>The dot product is calculated as:</p>
     * <pre>
     * result = x1*x2 + y1*y2 + z1*z2
     * </pre>
     *
     * <p>The dot product is useful for:</p>
     * <ul>
     *   <li>Computing the angle between vectors: cos(θ) = (v1 · v2) / (|v1| * |v2|)</li>
     *   <li>Testing orthogonality: vectors are perpendicular if dot product is 0</li>
     *   <li>Projecting one vector onto another</li>
     * </ul>
     *
     * @param other the vector to compute dot product with
     * @return the dot product as a scalar value
     * @throws NullPointerException if other is null
     */
    public float dot(Vector3D other) {
        Objects.requireNonNull(other, "Cannot compute dot product with null vector");
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    /**
     * Computes the cross product (vector product) of this vector with another vector.
     *
     * <p>The cross product is calculated as:</p>
     * <pre>
     * result = (y1*z2 - z1*y2, z1*x2 - x1*z2, x1*y2 - y1*x2)
     * </pre>
     *
     * <p>Properties of the cross product:</p>
     * <ul>
     *   <li>The result is perpendicular to both input vectors</li>
     *   <li>The magnitude equals the area of the parallelogram formed by the vectors</li>
     *   <li>The direction follows the right-hand rule</li>
     *   <li>Anti-commutative: v1 × v2 = -(v2 × v1)</li>
     * </ul>
     *
     * @param other the vector to compute cross product with
     * @return a new vector representing the cross product
     * @throws NullPointerException if other is null
     */
    public Vector3D cross(Vector3D other) {
        Objects.requireNonNull(other, "Cannot compute cross product with null vector");
        return new Vector3D(
            this.y * other.z - this.z * other.y,
            this.z * other.x - this.x * other.z,
            this.x * other.y - this.y * other.x
        );
    }

    /**
     * Computes the Euclidean length (magnitude) of this vector.
     *
     * <p>The length is calculated as:</p>
     * <pre>
     * length = sqrt(x² + y² + z²)
     * </pre>
     *
     * @return the length of the vector in millimeters
     */
    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Returns a normalized (unit length) version of this vector.
     *
     * <p>A normalized vector has length 1 and points in the same direction as
     * the original vector. It is calculated by dividing each component by the
     * vector's length.</p>
     *
     * <p>If this is a zero vector (length = 0), returns a zero vector to avoid
     * division by zero.</p>
     *
     * @return a new unit vector in the same direction, or zero vector if length is zero
     */
    public Vector3D normalize() {
        float len = length();
        if (len < 1e-6f) {  // Use epsilon comparison
            return ZERO;
        }
        return new Vector3D(x / len, y / len, z / len);
    }

    /**
     * Computes the Euclidean distance between this point and another point in 3D space.
     *
     * <p>The distance is calculated as:</p>
     * <pre>
     * distance = sqrt((x2-x1)² + (y2-y1)² + (z2-z1)²)
     * </pre>
     *
     * <p>This is equivalent to the length of the vector (other - this).</p>
     *
     * @param other the other point to measure distance to
     * @return the distance between the two points in millimeters
     * @throws NullPointerException if other is null
     */
    public float distance(Vector3D other) {
        Objects.requireNonNull(other, "Cannot compute distance to null vector");
        float dx = other.x - this.x;
        float dy = other.y - this.y;
        float dz = other.z - this.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Returns a new vector with the minimum components from this vector and another.
     *
     * <p>Component-wise minimum:</p>
     * <pre>
     * result = (min(x1, x2), min(y1, y2), min(z1, z2))
     * </pre>
     *
     * @param other the other vector
     * @return a new vector with minimum components
     * @throws NullPointerException if other is null
     */
    public Vector3D min(Vector3D other) {
        Objects.requireNonNull(other, "Cannot compute min with null vector");
        return new Vector3D(
            Math.min(this.x, other.x),
            Math.min(this.y, other.y),
            Math.min(this.z, other.z)
        );
    }

    /**
     * Returns a new vector with the maximum components from this vector and another.
     *
     * <p>Component-wise maximum:</p>
     * <pre>
     * result = (max(x1, x2), max(y1, y2), max(z1, z2))
     * </pre>
     *
     * @param other the other vector
     * @return a new vector with maximum components
     * @throws NullPointerException if other is null
     */
    public Vector3D max(Vector3D other) {
        Objects.requireNonNull(other, "Cannot compute max with null vector");
        return new Vector3D(
            Math.max(this.x, other.x),
            Math.max(this.y, other.y),
            Math.max(this.z, other.z)
        );
    }

    /**
     * Compares this vector to another object for equality.
     *
     * <p>Two vectors are considered equal if all their components are equal
     * using Float.compare() for proper handling of special float values.</p>
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Vector3D other = (Vector3D) obj;
        return Float.compare(other.x, x) == 0
            && Float.compare(other.y, y) == 0
            && Float.compare(other.z, z) == 0;
    }

    /**
     * Returns a hash code value for this vector.
     *
     * <p>The hash code is computed from all three components to ensure that
     * equal vectors have equal hash codes.</p>
     *
     * @return a hash code value for this vector
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    /**
     * Returns a string representation of this vector.
     *
     * <p>The format is: Vector3D(x, y, z)</p>
     *
     * @return a string representation of this vector
     */
    @Override
    public String toString() {
        return String.format("Vector3D(%.2f, %.2f, %.2f)", x, y, z);
    }
}
