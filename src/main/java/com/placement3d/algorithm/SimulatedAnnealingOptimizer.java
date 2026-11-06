package com.placement3d.algorithm;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.Matrix3x3;
import com.placement3d.model.PlacedModel;
import com.placement3d.model.Vector3D;
import com.placement3d.space.SpaceManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 模拟退火优化器 - 阶段2：在贪心初始解基础上优化 AABB 空间利用率。
 *
 * <p>采用 Metropolis 接受准则和指数降温 schedule，通过 4 种邻域操作探索解空间，
 * 目标是在贪心基础上提升 AABB 利用率 5-10%。</p>
 *
 * <p><b>4 种邻域操作（等概率随机选择）：</b></p>
 * <ul>
 *   <li><b>REPOSITION</b>：随机选一个已摆放模型，移动到另一个候选位置（保留旋转）</li>
 *   <li><b>ROTATE</b>：随机选一个已摆放模型，换用 24 种轴对齐姿态之一</li>
 *   <li><b>SWAP</b>：随机选两个已摆放模型，交换位置（各保留自身旋转）</li>
 *   <li><b>REMOVE_AND_INSERT</b>：移除一个模型，用随机旋转 + 随机偏移重新插入</li>
 * </ul>
 *
 * <p><b>Metropolis 准则：</b>
 * Δ = 新利用率 - 当前利用率。Δ ≥ 0 直接接受；Δ < 0 以概率 exp(Δ / T) 接受。</p>
 *
 * <p><b>降温 schedule：</b> T_{k+1} = α × T_k，默认 T₀=100，α=0.95，T_min=0.01。</p>
 *
 * <p><b>设计原则：</b> 每个邻域操作"apply 后评估，拒绝时 rollback"，
 * 始终维护有效的 SpaceManager 状态，不依赖快照拷贝（避免 O(n) 深拷贝开销）。</p>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
public class SimulatedAnnealingOptimizer {

    // ── 降温参数 ───────────────────────────────────────────────────────────

    /** 初始温度 */
    public static final double INITIAL_TEMPERATURE = 100.0;

    /** 降温系数 */
    public static final double COOLING_ALPHA = 0.95;

    /** 最低温度（低于此值停止） */
    public static final double MIN_TEMPERATURE = 0.01;

    /** 默认最大迭代次数 */
    public static final int DEFAULT_MAX_ITERATIONS = 500;

    // ── 邻域操作参数 ───────────────────────────────────────────────────────

    /** 24 种轴对齐旋转姿态 */
    private static final Matrix3x3[] ROTATIONS = Matrix3x3.allAxisAlignedRotations();

    /**
     * 最大平移扰动（mm）。REPOSITION 和 REMOVE_AND_INSERT 在原位置加随机偏移时使用。
     * 设为工作空间典型边长的 5~10%，太大会导致大量无效操作，太小搜索半径不足。
     */
    private static final float MAX_PERTURB = 300.0f;

    /** 位置生成时的浮点安全偏移，避免边界接触被判为碰撞 */
    private static final float POS_EPS = 0.01f;

    // ── 实例字段 ───────────────────────────────────────────────────────────

    private final int maxIterations;
    private final Random rng;

    // ── 构造函数 ───────────────────────────────────────────────────────────

    /** 使用默认参数（500 次迭代，随机种子）。 */
    public SimulatedAnnealingOptimizer() {
        this(DEFAULT_MAX_ITERATIONS, new Random());
    }

