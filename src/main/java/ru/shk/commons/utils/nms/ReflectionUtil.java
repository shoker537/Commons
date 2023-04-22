package ru.shk.commons.utils.nms;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ReflectionUtil {
    public static Object constructObject(Class<?> c, Object... arguments) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        return ConstructorUtils.invokeConstructor(c, arguments);
    }

    public static Object runMethod(Object c, String method, Object... arguments) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Class<?>[] arr = new Class[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            arr[i] = arguments[i].getClass();
        }
        return MethodUtils.getMatchingAccessibleMethod(c.getClass(), method, arr).invoke(c, arguments);
//        Class<?>[] arr = new Class[arguments.length];
//        for (int i = 0; i < arguments.length; i++)
//            arr[i] = arguments[i].getClass();
//        Class<?> sup = c.getClass();
//        Method m = null;
//        while (sup != null) {
//            try {
//                m = sup.getClass().getMethod(method, arr);
//                break;
//            } catch (NoSuchMethodException noSuchMethodException) {
//                try {
//                    m = sup.getClass().getDeclaredMethod(method, arr);
//                    break;
//                } catch (NoSuchMethodException noSuchMethodException1) {
//                    for (Method supMethod : sup.getMethods()) {
//                        if (supMethod.getName().equals(method) && (
//                                supMethod.getParameterTypes()).length == arr.length) {
//                            for (int j = 0; j < arr.length &&
//                                    arr[j].equals(supMethod.getParameterTypes()[j]); j++)
//                                m = supMethod;
//                            if (m != null)
//                                break;
//                            Class<?>[] newParams = new Class[(supMethod.getParameterTypes()).length];
//                            int k;
//                            for (k = 0; k < (supMethod.getParameterTypes()).length; k++)
//                                newParams[k] = primitiveToBasicClass(supMethod.getParameterTypes()[k]);
//                            for (k = 0; k < arr.length &&
//                                    arr[k].equals(newParams[k]); k++)
//                                m = supMethod;
//                            if (m != null)
//                                break;
//                        }
//                    }
//                    if (m != null)
//                        break;
//                    sup = sup.getSuperclass();
//                }
//            }
//        }
//        if (m == null || sup == null)
//            throw new NoSuchMethodException("Checked all superclasses and no method found");
//        if (!m.canAccess(c))
//            m.setAccessible(true);
//        return m.invoke(c, arguments);
    }

    private static Class<?> primitiveToBasicClass(Class<?> primitive) {
        if (primitive.equals(int.class))
            return Integer.class;
        if (primitive.equals(float.class))
            return Float.class;
        if (primitive.equals(double.class))
            return Double.class;
        if (primitive.equals(byte.class))
            return Byte.class;
        if (primitive.equals(long.class))
            return Long.class;
        return primitive;
    }

    public static void setField(Object c, String field, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field f = c.getClass().getDeclaredField(field);
        if (!f.canAccess(c))
            f.setAccessible(true);
        f.set(c, value);
    }
}
