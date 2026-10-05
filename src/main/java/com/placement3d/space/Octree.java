package com.placement3d.space;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.PlacedModel;
import com.placement3d.model.Vector3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 八叉树空间索引，用于加速3D空间查询和碰撞检测。
 *
 * <p>八叉树是一种树形数据结构，将三维空间递归划分为8个子立方体（octants）。
 * 它显著降低了碰撞检测的时间复杂度：</p>
 * <ul>
 *   <li><b>朴素方法：</b>O(n²) - 检查所有模型对</li>
 *   <li><b>八叉树方法：</b>O(n log n) 平均情况 - 只检查相邻区域的模型</li>
 * </ul>
 *
 * <p><b>空间划分规则：</b></p>
 * <pre>
 * 每个节点将其包围盒等分为8个子立方体：
 *     前下左 (0), 前下右 (1), 前上左 (2), 前上右 (3),
 *     后下左 (4), 后下右 (5), 后上左 (6), 后上右 (7)
 * </pre>
 *
 * <p><b>性能参数：</b></p>
 * <ul>
 *   <li>最大深度：6层（避免过深导致开销增加）</li>
 *   <li>节点容量：8个模型（超出后分裂）</li>
 *   <li>叶节点可容纳多个模型（避免极端分裂）</li>
 * </ul>
 *
 * <p><b>线程安全性：</b>本类不是线程安全的。外部需要同步访问。</p>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 * @see BoundingBox
 * @see PlacedModel
 */
public class Octree {

    /** 最大树深度（根节点为0） */
    public static final int MAX_DEPTH = 6;

    /** 节点最大容量（超出后分裂） */
    public static final int MAX_CAPACITY = 8;

    /** 当前节点的空间边界 */
    private final BoundingBox boundary;

    /** 当前节点深度 */
    private final int depth;

    /** 存储在当前节点的模型（仅叶节点使用） */
    private final List<PlacedModel> models;

    /** 8个子节点（null表示未分裂） */
    private Octree[] children;

    /** 是否已分裂 */
    private boolean divided;

    /** 全局模型集合（仅根节点使用，用于准确计数和去重） */
    private final Set<PlacedModel> globalModels;

    /**
     * 创建八叉树根节点。
     *
     * @param boundary 工作空间边界
     * @throws NullPointerException 如果boundary为null
     */
    public Octree(BoundingBox boundary) {
        this(boundary, 0);
    }

    /**
     * 创建八叉树节点（内部构造函数）。
     *
     * @param boundary 节点空间边界
     * @param depth 节点深度
     * @throws NullPointerException 如果boundary为null
     * @throws IllegalArgumentException 如果depth为负数
     */
    private Octree(BoundingBox boundary, int depth) {
        this.boundary = Objects.requireNonNull(boundary, "边界不能为null");
        if (depth < 0) {
            throw new IllegalArgumentException("深度必须为非负数：" + depth);
        }
        this.depth = depth;
        this.models = new ArrayList<>(MAX_CAPACITY);
        this.children = null;
        this.divided = false;
        // 只在根节点创建全局模型集合
        this.globalModels = (depth == 0) ? new HashSet<>() : null;
    }

    /**
     * 插入模型到八叉树。
     *
     * <p><b>插入逻辑：</b></p>
     * <ol>
     *   <li>检查模型的AABB是否与当前节点边界相交</li>
     *   <li>如果不相交，忽略该模型</li>
     *   <li>如果节点未分裂且未达到容量，直接存储</li>
     *   <li>如果节点已满且未达到最大深度，分裂节点</li>
     *   <li>递归插入到相应的子节点</li>
     * </ol>
     *
     * @param model 待插入的模型
     * @return true表示插入成功，false表示模型不在此节点范围内或已存在
     * @throws NullPointerException 如果model为null
     */
    public boolean insert(PlacedModel model) {
        Objects.requireNonNull(model, "模型不能为null");

        // 根节点：检查模型是否已存在（去重）
        if (depth == 0) {
            if (!globalModels.add(model)) {
                return false; // 模型已存在
            }
        }

        // 检查模型是否在当前节点范围内
        if (!boundary.intersects(model.getTransformedAABB())) {
            // 如果是根节点且不在范围内，需要从globalModels中移除
            if (depth == 0) {
                globalModels.remove(model);
            }
            return false;
        }

        // 如果未分裂且容量未满，直接存储
        if (!divided && models.size() < MAX_CAPACITY) {
            models.add(model);
            return true;
        }

        // 如果达到最大深度，强制存储（避免无限分裂）
        if (depth >= MAX_DEPTH) {
            models.add(model);
            return true;
        }

        // 需要分裂
        if (!divided) {
            subdivide();
        }

        // 插入到所有相交的子节点
        boolean inserted = false;
        for (Octree child : children) {
            if (child.insert(model)) {
                inserted = true;
            }
        }

        return inserted;
    }

