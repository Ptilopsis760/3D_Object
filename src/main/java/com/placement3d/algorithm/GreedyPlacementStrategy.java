package com.placement3d.algorithm;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.Matrix3x3;
import com.placement3d.model.ModelInfo;
import com.placement3d.model.PlacedModel;
import com.placement3d.model.Vector3D;
import com.placement3d.space.SpaceManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 贪心摆放策略 - 阶段1：初始化摆放算法。
 *
 * <p>采用DBLF（Deepest Bottom Left Fill，最深底部左侧填充）解码器，
 * 实现"大件优先"启发式策略，在1-2秒内达到65-70%的初始空间利用率。</p>
 *
 * <p><b>算法流程：</b></p>
 * <pre>
 * 1. 按体积从大到小排序模型（大件优先）
 * 2. 创建SpaceManager管理摆放状态
 * 3. 对每个模型：
 *    a. 生成候选位置（DBLF策略）
 *    b. 对每个候选位置尝试4个旋转角度（0°, 90°, 180°, 270°）
 *    c. 检查可行性（SpaceManager.canPlace）
 *    d. 计算评分
 *    e. 选择最高分的候选
 *    f. 摆放（SpaceManager.place）
 * 4. 返回所有已摆放模型
 * </pre>
 *
 * <p><b>DBLF位置生成策略：</b></p>
 * <ul>
 *   <li><b>初始候选：</b>工作空间的左下后角 (minX+gap, minY+gap, minZ)</li>
 *   <li><b>从已摆放模型生成：</b>
 *     <ul>
 *       <li>右侧：(maxX + gap, y, z)</li>
 *       <li>前侧：(x, maxY + gap, z)</li>
 *       <li>上方：(x, y, maxZ + gap) - 仅用于多层摆放</li>
 *     </ul>
 *   </li>
 *   <li><b>排序优先级：</b>Z坐标 < Y坐标 < X坐标（下层优先、后侧优先、左侧优先）</li>
 * </ul>
 *
 * <p><b>评分函数：</b></p>
 * <pre>
 * score = 0.5 × 利用率增量
 *       + 0.3 × 紧凑度（1 / (1 + 与最近模型的距离)）
 *       + 0.2 × 稳定性（1 / (1 + Z坐标)）
 *       - 0.1 × 边界惩罚（距离边界过近时）
 * </pre>
 *
 * <p><b>性能目标：</b></p>
 * <ul>
 *   <li>计算时间：1-2秒</li>
 *   <li>空间利用率：65-70%</li>
 *   <li>为后续优化阶段提供良好起点</li>
 * </ul>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 * @see PlacementStrategy
 * @see PlacementCandidate
 */
public class GreedyPlacementStrategy implements PlacementStrategy {

    /** 评分权重：利用率增量 */
    private static final double WEIGHT_UTILIZATION = 0.5;

    /** 评分权重：紧凑度 */
    private static final double WEIGHT_COMPACTNESS = 0.3;

    /** 评分权重：稳定性（低重心） */
    private static final double WEIGHT_STABILITY = 0.2;

    /** 评分惩罚：边界过近 */
    private static final double PENALTY_BOUNDARY = 0.1;

    /** 边界邻近阈值（毫米） */
    private static final float BOUNDARY_THRESHOLD = 10.0f;

    /**
     * 位置生成时的间隙epsilon（微小偏移）。
     * 用于避免浮点数精度导致的边界接触判定为碰撞。
     */
    private static final float POSITION_EPSILON = 0.01f;

    /**
     * 24 种轴对齐旋转姿态（旋转群 O）。
     *
     * <p><b>设计说明：</b></p>
     * <ul>
     *   <li>对应正方体 24 种不重复朝向：6 个面朝上 × 4 种绕法线旋转</li>
     *   <li>相比原来只有绕 Z 轴的 4 种姿态，候选空间扩大 6 倍</li>
     *   <li>零件可悬浮摆放，三轴自由旋转均合法（已与老师确认，C1/C6）</li>
     *   <li>由 {@link Matrix3x3#allAxisAlignedRotations()} 集中生成，退火阶段可直接复用</li>
     * </ul>
     */
    private static final Matrix3x3[] ROTATIONS = Matrix3x3.allAxisAlignedRotations();

