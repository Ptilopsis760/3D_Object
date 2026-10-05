package com.placement3d.model;

import com.placement3d.geometry.BoundingBox;
import java.util.Objects;

/**
 * Encapsulates all information about a 3D model for the placement algorithm.
 *
 * <p>This class serves as a comprehensive container for STL model data, including:
 * <ul>
 *   <li>File identification (fileName, modelId)</li>
 *   <li>Geometric data (mesh, boundingBox)</li>
 *   <li>Physical properties (volume, centerOfMass)</li>
 *   <li>Transformation support (rotation-aware bounding box computation)</li>
 * </ul>
 *
 * <p><b>Usage in Placement Algorithm:</b></p>
 * <ul>
 *   <li><b>Phase 1 (Greedy Initialization):</b> Models are sorted by {@link #getSortKey()}
 *       (volume-based) for "large items first" heuristic.</li>
 *   <li><b>Phase 2 (Simulated Annealing):</b> {@link #computeRotatedAABB(Matrix3x3)}
 *       evaluates rotated configurations without mutating the original model.</li>
 *   <li><b>Phase 3 (Local Search):</b> Fine-grained position adjustments use the
 *       original AABB for quick collision checks.</li>
 * </ul>
 *
 * <p><b>Immutability:</b> This class is immutable. All operations return new instances
 * or immutable views. The {@link #clone()} method creates deep copies for safe reuse
 * across algorithm iterations.</p>
 *
 * <p><b>Coordinate System:</b></p>
 * <ul>
 *   <li>World coordinate system: origin at bottom-left corner of printing platform</li>
 *   <li>X-axis: Right, Y-axis: Forward, Z-axis: Up</li>
 *   <li>Units: millimeters (mm)</li>
 *   <li>Rotations: Euler angles (degrees, XYZ order)</li>
 * </ul>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 * @see Mesh
 * @see BoundingBox
 * @see Matrix3x3
 */
public final class ModelInfo {

    /** The STL file name (e.g., "gear.stl") */
    private final String fileName;

    /** The triangular mesh data */
    private final Mesh mesh;

    /** The original axis-aligned bounding box (before any rotation) */
    private final BoundingBox originalAABB;

    /** The volume in cubic millimeters */
    private final float volume;

    /** The center of mass in world coordinates */
    private final Vector3D centerOfMass;

    /** The model type identifier (used to ensure coverage constraint: ≥1 of each type per batch) */
    private final int modelId;

