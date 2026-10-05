package com.placement3d.model;

import com.placement3d.geometry.BoundingBox;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a complete 3D mesh model composed of triangular facets.
 * This is the primary representation of STL models in the placement system.
 *
 * <p>A mesh consists of:
 * <ul>
 *   <li>A collection of triangular facets that define the surface</li>
 *   <li>An axis-aligned bounding box (AABB) for spatial queries</li>
 *   <li>Precomputed geometric properties: volume and centroid</li>
 *   <li>Optional metadata: source file name</li>
 * </ul>
 *
 * <p><strong>Immutability:</strong> This class is immutable. All transformation
 * operations return new Mesh instances rather than modifying the original.</p>
 *
 * <p><strong>Volume Calculation:</strong> Uses the divergence theorem (signed volume)
 * to compute the enclosed volume. Requires a closed, manifold mesh for accurate results.</p>
 *
 * <p><strong>Centroid Calculation:</strong> Computes the volume-weighted centroid
 * by treating each triangle as a tetrahedron from the origin.</p>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
public final class Mesh {

    /** Immutable list of triangles composing this mesh */
    private final List<Triangle> triangles;

    /** Axis-aligned bounding box enclosing the mesh */
    private final BoundingBox boundingBox;

    /** Volume of the mesh in cubic millimeters (mm³) */
    private final float volume;

    /** Centroid (center of mass) of the mesh */
    private final Vector3D centroid;

    /** Source file name (optional, may be null) */
    private final String fileName;

    /** Epsilon for floating-point comparisons */
    private static final double EPSILON = 1e-9;

    /**
     * Constructs a mesh from a list of triangles and a file name.
     * Automatically computes the bounding box, volume, and centroid.
     *
     * @param triangles the list of triangles forming the mesh surface
     * @param fileName the source file name (may be null)
     * @throws NullPointerException if triangles list is null or contains null elements
     * @throws IllegalArgumentException if triangles list is empty
     */
    public Mesh(List<Triangle> triangles, String fileName) {
        Objects.requireNonNull(triangles, "Triangles list cannot be null");
        if (triangles.isEmpty()) {
            throw new IllegalArgumentException("Cannot create mesh from empty triangle list");
        }

        // Validate no null triangles and create immutable defensive copy
        List<Triangle> validatedTriangles = new ArrayList<>(triangles.size());
        for (int i = 0; i < triangles.size(); i++) {
            Triangle triangle = Objects.requireNonNull(
                triangles.get(i),
                "Triangle list contains null element at index " + i
            );
            validatedTriangles.add(triangle);
        }
        this.triangles = Collections.unmodifiableList(validatedTriangles);
        this.fileName = fileName;

        // Compute derived properties
        this.boundingBox = computeBoundingBox();
        this.volume = computeVolume();
        this.centroid = computeCentroid();
    }

    /**
     * Computes the axis-aligned bounding box from all triangle vertices.
     * The bounding box encloses all vertices of all triangles in the mesh.
     *
     * @return the computed bounding box
     */
    private BoundingBox computeBoundingBox() {
        List<Vector3D> allVertices = new ArrayList<>(triangles.size() * 3);
        for (Triangle triangle : triangles) {
            allVertices.add(triangle.getV1());
            allVertices.add(triangle.getV2());
            allVertices.add(triangle.getV3());
        }
        return BoundingBox.fromPoints(allVertices);
    }

    /**
     * Computes the volume of the mesh using the divergence theorem.
     *
     * <p>The volume is calculated using the signed volume formula:
     * <pre>
     * V = (1/6) × Σ [(v1 · (v2 × v3))]
     * </pre>
     * where the sum is taken over all triangles, and each triangle forms
     * a tetrahedron with the origin.</p>
     *
     * <p><strong>Requirements:</strong> The mesh must be:
     * <ul>
     *   <li>Closed (watertight) - no holes in the surface</li>
     *   <li>Manifold - edges shared by exactly two triangles</li>
     *   <li>Consistently oriented - normals point outward</li>
     * </ul>
     * Violations may result in incorrect volume calculations.</p>
     *
     * @return the volume in cubic millimeters (mm³), always non-negative
     */
    private float computeVolume() {
        double signedVolume = 0.0;

        for (Triangle triangle : triangles) {
            Vector3D v1 = triangle.getV1();
            Vector3D v2 = triangle.getV2();
            Vector3D v3 = triangle.getV3();

            // Signed volume of tetrahedron formed by origin and triangle
            // V = (1/6) × v1 · (v2 × v3)
            double term = v1.dot(v2.cross(v3));
            signedVolume += term;
        }

        // Divide by 6 to get final volume
        double finalVolume = Math.abs(signedVolume / 6.0);
        return (float) finalVolume;
    }