    /**
     * 执行贪心摆放算法。
     *
     * @param models 待摆放的模型列表
     * @param workspace 工作空间边界
     * @param minGap 最小间隙（mm）
     * @return 摆放结果列表
     * @throws NullPointerException 如果models或workspace为null
     * @throws IllegalArgumentException 如果models为空或minGap为负数
     */
    @Override
    public List<PlacedModel> place(List<ModelInfo> models, BoundingBox workspace, float minGap) {
        // 参数验证
        Objects.requireNonNull(models, "模型列表不能为null");
        Objects.requireNonNull(workspace, "工作空间不能为null");
        if (models.isEmpty()) {
            throw new IllegalArgumentException("模型列表不能为空");
        }
        if (minGap < 0) {
            throw new IllegalArgumentException("最小间隙必须为非负数：" + minGap);
        }

        // 1. 按体积从大到小排序（大件优先）
        List<ModelInfo> sortedModels = new ArrayList<>(models);
        sortedModels.sort(Comparator.comparing(ModelInfo::getVolume).reversed());

        // 2. 创建空间管理器
        SpaceManager spaceManager = new SpaceManager(workspace, minGap);

        // 3. 逐个摆放模型
        for (ModelInfo model : sortedModels) {
            PlacementCandidate bestCandidate = findBestCandidate(model, spaceManager, workspace, minGap);

            if (bestCandidate != null) {
                // 创建PlacedModel并摆放
                PlacedModel placedModel = PlacedModel.create(
                    model,
                    bestCandidate.getRotation(),
                    bestCandidate.getPosition()
                );

                spaceManager.place(placedModel);
            } else {
                // 无法摆放此模型，记录日志（实际项目中使用logger）
                System.err.println("警告：无法摆放模型 " + model.getFileName() +
                                 "（体积=" + model.getVolume() + "mm³）");
            }
        }

        // 4. 返回所有已摆放模型
        return new ArrayList<>(spaceManager.getPlacedModels());
    }

    /**
     * 为给定模型找到最佳摆放候选。
     *
     * @param model 待摆放的模型
     * @param spaceManager 空间管理器
     * @param workspace 工作空间
     * @param minGap 最小间隙
     * @return 最佳候选，如果无可行候选则返回null
     */
    private PlacementCandidate findBestCandidate(
            ModelInfo model,
            SpaceManager spaceManager,
            BoundingBox workspace,
            float minGap) {

        // 生成候选位置
        List<Vector3D> candidatePositions = generateCandidatePositions(
            spaceManager, workspace, minGap, model
        );

        PlacementCandidate bestCandidate = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        // 对每个候选位置尝试所有 24 种轴对齐旋转姿态
        for (Vector3D position : candidatePositions) {
            for (Matrix3x3 rotation : ROTATIONS) {

                // 创建临时PlacedModel用于可行性检查
                PlacedModel testPlacement = PlacedModel.create(model, rotation, position);

                // 检查可行性
                if (spaceManager.canPlace(testPlacement)) {
                    // 计算评分
                    double score = calculateScore(
                        testPlacement, spaceManager, workspace, minGap
                    );

                    // 更新最佳候选
                    if (score > bestScore) {
                        bestScore = score;
                        bestCandidate = new PlacementCandidate(model, position, rotation, score);
                    }
                }
            }
        }

        return bestCandidate;
    }

