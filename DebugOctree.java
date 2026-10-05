// 快速验证Octree的insert逻辑
public class DebugOctree {
    public static void main(String[] args) {
        // 模拟testInsertManyModels的坐标计算
        System.out.println("=== 分析100个模型的坐标 ===");
        int outOfBounds = 0;
        for (int i = 0; i < 100; i++) {
            float x = (i % 10) * 400;
            float y = ((i / 10) % 10) * 250;
            float z = (i / 100) * 150;
            
            // AABB: [x, y, z] -> [x+100, y+100, z+100]
            float maxX = x + 100;
            float maxY = y + 100;
            float maxZ = z + 100;
            
            // 检查是否超出工作空间 [0,0,0] -> [4000, 2500, 1500]
            if (maxX > 4000 || maxY > 2500 || maxZ > 1500) {
                System.out.println("模型 " + i + " 超出边界: (" + x + "," + y + "," + z + ") -> (" + maxX + "," + maxY + "," + maxZ + ")");
                outOfBounds++;
            }
        }
        System.out.println("\n总计: 100个模型, 超出边界: " + outOfBounds + "个");
        System.out.println("预期可插入: " + (100 - outOfBounds) + "个");
    }
}
