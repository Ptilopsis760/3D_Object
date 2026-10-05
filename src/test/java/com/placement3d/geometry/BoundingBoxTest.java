package com.placement3d.geometry;

import com.placement3d.model.Matrix3x3;
import com.placement3d.model.Vector3D;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BoundingBox class.
 */
class BoundingBoxTest {

    private static final double EPSILON = 1e-10;

    @Test
    void testConstructor_ValidBox() {
        Vector3D min = new Vector3D(0, 0, 0);
        Vector3D max = new Vector3D(10, 20, 30);
        BoundingBox box = new BoundingBox(min, max);

        assertEquals(min, box.getMin());
        assertEquals(max, box.getMax());
    }

    @Test
    void testConstructor_NullPoints() {
        Vector3D min = new Vector3D(0, 0, 0);
        assertThrows(NullPointerException.class, () -> new BoundingBox(null, min));
        assertThrows(NullPointerException.class, () -> new BoundingBox(min, null));
    }

    @Test
    void testConstructor_InvalidBox() {
        Vector3D min = new Vector3D(10, 0, 0);
        Vector3D max = new Vector3D(0, 20, 30);
        assertThrows(IllegalArgumentException.class, () -> new BoundingBox(min, max));
    }

    @Test
    void testFromPoints_ValidList() {
        List<Vector3D> points = Arrays.asList(
            new Vector3D(1, 2, 3),
            new Vector3D(4, 5, 6),
            new Vector3D(-1, -2, -3)
        );
        BoundingBox box = BoundingBox.fromPoints(points);

        assertEquals(-1.0, box.getMin().x);
        assertEquals(-2.0, box.getMin().y);
        assertEquals(-3.0, box.getMin().z);
        assertEquals(4.0, box.getMax().x);
        assertEquals(5.0, box.getMax().y);
        assertEquals(6.0, box.getMax().z);
    }

    @Test
    void testFromPoints_SinglePoint() {
        List<Vector3D> points = Collections.singletonList(new Vector3D(5, 10, 15));
        BoundingBox box = BoundingBox.fromPoints(points);

        assertEquals(5.0, box.getMin().x);
        assertEquals(10.0, box.getMin().y);
        assertEquals(15.0, box.getMin().z);
        assertEquals(5.0, box.getMax().x);
        assertEquals(10.0, box.getMax().y);
        assertEquals(15.0, box.getMax().z);
    }

