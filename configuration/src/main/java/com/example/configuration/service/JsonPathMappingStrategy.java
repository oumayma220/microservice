package com.example.configuration.service;

import com.example.configuration.dao.entity.FieldMapping;
import com.example.configuration.dao.entity.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;


    public class JsonPathMappingStrategy implements MappingStrategy {
        private static final Logger logger = LoggerFactory.getLogger(JsonPathMappingStrategy.class);
        private static final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void mapFields(Product product, Map<String, Object> data, FieldMapping fieldMapping) {
            String source = fieldMapping.getSource();
            String target = fieldMapping.getTarget();

            try {
                // Convert the source pattern for direct property access if needed
                String effectiveSource = source;
              //  if (source.startsWith("$.[*].")) {
              //      effectiveSource = "$." + source.substring(5);
              //  }

                // Try direct access from the map if JsonPath fails
                Object value;
                try {
                    // First try with JsonPath
                    String json = objectMapper.writeValueAsString(data);
                    value = JsonPath.read(json, effectiveSource);

                    // Handle array result
                    if (value instanceof JSONArray) {
                        JSONArray array = (JSONArray) value;
                        if (!array.isEmpty()) {
                            value = array.get(0);
                        } else {
                            value = null;
                        }
                    }
                } catch (Exception e) {
                    // Fallback: try direct property access if JsonPath fails
                    logger.debug("JsonPath failed, trying direct property access for: {}", source);
                    String propertyName = source.substring(source.lastIndexOf('.') + 1);
                    if (propertyName.endsWith("]")) {
                        propertyName = propertyName.substring(0, propertyName.indexOf('['));
                    }
                    value = data.get(propertyName);
                }

                // Set the value if not null
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
    }