    /**
     * Computes the centroid (center of mass) of the mesh.
     *
     * <p>The centroid is computed as the volume-weighted average:
     * <pre>
     * C = (1 / (2×V)) × Σ [A_i × C_i]
     * </pre>
     * where A_i is the area of triangle i and C_i is its centroid.</p>
     *
     * <p>For more accuracy, this implementation uses the signed volume contribution
     * of each triangle tetrahedron to weight the centroid calculation.</p>
     *
     * @return the centroid point
     */
    private Vector3D computeCentroid() {
        double totalVolume = 0.0;
        double cx = 0.0, cy = 0.0, cz = 0.0;

        for (Triangle triangle : triangles) {
            Vector3D v1 = triangle.getV1();
            Vector3D v2 = triangle.getV2();
            Vector3D v3 = triangle.getV3();

            // Signed volume contribution of this triangle's tetrahedron
            double tetraVolume = v1.dot(v2.cross(v3)) / 6.0;
            totalVolume += tetraVolume;

            // Centroid of tetrahedron (origin + v1 + v2 + v3) / 4
            Vector3D tetraCentroid = new Vector3D(
                (v1.x + v2.x + v3.x) / 4.0f,
                (v1.y + v2.y + v3.y) / 4.0f,
                (v1.z + v2.z + v3.z) / 4.0f
            );

            // Weight by volume contribution
            cx += tetraVolume * tetraCentroid.x;
            cy += tetraVolume * tetraCentroid.y;
            cz += tetraVolume * tetraCentroid.z;
        }

        // Normalize by total volume
        if (Math.abs(totalVolume) > EPSILON) {
            cx /= totalVolume;
            cy /= totalVolume;
            cz /= totalVolume;
        }

        return new Vector3D((float) cx, (float) cy, (float) cz);
    }

    /**
     * Transforms this mesh by applying rotation and translation to all triangles.
     * Returns a new transformed Mesh instance.
     *
     * <p>The transformation is applied as: v' = R × v + t</p>
     *
     * <p>All geometric properties (bounding box, volume, centroid) are
     * automatically recalculated for the transformed mesh.</p>
     *
     * @param rotation the 3×3 rotation matrix
     * @param translation the translation vector
     * @return a new transformed Mesh
     * @throws NullPointerException if rotation or translation is null
     */
    public Mesh transform(Matrix3x3 rotation, Vector3D translation) {
        Objects.requireNonNull(rotation, "Rotation matrix cannot be null");
        Objects.requireNonNull(translation, "Translation vector cannot be null");

        List<Triangle> transformedTriangles = new ArrayList<>(triangles.size());
        for (Triangle triangle : triangles) {
            transformedTriangles.add(triangle.transform(rotation, translation));
        }

        return new Mesh(transformedTriangles, this.fileName);
    }

    /**
     * Returns the number of triangles in this mesh.
     *
     * @return the triangle count
     */
    public int getTriangleCount() {
        return triangles.size();
    }

    /**
     * Returns an immutable view of the triangles in this mesh.
     *
     * @return the list of triangles
     */
    public List<Triangle> getTriangles() {
        return triangles;
    }

    /**
     * Returns the bounding box of this mesh.
     *
     * @return the bounding box
     */
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    /**
     * Returns the volume of this mesh in cubic millimeters.
     *
     * @return the volume (mm³)
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Returns the centroid of this mesh.
     *
     * @return the centroid point
     */
    public Vector3D getCentroid() {
        return centroid;
    }

    /**
     * Returns the center of mass of this mesh.
     * This is an alias for {@link #getCentroid()} for API compatibility.
     *
     * @return the center of mass point
     */
    public Vector3D getCenterOfMass() {
        return centroid;
    }

    /**
     * Returns the source file name of this mesh.
     *
     * @return the file name, or null if not specified
     */
    public String getFileName() {
        return fileName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Mesh other = (Mesh) obj;
        return Float.compare(other.volume, volume) == 0 &&
               triangles.equals(other.triangles) &&
               boundingBox.equals(other.boundingBox) &&
               centroid.equals(other.centroid) &&
               Objects.equals(fileName, other.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(triangles, boundingBox, volume, centroid, fileName);
    }

    @Override
    public String toString() {
        return String.format(
            "Mesh{fileName='%s', triangles=%d, volume=%.2f mm³, centroid=%s, bounds=%s}",
            fileName != null ? fileName : "unnamed",
            triangles.size(),
            volume,
            centroid,
            boundingBox
        );
    }
}
