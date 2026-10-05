package com.placement3d.io;

import com.placement3d.model.Mesh;
import com.placement3d.model.Triangle;
import com.placement3d.model.Vector3D;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parser for STL (STereoLithography) files in Binary format.
 *
 * <p>STL is a widely-used file format for 3D printing and CAD software. It represents
 * 3D geometry as a collection of triangular facets. This parser supports the Binary STL
 * format, which is more compact and faster to parse than ASCII STL.</p>
 *
 * <h2>Binary STL Format Specification</h2>
 * <pre>
 * HEADER (80 bytes)
 * ├─ Bytes 0-79: Header string (typically contains model name, ignored by parser)
 *
 * TRIANGLE COUNT (4 bytes)
 * ├─ Bytes 80-83: Unsigned 32-bit integer (little-endian), number of triangles
 *
 * TRIANGLE DATA (50 bytes per triangle, repeated N times)
 * ├─ Bytes 0-11:   Normal vector (3 × float32, little-endian)
 * │                ├─ nx (4 bytes)
 * │                ├─ ny (4 bytes)
 * │                └─ nz (4 bytes)
 * ├─ Bytes 12-23:  Vertex 1 coordinates (3 × float32, little-endian)
 * │                ├─ v1x (4 bytes)
 * │                ├─ v1y (4 bytes)
 * │                └─ v1z (4 bytes)
 * ├─ Bytes 24-35:  Vertex 2 coordinates (3 × float32, little-endian)
 * │                ├─ v2x (4 bytes)
 * │                ├─ v2y (4 bytes)
 * │                └─ v2z (4 bytes)
 * ├─ Bytes 36-47:  Vertex 3 coordinates (3 × float32, little-endian)
 * │                ├─ v3x (4 bytes)
 * │                ├─ v3y (4 bytes)
 * │                └─ v3z (4 bytes)
 * └─ Bytes 48-49:  Attribute byte count (uint16, typically 0, ignored)
 *
 * Total file size = 80 + 4 + (50 × triangle_count) bytes
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * STLParser parser = new STLParser();
 * Mesh mesh = parser.parse("models/cube.stl");
 * System.out.println("Loaded mesh with " + mesh.getTriangleCount() + " triangles");
 * }</pre>
 *
 * <h2>Assumptions and Limitations</h2>
 * <ul>
 *   <li>Only Binary STL format is supported (ASCII STL is not supported)</li>
 *   <li>Uses little-endian byte order (standard for Binary STL)</li>
 *   <li>Normal vectors from the file are used but not validated</li>
 *   <li>Attribute bytes are ignored (rarely used in practice)</li>
 *   <li>No mesh validation (watertight, manifold, etc.) is performed</li>
 * </ul>
 *
 * @author 3D Placement System
 * @version 1.0
 * @see Mesh
 * @see Triangle
 */
public class STLParser {

    /** Size of the STL header in bytes */
    private static final int HEADER_SIZE = 80;

    /** Size of the triangle count field in bytes */
    private static final int TRIANGLE_COUNT_SIZE = 4;

    /** Size of each triangle record in bytes */
    private static final int TRIANGLE_SIZE = 50;

    /** Size of a float32 in bytes */
    private static final int FLOAT_SIZE = 4;

    /** Size of the attribute field in bytes */
    private static final int ATTRIBUTE_SIZE = 2;

    /**
     * Creates a new STL parser instance.
     */
    public STLParser() {
        // Default constructor
    }

