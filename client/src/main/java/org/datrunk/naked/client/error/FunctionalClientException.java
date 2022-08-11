package org.datrunk.naked.client.error;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
public class FunctionalClientException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private Instant timeStamp;
    private final HttpStatus statusCode;
    private final String statusText;

    @Getter
    @RequiredArgsConstructor
    public static class DTO implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Instant timestamp;
        private final String message;

        public DTO(FunctionalClientException ex) {
            timestamp = ex.getTimeStamp();
            message = ex.getMessage();
        }

        public static DTO of(final ClientHttpResponse response, final ObjectMapper mapper) throws IOException {
            try (InputStream is = response.getBody()) {
                try {
                    return mapper.readValue(is, FunctionalClientException.DTO.class);
                } catch (JsonParseException | JsonMappingException ex) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String message = reader.lines()
                        .collect(Collectors.joining(""));
                    return new DTO(Instant.now(), message);
                }
            }
        }
    }

    public FunctionalClientException(ClientHttpResponse response, ObjectMapper mapper) throws IOException {
        super(DTO.of(response, mapper)
            .getMessage());
        final FunctionalClientException.DTO dto = DTO.of(response, mapper);
        timeStamp = dto.getTimestamp();
        statusCode = response.getStatusCode();
        statusText = response.getStatusText();
    }
}
