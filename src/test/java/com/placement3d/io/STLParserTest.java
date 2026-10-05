package com.placement3d.io;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.Mesh;
import com.placement3d.model.Vector3D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the STLParser class.
 */
class STLParserTest {

    private STLParser parser;
    private static final double EPSILON = 1e-6;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new STLParser();
    }

    @Test
    @DisplayName("Should parse valid binary STL file with single triangle")
    void testParseSingleTriangle() throws IOException {
        // Create a simple STL file with one triangle
        Path stlFile = createBinarySTL(tempDir, "single_triangle.stl", 1);

        Mesh mesh = parser.parse(stlFile.toString());

        assertNotNull(mesh);
        assertEquals(1, mesh.getTriangleCount());
        assertNotNull(mesh.getBoundingBox());
        assertNotNull(mesh.getCentroid());
    }

    @Test
    @DisplayName("Should parse binary STL cube with 12 triangles")
    void testParseCube() throws IOException {
        // Create a cube STL file (12 triangles = 2 per face)
        Path stlFile = createCubeSTL(tempDir, "cube.stl");

        Mesh mesh = parser.parse(stlFile.toString());

        assertNotNull(mesh);
        assertEquals(12, mesh.getTriangleCount());

        // Verify bounding box is approximately a unit cube
        BoundingBox bbox = mesh.getBoundingBox();
        assertEquals(0.0, bbox.getMin().getX(), EPSILON);
        assertEquals(0.0, bbox.getMin().getY(), EPSILON);
        assertEquals(0.0, bbox.getMin().getZ(), EPSILON);
        assertEquals(1.0, bbox.getMax().getX(), EPSILON);
        assertEquals(1.0, bbox.getMax().getY(), EPSILON);
        assertEquals(1.0, bbox.getMax().getZ(), EPSILON);

        // Verify volume is close to 1.0 cubic units
        assertEquals(1.0, mesh.getVolume(), 0.1);
    }

    @Test
    @DisplayName("Should throw exception when file path is null")
    void testParseWithNullPath() {
        assertThrows(NullPointerException.class, () -> parser.parse(null));
    }

    @Test
    @DisplayName("Should throw exception when file does not exist")
    void testParseNonExistentFile() {
        assertThrows(IOException.class, () -> parser.parse("non_existent_file.stl"));
    }

    @Test
    @DisplayName("Should throw exception when file is too small")
    void testParseTooSmallFile() throws IOException {
        // Create a file that's too small to be valid STL (less than 84 bytes)
        Path stlFile = tempDir.resolve("too_small.stl");
        Files.write(stlFile, new byte[50]);

        assertThrows(IllegalArgumentException.class, () -> parser.parse(stlFile.toString()));
    }

    @Test
    @DisplayName("Should throw exception when triangle count is inconsistent with file size")
    void testParseInconsistentTriangleCount() throws IOException {
        // Create a file with incorrect triangle count
        Path stlFile = tempDir.resolve("bad_count.stl");
        ByteBuffer buffer = ByteBuffer.allocate(84);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Write header (80 bytes of zeros)
        buffer.position(80);

        // Write incorrect triangle count (claims 100 triangles but file is too small)
        buffer.putInt(100);

        Files.write(stlFile, buffer.array());

        assertThrows(IllegalArgumentException.class, () -> parser.parse(stlFile.toString()));
    }

    @Test
    @DisplayName("Should parse multiple triangles correctly")
    void testParseMultipleTriangles() throws IOException {
        int triangleCount = 24;
        Path stlFile = createBinarySTL(tempDir, "multi_triangle.stl", triangleCount);

        Mesh mesh = parser.parse(stlFile.toString());

        assertNotNull(mesh);
        assertEquals(triangleCount, mesh.getTriangleCount());
    }

    @Test
    @DisplayName("Should handle empty header correctly")
    void testParseWithEmptyHeader() throws IOException {
        // Create STL with empty header (all zeros)
        Path stlFile = createBinarySTL(tempDir, "empty_header.stl", 2);

        Mesh mesh = parser.parse(stlFile.toString());

        assertNotNull(mesh);
        assertEquals(2, mesh.getTriangleCount());
    }

    @Test
    @DisplayName("Should reject triangles with collinear vertices (degenerate triangles)")
    void testParseDegenerateTriangles() throws IOException {
        // Create STL with degenerate triangle (collinear vertices)
        Path stlFile = tempDir.resolve("degenerate.stl");
        ByteBuffer buffer = ByteBuffer.allocate(84 + 50);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Write header
        buffer.position(80);

        // Write triangle count
        buffer.putInt(1);

        // Write normal (will be ignored, auto-calculated instead)
        buffer.putFloat(0.0f);
        buffer.putFloat(0.0f);
        buffer.putFloat(1.0f);

        // Write collinear vertices (all on same line) - this creates a degenerate triangle
        buffer.putFloat(0.0f);
        buffer.putFloat(0.0f);
        buffer.putFloat(0.0f);

        buffer.putFloat(1.0f);
        buffer.putFloat(0.0f);
        buffer.putFloat(0.0f);

        buffer.putFloat(2.0f);
        buffer.putFloat(0.0f);
        buffer.putFloat(0.0f);

        // Write attribute bytes
        buffer.putShort((short) 0);

        Files.write(stlFile, buffer.array());

        // Triangle constructor validates and should throw exception for collinear vertices
        assertThrows(IllegalArgumentException.class, () -> parser.parse(stlFile.toString()));
    }

    @Test
    @DisplayName("Should parse large STL file efficiently")
    void testParseLargeFile() throws IOException {
        // Create a larger STL file with 1000 triangles
        int triangleCount = 1000;
        Path stlFile = createBinarySTL(tempDir, "large.stl", triangleCount);

        long startTime = System.currentTimeMillis();
        Mesh mesh = parser.parse(stlFile.toString());
        long endTime = System.currentTimeMillis();

        assertNotNull(mesh);
        assertEquals(triangleCount, mesh.getTriangleCount());

        // Should parse reasonably fast (less than 5 seconds)
        assertTrue(endTime - startTime < 5000, "Parsing should be fast");
    }

    // Helper methods

    /**
     * Creates a binary STL file with the specified number of triangles.
     * Each triangle is a simple right triangle in the XY plane.
     */
    private Path createBinarySTL(Path directory, String filename, int triangleCount) throws IOException {
        Path stlFile = directory.resolve(filename);

        // Calculate file size: 80 (header) + 4 (count) + 50 * triangleCount
        int fileSize = 80 + 4 + (50 * triangleCount);
        ByteBuffer buffer = ByteBuffer.allocate(fileSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Write header (80 bytes) - can be any data, typically model name
        byte[] header = "Binary STL Test File".getBytes();
        buffer.put(header);
        buffer.position(80);

        // Write triangle count
        buffer.putInt(triangleCount);

        // Write triangles
        for (int i = 0; i < triangleCount; i++) {
            writeTriangle(buffer, i);
        }

        Files.write(stlFile, buffer.array());
        return stlFile;
    }

    /**
     * Creates a binary STL file representing a unit cube (1x1x1).
     */
    private Path createCubeSTL(Path directory, String filename) throws IOException {
        Path stlFile = directory.resolve(filename);

        // A cube has 6 faces, each with 2 triangles = 12 triangles total
        int triangleCount = 12;
        int fileSize = 80 + 4 + (50 * triangleCount);
        ByteBuffer buffer = ByteBuffer.allocate(fileSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Write header
        buffer.position(80);

        // Write triangle count
        buffer.putInt(triangleCount);

        // Define cube vertices
        float[][] vertices = {
            {0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0},  // Bottom face
            {0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}   // Top face
        };

        // Define triangles (vertex indices and normals)
        int[][] faces = {
            // Bottom face (z=0, normal pointing down)
            {0, 1, 2}, {0, 2, 3},
            // Top face (z=1, normal pointing up)
            {4, 6, 5}, {4, 7, 6},
            // Front face (y=0)
            {0, 5, 1}, {0, 4, 5},
            // Back face (y=1)
            {3, 2, 6}, {3, 6, 7},
            // Left face (x=0)
            {0, 3, 7}, {0, 7, 4},
            // Right face (x=1)
            {1, 6, 2}, {1, 5, 6}
        };

        float[][] normals = {
            {0, 0, -1}, {0, 0, -1},  // Bottom
            {0, 0, 1}, {0, 0, 1},    // Top
            {0, -1, 0}, {0, -1, 0},  // Front
            {0, 1, 0}, {0, 1, 0},    // Back
            {-1, 0, 0}, {-1, 0, 0},  // Left
            {1, 0, 0}, {1, 0, 0}     // Right
        };

        // Write each triangle
        for (int i = 0; i < triangleCount; i++) {
            // Write normal
            buffer.putFloat(normals[i][0]);
            buffer.putFloat(normals[i][1]);
            buffer.putFloat(normals[i][2]);

            // Write three vertices
            for (int j = 0; j < 3; j++) {
                int vertexIndex = faces[i][j];
                buffer.putFloat(vertices[vertexIndex][0]);
                buffer.putFloat(vertices[vertexIndex][1]);
                buffer.putFloat(vertices[vertexIndex][2]);
            }

            // Write attribute bytes (unused)
            buffer.putShort((short) 0);
        }

        Files.write(stlFile, buffer.array());
        return stlFile;
    }

    /**
     * Writes a single triangle to the buffer.
     * Creates a simple right triangle in the XY plane with varying position based on index.
     */
    private void writeTriangle(ByteBuffer buffer, int index) {
        float offset = index * 0.1f;

        // Write normal (pointing up in Z direction)
        buffer.putFloat(0.0f);
        buffer.putFloat(0.0f);
        buffer.putFloat(1.0f);

        // Write vertex 1
        buffer.putFloat(0.0f + offset);
        buffer.putFloat(0.0f + offset);
        buffer.putFloat(0.0f);

        // Write vertex 2
        buffer.putFloat(1.0f + offset);
        buffer.putFloat(0.0f + offset);
        buffer.putFloat(0.0f);

        // Write vertex 3
        buffer.putFloat(0.5f + offset);
        buffer.putFloat(1.0f + offset);
        buffer.putFloat(0.0f);

        // Write attribute bytes (unused)
        buffer.putShort((short) 0);
    }
}
