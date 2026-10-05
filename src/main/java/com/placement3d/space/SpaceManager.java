package com.placement3d.space;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.PlacedModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 3D打印腔空间管理器，负责维护摆放状态和执行碰撞检测。
 *
 * <p>该类是摆放算法的核心组件，整合了空间索引（八叉树）和碰撞检测器，
 * 提供统一的接口用于：</p>
 * <ul>
 *   <li>检查模型是否可以摆放（无碰撞、无越界、满足间隙约束）</li>
 *   <li>添加/移除已摆放模型</li>
 *   <li>查询空间利用率和摆放统计</li>
 * </ul>
 *
 * <p><b>数据结构集成：</b></p>
 * <ul>
 *   <li><b>Octree：</b>加速空间查询，从O(n²)降至O(n log n)</li>
 *   <li><b>ArrayList：</b>维护摆放顺序，支持快速遍历</li>
 *   <li><b>CollisionDetector：</b>统一的碰撞检测逻辑</li>
 * </ul>
 *
 * <p><b>在三阶段算法中的使用：</b></p>
 * <ul>
 *   <li><b>阶段1（贪心初始化）：</b>反复调用canPlace和place添加模型</li>
 *   <li><b>阶段2（模拟退火）：</b>remove旧配置，place新配置</li>
 *   <li><b>阶段3（局部搜索）：</b>微调位置时remove-place-check循环</li>
 * </ul>
 *
 * <p><b>线程安全性：</b>本类不是线程安全的。如需多线程访问，外部需要同步。
 * 阶段2暂不要求线程安全，但代码结构预留了扩展空间。</p>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 * @see Octree
 * @see CollisionDetector
 * @see PlacedModel
 */
public class SpaceManager {

    /** 工作空间边界 */
    private final BoundingBox workspace;

    /** 八叉树空间索引 */
    private final Octree octree;

    /** 碰撞检测器 */
    private final CollisionDetector collisionDetector;

    /** 已摆放模型列表（维护插入顺序） */
    private final List<PlacedModel> placedModels;

    /** 最小间隙（毫米） */
    private final float minGap;

    /** 已占用体积（立方毫米） */
    private float occupiedVolume;

    /**
     * 创建空间管理器实例。
     *
     * @param workspace 工作空间边界（例如：4000×2500×1500mm）
     * @param minGap 最小间隙（毫米），默认5.0mm
     * @throws NullPointerException 如果workspace为null
     * @throws IllegalArgumentException 如果minGap为负数
     */
    public SpaceManager(BoundingBox workspace, float minGap) {
        this.workspace = Objects.requireNonNull(workspace, "工作空间不能为null");
        if (minGap < 0) {
            throw new IllegalArgumentException("最小间隙必须为非负数：" + minGap);
        }
        this.minGap = minGap;
        this.octree = new Octree(workspace);
        this.collisionDetector = new CollisionDetector();
        this.placedModels = new ArrayList<>();
        this.occupiedVolume = 0.0f;
    }

    /**
     * 创建空间管理器实例（使用默认最小间隙5.0mm）。
     *
     * @param workspace 工作空间边界
     * @throws NullPointerException 如果workspace为null
     */
    public SpaceManager(BoundingBox workspace) {
        this(workspace, CollisionDetector.DEFAULT_MIN_GAP);
    }