    /**
     * 移除模型从八叉树。
     *
     * <p><b>注意：</b>移除操作不会合并空的子节点。如果需要频繁插入/删除，
     * 考虑定期重建八叉树以保持性能。</p>
     *
     * @param model 待移除的模型
     * @return true表示移除成功，false表示模型不在树中
     * @throws NullPointerException 如果model为null
     */
    public boolean remove(PlacedModel model) {
        Objects.requireNonNull(model, "模型不能为null");

        // 根节点：从全局集合中移除
        if (depth == 0) {
            if (!globalModels.remove(model)) {
                return false; // 模型不存在
            }
        }

        // 检查模型是否可能在当前节点
        if (!boundary.intersects(model.getTransformedAABB())) {
            return false;
        }

        // 尝试从当前节点移除
        boolean removed = models.remove(model);

        // 如果已分裂，递归移除子节点
        if (divided) {
            for (Octree child : children) {
                if (child.remove(model)) {
                    removed = true;
                }
            }
        }

        return removed;
    }

    /**
     * 查询与指定AABB相交的所有模型。
     *
     * <p>这是碰撞检测的核心方法。通过空间索引，只返回可能碰撞的候选模型，
     * 大幅减少需要精确检测的模型对数量。</p>
     *
     * @param range 查询范围（AABB）
     * @return 与range相交的所有模型列表
     * @throws NullPointerException 如果range为null
     */
    public List<PlacedModel> query(BoundingBox range) {
        Objects.requireNonNull(range, "查询范围不能为null");

        List<PlacedModel> found = new ArrayList<>();
        queryRecursive(range, found);
        return found;
    }

    /**
     * 递归查询辅助方法。
     *
     * @param range 查询范围
     * @param found 累积结果列表
     */
    private void queryRecursive(BoundingBox range, List<PlacedModel> found) {
        // 如果查询范围不与当前节点相交，剪枝
        if (!boundary.intersects(range)) {
            return;
        }

        // 检查当前节点存储的模型
        for (PlacedModel model : models) {
            if (range.intersects(model.getTransformedAABB())) {
                found.add(model);
            }
        }

        // 递归查询子节点
        if (divided) {
            for (Octree child : children) {
                child.queryRecursive(range, found);
            }
        }
    }

    /**
     * 查询与指定模型可能碰撞的候选模型。
     *
     * <p>便捷方法，使用模型的变换后AABB作为查询范围。</p>
     *
     * @param model 查询模型
     * @return 可能与model碰撞的候选模型列表
     * @throws NullPointerException 如果model为null
     */
    public List<PlacedModel> queryCandidates(PlacedModel model) {
        Objects.requireNonNull(model, "模型不能为null");
        return query(model.getTransformedAABB());
    }

    /**
     * 清空八叉树所有内容。
     */
    public void clear() {
        models.clear();
        children = null;
        divided = false;
        // 根节点：清空全局集合
        if (depth == 0 && globalModels != null) {
            globalModels.clear();
        }
    }

    /**
     * 获取八叉树中的总模型数量。
     *
     * <p><b>注意：</b>由于模型可能跨越多个子节点存储，
     * 根节点使用全局集合去重计数，子节点不应直接调用此方法。</p>
     *
     * @return 模型总数（根节点返回唯一模型数，子节点返回所有存储的模型数）
     */
    public int size() {
        // 根节点：返回全局集合大小（去重后的准确数量）
        if (depth == 0 && globalModels != null) {
            return globalModels.size();
        }

        // 子节点：递归计数（保留原有逻辑用于内部调试）
        int count = models.size();
        if (divided) {
            for (Octree child : children) {
                count += child.size();
            }
        }
        return count;
    }

