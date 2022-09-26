package ru.shk.commons.utils.nms;

import org.apache.commons.lang.reflect.ConstructorUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionUtil {
    public static Object constructObject(Class<?> c, Object... arguments) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        return ConstructorUtils.invokeConstructor(c, arguments);
    }

    public static Object runMethod(Object c, String method, Object... arguments) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method1 = c.getClass().getMethod(method, (Class<?>[]) Arrays.stream(arguments).map(Object::getClass).toArray());
        if(!method1.canAccess(c)) method1.setAccessible(true);
        return method1.invoke(c, arguments);
    }
    public static void setField(Object c, String field, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field f = c.getClass().getField(field);
        if(!f.canAccess(c)) f.setAccessible(true);
        f.set(c, value);
    }
}
