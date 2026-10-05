package com.placement3d.geometry;

import com.placement3d.model.Matrix3x3;
import com.placement3d.model.Vector3D;
import java.util.List;
import java.util.Objects;

/**
 * Represents an Axis-Aligned Bounding Box (AABB) for 3D models.
 * This class is immutable - all operations return new BoundingBox instances.
 *
 * <p>A bounding box is defined by two corner points:
 * <ul>
 *   <li>min: the corner with minimum x, y, z coordinates</li>
 *   <li>max: the corner with maximum x, y, z coordinates</li>
 * </ul>
 *
 * <p>Used for:
 * <ul>
 *   <li>Representing the spatial extent of 3D models</li>
 *   <li>Fast collision detection (AABB test)</li>
 *   <li>Space management in the bin packing algorithm</li>
 *   <li>Enforcing the 5mm minimum gap constraint</li>
 * </ul>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
public final class BoundingBox {

    /** The minimum corner (smallest x, y, z) */
    private final Vector3D min;

    /** The maximum corner (largest x, y, z) */
    private final Vector3D max;

    /**
     * Creates a bounding box from two corner points.
     *
     * @param min the minimum corner point
     * @param max the maximum corner point
     * @throws NullPointerException if min or max is null
     * @throws IllegalArgumentException if min is not less than or equal to max in all dimensions
     */
    public BoundingBox(Vector3D min, Vector3D max) {
        Objects.requireNonNull(min, "Minimum point cannot be null");
        Objects.requireNonNull(max, "Maximum point cannot be null");

        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) {
            throw new IllegalArgumentException(
                "Invalid bounding box: min must be <= max in all dimensions. " +
                "min=" + min + ", max=" + max
            );
        }

        this.min = min;
        this.max = max;
    }

    /**
     * Creates a bounding box from a collection of points.
     * Computes the minimum and maximum coordinates across all points.
     *
     * @param points the collection of points
     * @return a new bounding box enclosing all points
     * @throws NullPointerException if points is null or contains null elements
     * @throws IllegalArgumentException if points is empty
     */
    public static BoundingBox fromPoints(List<Vector3D> points) {
        Objects.requireNonNull(points, "Points list cannot be null");
        if (points.isEmpty()) {
            throw new IllegalArgumentException("Cannot create bounding box from empty point list");
        }

        Vector3D first = Objects.requireNonNull(points.get(0), "Point list contains null element");
        float minX = first.getX(), minY = first.getY(), minZ = first.getZ();
        float maxX = first.getX(), maxY = first.getY(), maxZ = first.getZ();

        for (int i = 1; i < points.size(); i++) {
            Vector3D point = Objects.requireNonNull(points.get(i), "Point list contains null element at index " + i);
            minX = Math.min(minX, point.getX());
            minY = Math.min(minY, point.getY());
            minZ = Math.min(minZ, point.getZ());
            maxX = Math.max(maxX, point.getX());
            maxY = Math.max(maxY, point.getY());
            maxZ = Math.max(maxZ, point.getZ());
        }

        return new BoundingBox(new Vector3D(minX, minY, minZ), new Vector3D(maxX, maxY, maxZ));
    }

    /**
     * Gets the minimum corner point.
     *
     * @return the minimum corner
     */
    public Vector3D getMin() {
        return min;
    }

    /**
     * Gets the maximum corner point.
     *
     * @return the maximum corner
     */
    public Vector3D getMax() {
        return max;
    }

    /**
     * Gets the width (size along X-axis).
     *
     * @return the width in millimeters
     */
    public float getWidth() {
        return max.getX() - min.getX();
    }

    /**
     * Gets the height (size along Y-axis).
     *
     * @return the height in millimeters
     */
    public float getHeight() {
        return max.getY() - min.getY();
    }

    /**
     * Gets the depth (size along Z-axis).
     *
     * @return the depth in millimeters
     */
    public float getDepth() {
        return max.getZ() - min.getZ();
    }

    /**
     * Computes the volume of the bounding box.
     *
     * @return the volume in cubic millimeters
     */
    public float getVolume() {
        return getWidth() * getHeight() * getDepth();
    }

    /**
     * Computes the center point of the bounding box.
     *
     * @return the center point
     */
    public Vector3D getCenter() {
        return new Vector3D(
            (float)((min.getX() + max.getX()) / 2.0),
            (float)((min.getY() + max.getY()) / 2.0),
            (float)((min.getZ() + max.getZ()) / 2.0)
        );
    }

    /**
     * Checks if a point is contained within this bounding box (inclusive boundaries).
     *
     * @param point the point to check
     * @return true if the point is inside or on the boundary
     * @throws NullPointerException if point is null
     */
    public boolean contains(Vector3D point) {
        Objects.requireNonNull(point, "Point cannot be null");
        return point.getX() >= min.getX() && point.getX() <= max.getX() &&
               point.getY() >= min.getY() && point.getY() <= max.getY() &&
               point.getZ() >= min.getZ() && point.getZ() <= max.getZ();
    }

    /**
     * Checks if this bounding box intersects with another bounding box.
     * Two boxes intersect if they overlap in all three dimensions.
     *
     * @param other the other bounding box
     * @return true if the boxes intersect
     * @throws NullPointerException if other is null
     */
    public boolean intersects(BoundingBox other) {
        Objects.requireNonNull(other, "Other bounding box cannot be null");
        return !(this.max.getX() < other.min.getX() || this.min.getX() > other.max.getX() ||
                 this.max.getY() < other.min.getY() || this.min.getY() > other.max.getY() ||
                 this.max.getZ() < other.min.getZ() || this.min.getZ() > other.max.getZ());
    }

    /**
     * Expands the bounding box by a specified margin in all directions.
     * This is used to enforce the 5mm minimum gap constraint between models.
     *
     * @param margin the margin to add in millimeters (typically 5mm)
     * @return a new expanded bounding box
     * @throws IllegalArgumentException if margin is negative or would make the box invalid
     */
    public BoundingBox expand(double margin) {
        if (margin < 0) {
            throw new IllegalArgumentException("Margin must be non-negative: " + margin);
        }
        Vector3D expansion = new Vector3D((float)margin, (float)margin, (float)margin);
        Vector3D newMin = min.subtract(expansion);
        Vector3D newMax = max.add(expansion);
        return new BoundingBox(newMin, newMax);
    }

    /**
     * Transforms this bounding box by rotation and translation.
     * Since rotation can change the axis-aligned extent, this method computes
     * a new AABB that encloses all 8 corners after transformation.
     *
     * <p><b>Transformation semantics:</b></p>
     * <p>When a non-identity rotation is applied, the second parameter is treated as a
     * <b>pivot point</b> for rotation: {@code rotation * (corner - pivot) + pivot}</p>
     *
     * <p>When identity rotation is applied, the second parameter is treated as a
     * <b>translation vector</b>: {@code corner + translation}</p>
     *
     * <p><b>Usage patterns:</b></p>
     * <ul>
     *   <li>Rotation around center of mass: {@code transform(rotationMatrix, centerOfMass)}</li>
     *   <li>Rotation around world origin: {@code transform(rotationMatrix, Vector3D.ZERO)}</li>
     *   <li>Pure translation: {@code transform(Matrix3x3.IDENTITY, translationVector)}</li>
     * </ul>
     *
     * @param rotation the rotation matrix to apply
     * @param pivotOrTranslation pivot point for rotation, or translation vector for identity rotation
     * @return a new transformed bounding box (axis-aligned)
     * @throws NullPointerException if rotation or pivotOrTranslation is null
     */
    public BoundingBox transform(Matrix3x3 rotation, Vector3D pivotOrTranslation) {
        Objects.requireNonNull(rotation, "Rotation matrix cannot be null");
        Objects.requireNonNull(pivotOrTranslation, "Pivot/translation vector cannot be null");

        // Generate all 8 corners of the current bounding box
        Vector3D[] corners = new Vector3D[8];
        corners[0] = new Vector3D(min.getX(), min.getY(), min.getZ());
        corners[1] = new Vector3D(max.getX(), min.getY(), min.getZ());
        corners[2] = new Vector3D(min.getX(), max.getY(), min.getZ());
        corners[3] = new Vector3D(max.getX(), max.getY(), min.getZ());
        corners[4] = new Vector3D(min.getX(), min.getY(), max.getZ());
        corners[5] = new Vector3D(max.getX(), min.getY(), max.getZ());
        corners[6] = new Vector3D(min.getX(), max.getY(), max.getZ());
        corners[7] = new Vector3D(max.getX(), max.getY(), max.getZ());

        // Transform all corners: rotation around pivot point
        // Formula: rotated_corner = rotation * (corner - pivot) + pivot
        // This handles both rotation (non-identity) and no-op (identity) correctly
        Vector3D[] transformedCorners = new Vector3D[8];
        for (int i = 0; i < 8; i++) {
            Vector3D relative = corners[i].subtract(pivotOrTranslation);
            Vector3D rotated = rotation.multiply(relative);
            transformedCorners[i] = rotated.add(pivotOrTranslation);
        }

        // Find the new AABB that encloses all transformed corners
        float newMinX = transformedCorners[0].getX();
        float newMinY = transformedCorners[0].getY();
        float newMinZ = transformedCorners[0].getZ();
        float newMaxX = transformedCorners[0].getX();
        float newMaxY = transformedCorners[0].getY();
        float newMaxZ = transformedCorners[0].getZ();

        for (int i = 1; i < 8; i++) {
            Vector3D corner = transformedCorners[i];
            newMinX = Math.min(newMinX, corner.getX());
            newMinY = Math.min(newMinY, corner.getY());
            newMinZ = Math.min(newMinZ, corner.getZ());
            newMaxX = Math.max(newMaxX, corner.getX());
            newMaxY = Math.max(newMaxY, corner.getY());
            newMaxZ = Math.max(newMaxZ, corner.getZ());
        }

        return new BoundingBox(
            new Vector3D(newMinX, newMinY, newMinZ),
            new Vector3D(newMaxX, newMaxY, newMaxZ)
        );
    }

    /**
     * Computes the union of this bounding box with another.
     * The result is the smallest AABB that contains both boxes.
     *
     * @param other the other bounding box
     * @return a new bounding box containing both
     * @throws NullPointerException if other is null
     */
    public BoundingBox union(BoundingBox other) {
        Objects.requireNonNull(other, "Other bounding box cannot be null");
        return new BoundingBox(
            this.min.min(other.min),
            this.max.max(other.max)
        );
    }

    /**
     * Computes the surface area of the bounding box.
     * Used in space management algorithms.
     *
     * @return the surface area in square millimeters
     */
    public double getSurfaceArea() {
        double width = getWidth();
        double height = getHeight();
        double depth = getDepth();
        return 2.0 * (width * height + height * depth + depth * width);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BoundingBox other = (BoundingBox) obj;
        return min.equals(other.min) && max.equals(other.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    @Override
    public String toString() {
        return String.format("BoundingBox[min=%s, max=%s, size=(%.2f×%.2f×%.2f), volume=%.2f]",
            min, max, getWidth(), getHeight(), getDepth(), getVolume());
    }
}
