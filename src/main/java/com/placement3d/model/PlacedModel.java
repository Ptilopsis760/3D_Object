package com.placement3d.model;

import com.placement3d.geometry.BoundingBox;
import java.util.Objects;

/**
 * 表示已摆放的3D模型（不可变类）。
 *
 * <p>此类封装了一个已摆放模型的完整信息，包括：
 * <ul>
 *   <li>原始模型信息（ModelInfo）</li>
 *   <li>旋转矩阵（Matrix3x3）</li>
 *   <li>摆放位置（Vector3D）</li>
 *   <li>变换后的AABB（BoundingBox）- 自动计算</li>
 * </ul>
 *
 * <p><b>不可变性：</b>所有字段都是 {@code public final}，并且没有setter方法。
 * 这确保了一旦创建，PlacedModel实例就不能被修改，避免了意外的副作用。</p>
 *
 * <p><b>坐标系：</b></p>
 * <ul>
 *   <li>世界坐标系：原点在打印平台左下角</li>
 *   <li>X轴：右，Y轴：前，Z轴：上</li>
 *   <li>单位：毫米（mm）</li>
 *   <li>旋转：先围绕质心旋转，然后平移到目标位置</li>
 * </ul>
 *
 * <p><b>使用方式：</b></p>
 * <pre>
 * ModelInfo model = ...; // 从STL文件加载
 * Matrix3x3 rotation = Matrix3x3.fromEulerAngles(0, 45, 0); // 绕Y轴旋转45度
 * Vector3D position = new Vector3D(100, 200, 0); // 摆放位置
 *
 * PlacedModel placed = PlacedModel.create(model, rotation, position);
 *
 * // 访问字段
 * BoundingBox aabb = placed.transformedAABB; // 用于碰撞检测
 * Vector3D pos = placed.position; // 用于JSON输出
 * </pre>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 * @see ModelInfo
 * @see Matrix3x3
 * @see BoundingBox
 */
public final class PlacedModel {

    /** 原始模型信息 */
    public final ModelInfo model;

    /** 旋转矩阵（相对于模型质心） */
    public final Matrix3x3 rotationMatrix;

    /** 摆放位置（模型质心在世界坐标系中的位置） */
    public final Vector3D position;

    /** 变换后的轴对齐包围盒（旋转+平移后的AABB，用于碰撞检测） */
    public final BoundingBox transformedAABB;

    /**
     * 私有构造函数 - 使用静态工厂方法 {@link #create(ModelInfo, Matrix3x3, Vector3D)} 创建实例。
     *
     * @param model 原始模型信息
     * @param rotationMatrix 旋转矩阵
     * @param position 摆放位置
     * @param transformedAABB 变换后的AABB
     */
    private PlacedModel(ModelInfo model, Matrix3x3 rotationMatrix, Vector3D position, BoundingBox transformedAABB) {
        this.model = model;
        this.rotationMatrix = rotationMatrix;
        this.position = position;
        this.transformedAABB = transformedAABB;
    }

    /**
     * 公共构造函数（向后兼容）。
     *
     * <p>为了保持与现有代码的兼容性，提供此构造函数。
     * 推荐使用静态工厂方法 {@link #create(ModelInfo, Matrix3x3, Vector3D)}。</p>
     *
     * @param model 原始模型信息
     * @param rotationMatrix 旋转矩阵
     * @param position 摆放位置
     * @throws NullPointerException 如果任何参数为null
     */
    public PlacedModel(ModelInfo model, Matrix3x3 rotationMatrix, Vector3D position) {
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(rotationMatrix, "Rotation matrix cannot be null");
        Objects.requireNonNull(position, "Position cannot be null");

        this.model = model;
        this.rotationMatrix = rotationMatrix;
        this.position = position;

        // 计算transformedAABB
        BoundingBox rotatedAABB = model.computeRotatedAABB(rotationMatrix);
        Vector3D offset = position.subtract(model.getCenterOfMass());
        Vector3D newMin = rotatedAABB.getMin().add(offset);
        Vector3D newMax = rotatedAABB.getMax().add(offset);
        this.transformedAABB = new BoundingBox(newMin, newMax);
    }

