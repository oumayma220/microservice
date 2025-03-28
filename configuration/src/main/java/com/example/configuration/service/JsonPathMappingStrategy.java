package com.example.configuration.service;

import com.example.configuration.dao.entity.FieldMapping;
import com.example.configuration.dao.entity.Product;
import com.example.configuration.dto.FieldMappingDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


    public class JsonPathMappingStrategy implements MappingStrategy {
        private static final Logger logger = LoggerFactory.getLogger(JsonPathMappingStrategy.class);
        private static final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void mapFields(Product product, Map<String, Object> data, FieldMapping fieldMapping) {
            String source = fieldMapping.getSource();
            String target = fieldMapping.getTarget();

            try {
                String effectiveSource = source;

                Object value;
                try {
                    String json = objectMapper.writeValueAsString(data);
                    value = JsonPath.read(json, effectiveSource);

                    if (value instanceof JSONArray) {
                        JSONArray array = (JSONArray) value;
                        if (!array.isEmpty()) {
                            value = array.get(0);
                        } else {
                            value = null;
                        }
                    }
                } catch (Exception e) {
                    logger.debug("JsonPath failed, trying direct property access for: {}", source);
                    String propertyName = source.substring(source.lastIndexOf('.') + 1);
                    if (propertyName.endsWith("]")) {
                        propertyName = propertyName.substring(0, propertyName.indexOf('['));
                    }
                    value = data.get(propertyName);
                }

                if (value != null) {
                    ReflectionUtil.setFieldValue(product, target, value);
                    logger.debug("Successfully mapped '{}' to field '{}' with value: {}", source, target, value);
                } else {
                    logger.warn("No value found for source '{}' to map to target '{}'", source, target);
                }
            } catch (Exception e) {
                logger.error("Error mapping field {} using JsonPath {}: {}", target, source, e.getMessage(), e);
            }
        }

        @Override
        public void mapFieldstest(Product product, Map<String, Object> data, FieldMappingDTO fieldMappingDTO) {
            // If you need any special handling, you can override the default implementation
            MappingStrategy.super.mapFieldstest(product, data, fieldMappingDTO);
        }
    }