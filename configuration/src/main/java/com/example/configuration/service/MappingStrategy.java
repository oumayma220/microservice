package com.example.configuration.service;

import com.example.configuration.dao.entity.FieldMapping;
import com.example.configuration.dao.entity.Product;
import com.example.configuration.dto.FieldMappingDTO;

import java.util.Map;

public interface MappingStrategy {
    void mapFields(Product product, Map<String, Object> data, FieldMapping fieldMapping);
    default void mapFieldstest(Product product, Map<String, Object> data, FieldMappingDTO fieldMappingDTO) {
        FieldMapping fieldMapping = convertDtoToFieldMapping(fieldMappingDTO);
        mapFields(product, data, fieldMapping);
    }


    private FieldMapping convertDtoToFieldMapping(FieldMappingDTO fieldMappingDTO) {
        FieldMapping fieldMapping = new FieldMapping();
        fieldMapping.setSource(fieldMappingDTO.getSource());
        fieldMapping.setTarget(fieldMappingDTO.getTarget());
        // Add any additional mapping logic if needed
        return fieldMapping;
    }
}