    /**
     * 生成候选摆放位置（DBLF策略）。
     *
     * <p><b>位置生成规则：</b></p>
     * <ul>
     *   <li>初始位置：工作空间左下后角</li>
     *   <li>从已摆放模型生成：右侧、前侧、上方</li>
     *   <li>排序：Z < Y < X（下层优先、后侧优先、左侧优先）</li>
     * </ul>
     *
     * @param spaceManager 空间管理器
     * @param workspace 工作空间
     * @param minGap 最小间隙
     * @param model 待摆放的模型（用于计算质心偏移）
     * @return 候选位置列表
     */
    private List<Vector3D> generateCandidatePositions(
            SpaceManager spaceManager,
            BoundingBox workspace,
            float minGap,
            ModelInfo model) {

        List<Vector3D> candidates = new ArrayList<>();

        // 1. 添加初始候选位置（工作空间左下后角）
        Vector3D workspaceMin = workspace.getMin();
        BoundingBox modelAABB = model.getOriginalAABB();

        // 计算质心位置，使得模型的AABB最小角在(minGap, minGap, minGap)处
        // AABB最小角 = 质心 - (质心 - AABB.min)
        // 要让 AABB最小角 = workspaceMin + (minGap, minGap, minGap)
        // 则 质心 = workspaceMin + (minGap, minGap, minGap) + (质心 - AABB.min)
        Vector3D centerOffset = model.getCenterOfMass().subtract(modelAABB.getMin());
        Vector3D initialPosition = new Vector3D(
            workspaceMin.getX() + minGap + centerOffset.getX(),
            workspaceMin.getY() + minGap + centerOffset.getY(),
            workspaceMin.getZ() + minGap + centerOffset.getZ()  // Z也需要保持minGap
        );
        candidates.add(initialPosition);

        // 2. 从已摆放模型生成候选位置
        for (PlacedModel placed : spaceManager.getPlacedModels()) {
            BoundingBox placedAABB = placed.getTransformedAABB();
            Vector3D placedMin = placedAABB.getMin();
            Vector3D placedMax = placedAABB.getMax();

            // 右侧候选（X方向）
            // 新模型的左边界对齐到已摆放模型的右边界 + minGap + epsilon
            // epsilon避免浮点数精度导致边界接触判定为碰撞
            Vector3D rightCandidate = new Vector3D(
                placedMax.getX() + minGap + POSITION_EPSILON + centerOffset.getX(),
                placedMin.getY() + centerOffset.getY(),
                placedMin.getZ() + centerOffset.getZ()
            );
            candidates.add(rightCandidate);

            // 前侧候选（Y方向）
            // 新模型的后边界对齐到已摆放模型的前边界 + minGap + epsilon
            Vector3D frontCandidate = new Vector3D(
                placedMin.getX() + centerOffset.getX(),
                placedMax.getY() + minGap + POSITION_EPSILON + centerOffset.getY(),
                placedMin.getZ() + centerOffset.getZ()
            );
            candidates.add(frontCandidate);

            // 上方候选（Z方向）- 多层摆放
            // 新模型的底边界对齐到已摆放模型的顶边界 + minGap + epsilon
            Vector3D topCandidate = new Vector3D(
                placedMin.getX() + centerOffset.getX(),
                placedMin.getY() + centerOffset.getY(),
                placedMax.getZ() + minGap + POSITION_EPSILON + centerOffset.getZ()
            );
            candidates.add(topCandidate);
        }

        // 3. 排序：Z < Y < X（下层优先、后侧优先、左侧优先）
        candidates.sort(Comparator
            .comparing(Vector3D::getZ)
            .thenComparing(Vector3D::getY)
            .thenComparing(Vector3D::getX)
        );

        return candidates;
    }

    /**
     * 计算摆放候选的评分。
     *
     * <p><b>评分公式：</b></p>
     * <pre>
     * score = 0.5 × 利用率增量
     *       + 0.3 × 紧凑度
     *       + 0.2 × 稳定性
     *       - 0.1 × 边界惩罚
     * </pre>
     *
     * @param placement 待评估的摆放
     * @param spaceManager 空间管理器
     * @param workspace 工作空间
     * @param minGap 最小间隙
     * @return 评分（越高越好）
     */
    private double calculateScore(
            PlacedModel placement,
            SpaceManager spaceManager,
            BoundingBox workspace,
            float minGap) {

        double score = 0.0;

        // 1. 利用率增量（用当前模型的 AABB 体积估算对总利用率的贡献）
        float volumeIncrement = placement.getTransformedAABB().getVolume();
        float workspaceVolume = workspace.getVolume();
        double utilizationIncrement = volumeIncrement / workspaceVolume;
        score += WEIGHT_UTILIZATION * utilizationIncrement;

        // 2. 紧凑度（与最近已摆放模型的距离）
        double compactness = calculateCompactness(placement, spaceManager);
        score += WEIGHT_COMPACTNESS * compactness;

        // 3. 稳定性（质心高度越低越稳定）
        double stability = calculateStability(placement, workspace);
        score += WEIGHT_STABILITY * stability;

        // 4. 边界惩罚（太接近边界会降低分数）
        double boundaryPenalty = calculateBoundaryPenalty(placement, workspace, minGap);
        score -= PENALTY_BOUNDARY * boundaryPenalty;

        return score;
    }

