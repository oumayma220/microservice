package com.example.configuration.service;

import com.example.configuration.dao.entity.FieldMapping;
import com.example.configuration.dao.entity.Product;

import java.util.Map;

public interface MappingStrategy {
    void mapFields(Product product, Map<String, Object> data, FieldMapping fieldMapping);
}
