package com.placement3d.demo;

import com.placement3d.model.Vector3D;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 调试STL文件中的退化三角形
 */
public class DebugSTL {

    public static void main(String[] args) throws IOException {
        String[] files = {
            "STLmodels/33_shell_3.stl",
            "STLmodels/33_shell_4.stl",
            "STLmodels/33_shell_9.stl"
        };

        for (String filePath : files) {
            System.out.println("\n========================================");
            System.out.println("分析文件: " + filePath);
            System.out.println("========================================");
            analyzeFile(filePath);
        }
    }

    private static void analyzeFile(String filePath) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(filePath));
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        // 跳过80字节头
        buffer.position(80);
        long triangleCount = Integer.toUnsignedLong(buffer.getInt());

        System.out.println("三角形总数: " + triangleCount);

        int degenerateCount = 0;
        int tinyAreaCount = 0;
        double minCrossLength = Double.MAX_VALUE;
        int minCrossIndex = -1;

        for (int i = 0; i < triangleCount; i++) {
            // 跳过法向量 (12字节)
            buffer.position(buffer.position() + 12);

            // 读取三个顶点
            Vector3D v1 = readVector(buffer);
            Vector3D v2 = readVector(buffer);
            Vector3D v3 = readVector(buffer);

            // 跳过属性字节 (2字节)
            buffer.position(buffer.position() + 2);

            // 计算叉积
            Vector3D edge1 = v2.subtract(v1);
            Vector3D edge2 = v3.subtract(v1);
            Vector3D cross = edge1.cross(edge2);
            double crossLength = cross.length();

            if (crossLength < minCrossLength) {
                minCrossLength = crossLength;
                minCrossIndex = i;
            }

            if (crossLength < 1e-6) {
                degenerateCount++;
                if (degenerateCount <= 5) {
                    System.out.println("\n退化三角形 #" + i + ":");
                    System.out.println("  v1: " + v1);
                    System.out.println("  v2: " + v2);
                    System.out.println("  v3: " + v3);
                    System.out.println("  叉积长度: " + crossLength);
                    System.out.println("  面积: " + (crossLength / 2.0) + " mm²");
                }
            } else if (crossLength < 0.01) {
                tinyAreaCount++;
            }
        }

        System.out.println("\n统计结果:");
        System.out.println("  退化三角形 (叉积<1e-6): " + degenerateCount);
        System.out.println("  微小面积 (叉积<0.01): " + tinyAreaCount);
        System.out.println("  最小叉积长度: " + minCrossLength + " (三角形#" + minCrossIndex + ")");
        System.out.println("  建议EPSILON阈值: " + (minCrossLength * 0.5));
    }

    private static Vector3D readVector(ByteBuffer buffer) {
        float x = buffer.getFloat();
        float y = buffer.getFloat();
        float z = buffer.getFloat();
        return new Vector3D(x, y, z);
    }
}
