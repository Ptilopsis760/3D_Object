package com.placement3d.algorithm;

import com.placement3d.model.ModelInfo;
import com.placement3d.model.Vector3D;
import com.placement3d.model.Matrix3x3;
import java.util.Objects;

/**
 * 表示一个候选摆放方案。
 *
 * <p>此类封装了贪心算法生成的候选位置及其相关信息：
 * <ul>
 *   <li>待摆放的模型（model）</li>
 *   <li>候选位置（position）- 模型质心将被放置的位置</li>
 *   <li>旋转矩阵（rotation）- 旋转变换</li>
 *   <li>评分（score）- 由PlacementEvaluator计算的质量评分</li>
 * </ul>
 *
 * <p><b>用途：</b>在DBLF（Deepest Bottom Left Fill）算法中，
 * 每个模型可能有多个候选位置。PlacementEvaluator对每个候选进行评分，
 * 然后选择得分最高的位置。</p>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 * @see PlacementEvaluator
 */
public final class PlacementCandidate implements Comparable<PlacementCandidate> {

    /** 待摆放的模型信息 */
    private final ModelInfo model;

    /** 摆放位置（模型质心的世界坐标） */
    private final Vector3D position;

    /** 旋转矩阵（相对于质心的旋转） */
    private final Matrix3x3 rotation;

    /** 评分（越高越好） */
    private final double score;

    /**
     * 创建一个摆放候选方案。
     *
     * @param model 待摆放的模型信息
     * @param position 摆放位置（模型质心的世界坐标）
     * @param rotation 旋转矩阵
     * @param score 评分（越高越好）
     * @throws NullPointerException 如果任何引用参数为null
     */
    public PlacementCandidate(ModelInfo model, Vector3D position, Matrix3x3 rotation, double score) {
        this.model = Objects.requireNonNull(model, "模型不能为null");
        this.position = Objects.requireNonNull(position, "位置不能为null");
        this.rotation = Objects.requireNonNull(rotation, "旋转矩阵不能为null");
        this.score = score;
    }

    /**
     * 创建一个摆放候选方案（无旋转，初始评分为0）。
     *
     * @param model 待摆放的模型信息
     * @param position 摆放位置（模型质心的世界坐标）
     * @throws NullPointerException 如果任何引用参数为null
     */
    public PlacementCandidate(ModelInfo model, Vector3D position) {
        this(model, position, Matrix3x3.IDENTITY, 0.0);
    }

    /**
     * 获取待摆放的模型。
     *
     * @return 模型信息
     */
    public ModelInfo getModel() {
        return model;
    }

    /**
     * 获取候选位置。
     *
     * @return 候选位置
     */
    public Vector3D getPosition() {
        return position;
    }

    /**
     * 获取旋转矩阵。
     *
     * @return 旋转矩阵
     */
    public Matrix3x3 getRotation() {
        return rotation;
    }

    /**
     * 获取评分。
     *
     * @return 评分（越高越好）
     */
    public double getScore() {
        return score;
    }

    /**
     * 创建一个新的候选方案，使用指定的评分。
     *
     * @param newScore 新的评分
     * @return 新的PlacementCandidate实例
     */
    public PlacementCandidate withScore(double newScore) {
        return new PlacementCandidate(this.model, this.position, this.rotation, newScore);
    }

    /**
     * 比较两个候选方案（按评分降序排列）。
     *
     * @param other 另一个候选方案
     * @return 比较结果（评分高的排在前面）
     */
    @Override
    public int compareTo(PlacementCandidate other) {
        // 评分高的排在前面（降序）
        return Double.compare(other.score, this.score);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PlacementCandidate other = (PlacementCandidate) obj;
        return Double.compare(other.score, score) == 0 &&
               model.equals(other.model) &&
               position.equals(other.position) &&
               rotation.equals(other.rotation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, position, rotation, score);
    }

    @Override
    public String toString() {
        return String.format(
            "PlacementCandidate[model=%s, position=%s, score=%.4f]",
            model.getFileName(),
            position,
            score
        );
    }
}
