package com.traceroot.platform.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Collections;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        // Store null as null. Store empty list as "[]".
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert recommendedChecks list to database column", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        // If the DB column is null or blank, return an empty list
        // so callers do not have to handle null everywhere.
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    dbData,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert database column to recommendedChecks list", e);
        }
    }
}