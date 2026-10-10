package cn.blockforge.generated.nnnnr1201forge2;

/** 配置输入先完整校验，不能把空白、非整数概率或无穷大写进文件。 */
public final class ConfigNumberInput {
    public static Number parse(String text, boolean integer) {
        String value = text.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("请填写数字");
        try {
            if (integer) return Integer.valueOf(value);
            double number = Double.parseDouble(value);
            if (!Double.isFinite(number)) throw new IllegalArgumentException("必须是有限数字");
            return Double.valueOf(number);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(integer ? "请填写整数" : "请填写有效数字", exception);
        }
    }

    private ConfigNumberInput() {}
}