    /**
     * 检查模型是否可以摆放（无碰撞、无越界、满足间隙约束）。
     *
     * <p><b>检测流程：</b></p>
     * <ol>
     *   <li>检查是否越界或违反边界间隙约束</li>
     *   <li>通过八叉树查询可能碰撞的候选模型（空间剪枝）</li>
     *   <li>对每个候选模型进行精确碰撞检测和间隙检查</li>
     *   <li>全部通过返回true，否则返回false</li>
     * </ol>
     *
     * <p><b>性能优化：</b></p>
     * <ul>
     *   <li>八叉树查询：O(log n) - 只检查相邻区域</li>
     *   <li>AABB快速剔除：O(1) - 大部分情况下避免精细检测</li>
     * </ul>
     *
     * @param model 待检查的模型
     * @return true表示可以摆放，false表示存在冲突
     * @throws NullPointerException 如果model为null
     */
    public boolean canPlace(PlacedModel model) {
        Objects.requireNonNull(model, "模型不能为null");

        // 1. 检查边界约束
        if (collisionDetector.checkBoundaryViolation(model, workspace, minGap)) {
            return false;
        }

        // 2. 查询可能碰撞的候选模型
        List<PlacedModel> candidates = octree.queryCandidates(model);

        // 3. 对每个候选进行精确碰撞检测
        for (PlacedModel candidate : candidates) {
            if (collisionDetector.checkCollision(model, candidate, minGap)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 摆放模型到空间中。
     *
     * <p><b>前置条件：</b>调用者必须先通过{@link #canPlace(PlacedModel)}检查。
     * 如果未检查直接调用，会抛出异常。</p>
     *
     * <p><b>操作步骤：</b></p>
     * <ol>
     *   <li>再次检查canPlace（防御性编程）</li>
     *   <li>添加到八叉树索引</li>
     *   <li>添加到已摆放模型列表</li>
     *   <li>更新已占用体积</li>
     * </ol>
     *
     * @param model 待摆放的模型
     * @throws NullPointerException 如果model为null
     * @throws IllegalStateException 如果模型无法摆放（碰撞或越界）
     */
    public void place(PlacedModel model) {
        Objects.requireNonNull(model, "模型不能为null");

        // 防御性检查：确保模型可以摆放
        if (!canPlace(model)) {
            throw new IllegalStateException(
                "无法摆放模型：" + model.getFileName() + "，存在碰撞或越界。" +
                "位置=" + model.getPosition() + "，AABB=" + model.getTransformedAABB()
            );
        }

        // 添加到八叉树
        octree.insert(model);

        // 添加到列表
        placedModels.add(model);

        // 更新已占用体积
        occupiedVolume += model.getVolume();
    }

    /**
     * 从空间中移除模型。
     *
     * <p><b>操作步骤：</b></p>
     * <ol>
     *   <li>从八叉树中移除</li>
     *   <li>从已摆放模型列表中移除</li>
     *   <li>更新已占用体积</li>
     * </ol>
     *
     * @param model 待移除的模型
     * @return true表示移除成功，false表示模型不在空间中
     * @throws NullPointerException 如果model为null
     */
    public boolean remove(PlacedModel model) {
        Objects.requireNonNull(model, "模型不能为null");

        // 从列表中移除
        boolean removedFromList = placedModels.remove(model);

        if (removedFromList) {
            // 从八叉树中移除
            octree.remove(model);

            // 更新已占用体积
            occupiedVolume -= model.getVolume();

            return true;
        }

        return false;
    }

    /**
     * 获取已摆放模型列表（不可修改视图）。
     *
     * <p>返回的列表保持插入顺序，可用于：</p>
     * <ul>
     *   <li>遍历所有已摆放模型</li>
     *   <li>统计每种模型的数量</li>
     *   <li>生成JSON输出</li>
     * </ul>
     *
     * @return 已摆放模型的不可修改列表
     */
    public List<PlacedModel> getPlacedModels() {
        return Collections.unmodifiableList(placedModels);
    }

    /**
     * 获取空间利用率。
     *
     * <p>计算公式：利用率 = 已占用体积 / 工作空间体积</p>
     *
     * <p><b>性能目标：</b></p>
     * <ul>
     *   <li>简单模型：≥78%</li>
     *   <li>复杂模型：≥75%</li>
     * </ul>
     *
     * @return 利用率（0.0 到 1.0之间）
     */
    public float getUtilizationRate() {
        float workspaceVolume = workspace.getVolume();
        if (workspaceVolume <= 0) {
            return 0.0f;
        }
        return occupiedVolume / workspaceVolume;
    }

    /**
     * 获取已摆放模型的数量。
     *
     * @return 模型数量
     */
    public int getPlacedCount() {
        return placedModels.size();
    }

    /**
     * 获取已占用体积（立方毫米）。
     *
     * @return 已占用体积
     */
    public float getOccupiedVolume() {
        return occupiedVolume;
    }

    /**
     * 获取工作空间边界。
     *
     * @return 工作空间边界
     */
    public BoundingBox getWorkspace() {
        return workspace;
    }

    /**
     * 获取最小间隙设置。
     *
     * @return 最小间隙（毫米）
     */
    public float getMinGap() {
        return minGap;
    }

    /**
     * 获取八叉树实例（用于高级查询）。
     *
     * <p><b>注意：</b>直接操作八叉树可能破坏空间管理器的一致性。
     * 仅用于只读查询。</p>
     *
     * @return 八叉树实例
     */
    public Octree getOctree() {
        return octree;
    }

    /**
     * 获取碰撞检测器实例（用于自定义检测）。
     *
     * @return 碰撞检测器实例
     */
    public CollisionDetector getCollisionDetector() {
        return collisionDetector;
    }

    /**
     * 清空所有已摆放模型。
     *
     * <p>重置空间管理器到初始状态，可用于：</p>
     * <ul>
     *   <li>开始新的摆放尝试</li>
     *   <li>测试不同的算法配置</li>
     *   <li>回退到之前的检查点</li>
     * </ul>
     */
    public void clear() {
        placedModels.clear();
        octree.clear();
        occupiedVolume = 0.0f;
    }

    /**
     * 验证空间管理器的内部一致性（用于测试和调试）。
     *
     * <p>检查项：</p>
     * <ul>
     *   <li>placedModels列表与octree中的模型数量一致</li>
     *   <li>occupiedVolume与实际体积总和一致</li>
     *   <li>无重复模型</li>
     * </ul>
     *
     * @return true表示一致性检查通过
     */
    public boolean verifyConsistency() {
        // 检查数量一致性
        if (placedModels.size() != octree.size()) {
            return false;
        }

        // 检查体积一致性
        float actualVolume = 0.0f;
        for (PlacedModel model : placedModels) {
            actualVolume += model.getVolume();
        }

        // 使用浮点数比较容差
        float volumeDiff = Math.abs(actualVolume - occupiedVolume);
        if (volumeDiff > 1e-3f) {  // 1立方毫米容差
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format(
            "SpaceManager[workspace=%s, minGap=%.1fmm, placed=%d, utilization=%.2f%%, volume=%.2fmm³]",
            workspace,
            minGap,
            placedModels.size(),
            getUtilizationRate() * 100,
            occupiedVolume
        );
    }
}
