import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.*;
import com.placement3d.space.*;
import java.util.ArrayList;
import java.util.List;

public class debug_tests {
    public static void main(String[] args) {
        System.out.println("=== 调试测试失败 ===\n");
        
        // 测试1: OctreeTest.testInsertManyModels
        System.out.println("测试1: Octree插入100个模型");
        BoundingBox workspace = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(4000, 2500, 1500)
        );
        Octree octree = new Octree(workspace);
        
        int insertCount = 0;
        for (int i = 0; i < 100; i++) {
            float x = (i % 10) * 400;
            float y = ((i / 10) % 10) * 250;
            float z = (i / 100) * 150;
            PlacedModel model = createModel("M" + i, x, y, z, x + 100, y + 100, z + 100);
            boolean inserted = octree.insert(model);
            if (inserted) {
                insertCount++;
            } else {
                System.out.println("  模型 " + i + " 插入失败: x=" + x + ", y=" + y + ", z=" + z);
            }
        }
        System.out.println("  期望: 100, 实际: " + octree.size() + ", 插入成功: " + insertCount);
        System.out.println();
        
        // 测试2: SpaceManager碰撞检测
        System.out.println("测试2: SpaceManager碰撞检测");
        BoundingBox workspace2 = new BoundingBox(
            new Vector3D(0, 0, 0),
            new Vector3D(1000, 1000, 1000)
        );
        SpaceManager spaceManager = new SpaceManager(workspace2, 5.0f);
        
        List<Vector3D> vertices = new ArrayList<>();
        vertices.add(new Vector3D(0, 0, 0));
        vertices.add(new Vector3D(100, 0, 0));
        vertices.add(new Vector3D(100, 100, 0));
        
        List<Triangle> triangles = new ArrayList<>();
        triangles.add(new Triangle(vertices.get(0), vertices.get(1), vertices.get(2)));
        
        Mesh mesh = new Mesh(triangles, "cube.stl");
        ModelInfo cubeModel = new ModelInfo("cube.stl", mesh, 0);
        
        PlacedModel model1 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(100, 100, 100));
        spaceManager.place(model1);
        System.out.println("  Model1 AABB: " + model1.getTransformedAABB());
        
        PlacedModel model2 = new PlacedModel(cubeModel, Matrix3x3.IDENTITY, new Vector3D(150, 150, 150));
        System.out.println("  Model2 AABB: " + model2.getTransformedAABB());
        
        boolean canPlace = spaceManager.canPlace(model2);
        System.out.println("  Model2 canPlace: " + canPlace + " (期望: false)");
        
        // 计算两个模型的实际距离
        BoundingBox bbox1 = model1.getTransformedAABB();
        BoundingBox bbox2 = model2.getTransformedAABB();
        System.out.println("  Model1 范围: [" + bbox1.getMin() + " -> " + bbox1.getMax() + "]");
        System.out.println("  Model2 范围: [" + bbox2.getMin() + " -> " + bbox2.getMax() + "]");
        
        // 检查是否相交
        boolean intersects = bbox1.intersects(bbox2);
        System.out.println("  AABB相交: " + intersects);
        
        // 检查扩展后的AABB
        BoundingBox expanded1 = bbox1.expand(5.0);
        boolean expandedIntersects = expanded1.intersects(bbox2);
        System.out.println("  扩展5mm后相交: " + expandedIntersects);
    }
    
    private static PlacedModel createModel(String id, float minX, float minY, float minZ,
                                           float maxX, float maxY, float maxZ) {
        float dx = Math.max(maxX - minX, 1.0f);
        float dy = Math.max(maxY - minY, 1.0f);
        float dz = Math.max(maxZ - minZ, 1.0f);
        
        maxX = minX + dx;
        maxY = minY + dy;
        maxZ = minZ + dz;
        
        BoundingBox bbox = new BoundingBox(
            new Vector3D(minX, minY, minZ),
            new Vector3D(maxX, maxY, maxZ)
        );
        
        Vector3D v1 = new Vector3D(minX, minY, minZ);
        Vector3D v2 = new Vector3D(maxX, minY, minZ);
        Vector3D v3 = new Vector3D(minX, maxY, minZ);
        Triangle triangle = new Triangle(v1, v2, v3);
        
        List<Triangle> triangles = new java.util.ArrayList<>();
        triangles.add(triangle);
        
        Mesh mesh = new Mesh(triangles, id + ".stl");
        
        ModelInfo modelInfo = new ModelInfo(
            id + ".stl",
            mesh,
            0
        );
        
        return PlacedModel.create(modelInfo, Matrix3x3.IDENTITY, bbox.getCenter());
    }
}
