package com.placement3d.space;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.PlacedModel;
import com.placement3d.model.Vector3D;
import java.util.Objects;

/**
 * 碰撞检测器，负责检测3D模型之间的碰撞和间隙约束。
 *
 * <p>采用两阶段检测策略以优化性能：</p>
 * <ol>
 *   <li><b>阶段1：AABB快速剔除</b> - 使用轴对齐包围盒进行粗检测（O(1)复杂度）</li>
 *   <li><b>阶段2：精细检测（预留）</b> - 对AABB相交的候选对进行精确几何检测</li>
 * </ol>
 *
 * <p><b>间隙约束执行：</b></p>
 * <p>所有模型之间必须保持至少5mm的最小间隙。这通过将每个模型的AABB外扩5mm
 * 形成"禁入区"来实现。任何新模型不得与现有模型的禁入区相交。</p>
 *
 * <p><b>性能特征：</b></p>
 * <ul>
 *   <li>AABB检测：O(1) 时间复杂度</li>
 *   <li>当前实现仅使用AABB，适用于Demo项目</li>
 *   <li>未来可扩展：添加SAT/GJK算法用于精确检测</li>
 * </ul>
 *
 * <p><b>线程安全性：</b>本类无状态，所有方法都是线程安全的。</p>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 * @see BoundingBox
 * @see PlacedModel
 */
public final class CollisionDetector {

    /** 默认最小间隙（毫米） */
    public static final float DEFAULT_MIN_GAP = 5.0f;

    /** 浮点数比较的容差（毫米） */
    private static final float EPSILON = 1e-4f;

    /**
     * 创建碰撞检测器实例。
     *
     * <p>此类无状态，可复用同一实例。</p>
     */
    public CollisionDetector() {
        // 无状态类，无需初始化
    }

    /**
     * 检测两个已摆放模型是否发生碰撞或违反间隙约束。
     *
     * <p><b>检测逻辑：</b></p>
     * <ol>
     *   <li>将model1的AABB外扩minGap毫米</li>
     *   <li>检查扩展后的AABB是否与model2的AABB相交</li>
     *   <li>相交则返回true（碰撞或间隙不足），否则返回false</li>
     * </ol>
     *
     * @param model1 第一个模型
     * @param model2 第二个模型
     * @param minGap 最小间隙（毫米）
     * @return true表示碰撞或间隙不足，false表示无碰撞且间隙充足
     * @throws NullPointerException 如果任何模型为null
     * @throws IllegalArgumentException 如果minGap为负数
     */
    public boolean checkCollision(PlacedModel model1, PlacedModel model2, float minGap) {
        Objects.requireNonNull(model1, "model1不能为null");
        Objects.requireNonNull(model2, "model2不能为null");

        if (minGap < 0) {
            throw new IllegalArgumentException("最小间隙必须为非负数：" + minGap);
        }

        // 获取两个模型的变换后AABB
        BoundingBox bbox1 = model1.getTransformedAABB();
        BoundingBox bbox2 = model2.getTransformedAABB();

        // 将bbox1外扩minGap，形成禁入区
        BoundingBox expandedBbox1 = bbox1.expand(minGap);

        // 检查禁入区是否与bbox2相交
        return expandedBbox1.intersects(bbox2);
    }

    /**
     * 检测两个已摆放模型是否发生碰撞或违反默认间隙约束（5mm）。
     *
     * @param model1 第一个模型
     * @param model2 第二个模型
     * @return true表示碰撞或间隙不足，false表示无碰撞且间隙充足
     * @throws NullPointerException 如果任何模型为null
     */
    public boolean checkCollision(PlacedModel model1, PlacedModel model2) {
        return checkCollision(model1, model2, DEFAULT_MIN_GAP);
    }

    /**
     * 检测模型的AABB是否完全在工作空间内（考虑边界间隙）。
     *
     * <p><b>边界约束：</b></p>
     * <ul>
     *   <li>模型的AABB必须完全位于工作空间内</li>
     *   <li>模型与工作空间边界之间必须保持minGap的间隙</li>
     * </ul>
     *
     * @param model 待检测的模型
     * @param workspace 工作空间边界
     * @param minGap 边界最小间隙（毫米）
     * @return true表示越界或违反边界间隙，false表示在边界内且间隙充足
     * @throws NullPointerException 如果任何参数为null
     * @throws IllegalArgumentException 如果minGap为负数
     */
    public boolean checkBoundaryViolation(PlacedModel model, BoundingBox workspace, float minGap) {
        Objects.requireNonNull(model, "模型不能为null");
        Objects.requireNonNull(workspace, "工作空间不能为null");

        if (minGap < 0) {
            throw new IllegalArgumentException("最小间隙必须为非负数：" + minGap);
        }

        BoundingBox modelBbox = model.getTransformedAABB();

        // 将工作空间内缩minGap，形成有效摆放区域
        BoundingBox validRegion = new BoundingBox(
            workspace.getMin().add(new Vector3D(minGap, minGap, minGap)),
            workspace.getMax().subtract(new Vector3D(minGap, minGap, minGap))
        );

        // 检查模型的AABB是否完全在有效区域内
        Vector3D modelMin = modelBbox.getMin();
        Vector3D modelMax = modelBbox.getMax();
        Vector3D regionMin = validRegion.getMin();
        Vector3D regionMax = validRegion.getMax();

        // 任一维度超出边界即为越界
        return modelMin.getX() < regionMin.getX() - EPSILON ||
               modelMin.getY() < regionMin.getY() - EPSILON ||
               modelMin.getZ() < regionMin.getZ() - EPSILON ||
               modelMax.getX() > regionMax.getX() + EPSILON ||
               modelMax.getY() > regionMax.getY() + EPSILON ||
               modelMax.getZ() > regionMax.getZ() + EPSILON;
    }