    /**
     * 计算紧凑度得分（与最近模型的距离）。
     *
     * @param placement 待评估的摆放
     * @param spaceManager 空间管理器
     * @return 紧凑度得分（0-100）
     */
    private double calculateCompactness(PlacedModel placement, SpaceManager spaceManager) {
        List<PlacedModel> placedModels = spaceManager.getPlacedModels();

        if (placedModels.isEmpty()) {
            return 100.0; // 第一个模型，紧凑度最高
        }

        // 找到最近的已摆放模型
        float minDistance = Float.MAX_VALUE;
        BoundingBox placementAABB = placement.getTransformedAABB();

        for (PlacedModel placed : placedModels) {
            float distance = spaceManager.getCollisionDetector()
                .computeDistance(placementAABB, placed.getTransformedAABB());
            minDistance = Math.min(minDistance, distance);
        }

        // 将距离转换为紧凑度得分（距离越小，紧凑度越高）
        // 使用倒数函数：compactness = 100 / (1 + distance/100)
        return 100.0 / (1.0 + minDistance / 100.0);
    }

    /**
     * 计算稳定性得分（质心高度）。
     *
     * @param placement 待评估的摆放
     * @param workspace 工作空间
     * @return 稳定性得分（0-100）
     */
    private double calculateStability(PlacedModel placement, BoundingBox workspace) {
        float zPosition = placement.getPosition().getZ();
        float workspaceHeight = workspace.getDepth(); // Z轴高度

        // 质心越低越稳定
        // stability = 100 * (1 - z / workspaceHeight)
        return 100.0 * (1.0 - zPosition / workspaceHeight);
    }

    /**
     * 计算边界惩罚（距离边界过近时）。
     *
     * @param placement 待评估的摆放
     * @param workspace 工作空间
     * @param minGap 最小间隙
     * @return 边界惩罚得分（0-100）
     */
    private double calculateBoundaryPenalty(
            PlacedModel placement,
            BoundingBox workspace,
            float minGap) {

        BoundingBox placementAABB = placement.getTransformedAABB();
        Vector3D pMin = placementAABB.getMin();
        Vector3D pMax = placementAABB.getMax();
        Vector3D wMin = workspace.getMin();
        Vector3D wMax = workspace.getMax();

        // 计算到各边界的距离
        float[] distances = {
            pMin.getX() - wMin.getX(),
            pMin.getY() - wMin.getY(),
            pMin.getZ() - wMin.getZ(),
            wMax.getX() - pMax.getX(),
            wMax.getY() - pMax.getY(),
            wMax.getZ() - pMax.getZ()
        };

        // 找到最小边界距离
        float minBoundaryDistance = Float.MAX_VALUE;
        for (float distance : distances) {
            minBoundaryDistance = Math.min(minBoundaryDistance, distance);
        }

        // 如果距离小于阈值，施加惩罚
        if (minBoundaryDistance < BOUNDARY_THRESHOLD) {
            // 距离越近，惩罚越大
            return 100.0 * (1.0 - minBoundaryDistance / BOUNDARY_THRESHOLD);
        }

        return 0.0; // 无惩罚
    }

    @Override
    public String toString() {
        return "GreedyPlacementStrategy[DBLF]";
    }