    @Test
    void testFromPoints_EmptyList() {
        List<Vector3D> points = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> BoundingBox.fromPoints(points));
    }

    @Test
    void testFromPoints_NullList() {
        assertThrows(NullPointerException.class, () -> BoundingBox.fromPoints(null));
    }

    @Test
    void testFromPoints_NullElement() {
        List<Vector3D> points = Arrays.asList(
            new Vector3D(1, 2, 3),
            null,
            new Vector3D(4, 5, 6)
        );
        assertThrows(NullPointerException.class, () -> BoundingBox.fromPoints(points));
    }

    @Test
    void testGetWidth() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 20, 30)
        );
        assertEquals(10.0, box.getWidth(), EPSILON);
    }

    @Test
    void testGetHeight() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 20, 30)
        );
        assertEquals(20.0, box.getHeight(), EPSILON);
    }

    @Test
    void testGetDepth() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 20, 30)
        );
        assertEquals(30.0, box.getDepth(), EPSILON);
    }

    @Test
    void testGetVolume() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 20, 30)
        );
        assertEquals(6000.0, box.getVolume(), EPSILON);
    }

    @Test
    void testGetVolume_ZeroVolume() {
        BoundingBox box = new BoundingBox(
            new Vector3D(5, 5, 5),
            new Vector3D(5, 5, 5)
        );
        assertEquals(0.0, box.getVolume(), EPSILON);
    }

    @Test
    void testGetCenter() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 20, 30)
        );
        Vector3D center = box.getCenter();

        assertEquals(5.0, center.x, EPSILON);
        assertEquals(10.0, center.y, EPSILON);
        assertEquals(15.0, center.z, EPSILON);
    }

    @Test
    void testContains_PointInside() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        assertTrue(box.contains(new Vector3D(5, 5, 5)));
    }

    @Test
    void testContains_PointOnBoundary() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        assertTrue(box.contains(new Vector3D(0, 0, 0)));
        assertTrue(box.contains(new Vector3D(10, 10, 10)));
        assertTrue(box.contains(new Vector3D(5, 0, 5)));
    }

    @Test
    void testContains_PointOutside() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        assertFalse(box.contains(new Vector3D(-1, 5, 5)));
        assertFalse(box.contains(new Vector3D(5, 11, 5)));
        assertFalse(box.contains(new Vector3D(5, 5, 15)));
    }

    @Test
    void testContains_NullPoint() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        assertThrows(NullPointerException.class, () -> box.contains(null));
    }

    @Test
    void testIntersects_Overlapping() {
        BoundingBox box1 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        BoundingBox box2 = new BoundingBox(
            new Vector3D(5, 5, 5),
            new Vector3D(15, 15, 15)
        );
        assertTrue(box1.intersects(box2));
        assertTrue(box2.intersects(box1));
    }

    @Test
    void testIntersects_Touching() {
        BoundingBox box1 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        BoundingBox box2 = new BoundingBox(
            new Vector3D(10, 0, 0),
            new Vector3D(20, 10, 10)
        );
        // Touching at boundary is considered intersection
        assertTrue(box1.intersects(box2));
    }

    @Test
    void testIntersects_Separated() {
        BoundingBox box1 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        BoundingBox box2 = new BoundingBox(
            new Vector3D(15, 15, 15),
            new Vector3D(25, 25, 25)
        );
        assertFalse(box1.intersects(box2));
        assertFalse(box2.intersects(box1));
    }

    @Test
    void testIntersects_Contained() {
        BoundingBox box1 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(20, 20, 20)
        );
        BoundingBox box2 = new BoundingBox(
            new Vector3D(5, 5, 5),
            new Vector3D(15, 15, 15)
        );
        assertTrue(box1.intersects(box2));
        assertTrue(box2.intersects(box1));
    }

    @Test
    void testIntersects_NullBox() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        assertThrows(NullPointerException.class, () -> box.intersects(null));
    }

    @Test
    void testExpand_ValidMargin() {
        BoundingBox box = new BoundingBox(
            new Vector3D(5, 5, 5),
            new Vector3D(10, 10, 10)
        );
        BoundingBox expanded = box.expand(5.0);

        assertEquals(0.0, expanded.getMin().x, EPSILON);
        assertEquals(0.0, expanded.getMin().y, EPSILON);
        assertEquals(0.0, expanded.getMin().z, EPSILON);
        assertEquals(15.0, expanded.getMax().x, EPSILON);
        assertEquals(15.0, expanded.getMax().y, EPSILON);
        assertEquals(15.0, expanded.getMax().z, EPSILON);
    }

    @Test
    void testExpand_ZeroMargin() {
        BoundingBox box = new BoundingBox(
            new Vector3D(5, 5, 5),
            new Vector3D(10, 10, 10)
        );
        BoundingBox expanded = box.expand(0.0);

        assertEquals(box, expanded);
    }

    @Test
    void testExpand_NegativeMargin() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        assertThrows(IllegalArgumentException.class, () -> box.expand(-5.0));
    }

    @Test
    void testTransform_IdentityRotation() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        BoundingBox transformed = box.transform(Matrix3x3.identity(), new Vector3D());

        assertEquals(box, transformed);
    }

    @Test
    void testTransform_Translation() {
        // Note: transform() now uses pivot-based rotation semantics
        // For pure translation, the box needs to be moved by adjusting min/max directly
        // This test verifies that transform with IDENTITY rotation and pivot at origin
        // behaves correctly (returns same box since rotation around origin with IDENTITY is no-op)
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );

        // Transform with IDENTITY around origin should return the same box
        BoundingBox transformed = box.transform(Matrix3x3.IDENTITY, Vector3D.ZERO);

        assertEquals(0.0, transformed.getMin().x, EPSILON);
        assertEquals(0.0, transformed.getMin().y, EPSILON);
        assertEquals(0.0, transformed.getMin().z, EPSILON);
        assertEquals(10.0, transformed.getMax().x, EPSILON);
        assertEquals(10.0, transformed.getMax().y, EPSILON);
        assertEquals(10.0, transformed.getMax().z, EPSILON);
    }

    @Test
    void testTransform_Rotation90Z() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 5, 3)
        );
        Matrix3x3 rotation = Matrix3x3.rotationZ(90);
        BoundingBox transformed = box.transform(rotation, Vector3D.ZERO);

        // After 90° Z rotation: (10,5,3) box becomes (-5,10,3) extent
        assertEquals(-5.0, transformed.getMin().x, EPSILON);
        assertEquals(0.0, transformed.getMin().y, EPSILON);
        assertEquals(0.0, transformed.getMin().z, EPSILON);
        assertEquals(0.0, transformed.getMax().x, EPSILON);
        assertEquals(10.0, transformed.getMax().y, EPSILON);
        assertEquals(3.0, transformed.getMax().z, EPSILON);
    }

    @Test
    void testTransform_NullParameters() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        assertThrows(NullPointerException.class,
            () -> box.transform(null, Vector3D.ZERO));
        assertThrows(NullPointerException.class,
            () -> box.transform(Matrix3x3.IDENTITY, null));
    }

    @Test
    void testUnion() {
        BoundingBox box1 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        BoundingBox box2 = new BoundingBox(
            new Vector3D(5, 5, 5),
            new Vector3D(15, 15, 15)
        );
        BoundingBox union = box1.union(box2);

        assertEquals(0.0, union.getMin().x);
        assertEquals(0.0, union.getMin().y);
        assertEquals(0.0, union.getMin().z);
        assertEquals(15.0, union.getMax().x);
        assertEquals(15.0, union.getMax().y);
        assertEquals(15.0, union.getMax().z);
    }

    @Test
    void testUnion_Disjoint() {
        BoundingBox box1 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(5, 5, 5)
        );
        BoundingBox box2 = new BoundingBox(
            new Vector3D(10, 10, 10),
            new Vector3D(15, 15, 15)
        );
        BoundingBox union = box1.union(box2);

        assertEquals(0.0, union.getMin().x);
        assertEquals(15.0, union.getMax().x);
    }

    @Test
    void testUnion_NullBox() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        assertThrows(NullPointerException.class, () -> box.union(null));
    }

    @Test
    void testGetSurfaceArea() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 20, 30)
        );
        // Surface area = 2*(10*20 + 20*30 + 30*10) = 2*(200 + 600 + 300) = 2200
        assertEquals(2200.0, box.getSurfaceArea(), EPSILON);
    }

    @Test
    void testGetSurfaceArea_Cube() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        // Surface area = 6 * 10 * 10 = 600
        assertEquals(600.0, box.getSurfaceArea(), EPSILON);
    }

    @Test
    void testEquals() {
        BoundingBox box1 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        BoundingBox box2 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        BoundingBox box3 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 11)
        );

        assertEquals(box1, box2);
        assertNotEquals(box1, box3);
        assertNotEquals(box1, null);
        assertNotEquals(box1, "not a box");
    }

    @Test
    void testHashCode() {
        BoundingBox box1 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        BoundingBox box2 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 10, 10)
        );
        assertEquals(box1.hashCode(), box2.hashCode());
    }

    @Test
    void testToString() {
        BoundingBox box = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(10, 20, 30)
        );
        String str = box.toString();
        assertTrue(str.contains("BoundingBox"));
        assertTrue(str.contains("10.00"));
        assertTrue(str.contains("20.00"));
        assertTrue(str.contains("30.00"));
    }

    @Test
    void testIntersects_WithGapConstraint() {
        // Test case for 5mm gap constraint enforcement
        BoundingBox box1 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(100, 100, 100)
        );
        BoundingBox box2 = new BoundingBox(
            new Vector3D(105, 0, 0),
            new Vector3D(200, 100, 100)
        );

        // Without expansion, boxes should not intersect (5mm gap)
        assertFalse(box1.intersects(box2));

        // With 5mm expansion, they should intersect (simulating gap violation)
        BoundingBox expanded1 = box1.expand(5.0);
        assertTrue(expanded1.intersects(box2));
    }

    @Test
    void testImmutability() {
        Vector3D min = new Vector3D(0, 0, 0);
        Vector3D max = new Vector3D(10, 10, 10);
        BoundingBox box = new BoundingBox(min, max);

        // Operations should return new instances
        BoundingBox expanded = box.expand(5.0);
        assertNotSame(box, expanded);
        assertEquals(0.0, box.getMin().x); // Original unchanged

        BoundingBox transformed = box.transform(Matrix3x3.IDENTITY, new Vector3D(1, 1, 1));
        assertNotSame(box, transformed);
        assertEquals(0.0, box.getMin().x); // Original unchanged
    }
}
