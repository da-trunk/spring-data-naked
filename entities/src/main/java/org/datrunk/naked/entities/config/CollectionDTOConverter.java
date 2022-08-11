package org.datrunk.naked.entities.config;

import java.io.IOException;

import org.datrunk.naked.entities.CollectionDTO;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CollectionDTOConverter implements Converter<String, CollectionDTO<?>> {
    private final ObjectMapper objectMapper;

    public CollectionDTOConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public CollectionDTO<?> convert(String json) {
        try {
            return objectMapper.readValue(json, CollectionDTO.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
