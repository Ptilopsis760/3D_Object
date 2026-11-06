package com.placement3d.io;

import com.placement3d.geometry.BoundingBox;
import com.placement3d.model.Matrix3x3;
import com.placement3d.model.PlacedModel;
import com.placement3d.model.Vector3D;

import java.util.List;
import java.util.Objects;

/**
 * 摆放结果 JSON 序列化器（手写，无第三方依赖）。
 *
 * <p>将一组 {@link PlacedModel} 序列化为前端 UI 可直接消费的 JSON 字符串，
 * 包含工作空间、利用率、每个模型的位置/旋转/包围盒/4×4 变换矩阵。</p>
 *
 * <p><b>变换语义：</b>输出满足 {@code 世界坐标 = R · (v − pivot) + position}，
 * 其中 v 是 STL 原始顶点。等价的 4×4 矩阵 [R | t] 中
 * {@code t = position − R · pivot}，UI 端对原始顶点做一次矩阵乘法即可还原世界坐标。</p>
 *
 * @author 3D Placement System Team
 * @version 1.0.0
 */
public final class PlacementResultSerializer {

    private PlacementResultSerializer() {
        // 工具类，禁止实例化
    }

    /**
     * 序列化摆放结果列表为 JSON 字符串。
     *
     * @param placedModels 已摆放模型列表
     * @param workspace    工作空间边界
     * @return JSON 字符串
     * @throws NullPointerException 如果任何参数为 null
     */
    public static String toJson(List<PlacedModel> placedModels, BoundingBox workspace) {
        Objects.requireNonNull(placedModels, "placedModels 不能为 null");
        Objects.requireNonNull(workspace, "workspace 不能为 null");

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // workspace
        appendWorkspace(sb, workspace, 1);
        sb.append(",\n");

        // totalPlaced
        sb.append("  \"totalPlaced\": ").append(placedModels.size()).append(",\n");

        // aabbUtilization
        float workspaceVolume = workspace.getVolume();
        float totalAabbVolume = 0.0f;
        for (PlacedModel m : placedModels) {
            totalAabbVolume += m.getTransformedAABB().getVolume();
        }
        double utilization = workspaceVolume > 0
            ? totalAabbVolume / workspaceVolume : 0.0;
        sb.append("  \"aabbUtilization\": ")
            .append(String.format(java.util.Locale.ROOT, "%.6f", utilization))
            .append(",\n");

        // models 数组
        sb.append("  \"models\": [");
        if (placedModels.isEmpty()) {
            sb.append("]\n");
        } else {
            sb.append("\n");
            for (int i = 0; i < placedModels.size(); i++) {
                appendModel(sb, placedModels.get(i), 2);
                if (i < placedModels.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ]\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 序列化单个 PlacedModel 为 JSON 对象字符串（供调试用）。
     *
     * @param model 已摆放模型
     * @return JSON 对象字符串
     * @throws NullPointerException 如果 model 为 null
     */
    public static String toJson(PlacedModel model) {
        Objects.requireNonNull(model, "model 不能为 null");
        StringBuilder sb = new StringBuilder();
        appendModel(sb, model, 0);
        return sb.toString();
    }

    // ────────────────────────────────────────────────────────────────────

    private static void appendWorkspace(StringBuilder sb, BoundingBox ws, int indent) {
        String pad = indent(indent);
        sb.append(pad).append("\"workspace\": {\n");
        appendVec3(sb, "min", ws.getMin(), indent + 1);
        sb.append(",\n");
        appendVec3(sb, "max", ws.getMax(), indent + 1);
        sb.append("\n").append(pad).append("}");
    }

    private static void appendModel(StringBuilder sb, PlacedModel model, int indent) {
        String pad = indent(indent);
        String pad1 = indent(indent + 1);

        sb.append(pad).append("{\n");

        // fileName
        sb.append(pad1).append("\"fileName\": \"")
            .append(escapeJson(model.getFileName())).append("\",\n");

        // modelId
        sb.append(pad1).append("\"modelId\": ").append(model.getModelId()).append(",\n");

        // position
        appendVec3(sb, "position", model.getPosition(), indent + 1);
        sb.append(",\n");

        // pivot（原始质心）
        Vector3D pivot = model.getModel().getCenterOfMass();
        appendVec3(sb, "pivot", pivot, indent + 1);
        sb.append(",\n");

        // euler
        double[] euler = model.getRotationMatrix().toEulerAngles();
        sb.append(pad1).append("\"euler\": [")
            .append(String.format(java.util.Locale.ROOT, "%.4f", euler[0])).append(", ")
            .append(String.format(java.util.Locale.ROOT, "%.4f", euler[1])).append(", ")
            .append(String.format(java.util.Locale.ROOT, "%.4f", euler[2]))
            .append("],\n");

        // matrix4x4：[R | t]，t = position − R·pivot
        Matrix3x3 R = model.getRotationMatrix();
        Vector3D pos = model.getPosition();
        double[][] r = R.getData();
        // t = position - R * pivot
        double tx = pos.x - (r[0][0] * pivot.x + r[0][1] * pivot.y + r[0][2] * pivot.z);
        double ty = pos.y - (r[1][0] * pivot.x + r[1][1] * pivot.y + r[1][2] * pivot.z);
        double tz = pos.z - (r[2][0] * pivot.x + r[2][1] * pivot.y + r[2][2] * pivot.z);

        sb.append(pad1).append("\"matrix4x4\": [\n");
        appendMatrixRow(sb, r[0][0], r[0][1], r[0][2], tx, indent + 2, false);
        sb.append(",\n");
        appendMatrixRow(sb, r[1][0], r[1][1], r[1][2], ty, indent + 2, false);
        sb.append(",\n");
        appendMatrixRow(sb, r[2][0], r[2][1], r[2][2], tz, indent + 2, false);
        sb.append(",\n");
        appendMatrixRow(sb, 0.0, 0.0, 0.0, 1.0, indent + 2, true);
        sb.append("\n");
        sb.append(pad1).append("],\n");

        // aabb
        BoundingBox aabb = model.getTransformedAABB();
        sb.append(pad1).append("\"aabb\": {\n");
        appendVec3(sb, "min", aabb.getMin(), indent + 2);
        sb.append(",\n");
        appendVec3(sb, "max", aabb.getMax(), indent + 2);
        sb.append(",\n");
        sb.append(indent(indent + 2)).append("\"volume\": ")
            .append(String.format(java.util.Locale.ROOT, "%.2f", aabb.getVolume()))
            .append("\n");
        sb.append(pad1).append("}\n");

        sb.append(pad).append("}");
    }

    private static void appendVec3(StringBuilder sb, String name, Vector3D v, int indent) {
        String pad = indent(indent);
        sb.append(pad).append("\"").append(name).append("\": [")
            .append(String.format(java.util.Locale.ROOT, "%.3f", (double) v.x)).append(", ")
            .append(String.format(java.util.Locale.ROOT, "%.3f", (double) v.y)).append(", ")
            .append(String.format(java.util.Locale.ROOT, "%.3f", (double) v.z))
            .append("]");
    }

    private static void appendMatrixRow(StringBuilder sb,
                                        double a, double b, double c, double d,
                                        int indent, boolean isLast) {
        String pad = indent(indent);
        sb.append(pad).append("[")
            .append(String.format(java.util.Locale.ROOT, "%.6f", a)).append(", ")
            .append(String.format(java.util.Locale.ROOT, "%.6f", b)).append(", ")
            .append(String.format(java.util.Locale.ROOT, "%.6f", c)).append(", ")
            .append(String.format(java.util.Locale.ROOT, "%.6f", d))
            .append("]");
    }

    private static String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    /**
     * 转义 JSON 字符串中的特殊字符。
     */
    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }
}
