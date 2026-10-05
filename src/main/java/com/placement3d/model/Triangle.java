package com.placement3d.model;

import java.util.Objects;

/**
 * Represents a triangular facet in a 3D mesh, the fundamental building block of STL models.
 * Each triangle consists of three vertices and a normal vector perpendicular to its surface.
 *
 * <p>This immutable class provides geometric operations such as area calculation,
 * centroid computation, and affine transformations. Normal vectors follow the
 * right-hand rule: when vertices are ordered counter-clockwise, the normal points outward.</p>
 *
 * <p><strong>Coordinate System:</strong> Uses a single-layer world coordinate system with
 * origin at the bottom-left corner of the print platform. Units are in millimeters (mm).</p>
 *
 * @author 3D Placement System
 * @version 1.0
 */
public final class Triangle {

    /** First vertex of the triangle */
    private final Vector3D v1;

    /** Second vertex of the triangle */
    private final Vector3D v2;

    /** Third vertex of the triangle */
    private final Vector3D v3;

    /** Normal vector perpendicular to the triangle surface */
    private final Vector3D normal;

    /**
     * Epsilon for floating-point comparisons.
     *
     * <p>设置为1e-6 (0.001mm) 以适应实际STL文件精度。
     * 3D打印的实际精度约为±0.1mm，STL文件坐标精度通常在0.01-0.1mm。
     * 使用1e-6可以有效过滤真正的退化三角形（面积<0.000001mm²），
     * 同时保留薄壁结构中的有效三角形。</p>
     */
    private static final double EPSILON = 1e-6;

    /**
     * Constructs a triangle from three vertices with automatic normal computation.
     * The normal is computed using the right-hand rule: (v2 - v1) × (v3 - v1).
     *
     * @param v1 the first vertex
     * @param v2 the second vertex
     * @param v3 the third vertex
     * @throws NullPointerException if any vertex is null
     * @throws IllegalArgumentException if vertices are collinear (degenerate triangle)
     */
    public Triangle(Vector3D v1, Vector3D v2, Vector3D v3) {
        this.v1 = Objects.requireNonNull(v1, "Vertex v1 cannot be null");
        this.v2 = Objects.requireNonNull(v2, "Vertex v2 cannot be null");
        this.v3 = Objects.requireNonNull(v3, "Vertex v3 cannot be null");
        this.normal = computeNormal();
    }

    /**
     * Constructs a triangle from three vertices and an explicit normal vector.
     * This constructor is useful when loading STL files that include pre-computed normals.
     *
     * <p><strong>Note:</strong> The provided normal is not validated against the computed normal.
     * For automatic normal computation, use {@link #Triangle(Vector3D, Vector3D, Vector3D)}.</p>
     *
     * @param v1 the first vertex
     * @param v2 the second vertex
     * @param v3 the third vertex
     * @param normal the normal vector
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if the normal is a zero vector
     */
    public Triangle(Vector3D v1, Vector3D v2, Vector3D v3, Vector3D normal) {
        this.v1 = Objects.requireNonNull(v1, "Vertex v1 cannot be null");
        this.v2 = Objects.requireNonNull(v2, "Vertex v2 cannot be null");
        this.v3 = Objects.requireNonNull(v3, "Vertex v3 cannot be null");
        this.normal = Objects.requireNonNull(normal, "Normal vector cannot be null");

        if (normal.length() < EPSILON) {
            throw new IllegalArgumentException("Normal vector cannot be a zero vector");
        }
    }

    /**
     * Computes the normal vector of this triangle using the cross product.
     * The normal is calculated as (v2 - v1) × (v3 - v1) and then normalized.
     *
     * <p>The direction follows the right-hand rule: if vertices are ordered
     * counter-clockwise when viewed from outside, the normal points outward.</p>
     *
     * @return the normalized normal vector
     * @throws IllegalArgumentException if vertices are collinear (degenerate triangle)
     */
    private Vector3D computeNormal() {
        Vector3D edge1 = v2.subtract(v1);
        Vector3D edge2 = v3.subtract(v1);
        Vector3D crossProduct = edge1.cross(edge2);

        // Check for degenerate triangle (collinear vertices)
        if (crossProduct.length() < EPSILON) {
            throw new IllegalArgumentException(
                "Cannot create triangle from collinear vertices: " + v1 + ", " + v2 + ", " + v3
            );
        }

        return crossProduct.normalize();
    }

    /**
     * Calculates the area of this triangle using the cross product formula.
     * Area = 0.5 × ||(v2 - v1) × (v3 - v1)||
     *
     * @return the area in square millimeters (mm²)
     */
    public double getArea() {
        Vector3D edge1 = v2.subtract(v1);
        Vector3D edge2 = v3.subtract(v1);
        return 0.5 * edge1.cross(edge2).length();
    }

    /**
     * Calculates the centroid (geometric center) of this triangle.
     * The centroid is the average of the three vertices: (v1 + v2 + v3) / 3.
     *
     * @return the centroid point
     */
    public Vector3D getCentroid() {
        return new Vector3D(
            (v1.x + v2.x + v3.x) / 3.0f,
            (v1.y + v2.y + v3.y) / 3.0f,
            (v1.z + v2.z + v3.z) / 3.0f
        );
    }

    /**
     * Applies an affine transformation to this triangle, producing a new transformed triangle.
     * The transformation consists of rotation followed by translation:
     * v' = R × v + t, where R is the rotation matrix and t is the translation vector.
     *
     * <p>Both vertices and the normal vector are rotated. The normal is not translated.</p>
     *
     * @param rotation the 3×3 rotation matrix
     * @param translation the translation vector
     * @return a new transformed Triangle
     * @throws NullPointerException if rotation or translation is null
     */
    public Triangle transform(Matrix3x3 rotation, Vector3D translation) {
        Objects.requireNonNull(rotation, "Rotation matrix cannot be null");
        Objects.requireNonNull(translation, "Translation vector cannot be null");

        // Transform vertices: v' = R × v + t
        Vector3D transformedV1 = rotation.multiply(v1).add(translation);
        Vector3D transformedV2 = rotation.multiply(v2).add(translation);
        Vector3D transformedV3 = rotation.multiply(v3).add(translation);

        // Transform normal: n' = R × n (no translation for normals)
        Vector3D transformedNormal = rotation.multiply(normal).normalize();

        return new Triangle(transformedV1, transformedV2, transformedV3, transformedNormal);
    }

    /**
     * Returns the first vertex of the triangle.
     *
     * @return vertex v1
     */
    public Vector3D getV1() {
        return v1;
    }

    /**
     * Returns the second vertex of the triangle.
     *
     * @return vertex v2
     */
    public Vector3D getV2() {
        return v2;
    }

    /**
     * Returns the third vertex of the triangle.
     *
     * @return vertex v3
     */
    public Vector3D getV3() {
        return v3;
    }

    /**
     * Returns the normal vector of the triangle.
     *
     * @return the normal vector
     */
    public Vector3D getNormal() {
        return normal;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Triangle other = (Triangle) obj;
        return v1.equals(other.v1) &&
               v2.equals(other.v2) &&
               v3.equals(other.v3) &&
               normal.equals(other.normal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(v1, v2, v3, normal);
    }

    @Override
    public String toString() {
        return String.format(
            "Triangle{v1=%s, v2=%s, v3=%s, normal=%s, area=%.3f}",
            v1, v2, v3, normal, getArea()
        );
    }
}
