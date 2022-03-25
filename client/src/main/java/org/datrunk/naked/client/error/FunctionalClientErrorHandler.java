package org.datrunk.naked.client.error;

import java.io.IOException;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class FunctionalClientErrorHandler extends DefaultResponseErrorHandler {
    private final ObjectMapper objectMapper;

    public FunctionalClientErrorHandler(ObjectMapper objectMapper, RestTemplateBuilder restTemplateBuilder) {
        this.objectMapper = objectMapper;
        restTemplateBuilder.errorHandler(this);
//        restTemplate.setErrorHandler(this);
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if (response.getStatusCode()
            .is4xxClientError()
            || response.getStatusCode()
                .is5xxServerError()) {
            throw new FunctionalClientException(response, objectMapper);
        }
    }
}