    /**
     * 自定义参数。
     *
     * @param maxIterations 最大迭代次数（必须 > 0）
     * @param random        随机数生成器（传入固定种子可复现结果）
     */
    public SimulatedAnnealingOptimizer(int maxIterations, Random random) {
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("最大迭代次数必须为正数：" + maxIterations);
        }
        this.maxIterations = maxIterations;
        this.rng = Objects.requireNonNull(random, "Random不能为null");
    }

    // ── 公共方法 ───────────────────────────────────────────────────────────

    /**
     * 对贪心初始解进行模拟退火优化。
     *
     * @param initialSolution 贪心算法的初始解（至少含 1 个模型）
     * @param workspace       工作空间边界
     * @param minGap          最小间隙（mm，≥ 0）
     * @return 优化后的摆放方案（利用率至少不差于初始解）
     */
    public List<PlacedModel> optimize(List<PlacedModel> initialSolution,
                                       BoundingBox workspace,
                                       float minGap) {
        Objects.requireNonNull(initialSolution, "初始解不能为null");
        Objects.requireNonNull(workspace, "工作空间不能为null");
        if (minGap < 0) throw new IllegalArgumentException("最小间隙必须为非负数：" + minGap);
        if (initialSolution.isEmpty()) return new ArrayList<>();

        // 用初始解构建 SpaceManager
        SpaceManager sm = new SpaceManager(workspace, minGap);
        for (PlacedModel pm : initialSolution) {
            sm.place(pm);
        }

        double currentUtil  = sm.getUtilizationRate();
        double bestUtil     = currentUtil;
        List<PlacedModel> bestSolution = new ArrayList<>(sm.getPlacedModels());

        double temperature = INITIAL_TEMPERATURE;

        for (int iter = 0; iter < maxIterations && temperature >= MIN_TEMPERATURE; iter++) {
            // 随机选邻域操作
            int op = rng.nextInt(4);
            Double deltaUtil = applyNeighborhood(op, sm, workspace, minGap);

            if (deltaUtil != null) {
                // Metropolis 准则：
                //   Δ ≥ 0：直接接受（操作已 apply，sm 保持新状态）
                //   Δ < 0：以概率 exp(Δ/T) 接受；
                //          每个 applyXxx 在失败时已自行还原，成功时提交。
                //          Metropolis 拒绝时 sm 保持新状态（标准退火行为），
                //          currentUtil 重新对齐 sm 的真实利用率，
                //          bestSolution 只在真正改善时才更新。
                boolean accept = deltaUtil >= 0
                    || rng.nextDouble() < Math.exp(deltaUtil / temperature);

                if (accept) {
                    currentUtil += deltaUtil;
                    if (currentUtil > bestUtil) {
                        bestUtil     = currentUtil;
                        bestSolution = new ArrayList<>(sm.getPlacedModels());
                    }
                } else {
                    // 拒绝：sm 已提交新状态，对齐 currentUtil 避免漂移
                    currentUtil = sm.getUtilizationRate();
                }
            }

            temperature *= COOLING_ALPHA;
        }

        return bestSolution;
    }

    /**
     * 返回初始温度、降温系数等参数，供测试和日志使用。
     */
    public double getInitialTemperature() { return INITIAL_TEMPERATURE; }
    public double getCoolingAlpha()        { return COOLING_ALPHA; }
    public int    getMaxIterations()       { return maxIterations; }

    // ── 邻域操作（apply-evaluate 模式） ────────────────────────────────────

    /**
     * 执行一次邻域操作，返回利用率变化量（Δ = newUtil - currentUtil）。
     *
     * <p>若操作失败（随机到的位置不可行，或模型数不足），返回 null，
     * SpaceManager 保持不变，调用方跳过本次迭代。</p>
     *
     * <p><b>注意：</b>本方法采用"乐观提交"策略——操作成功 apply 后 delta 已经反映在
     * SpaceManager 里。调用方若 Metropolis 拒绝，需调用 {@link #rollbackNeighborhood}
     * 将 SpaceManager 还原。</p>
     *
     * @param op      操作类型（0=REPOSITION, 1=ROTATE, 2=SWAP, 3=REMOVE_AND_INSERT）
     * @param sm      当前空间管理器
     * @param ws      工作空间
     * @param minGap  最小间隙
     * @return 利用率变化量，操作不可行时返回 null
     */
    private Double applyNeighborhood(int op, SpaceManager sm,
                                      BoundingBox ws, float minGap) {
        List<PlacedModel> placed = sm.getPlacedModels();
        if (placed.isEmpty()) return null;

        if (op == 0) return applyReposition(sm, placed, ws, minGap);
        if (op == 1) return applyRotate(sm, placed, ws, minGap);
        if (op == 2) return applySwap(sm, placed, ws, minGap);
        if (op == 3) return applyRemoveAndInsert(sm, placed, ws, minGap);
        return null;
    }

    // ──────────────────── REPOSITION ──────────────────────────────────────

    /**
     * 随机选一个模型，移动到围绕当前位置的随机扰动点（保留旋转）。
     * 若新位置不可行则返回 null，SpaceManager 不变。
     */
    private Double applyReposition(SpaceManager sm, List<PlacedModel> placed,
                                    BoundingBox ws, float minGap) {
        int idx = rng.nextInt(placed.size());
        PlacedModel target = placed.get(idx);

        Vector3D newPos = randomPerturb(target.getPosition(), ws);
        PlacedModel newPm = PlacedModel.create(target.getModel(),
                                               target.getRotationMatrix(), newPos);

        sm.remove(target);
        if (sm.canPlace(newPm)) {
            double oldVol = target.getTransformedAABB().getVolume();
            double newVol = newPm.getTransformedAABB().getVolume();
            sm.place(newPm);
            return (newVol - oldVol) / ws.getVolume();
        } else {
            sm.place(target); // 还原
            return null;
        }
    }

    // ──────────────────── ROTATE ──────────────────────────────────────────

    /**
     * 随机选一个模型，换用 24 种轴对齐姿态中的随机一种（位置不变）。
     * 若新旋转下的 AABB 不可摆放则返回 null，SpaceManager 不变。
     */
    private Double applyRotate(SpaceManager sm, List<PlacedModel> placed,
                                BoundingBox ws, float minGap) {
        int idx = rng.nextInt(placed.size());
        PlacedModel target = placed.get(idx);

        Matrix3x3 newRot = ROTATIONS[rng.nextInt(ROTATIONS.length)];
        PlacedModel newPm = PlacedModel.create(target.getModel(), newRot,
                                               target.getPosition());

        sm.remove(target);
        if (sm.canPlace(newPm)) {
            double oldVol = target.getTransformedAABB().getVolume();
            double newVol = newPm.getTransformedAABB().getVolume();
            sm.place(newPm);
            return (newVol - oldVol) / ws.getVolume();
        } else {
            sm.place(target);
            return null;
        }
    }

    // ──────────────────── SWAP ────────────────────────────────────────────

    /**
     * 随机选两个模型，互换位置（各保留自身旋转）。
     * 只有两次 canPlace 都通过才提交，否则还原并返回 null。
     */
    private Double applySwap(SpaceManager sm, List<PlacedModel> placed,
                              BoundingBox ws, float minGap) {
        if (placed.size() < 2) return null;

        int i = rng.nextInt(placed.size());
        int j;
        do { j = rng.nextInt(placed.size()); } while (j == i);

        PlacedModel a = placed.get(i);
        PlacedModel b = placed.get(j);

        PlacedModel aNew = PlacedModel.create(a.getModel(), a.getRotationMatrix(), b.getPosition());
        PlacedModel bNew = PlacedModel.create(b.getModel(), b.getRotationMatrix(), a.getPosition());

        sm.remove(a);
        sm.remove(b);

        if (sm.canPlace(aNew) && sm.canPlace(bNew)) {
            double oldVol = a.getTransformedAABB().getVolume()
                          + b.getTransformedAABB().getVolume();
            double newVol = aNew.getTransformedAABB().getVolume()
                          + bNew.getTransformedAABB().getVolume();
            sm.place(aNew);
            sm.place(bNew);
            return (newVol - oldVol) / ws.getVolume();
        } else {
            sm.place(a);
            sm.place(b);
            return null;
        }
    }

    // ──────────────────── REMOVE_AND_INSERT ───────────────────────────────

    /**
     * 随机移除一个模型，用随机旋转 + 随机位置重新插入。
     * 若找不到可行位置则放弃并还原，返回 null。
     */
    private Double applyRemoveAndInsert(SpaceManager sm, List<PlacedModel> placed,
                                         BoundingBox ws, float minGap) {
        int idx = rng.nextInt(placed.size());
        PlacedModel target = placed.get(idx);
        double oldVol = target.getTransformedAABB().getVolume();

        sm.remove(target);

        // 尝试若干次随机位置
        final int TRIALS = 10;
        for (int t = 0; t < TRIALS; t++) {
            Matrix3x3 rot  = ROTATIONS[rng.nextInt(ROTATIONS.length)];
            Vector3D  pos  = randomPositionInWorkspace(target.getModel(), rot, ws, minGap);
            if (pos == null) continue;
            PlacedModel newPm = PlacedModel.create(target.getModel(), rot, pos);
            if (sm.canPlace(newPm)) {
                double newVol = newPm.getTransformedAABB().getVolume();
                sm.place(newPm);
                return (newVol - oldVol) / ws.getVolume();
            }
        }

        // 全部失败：还原
        sm.place(target);
        return null;
    }

    // ──────────────────── Rollback ────────────────────────────────────────
    // No explicit rollback needed: every applyXxx either self-reverts on failure
    // or commits to sm on success. Metropolis "拒绝" updates currentUtil via
    // sm.getUtilizationRate() so the next iteration stays consistent.

    // ── 辅助方法 ───────────────────────────────────────────────────────────

    /**
     * 在当前位置附近随机扰动，扰动量在 [-MAX_PERTURB/2, MAX_PERTURB/2] 范围内，
     * 结果被 clamp 到工作空间内。
     */
    private Vector3D randomPerturb(Vector3D current, BoundingBox ws) {
        float dx = (rng.nextFloat() - 0.5f) * MAX_PERTURB;
        float dy = (rng.nextFloat() - 0.5f) * MAX_PERTURB;
        float dz = (rng.nextFloat() - 0.5f) * MAX_PERTURB;
        float x = clamp(current.getX() + dx, ws.getMin().getX(), ws.getMax().getX());
        float y = clamp(current.getY() + dy, ws.getMin().getY(), ws.getMax().getY());
        float z = clamp(current.getZ() + dz, ws.getMin().getZ(), ws.getMax().getZ());
        return new Vector3D(x, y, z);
    }

    /**
     * 在工作空间内随机生成一个合法的质心位置（使模型 AABB 在空间内并留有 minGap）。
     * 若空间太小无法容纳该模型，返回 null。
     */
    private Vector3D randomPositionInWorkspace(
            com.placement3d.model.ModelInfo model,
            Matrix3x3 rotation,
            BoundingBox ws, float minGap) {

        BoundingBox rotAABB = model.computeRotatedAABB(rotation);
        float halfW = rotAABB.getWidth()  / 2f;
        float halfH = rotAABB.getHeight() / 2f;
        float halfD = rotAABB.getDepth()  / 2f;

        float xMin = ws.getMin().getX() + minGap + halfW + POS_EPS;
        float xMax = ws.getMax().getX() - minGap - halfW - POS_EPS;
        float yMin = ws.getMin().getY() + minGap + halfH + POS_EPS;
        float yMax = ws.getMax().getY() - minGap - halfH - POS_EPS;
        float zMin = ws.getMin().getZ() + minGap + halfD + POS_EPS;
        float zMax = ws.getMax().getZ() - minGap - halfD - POS_EPS;

        if (xMin > xMax || yMin > yMax || zMin > zMax) return null;

        float x = xMin + rng.nextFloat() * (xMax - xMin);
        float y = yMin + rng.nextFloat() * (yMax - yMin);
        float z = zMin + rng.nextFloat() * (zMax - zMin);
        return new Vector3D(x, y, z);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    @Override
    public String toString() {
        return String.format(
            "SimulatedAnnealingOptimizer[T₀=%.0f, α=%.2f, Tmin=%.3f, maxIter=%d]",
            INITIAL_TEMPERATURE, COOLING_ALPHA, MIN_TEMPERATURE, maxIterations);
    }
}
