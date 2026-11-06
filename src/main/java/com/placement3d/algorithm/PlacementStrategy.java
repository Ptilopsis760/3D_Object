package com.placement3d.algorithm;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.ModelInfo;
import com.placement3d.model.PlacedModel;
import java.util.List;

/**
 * 摆放策略接口，定义3D模型摆放算法的统一契约。
 *
 * <p>该接口抽象了不同摆放算法的实现细节，使得算法可以灵活切换和组合。</p>
 *
 * <p><b>已实现的策略：</b></p>
 * <ul>
 *   <li>{@link GreedyPlacementStrategy} - 贪心初始化策略（阶段1）</li>
 * </ul>
 *
 * <p><b>未来可扩展的策略：</b></p>
 * <ul>
 *   <li>SimulatedAnnealingStrategy - 模拟退火优化（阶段2）</li>
 *   <li>LocalSearchStrategy - 局部搜索精修（阶段3）</li>
 *   <li>GeneticAlgorithmStrategy - 遗传算法</li>
 *   <li>ReinforcementLearningStrategy - 强化学习</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>
 * // 创建策略实例
 * PlacementStrategy strategy = new GreedyPlacementStrategy();
 *
 * // 准备输入数据
 * List&lt;ModelInfo&gt; models = loadModelsFromSTL();
 * BoundingBox workspace = new BoundingBox(
 *     new Vector3D(0, 0, 0),
 *     new Vector3D(4000, 2500, 1500)
 * );
 *
 * // 执行摆放算法
 * List&lt;PlacedModel&gt; result = strategy.place(models, workspace, 5.0f);
 *
 * // 评估结果
 * float utilization = calculateUtilization(result, workspace);
 * System.out.println("空间利用率: " + utilization + "%");
 * </pre>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 * @see PlacedModel
 * @see ModelInfo
 * @see BoundingBox
 */
public interface PlacementStrategy {

    /**
     * 执行摆放算法，将一组3D模型放置到指定工作空间内。
     *
     * <p><b>输入约束：</b></p>
     * <ul>
     *   <li>models列表不能为null或为空</li>
     *   <li>workspace必须是有效的长方体空间</li>
     *   <li>minGap必须为非负数（通常为5.0mm）</li>
     * </ul>
     *
     * <p><b>输出保证：</b></p>
     * <ul>
     *   <li>所有摆放的模型完全在workspace内</li>
     *   <li>任意两个模型之间的间隙 ≥ minGap</li>
     *   <li>模型与workspace边界的间隙 ≥ minGap</li>
     *   <li>无碰撞（模型之间无重叠）</li>
     * </ul>
     *
     * <p><b>性能目标：</b></p>
     * <ul>
     *   <li>阶段1（贪心）：1-2秒，利用率65-70%</li>
     *   <li>阶段2（退火）：8-12秒，利用率提升5-10%</li>
     *   <li>阶段3（局部搜索）：2-3秒，利用率提升2-3%</li>
     *   <li>总计：12-17秒，利用率78-85%</li>
     * </ul>
     *
     * <p><b>失败处理：</b></p>
     * <ul>
     *   <li>如果无法摆放所有模型，返回部分结果（尽可能多的模型）</li>
     *   <li>实现类应记录无法摆放的模型（日志或统计信息）</li>
     *   <li>不抛出异常（除非输入参数无效）</li>
     * </ul>
     *
     * @param models 待摆放的模型列表（不会被修改）
     * @param workspace 工作空间边界（例如：4000×2500×1500mm）
     * @param minGap 最小间隙（mm），模型之间及模型与边界之间必须保持的最小距离
     * @return 摆放结果列表，每个PlacedModel包含位置、旋转和变换后的AABB
     * @throws NullPointerException 如果models或workspace为null
     * @throws IllegalArgumentException 如果models为空或minGap为负数
     */
    List<PlacedModel> place(List<ModelInfo> models, BoundingBox workspace, float minGap);

    /**
     * 多批次复制摆放：先保证每类至少 1 个（覆盖约束），再循环追加副本直至
     * 达到目标利用率、达到每类副本上限、或空间饱和（一轮完整循环无法放入任何副本）。
     *
     * <p><b>覆盖约束</b>：第一批次对每个 ModelInfo 摆放一次，确保
     * "每批次中每种模型至少摆放1个"。后续批次按体积从大到小循环追加副本。</p>
     *
     * <p><b>停止条件</b>（满足任一即停）：</p>
     * <ul>
     *   <li>当前利用率 ≥ targetUtilization</li>
     *   <li>每类副本数均达 maxCopiesPerType</li>
     *   <li>一轮完整循环未放入任何模型（空间饱和）</li>
     * </ul>
     *
     * @param models 模型类型列表（每类 1 个 ModelInfo，不会被修改）
     * @param workspace 工作空间边界
     * @param minGap 最小间隙（mm）
     * @param targetUtilization 目标利用率（0.0-1.0），达到即停；传 1.0 表示尽量填满
     * @param maxCopiesPerType 每类最多摆放副本数（含覆盖的 1 个），必须 ≥ 1
     * @return 所有已摆放模型列表
     * @throws NullPointerException 如果 models 或 workspace 为 null
     * @throws IllegalArgumentException 如果 models 为空、minGap 为负、
     *         targetUtilization 不在 [0,1]、或 maxCopiesPerType < 1
     */
    List<PlacedModel> placeMultiBatch(List<ModelInfo> models, BoundingBox workspace,
                                      float minGap, float targetUtilization, int maxCopiesPerType);
}
