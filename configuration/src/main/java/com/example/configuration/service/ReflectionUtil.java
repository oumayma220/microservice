package com.example.configuration.service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class ReflectionUtil {

    public static void setFieldValue(Object object, String fieldName, Object value) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);

            Object convertedValue = convertValue(field.getType(), value);

            ReflectionUtils.setField(field, object, convertedValue);        } catch (NoSuchFieldException  e) {
            throw new RuntimeException("Erreur lors de l'affectation du champ : " + fieldName, e);
        }
    }
    private static Object convertValue(Class<?> fieldType, Object value) {
        if (value == null) {
            return null;
        }

        String stringValue = value.toString();

        if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
            return Double.valueOf(stringValue);
        } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
            return Integer.valueOf(stringValue);
        } else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
            return Float.valueOf(stringValue);
        } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
            return Long.valueOf(stringValue);
        } else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
            return Boolean.valueOf(stringValue);
        } else if (fieldType.equals(String.class)) {
            return stringValue;
        }

        // add more types as needed
        return value; // fallback
    }

}
