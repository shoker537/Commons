package ru.shk.commons.utils.nms.mappings.wrappers;

import lombok.SneakyThrows;
import ru.shk.commons.utils.nms.ReflectionUtil;

public record FieldWrapper(String name) {
    @SneakyThrows
    public <T> T get(Object o) {
        return (T) ReflectionUtil.getField(o, name);
    }
    @SneakyThrows
    public <T> T getStatic(Class<?> c) {
        return (T) ReflectionUtil.getStaticField(c, name);
    }

    @SneakyThrows
    public void set(Object o, Object value) {
        ReflectionUtil.setField(o, name, value);
    }
    @SneakyThrows
    public void setStatic(Class<?> c, Object value) {
        ReflectionUtil.setStaticField(c, name, value);
    }
}