    /**
     * 获取八叉树的实际深度。
     *
     * @return 最大深度
     */
    public int getActualDepth() {
        if (!divided) {
            return depth;
        }
        int maxChildDepth = depth;
        for (Octree child : children) {
            maxChildDepth = Math.max(maxChildDepth, child.getActualDepth());
        }
        return maxChildDepth;
    }

    /**
     * 获取所有存储的模型（不可修改视图）。
     *
     * @return 所有模型的列表
     */
    public List<PlacedModel> getAllModels() {
        List<PlacedModel> allModels = new ArrayList<>();
        collectAllModels(allModels);
        return Collections.unmodifiableList(allModels);
    }

    /**
     * 递归收集所有模型。
     *
     * @param accumulator 累积列表
     */
    private void collectAllModels(List<PlacedModel> accumulator) {
        accumulator.addAll(models);
        if (divided) {
            for (Octree child : children) {
                child.collectAllModels(accumulator);
            }
        }
    }

    /**
     * 将当前节点分裂为8个子节点。
     */
    private void subdivide() {
        if (divided) {
            return;
        }

        Vector3D min = boundary.getMin();
        Vector3D max = boundary.getMax();
        Vector3D center = boundary.getCenter();

        children = new Octree[8];

        // 前下左 (0)
        children[0] = new Octree(
            new BoundingBox(
                new Vector3D(min.getX(), min.getY(), min.getZ()),
                new Vector3D(center.getX(), center.getY(), center.getZ())
            ),
            depth + 1
        );

        // 前下右 (1)
        children[1] = new Octree(
            new BoundingBox(
                new Vector3D(center.getX(), min.getY(), min.getZ()),
                new Vector3D(max.getX(), center.getY(), center.getZ())
            ),
            depth + 1
        );

        // 前上左 (2)
        children[2] = new Octree(
            new BoundingBox(
                new Vector3D(min.getX(), center.getY(), min.getZ()),
                new Vector3D(center.getX(), max.getY(), center.getZ())
            ),
            depth + 1
        );

        // 前上右 (3)
        children[3] = new Octree(
            new BoundingBox(
                new Vector3D(center.getX(), center.getY(), min.getZ()),
                new Vector3D(max.getX(), max.getY(), center.getZ())
            ),
            depth + 1
        );

        // 后下左 (4)
        children[4] = new Octree(
            new BoundingBox(
                new Vector3D(min.getX(), min.getY(), center.getZ()),
                new Vector3D(center.getX(), center.getY(), max.getZ())
            ),
            depth + 1
        );

        // 后下右 (5)
        children[5] = new Octree(
            new BoundingBox(
                new Vector3D(center.getX(), min.getY(), center.getZ()),
                new Vector3D(max.getX(), center.getY(), max.getZ())
            ),
            depth + 1
        );

        // 后上左 (6)
        children[6] = new Octree(
            new BoundingBox(
                new Vector3D(min.getX(), center.getY(), center.getZ()),
                new Vector3D(center.getX(), max.getY(), max.getZ())
            ),
            depth + 1
        );

        // 后上右 (7)
        children[7] = new Octree(
            new BoundingBox(
                new Vector3D(center.getX(), center.getY(), center.getZ()),
                new Vector3D(max.getX(), max.getY(), max.getZ())
            ),
            depth + 1
        );

        divided = true;

        // 将现有模型重新分配到子节点
        List<PlacedModel> oldModels = new ArrayList<>(models);
        models.clear();

        for (PlacedModel model : oldModels) {
            boolean inserted = false;
            for (Octree child : children) {
                if (child.insert(model)) {
                    inserted = true;
                }
            }
            // 如果模型无法插入任何子节点（理论上不应发生），保留在当前节点
            if (!inserted) {
                models.add(model);
            }
        }
    }

    @Override
    public String toString() {
        return String.format(
            "Octree[depth=%d, models=%d, divided=%s, boundary=%s]",
            depth, models.size(), divided, boundary
        );
    }
}