    /**
     * Creates a new ModelInfo instance.
     *
     * <p>This constructor computes derived properties (originalAABB, volume, centerOfMass)
     * from the mesh data. These properties are cached for efficient access during
     * algorithm iterations.</p>
     *
     * @param fileName the STL file name (e.g., "gear.stl")
     * @param mesh the triangular mesh loaded from the STL file
     * @param modelId the model type identifier (0-based index)
     * @throws NullPointerException if fileName or mesh is null
     * @throws IllegalArgumentException if fileName is empty or modelId is negative
     */
    public ModelInfo(String fileName, Mesh mesh, int modelId) {
        Objects.requireNonNull(fileName, "File name cannot be null");
        Objects.requireNonNull(mesh, "Mesh cannot be null");

        if (fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        if (modelId < 0) {
            throw new IllegalArgumentException("Model ID must be non-negative: " + modelId);
        }

        this.fileName = fileName;
        this.mesh = mesh;
        this.modelId = modelId;

        // Cache derived properties from mesh
        this.originalAABB = mesh.getBoundingBox();
        this.volume = mesh.getVolume();
        this.centerOfMass = mesh.getCentroid();
    }

    /**
     * Gets the STL file name.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the mesh data.
     *
     * @return the mesh
     */
    public Mesh getMesh() {
        return mesh;
    }

    /**
     * Gets the original axis-aligned bounding box (before rotation).
     *
     * @return the original AABB
     */
    public BoundingBox getOriginalAABB() {
        return originalAABB;
    }

    /**
     * Gets the volume in cubic millimeters.
     *
     * @return the volume
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Gets the center of mass in world coordinates.
     *
     * @return the center of mass
     */
    public Vector3D getCenterOfMass() {
        return centerOfMass;
    }

    /**
     * Gets the model type identifier.
     *
     * <p>This ID distinguishes different model types (e.g., gear vs. bracket vs. housing).
     * The placement algorithm uses this to enforce the coverage constraint: each batch
     * must contain at least one instance of each model type.</p>
     *
     * @return the model ID (0-based)
     */
    public int getModelId() {
        return modelId;
    }

    /**
     * Computes the axis-aligned bounding box after applying a rotation.
     *
     * <p>This method does <b>NOT</b> mutate the model's mesh data. It computes
     * the new AABB by transforming the 8 corners of the original bounding box
     * through the rotation matrix, then finding the axis-aligned extent of the
     * transformed corners.</p>
     *
     * <p><b>Algorithm:</b></p>
     * <pre>
     * 1. Extract 8 corners from originalAABB
     * 2. For each corner:
     *      rotatedCorner = rotation * (corner - centerOfMass) + centerOfMass
     * 3. Compute new AABB enclosing all 8 rotated corners
     * </pre>
     *
     * <p><b>Use Case:</b> During simulated annealing (Phase 2), the algorithm
     * evaluates thousands of rotation candidates. This method enables efficient
     * collision checking without modifying the underlying mesh.</p>
     *
     * @param rotation the rotation matrix to apply
     * @return a new bounding box representing the rotated extent
     * @throws NullPointerException if rotation is null
     */
    public BoundingBox computeRotatedAABB(Matrix3x3 rotation) {
        Objects.requireNonNull(rotation, "Rotation matrix cannot be null");

        // Rotate around center of mass for physical realism
        return originalAABB.transform(rotation, centerOfMass);
    }

    /**
     * Returns the sort key for volume-based ordering.
     *
     * <p>Used in Phase 1 (Greedy Initialization) to implement the "large items first"
     * heuristic. Larger models are placed before smaller ones to maximize space
     * utilization and avoid fragmentation.</p>
     *
     * <p><b>Sorting Order:</b> Descending (larger volumes have higher priority)</p>
     *
     * @return the volume (used as the sort key)
     */
    public float getSortKey() {
        return volume;
    }

    /**
     * Creates a deep copy of this ModelInfo.
     *
     * <p>Since this class is immutable and all fields are either primitive types,
     * immutable objects, or immutable references, the clone is a shallow copy
     * at the object reference level but behaves as a deep copy semantically.</p>
     *
     * <p><b>Use Case:</b> The placement algorithm creates multiple solution candidates
     * during simulated annealing. Cloning prevents unintended state sharing between
     * candidates.</p>
     *
     * @return a new ModelInfo instance with identical data
     */
    public ModelInfo clone() {
        // All fields are immutable, so we can safely share references
        return new ModelInfo(this.fileName, this.mesh, this.modelId);
    }

    /**
     * Checks equality based on all fields.
     *
     * <p>Two ModelInfo instances are equal if they have the same fileName, mesh,
     * and modelId. Derived properties (volume, centerOfMass, originalAABB) are
     * not compared explicitly since they are deterministically computed from the mesh.</p>
     *
     * @param obj the object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ModelInfo other = (ModelInfo) obj;
        return modelId == other.modelId &&
               Float.compare(other.volume, volume) == 0 &&
               fileName.equals(other.fileName) &&
               mesh.equals(other.mesh) &&
               centerOfMass.equals(other.centerOfMass);
    }

    /**
     * Computes hash code based on identifying fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(fileName, mesh, modelId, volume, centerOfMass);
    }

    /**
     * Returns a string representation for debugging.
     *
     * <p>Format: {@code ModelInfo[id=0, file=gear.stl, volume=12345.67mm³, bbox=...]}</p>
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return String.format(
            "ModelInfo[id=%d, file=%s, volume=%.2fmm³, bbox=%s]",
            modelId, fileName, volume, originalAABB
        );
    }
}