    /**
     * 多批次复制摆放实现。
     *
     * <p><b>流程：</b></p>
     * <ol>
     *   <li>阶段一（覆盖批次）：按体积从大到小对每类模型各摆 1 个，满足覆盖约束</li>
     *   <li>阶段二（追加批次）：循环对每类模型尝试追加一个副本，直到满足任一停止条件</li>
     * </ol>
     *
     * <p><b>停止条件：</b></p>
     * <ul>
     *   <li>当前 AABB 利用率 ≥ targetUtilization</li>
     *   <li>每类副本数均已达 maxCopiesPerType</li>
     *   <li>一轮完整循环未放入任何副本（空间饱和）</li>
     * </ul>
     *
     * @param models             模型类型列表（每类 1 个 ModelInfo）
     * @param workspace          工作空间边界
     * @param minGap             最小间隙（mm）
     * @param targetUtilization  目标 AABB 利用率（0.0-1.0）
     * @param maxCopiesPerType   每类最多摆放副本数（含覆盖的第 1 个），必须 ≥ 1
     * @return 所有已摆放模型列表
     */
    @Override
    public List<PlacedModel> placeMultiBatch(
            List<ModelInfo> models,
            BoundingBox workspace,
            float minGap,
            float targetUtilization,
            int maxCopiesPerType) {

        // 参数验证
        Objects.requireNonNull(models, "模型列表不能为null");
        Objects.requireNonNull(workspace, "工作空间不能为null");
        if (models.isEmpty()) {
            throw new IllegalArgumentException("模型列表不能为空");
        }
        if (minGap < 0) {
            throw new IllegalArgumentException("最小间隙必须为非负数：" + minGap);
        }
        if (targetUtilization < 0 || targetUtilization > 1) {
            throw new IllegalArgumentException(
                "目标利用率必须在[0,1]范围内：" + targetUtilization);
        }
        if (maxCopiesPerType < 1) {
            throw new IllegalArgumentException(
                "每类最大副本数必须≥1：" + maxCopiesPerType);
        }

        // 按体积从大到小排序，大件优先
        List<ModelInfo> sortedModels = new ArrayList<>(models);
        sortedModels.sort(Comparator.comparing(ModelInfo::getVolume).reversed());

        SpaceManager spaceManager = new SpaceManager(workspace, minGap);
        float workspaceVolume = workspace.getVolume();

        // 记录每类已摆放的副本数
        int[] copyCount = new int[sortedModels.size()];

        // ── 阶段一：覆盖批次，每类至少摆 1 个 ──────────────────────────────
        for (int i = 0; i < sortedModels.size(); i++) {
            ModelInfo model = sortedModels.get(i);
            PlacementCandidate best =
                findBestCandidate(model, spaceManager, workspace, minGap);
            if (best != null) {
                spaceManager.place(
                    PlacedModel.create(model, best.getRotation(), best.getPosition()));
                copyCount[i]++;
            } else {
                System.err.println("警告（覆盖批次）：无法摆放模型 " + model.getFileName());
            }
            // 覆盖批次也需检查利用率，以便极小空间尽早退出
            if (spaceManager.getUtilizationRate() >= targetUtilization) {
                return new ArrayList<>(spaceManager.getPlacedModels());
            }
        }

        // ── 阶段二：追加批次，循环直到饱和或达到目标 ────────────────────────
        while (true) {
            // 检查利用率
            if (spaceManager.getUtilizationRate() >= targetUtilization) {
                break;
            }

            // 检查副本上限：全部达到上限则停止
            boolean allAtMax = true;
            for (int count : copyCount) {
                if (count < maxCopiesPerType) {
                    allAtMax = false;
                    break;
                }
            }
            if (allAtMax) {
                break;
            }

            // 一轮：对每类尝试追加一个副本
            boolean anyPlacedThisRound = false;
            for (int i = 0; i < sortedModels.size(); i++) {
                if (copyCount[i] >= maxCopiesPerType) {
                    continue;
                }
                ModelInfo model = sortedModels.get(i);
                PlacementCandidate best =
                    findBestCandidate(model, spaceManager, workspace, minGap);
                if (best != null) {
                    spaceManager.place(
                        PlacedModel.create(model, best.getRotation(), best.getPosition()));
                    copyCount[i]++;
                    anyPlacedThisRound = true;
                }
                if (spaceManager.getUtilizationRate() >= targetUtilization) {
                    return new ArrayList<>(spaceManager.getPlacedModels());
                }
            }

            // 空间饱和：整轮没有放入任何模型
            if (!anyPlacedThisRound) {
                break;
            }
        }

        return new ArrayList<>(spaceManager.getPlacedModels());
    }

    @Override
    public String toString() {
        return "GreedyPlacementStrategy[DBLF, 24-rotations]";
    }
}
