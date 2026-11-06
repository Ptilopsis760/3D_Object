package com.placement3d.algorithm;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.ModelInfo;
import com.placement3d.model.PlacedModel;
import com.placement3d.model.Vector3D;

import java.util.List;
import java.util.Objects;

/**
 * 摆放评分函数模块 - 评估候选摆放方案的质量。
 *
 * <p>此类为贪心算法（阶段1）提供评分能力，通过多维度评估候选方案：
 * <ul>
 *   <li><b>空间利用率</b> (权重0.5) - 已摆放模型占工作空间的比例</li>
 *   <li><b>紧凑度</b> (权重0.3) - 候选位置与已摆放模型质心的接近程度</li>
 *   <li><b>稳定性</b> (权重0.2) - 候选位置的高度（越低越好）</li>
 * </ul>
 *
 * <p><b>评分公式：</b></p>
 * <pre>
 * 总评分 = 0.5 × 利用率评分 + 0.3 × 紧凑度评分 + 0.2 × 稳定性评分
 * </pre>
 *
 * <p><b>使用方式：</b></p>
 * <pre>
 * BoundingBox workspace = new BoundingBox(...);
 * PlacementEvaluator evaluator = new PlacementEvaluator(workspace, 0.5f, 0.3f, 0.2f);
 *
 * PlacementCandidate candidate = new PlacementCandidate(position, model);
 * double score = evaluator.evaluate(candidate, placedModels);
 * candidate.setScore(score);
 * </pre>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 * @see PlacementCandidate
 */
public class PlacementEvaluator {

    /** 工作空间 */
    private final BoundingBox workspace;

    /** 利用率权重（默认0.5） */
    private final float utilizationWeight;

    /** 紧凑度权重（默认0.3） */
    private final float compactnessWeight;

    /** 稳定性权重（默认0.2） */
    private final float stabilityWeight;

    /** 工作空间体积（缓存） */
    private final float workspaceVolume;

    /** 工作空间对角线长度（缓存） */
    private final float workspaceDiagonal;

    /** 工作空间高度（缓存） */
    private final float workspaceHeight;

    /**
     * 构造函数 - 使用默认权重。
     *
     * @param workspace 工作空间
     * @throws NullPointerException 如果workspace为null
     */
    public PlacementEvaluator(BoundingBox workspace) {
        this(workspace, 0.5f, 0.3f, 0.2f);
    }

    /**
     * 构造函数 - 自定义权重。
     *
     * @param workspace 工作空间
     * @param utilizationWeight 利用率权重（默认0.5）
     * @param compactnessWeight 紧凑度权重（默认0.3）
     * @param stabilityWeight 稳定性权重（默认0.2）
     * @throws NullPointerException 如果workspace为null
     * @throws IllegalArgumentException 如果权重为负或权重和不为1.0
     */
    public PlacementEvaluator(BoundingBox workspace,
                             float utilizationWeight,
                             float compactnessWeight,
                             float stabilityWeight) {
        this.workspace = Objects.requireNonNull(workspace, "Workspace cannot be null");

        if (utilizationWeight < 0 || compactnessWeight < 0 || stabilityWeight < 0) {
            throw new IllegalArgumentException("Weights must be non-negative");
        }

        float weightSum = utilizationWeight + compactnessWeight + stabilityWeight;
        if (Math.abs(weightSum - 1.0f) > 0.001f) {
            throw new IllegalArgumentException(
                String.format("Weights must sum to 1.0, got %.3f", weightSum)
            );
        }

        this.utilizationWeight = utilizationWeight;
        this.compactnessWeight = compactnessWeight;
        this.stabilityWeight = stabilityWeight;

        // 缓存计算值
        this.workspaceVolume = workspace.getVolume();
        this.workspaceHeight = workspace.getDepth();

        // 计算对角线长度：√(width² + height² + depth²)
        float width = workspace.getWidth();
        float height = workspace.getHeight();
        float depth = workspace.getDepth();
        this.workspaceDiagonal = (float) Math.sqrt(width * width + height * height + depth * depth);
    }

    /**
     * 评估候选方案并返回带评分的新候选。
     *
     * @param candidate 候选方案
     * @param placedModels 已摆放的模型列表
     * @return 带有评分的新候选方案
     * @throws NullPointerException 如果任何参数为null
     */
    public PlacementCandidate evaluateCandidate(PlacementCandidate candidate, List<PlacedModel> placedModels) {
        double score = evaluate(candidate, placedModels);
        return candidate.withScore(score);
    }

