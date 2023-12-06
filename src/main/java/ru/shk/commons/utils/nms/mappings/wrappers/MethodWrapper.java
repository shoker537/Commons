package ru.shk.commons.utils.nms.mappings.wrappers;

import lombok.SneakyThrows;
import ru.shk.commons.utils.nms.ReflectionUtil;

public record MethodWrapper(String name) {
    @SneakyThrows
    public void run(Object o, Object... args) {
        ReflectionUtil.runMethodAutoDefineTypes(o.getClass(), o, name, args);
    }
    @SneakyThrows
    public void run(Class<?> c, Object o, Object... args) {
        ReflectionUtil.runMethodAutoDefineTypes(c, o, name, args);
    }
}
