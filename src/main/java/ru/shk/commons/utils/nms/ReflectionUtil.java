package ru.shk.commons.utils.nms;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.utils.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReflectionUtil {
    private static final Cache<CachedField, Field> fieldsCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(15)).build();
    private static final Cache<CachedMethod, Method> methodsCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(15)).build();
    public static Object constructObject(Class<?> c, Object... arguments) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        return ConstructorUtils.invokeConstructor(c, arguments);
    }

    public static void printAvailableMethods(Class<?> clazz){
        Logger.warning(" > Printing available methods at class "+clazz.getSimpleName());
        for (Method method : clazz.getMethods()) {
            Logger.info("   "+(method.getReturnType().getSimpleName())+" "+method.getName()+"("+ Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(", "))+")");
        }
    }
    @SneakyThrows
    public static Object runMethodAutoDefineTypes(Class<?> clazz, Object instance, String methodName, Object... args) {
        return runMethodDefiningTypes(clazz, instance, methodName, new Class[]{}, args);
    }
    @SneakyThrows
    public static Object runMethod(Class<?> clazz, Object instance, String methodName, List<Object> args) {
        return runMethodAutoDefineTypes(clazz, instance, methodName, null, args.toArray());
    }
    @SneakyThrows
    public static Object runMethod(Class<?> clazz, Object instance, String methodName, List<Class<?>> types, List<Object> args) {
        return runMethodDefiningTypes(clazz, instance, methodName, (Class<?>[]) types.toArray(), args.toArray());
    }
    @SneakyThrows
    public static Object runMethodWithSingleArgument(Class<?> clazz, Object instance, String methodName, Class<?> type, Object arg) {
        return runMethodDefiningTypes(clazz, instance, methodName, new Class[]{type}, new Object[]{arg});
    }
    @SneakyThrows
    public static Object runMethodDefiningTypes(Class<?> clazz, Object instance, String methodName, @Nullable Class<?>[] types, @NonNull Object[] args) {
        if(types!=null && types.length!=0 && types.length!=args.length) throw new IllegalArgumentException("Types array provided but size mismatches with arguments");
        Class<?>[] arr = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            if(types!=null && types.length!=0) arr[i] = types[i]; else arr[i] = args[i].getClass();
        }
        Method m = getMethodFromCache(clazz, methodName, arr, args);
        boolean saveToCache = m==null;
        try {
            m = clazz.getMethod(methodName, arr);
        } catch (NoSuchMethodException e){}
        if(m==null) {
            try {
                m = clazz.getDeclaredMethod(methodName, arr);
            } catch (NoSuchMethodException e){}
        }

        if(m==null){
            MethodUtils.getMatchingMethod(clazz, methodName, arr);
        }
        if(m==null) m = MethodUtils.getMatchingAccessibleMethod(clazz, methodName, arr);
        if(m==null) {
            printAvailableMethods(clazz);
            throw new NoSuchMethodException("Method "+methodName+"("+ Arrays.stream(args).map(o -> o.getClass().getSimpleName()).collect(Collectors.joining(", "))+") not found in "+clazz.getSimpleName()+" class");
        }
        if(saveToCache) cacheMethod(m, clazz, methodName, types, args);
        if(!Modifier.isPublic(m.getModifiers())) m.setAccessible(true);
        return m.invoke(clazz.cast(instance), args);
    }
    @SneakyThrows
    public static Object runMethod(Object c, String method, Object... arguments) {
        return runMethodAutoDefineTypes(c.getClass(), c, method, arguments);
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
        Field f = getFieldFromCache(c.getClass(), field);
        if(f==null) {
            f = c.getClass().getDeclaredField(field);
            cacheField(f, c.getClass(), field);
        }
        if (!f.canAccess(c))
            f.setAccessible(true);
        f.set(c, value);
    }

    public static Object getField(Object c, String field) throws NoSuchFieldException, IllegalAccessException {
        Field f = getFieldFromCache(c.getClass(), field);
        if(f==null) {
            f = c.getClass().getDeclaredField(field);
            cacheField(f, c.getClass(), field);
        }
        if (!f.canAccess(c))
            f.setAccessible(true);
        return f.get(c);
    }
    public static Object getStaticField(Class<?> c, String field) throws NoSuchFieldException, IllegalAccessException {
        Field f = getFieldFromCache(c, field);
        if(f==null) {
            f = c.getDeclaredField(field);
            cacheField(f, c, field);
        }
        if (!f.canAccess(null))
            f.setAccessible(true);
        return f.get(null);
    }

    public static void setStaticField(Class<?> c, String field, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field f = getFieldFromCache(c, field);
        if(f==null) {
            f = c.getDeclaredField(field);
            cacheField(f, c, field);
        }
        if (!f.canAccess(null))
            f.setAccessible(true);
        f.set(null, value);
    }

    @Nullable private static Field getFieldFromCache(Class<?> aClass, String fieldName){
        Optional<CachedField> cached = fieldsCache.asMap().keySet().stream().filter(cachedField -> cachedField.fieldName.equals(fieldName) && cachedField.getClass().equals(aClass)).findAny();
        return cached.map(fieldsCache::getIfPresent).orElse(null);
    }
    @Nullable private static Method getMethodFromCache(Class<?> aClass, String methodName, Class<?>[] types, Object[] args){
        Optional<CachedMethod> cached = methodsCache.asMap().keySet().stream().filter(cachedMethod -> cachedMethod.methodName.equals(methodName) && cachedMethod.getClass().equals(aClass) && Arrays.equals(types, cachedMethod.types) && Arrays.equals(args, cachedMethod.arguments)).findAny();
        return cached.map(methodsCache::getIfPresent).orElse(null);
    }
    private static void cacheMethod(@NonNull Method method, Class<?> aClass, String name, Class<?>[] types, Object[] args){
        methodsCache.put(new CachedMethod(aClass, name, types, args), method);
    }
    private static void cacheField(@NonNull Field field, Class<?> aClass, String name){
        fieldsCache.put(new CachedField(aClass, name), field);
    }

    private record CachedField(@NonNull Class<?> aClass, @NonNull String fieldName){
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            CachedField that = (CachedField) o;

            return new EqualsBuilder().append(aClass, that.aClass).append(fieldName, that.fieldName).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(aClass).append(fieldName).toHashCode();
        }
    }
    private record CachedMethod(@NonNull Class<?> aClass, @NonNull String methodName, @NonNull Class<?>[] types, @NonNull Object[] arguments){
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            CachedMethod that = (CachedMethod) o;

            return new EqualsBuilder().append(aClass, that.aClass).append(methodName, that.methodName).append(types, that.types).append(arguments, that.arguments).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(aClass).append(methodName).append(types).append(arguments).toHashCode();
        }
    }
}
