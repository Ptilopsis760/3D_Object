import com.placement3d.algorithm.*;
import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.*;
import com.placement3d.space.*;
import java.util.*;

public class debug_placement {
    public static void main(String[] args) {
        // 创建工作空间
        BoundingBox workspace = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(1000, 1000, 1000)
        );
        
        // 创建100mm立方体
        List<Triangle> triangles = new ArrayList<>();
        Vector3D v000 = new Vector3D(0, 0, 0);
        Vector3D v100 = new Vector3D(100, 0, 0);
        Vector3D v110 = new Vector3D(100, 100, 0);
        Vector3D v010 = new Vector3D(0, 100, 0);
        Vector3D v001 = new Vector3D(0, 0, 100);
        Vector3D v101 = new Vector3D(100, 0, 100);
        Vector3D v111 = new Vector3D(100, 100, 100);
        Vector3D v011 = new Vector3D(0, 100, 100);

        triangles.add(new Triangle(v000, v100, v110));
        triangles.add(new Triangle(v000, v110, v010));
        triangles.add(new Triangle(v001, v111, v101));
        triangles.add(new Triangle(v001, v011, v111));
        triangles.add(new Triangle(v000, v101, v100));
        triangles.add(new Triangle(v000, v001, v101));
        triangles.add(new Triangle(v010, v110, v111));
        triangles.add(new Triangle(v010, v111, v011));
        triangles.add(new Triangle(v000, v010, v011));
        triangles.add(new Triangle(v000, v011, v001));
        triangles.add(new Triangle(v100, v111, v110));
        triangles.add(new Triangle(v100, v101, v111));

        Mesh mesh = new Mesh(triangles, "cube_100.stl");
        ModelInfo model = new ModelInfo("cube_100.stl", mesh, 0);
        
        System.out.println("模型信息:");
        System.out.println("  文件名: " + model.getFileName());
        System.out.println("  体积: " + model.getVolume());
        System.out.println("  质心: " + model.getCenterOfMass());
        System.out.println("  原始AABB: " + model.getOriginalAABB());
        
        // 测试摆放
        SpaceManager spaceManager = new SpaceManager(workspace, 5.0f);
        
        // 尝试最简单的位置
        Vector3D testPosition = new Vector3D(60, 60, 55);
        PlacedModel testPlacement = PlacedModel.create(model, Matrix3x3.IDENTITY, testPosition);
        
        System.out.println("\n测试摆放:");
        System.out.println("  位置: " + testPosition);
        System.out.println("  变换后AABB: " + testPlacement.getTransformedAABB());
        System.out.println("  canPlace: " + spaceManager.canPlace(testPlacement));
        
        if (!spaceManager.canPlace(testPlacement)) {
            // 检查边界
            CollisionDetector detector = new CollisionDetector();
            boolean boundaryViolation = detector.checkBoundaryViolation(testPlacement, workspace, 5.0f);
            System.out.println("  边界违规: " + boundaryViolation);
            
            BoundingBox aabb = testPlacement.getTransformedAABB();
            System.out.println("  AABB min: " + aabb.getMin());
            System.out.println("  AABB max: " + aabb.getMax());
            System.out.println("  Workspace min: " + workspace.getMin());
            System.out.println("  Workspace max: " + workspace.getMax());
        }
    }
}
