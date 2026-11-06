package com.placement3d.demo;

import com.placement3d.algorithm.GreedyPlacementStrategy;
import com.placement3d.algorithm.PlacementStrategy;
import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.*;
import com.placement3d.space.SpaceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 调试贪心摆放算法的间隙问题
 */
public class DebugGreedyPlacement {

    private static final float MIN_GAP = 5.0f;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  调试贪心摆放算法间隙问题");
        System.out.println("========================================\n");

        // 创建3个100mm立方体
        List<ModelInfo> models = new ArrayList<>();
        models.add(createCubeMesh(100, 0));
        models.add(createCubeMesh(100, 1));
        models.add(createCubeMesh(100, 2));

        // 创建工作空间
        BoundingBox workspace = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(1000, 1000, 500)
        );

        // 执行摆放
        PlacementStrategy strategy = new GreedyPlacementStrategy();
        List<PlacedModel> result = strategy.place(models, workspace, MIN_GAP);

        System.out.println("摆放结果: " + result.size() + " 个模型\n");

        // 输出每个模型的详细信息
        for (int i = 0; i < result.size(); i++) {
            PlacedModel placed = result.get(i);
            BoundingBox aabb = placed.getTransformedAABB();

            System.out.println("模型 " + i + ":");
            System.out.println("  质心位置: " + placed.getPosition());
            System.out.println("  AABB: " + aabb.getMin() + " -> " + aabb.getMax());
            System.out.println("  尺寸: " + String.format("%.2f × %.2f × %.2f mm",
                aabb.getWidth(), aabb.getHeight(), aabb.getDepth()));
            System.out.println();
        }

        // 检查所有模型对之间的间隙
        System.out.println("========================================");
        System.out.println("  间隙检查");
        System.out.println("========================================\n");

        for (int i = 0; i < result.size(); i++) {
            for (int j = i + 1; j < result.size(); j++) {
                BoundingBox box1 = result.get(i).getTransformedAABB();
                BoundingBox box2 = result.get(j).getTransformedAABB();

                float gapX = Math.max(0, Math.min(
                    box2.getMin().getX() - box1.getMax().getX(),
                    box1.getMin().getX() - box2.getMax().getX()
                ));

                float gapY = Math.max(0, Math.min(
                    box2.getMin().getY() - box1.getMax().getY(),
                    box1.getMin().getY() - box2.getMax().getY()
                ));

                float gapZ = Math.max(0, Math.min(
                    box2.getMin().getZ() - box1.getMax().getZ(),
                    box1.getMin().getZ() - box2.getMax().getZ()
                ));

                float minGap = Math.max(Math.max(gapX, gapY), gapZ);

                System.out.println("模型 " + i + " <-> 模型 " + j + ":");
                System.out.println("  X方向间隙: " + String.format("%.2f mm", gapX));
                System.out.println("  Y方向间隙: " + String.format("%.2f mm", gapY));
                System.out.println("  Z方向间隙: " + String.format("%.2f mm", gapZ));
                System.out.println("  最小间隙: " + String.format("%.2f mm", minGap));

                if (minGap < MIN_GAP) {
                    System.out.println("  ❌ 违反间隙约束！");
                } else {
                    System.out.println("  ✓ 满足间隙约束");
                }
                System.out.println();
            }
        }
    }

    private static ModelInfo createCubeMesh(float size, int id) {
        List<Triangle> triangles = new ArrayList<>();

        // 创建立方体的12个三角形
        float half = size / 2;
        Vector3D[] vertices = {
            new Vector3D(-half, -half, -half),
            new Vector3D(half, -half, -half),
            new Vector3D(half, half, -half),
            new Vector3D(-half, half, -half),
            new Vector3D(-half, -half, half),
            new Vector3D(half, -half, half),
            new Vector3D(half, half, half),
            new Vector3D(-half, half, half)
        };

        // 底面
        triangles.add(new Triangle(vertices[0], vertices[1], vertices[2]));
        triangles.add(new Triangle(vertices[0], vertices[2], vertices[3]));
        // 顶面
        triangles.add(new Triangle(vertices[4], vertices[6], vertices[5]));
        triangles.add(new Triangle(vertices[4], vertices[7], vertices[6]));
        // 前面
        triangles.add(new Triangle(vertices[0], vertices[4], vertices[5]));
        triangles.add(new Triangle(vertices[0], vertices[5], vertices[1]));
        // 后面
        triangles.add(new Triangle(vertices[2], vertices[6], vertices[7]));
        triangles.add(new Triangle(vertices[2], vertices[7], vertices[3]));
        // 左面
        triangles.add(new Triangle(vertices[0], vertices[3], vertices[7]));
        triangles.add(new Triangle(vertices[0], vertices[7], vertices[4]));
        // 右面
        triangles.add(new Triangle(vertices[1], vertices[5], vertices[6]));
        triangles.add(new Triangle(vertices[1], vertices[6], vertices[2]));

        Mesh mesh = new Mesh(triangles, "cube_" + id + ".stl");
        return new ModelInfo("cube_" + id + ".stl", mesh, id);
    }
}