    /**
     * 创建一个已摆放的模型实例（静态工厂方法）。
     *
     * <p>此方法自动计算变换后的AABB，计算步骤如下：</p>
     * <pre>
     * 1. 调用 model.computeRotatedAABB(rotation) 获取围绕质心旋转后的AABB
     * 2. 计算平移偏移量：offset = position - model.getCenterOfMass()
     * 3. 将旋转后的AABB平移到目标位置：
     *    newMin = rotatedAABB.min + offset
     *    newMax = rotatedAABB.max + offset
     * </pre>
     *
     * <p><b>变换语义：</b></p>
     * <ul>
     *   <li>旋转：围绕模型的质心（centerOfMass）进行旋转</li>
     *   <li>平移：将旋转后的模型质心移动到 {@code position} 指定的位置</li>
     *   <li>AABB：轴对齐包围盒自动适应旋转后的几何形状</li>
     * </ul>
     *
     * @param model 原始模型信息（不能为null）
     * @param rotation 旋转矩阵（不能为null，使用 {@link Matrix3x3#IDENTITY} 表示无旋转）
     * @param position 摆放位置（不能为null，表示模型质心的世界坐标）
     * @return 新的PlacedModel实例
     * @throws NullPointerException 如果任何参数为null
     */
    public static PlacedModel create(ModelInfo model, Matrix3x3 rotation, Vector3D position) {
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(rotation, "Rotation matrix cannot be null");
        Objects.requireNonNull(position, "Position cannot be null");

        // 步骤1：获取旋转后的AABB（围绕质心旋转，但仍在原位置）
        BoundingBox rotatedAABB = model.computeRotatedAABB(rotation);

        // 步骤2：计算平移偏移量（从原质心位置到目标位置）
        Vector3D offset = position.subtract(model.getCenterOfMass());

        // 步骤3：将旋转后的AABB平移到目标位置
        Vector3D newMin = rotatedAABB.getMin().add(offset);
        Vector3D newMax = rotatedAABB.getMax().add(offset);
        BoundingBox transformedAABB = new BoundingBox(newMin, newMax);

        return new PlacedModel(model, rotation, position, transformedAABB);
    }

    /**
     * 获取原始模型信息。
     *
     * @return 原始模型信息
     */
    public ModelInfo getModel() {
        return model;
    }

    /**
     * 获取旋转矩阵。
     *
     * @return 旋转矩阵
     */
    public Matrix3x3 getRotationMatrix() {
        return rotationMatrix;
    }

    /**
     * 获取摆放位置（模型质心的世界坐标）。
     *
     * @return 摆放位置
     */
    public Vector3D getPosition() {
        return position;
    }

    /**
     * 获取变换后的轴对齐包围盒。
     *
     * <p>此AABB已经应用了旋转和平移变换，可以直接用于：</p>
     * <ul>
     *   <li>碰撞检测（与其他PlacedModel的AABB进行相交测试）</li>
     *   <li>空间管理（八叉树插入、查询）</li>
     *   <li>边界检查（确保模型在工作空间内）</li>
     * </ul>
     *
     * @return 变换后的AABB
     */
    public BoundingBox getTransformedAABB() {
        return transformedAABB;
    }

    /**
     * 获取模型文件名（便利方法）。
     *
     * @return 文件名
     */
    public String getFileName() {
        return model.getFileName();
    }

    /**
     * 获取模型体积（便利方法）。
     *
     * @return 体积（立方毫米）
     */
    public float getVolume() {
        return model.getVolume();
    }

    /**
     * 获取模型ID（便利方法）。
     *
     * @return 模型ID
     */
    public int getModelId() {
        return model.getModelId();
    }

    /**
     * 检查两个已摆放模型是否相等。
     *
     * <p>两个PlacedModel相等当且仅当它们的模型、旋转矩阵和位置都相等。
     * transformedAABB不需要显式比较，因为它是从其他字段派生的。</p>
     *
     * @param obj 要比较的对象
     * @return 如果相等返回true，否则返回false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PlacedModel other = (PlacedModel) obj;
        return model.equals(other.model) &&
               rotationMatrix.equals(other.rotationMatrix) &&
               position.equals(other.position);
    }

    /**
     * 计算哈希码。
     *
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return Objects.hash(model, rotationMatrix, position);
    }

    /**
     * 返回字符串表示（用于调试）。
     *
     * <p>格式：{@code PlacedModel[model=gear.stl, position=(100.00, 200.00, 0.00), aabb=...]}</p>
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return String.format(
            "PlacedModel[model=%s, position=%s, aabb=%s]",
            model.getFileName(),
            position,
            transformedAABB
        );
    }
}
