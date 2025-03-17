package com.example.configuration.service;

import com.example.configuration.dao.entity.FieldMapping;
import com.example.configuration.dao.entity.Product;

import java.util.Map;

public class ReflectionMappingStrategy implements MappingStrategy {
    @Override
    public void mapFields(Product product, Map<String, Object> data, FieldMapping fieldMapping) {
        String sourceField = fieldMapping.getSource();
        String targetField = fieldMapping.getTarget();

        if (data.containsKey(sourceField)) {
            Object value = data.get(sourceField);
            ReflectionUtil.setFieldValue(product, targetField, value);
        }
    }
}