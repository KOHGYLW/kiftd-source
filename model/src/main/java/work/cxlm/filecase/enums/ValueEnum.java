package work.cxlm.filecase.enums;


import org.springframework.util.Assert;

import java.util.stream.Stream;

/**
 * 定值类型的枚举类接口
 * created 2020/10/27 14:02
 *
 * @author johnniang
 * @author Chiru
 */
public interface ValueEnum<T> {

    /**
     * 将值转化为枚举类实例
     *
     * @param enumType 枚举类类对象
     * @param value    值对象
     * @param <V>      值的泛型
     * @param <E>      枚举类的泛型
     * @return 相应的枚举类实例
     */
    static <V, E extends ValueEnum<V>> E valueToEnum(Class<E> enumType, V value) {
        Assert.notNull(enumType, "enumType 不能为 null");
        Assert.isTrue(enumType.isEnum(), "enumType 必须为枚举类");
        Assert.notNull(value, "value 不能为 null");

        return Stream.of(enumType.getEnumConstants())
                .filter(t -> t.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无法识别的值：" + value));
    }

    /**
     * 获取枚举类型实际值
     *
     * @return 枚举类值
     */
    T getValue();
}