    /**
     * 检测模型的AABB是否完全在工作空间内（使用默认边界间隙5mm）。
     *
     * @param model 待检测的模型
     * @param workspace 工作空间边界
     * @return true表示越界或违反边界间隙，false表示在边界内且间隙充足
     * @throws NullPointerException 如果任何参数为null
     */
    public boolean checkBoundaryViolation(PlacedModel model, BoundingBox workspace) {
        return checkBoundaryViolation(model, workspace, DEFAULT_MIN_GAP);
    }

    /**
     * 计算两个AABB之间的最小距离。
     *
     * <p>用于评分函数和间隙分析。</p>
     *
     * <p><b>距离定义：</b></p>
     * <ul>
     *   <li>如果AABB相交：距离为0</li>
     *   <li>如果AABB分离：返回最近点之间的欧几里得距离</li>
     * </ul>
     *
     * @param bbox1 第一个AABB
     * @param bbox2 第二个AABB
     * @return 最小距离（毫米），相交时返回0
     * @throws NullPointerException 如果任何AABB为null
     */
    public float computeDistance(BoundingBox bbox1, BoundingBox bbox2) {
        Objects.requireNonNull(bbox1, "bbox1不能为null");
        Objects.requireNonNull(bbox2, "bbox2不能为null");

        // 如果相交，距离为0
        if (bbox1.intersects(bbox2)) {
            return 0.0f;
        }

        // 计算各轴上的距离
        float dx = computeAxisDistance(
            bbox1.getMin().getX(), bbox1.getMax().getX(),
            bbox2.getMin().getX(), bbox2.getMax().getX()
        );

        float dy = computeAxisDistance(
            bbox1.getMin().getY(), bbox1.getMax().getY(),
            bbox2.getMin().getY(), bbox2.getMax().getY()
        );

        float dz = computeAxisDistance(
            bbox1.getMin().getZ(), bbox1.getMax().getZ(),
            bbox2.getMin().getZ(), bbox2.getMax().getZ()
        );

        // 欧几里得距离
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 计算两个一维区间在单轴上的距离。
     *
     * @param min1 区间1的最小值
     * @param max1 区间1的最大值
     * @param min2 区间2的最小值
     * @param max2 区间2的最大值
     * @return 单轴距离，重叠时返回0
     */
    private float computeAxisDistance(float min1, float max1, float min2, float max2) {
        // 如果区间重叠，距离为0
        if (max1 >= min2 && max2 >= min1) {
            return 0.0f;
        }

        // 区间分离，返回间隙
        if (max1 < min2) {
            return min2 - max1;
        } else {
            return min1 - max2;
        }
    }

    /**
     * 检查AABB是否与工作空间的某一边界相邻（距离 <= threshold）。
     *
     * <p>用于评分函数中的"贴墙"奖励。</p>
     *
     * @param bbox 待检测的AABB
     * @param workspace 工作空间边界
     * @param threshold 相邻阈值（毫米）
     * @return true表示与至少一个边界相邻
     * @throws NullPointerException 如果任何参数为null
     * @throws IllegalArgumentException 如果threshold为负数
     */
    public boolean isAdjacentToBoundary(BoundingBox bbox, BoundingBox workspace, float threshold) {
        Objects.requireNonNull(bbox, "AABB不能为null");
        Objects.requireNonNull(workspace, "工作空间不能为null");

        if (threshold < 0) {
            throw new IllegalArgumentException("阈值必须为非负数：" + threshold);
        }

        Vector3D bboxMin = bbox.getMin();
        Vector3D bboxMax = bbox.getMax();
        Vector3D wsMin = workspace.getMin();
        Vector3D wsMax = workspace.getMax();

        // 检查是否接近任一边界
        return Math.abs(bboxMin.getX() - wsMin.getX()) <= threshold ||
               Math.abs(bboxMin.getY() - wsMin.getY()) <= threshold ||
               Math.abs(bboxMin.getZ() - wsMin.getZ()) <= threshold ||
               Math.abs(bboxMax.getX() - wsMax.getX()) <= threshold ||
               Math.abs(bboxMax.getY() - wsMax.getY()) <= threshold ||
               Math.abs(bboxMax.getZ() - wsMax.getZ()) <= threshold;
    }

    @Override
    public String toString() {
        return "CollisionDetector[minGap=" + DEFAULT_MIN_GAP + "mm, mode=AABB]";
    }
}