    /**
     * Parses a Binary STL file and returns the resulting mesh.
     *
     * <p>This method reads the entire file into memory and constructs a {@link Mesh}
     * object containing all triangular facets. The file must be in Binary STL format;
     * ASCII STL files will cause parsing errors.</p>
     *
     * @param filePath the absolute or relative path to the STL file
     * @return a Mesh object containing all triangles from the file
     * @throws NullPointerException if filePath is null
     * @throws IllegalArgumentException if the file is not a valid Binary STL file
     * @throws IOException if an I/O error occurs while reading the file
     */
    public Mesh parse(String filePath) throws IOException {
        Objects.requireNonNull(filePath, "File path cannot be null");

        Path path = Paths.get(filePath);

        // Validate file exists
        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + filePath);
        }

        // Validate file is readable
        if (!Files.isReadable(path)) {
            throw new IOException("File is not readable: " + filePath);
        }

        // Read the entire file into a byte array
        byte[] fileData = readFile(path);

        // Parse the binary data
        return parseBinarySTL(fileData, filePath);
    }

    /**
     * Reads the entire file into a byte array.
     *
     * @param path the file path
     * @return the file contents as a byte array
     * @throws IOException if an I/O error occurs
     */
    private byte[] readFile(Path path) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(path.toFile()))) {
            return inputStream.readAllBytes();
        }
    }

    /**
     * Parses Binary STL data from a byte array.
     *
     * @param data the binary STL file data
     * @param filePath the original file path (for error messages)
     * @return a Mesh object
     * @throws IllegalArgumentException if the data is invalid
     */
    private Mesh parseBinarySTL(byte[] data, String filePath) {
        // Validate minimum file size (header + triangle count)
        if (data.length < HEADER_SIZE + TRIANGLE_COUNT_SIZE) {
            throw new IllegalArgumentException(
                "File too small to be a valid Binary STL file: " + filePath +
                " (expected at least " + (HEADER_SIZE + TRIANGLE_COUNT_SIZE) + " bytes, got " + data.length + ")"
            );
        }

        // Wrap data in ByteBuffer with little-endian byte order
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Skip the 80-byte header (not used for validation)
        buffer.position(HEADER_SIZE);

        // Read triangle count (unsigned 32-bit integer)
        long triangleCount = Integer.toUnsignedLong(buffer.getInt());

        // Validate triangle count fits in int (for ArrayList sizing)
        if (triangleCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                "Triangle count exceeds maximum supported: " + triangleCount + " (max: " + Integer.MAX_VALUE + ")"
            );
        }

        // Validate expected file size
        long expectedSize = HEADER_SIZE + TRIANGLE_COUNT_SIZE + (triangleCount * TRIANGLE_SIZE);
        if (data.length < expectedSize) {
            throw new IllegalArgumentException(
                "File size mismatch in STL file: " + filePath +
                " (expected " + expectedSize + " bytes for " + triangleCount + " triangles, got " + data.length + ")"
            );
        }

        // Parse triangles
        List<Triangle> triangles = new ArrayList<>((int) triangleCount);

        for (int i = 0; i < triangleCount; i++) {
            try {
                Triangle triangle = parseTriangle(buffer);
                triangles.add(triangle);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "Error parsing triangle " + i + " in file: " + filePath, e
                );
            }
        }

        // Create and return the mesh
        return new Mesh(triangles, filePath);
    }

    /**
     * Parses a single triangle from the ByteBuffer.
     *
     * <p>Binary STL triangle structure (50 bytes):
     * <ul>
     *   <li>Normal vector (12 bytes): nx, ny, nz</li>
     *   <li>Vertex 1 (12 bytes): v1x, v1y, v1z</li>
     *   <li>Vertex 2 (12 bytes): v2x, v2y, v2z</li>
     *   <li>Vertex 3 (12 bytes): v3x, v3y, v3z</li>
     *   <li>Attribute byte count (2 bytes): unused</li>
     * </ul>
     *
     * @param buffer the ByteBuffer positioned at the start of a triangle record
     * @return a Triangle object
     */
    private Triangle parseTriangle(ByteBuffer buffer) {
        // Skip normal vector (3 floats = 12 bytes)
        // STL file normals are often unreliable; Triangle will auto-calculate accurate normals
        buffer.position(buffer.position() + 3 * FLOAT_SIZE);

        // Read vertex 1 (3 floats = 12 bytes)
        float v1x = buffer.getFloat();
        float v1y = buffer.getFloat();
        float v1z = buffer.getFloat();
        Vector3D vertex1 = new Vector3D(v1x, v1y, v1z);

        // Read vertex 2 (3 floats = 12 bytes)
        float v2x = buffer.getFloat();
        float v2y = buffer.getFloat();
        float v2z = buffer.getFloat();
        Vector3D vertex2 = new Vector3D(v2x, v2y, v2z);

        // Read vertex 3 (3 floats = 12 bytes)
        float v3x = buffer.getFloat();
        float v3y = buffer.getFloat();
        float v3z = buffer.getFloat();
        Vector3D vertex3 = new Vector3D(v3x, v3y, v3z);

        // Skip attribute bytes (2 bytes, typically 0)
        buffer.position(buffer.position() + ATTRIBUTE_SIZE);

        // Ignore file normal and let Triangle auto-calculate for accuracy
        return new Triangle(vertex1, vertex2, vertex3);
    }
}