    /**
     * 评估候选方案并返回评分。
     *
     * @param candidate 候选方案
     * @param placedModels 已摆放的模型列表
     * @return 总评分（0.0 - 1.0）
     * @throws NullPointerException 如果任何参数为null
     */
    public double evaluate(PlacementCandidate candidate, List<PlacedModel> placedModels) {
        Objects.requireNonNull(candidate, "Candidate cannot be null");
        Objects.requireNonNull(placedModels, "Placed models list cannot be null");

        Vector3D candidatePosition = candidate.getPosition();
        ModelInfo newModel = candidate.getModel();

        // 计算三个维度的评分
        double utilScore = evaluateUtilization(placedModels, newModel);
        double compactScore = evaluateCompactness(candidatePosition, placedModels);
        double stableScore = evaluateStability(candidatePosition);

        // 加权求和
        double totalScore = utilizationWeight * utilScore +
                           compactnessWeight * compactScore +
                           stabilityWeight * stableScore;

        // 确保在[0.0, 1.0]范围内
        return Math.max(0.0, Math.min(1.0, totalScore));
    }

    /**
     * 计算空间利用率评分（按 AABB 体积口径）。
     *
     * <p>公式：utilization = 已摆放模型 AABB 体积之和 / 工作空间体积</p>
     *
     * <p><b>口径说明：</b>经与老师确认（2026-11-06），利用率按包围盒（AABB）体积计算，
     * 不按真实体积。此处与 {@link com.placement3d.space.SpaceManager#getUtilizationRate()}
     * 保持一致。</p>
     *
     * @param placedModels 已摆放的模型列表
     * @param newModel 新增模型
     * @return 利用率评分（0.0 - 1.0）
     */
    private double evaluateUtilization(List<PlacedModel> placedModels, ModelInfo newModel) {
        // 计算已摆放模型的 AABB 体积总和（与利用率口径一致）
        float totalVolume = 0.0f;
        for (PlacedModel placed : placedModels) {
            totalVolume += placed.getTransformedAABB().getVolume();
        }

        // 加上新模型的 AABB 体积（用原始AABB，因为此时还未摆放、无transformedAABB）
        totalVolume += newModel.getOriginalAABB().getVolume();

        // 计算利用率
        float utilization = totalVolume / workspaceVolume;

        // 确保在[0.0, 1.0]范围内
        return Math.max(0.0, Math.min(1.0, utilization));
    }

    /**
     * 计算紧凑度评分。
     *
     * <p>公式：紧凑度 = 1.0 - (候选位置到质心的距离 / 工作空间对角线长度)</p>
     * <p>质心 = 所有已摆放模型质心的平均位置</p>
     *
     * @param candidatePosition 候选位置
     * @param placedModels 已摆放的模型列表
     * @return 紧凑度评分（0.0 - 1.0）
     */
    private double evaluateCompactness(Vector3D candidatePosition, List<PlacedModel> placedModels) {
        // 如果没有已摆放的模型，紧凑度固定为1.0
        if (placedModels.isEmpty()) {
            return 1.0;
        }

        // 计算已摆放模型的质心
        Vector3D centroid = computeCentroid(placedModels);

        // 计算候选位置到质心的距离
        float distance = candidatePosition.distance(centroid);

        // 计算紧凑度
        float compactness = 1.0f - (distance / workspaceDiagonal);

        // 确保在[0.0, 1.0]范围内
        return Math.max(0.0, Math.min(1.0, compactness));
    }

    /**
     * 计算稳定性评分。
     *
     * <p>公式：稳定性 = 1.0 - (候选位置Z坐标 / 工作空间高度)</p>
     * <p>倾向于低层摆放，Z越小越好。</p>
     *
     * @param candidatePosition 候选位置
     * @return 稳定性评分（0.0 - 1.0）
     */
    private double evaluateStability(Vector3D candidatePosition) {
        // 计算相对高度
        float relativeHeight = candidatePosition.getZ() / workspaceHeight;

        // 计算稳定性（高度越低，稳定性越高）
        float stability = 1.0f - relativeHeight;

        // 确保在[0.0, 1.0]范围内
        return Math.max(0.0, Math.min(1.0, stability));
    }

    /**
     * 计算已摆放模型的质心。
     *
     * <p>质心是所有已摆放模型位置的平均值。</p>
     *
     * @param placedModels 已摆放的模型列表
     * @return 质心位置
     */
    private Vector3D computeCentroid(List<PlacedModel> placedModels) {
        if (placedModels.isEmpty()) {
            return Vector3D.ZERO;
        }

        float sumX = 0.0f;
        float sumY = 0.0f;
        float sumZ = 0.0f;

        for (PlacedModel placed : placedModels) {
            Vector3D pos = placed.getPosition();
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
        }

        int count = placedModels.size();
        return new Vector3D(sumX / count, sumY / count, sumZ / count);
    }

    /**
     * 获取工作空间。
     *
     * @return 工作空间
     */
    public BoundingBox getWorkspace() {
        return workspace;
    }

    /**
     * 获取利用率权重。
     *
     * @return 利用率权重
     */
    public float getUtilizationWeight() {
        return utilizationWeight;
    }

    /**
     * 获取紧凑度权重。
     *
     * @return 紧凑度权重
     */
    public float getCompactnessWeight() {
        return compactnessWeight;
    }

    /**
     * 获取稳定性权重。
     *
     * @return 稳定性权重
     */
    public float getStabilityWeight() {
        return stabilityWeight;
    }
}
