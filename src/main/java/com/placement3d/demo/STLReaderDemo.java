package com.placement3d.demo;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.io.STLParser;
import com.placement3d.model.Mesh;
import com.placement3d.model.ModelInfo;
import com.placement3d.model.Vector3D;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * STL文件读取演示程序
 *
 * 功能：
 * 1. 读取STLmodels文件夹中的所有.stl文件
 * 2. 解析每个文件的网格数据
 * 3. 输出模型的基本信息（三角形数量、体积、包围盒等）
 */
public class STLReaderDemo {

    public static void main(String[] args) {
        String stlFolderPath = "STLmodels";

        System.out.println("========================================");
        System.out.println("  STL文件读取与解析测试");
        System.out.println("========================================\n");

        File folder = new File(stlFolderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("错误：STLmodels文件夹不存在！");
            return;
        }

        // 获取所有.stl文件
        File[] stlFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".stl"));

        if (stlFiles == null || stlFiles.length == 0) {
            System.err.println("错误：STLmodels文件夹中没有.stl文件！");
            return;
        }

        System.out.println("找到 " + stlFiles.length + " 个STL文件\n");

        List<ModelInfo> models = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        // 逐个解析文件
        for (int i = 0; i < stlFiles.length; i++) {
            File file = stlFiles[i];
            System.out.println("----------------------------------------");
            System.out.println("(" + (i + 1) + "/" + stlFiles.length + ") 解析文件: " + file.getName());

            try {
                long startTime = System.currentTimeMillis();

                // 使用STLParser解析文件（需要创建实例）
                STLParser parser = new STLParser();
                Mesh mesh = parser.parse(file.getAbsolutePath());

                // 创建ModelInfo
                ModelInfo modelInfo = new ModelInfo(file.getName(), mesh, i);
                models.add(modelInfo);

                long endTime = System.currentTimeMillis();

                // 输出模型信息
                System.out.println("  ✓ 解析成功 (耗时: " + (endTime - startTime) + "ms)");
                System.out.println("  - 三角形数量: " + mesh.getTriangles().size());
                System.out.println("  - 体积: " + String.format("%.2f", modelInfo.getVolume()) + " mm³");

                BoundingBox bbox = modelInfo.getOriginalAABB();
                System.out.println("  - 包围盒尺寸: " +
                    String.format("%.2f × %.2f × %.2f mm",
                        bbox.getWidth(), bbox.getHeight(), bbox.getDepth()));

                Vector3D centroid = modelInfo.getCenterOfMass();
                System.out.println("  - 质心坐标: (" +
                    String.format("%.2f, %.2f, %.2f",
                        centroid.getX(), centroid.getY(), centroid.getZ()) + ")");

                successCount++;

            } catch (IOException e) {
                System.err.println("  ✗ 解析失败: " + e.getMessage());
                failCount++;
            } catch (Exception e) {
                System.err.println("  ✗ 解析失败（异常）: " + e.getMessage());
                e.printStackTrace();
                failCount++;
            }
        }

        // 输出汇总信息
        System.out.println("\n========================================");
        System.out.println("  解析结果汇总");
        System.out.println("========================================");
        System.out.println("总文件数: " + stlFiles.length);
        System.out.println("成功: " + successCount);
        System.out.println("失败: " + failCount);
        System.out.println("成功率: " + String.format("%.1f%%", (successCount * 100.0 / stlFiles.length)));

        if (successCount > 0) {
            System.out.println("\n已解析模型列表：");
            for (int i = 0; i < models.size(); i++) {
                ModelInfo model = models.get(i);
                System.out.println(String.format("  %2d. %-30s | %8d 三角形 | %10.2f mm³",
                    (i + 1),
                    model.getFileName(),
                    model.getMesh().getTriangles().size(),
                    model.getVolume()));
            }
        }

        System.out.println("\n测试完成！");
    }
}
