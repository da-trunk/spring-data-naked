package org.datrunk.naked.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;

import org.apache.logging.log4j.Level;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Retries on specific responses
 *
 * If OAuth properties are included in the properties passed to the constuctor, then every request made through this client will acquire a
 * OAuth token if necessary and attach OAuth headers to the request. If OAuth properties are not provided, these headers will not be
 * attached.
 * 
 * TODO: consider making this an interface and splitting implementations into separate classes (one for PagingClient and the other for
 * StreamingClient).
 * 
 * @see ClientProperties
 *
 */
@Log4j2
public class FunctionalClient {
    protected String baseUrl;
    @Getter
    final long[] retrySleepDurations;

    public FunctionalClient(final String baseUrl, final long[] retrySleepDurations) {
        this.baseUrl = baseUrl;
        this.retrySleepDurations = retrySleepDurations;
    }

    public FunctionalClient(RestTemplate restTemplate, final ClientProperties properties) {
        this(properties.getLocation(), properties.getRetrySleepMillis());
        properties.getOAuth()
            .ifPresent(oauthProperties -> {
                addHeaders(restTemplate, properties.getOauth());
            });
    }

    protected void addHeaders(RestTemplate restTemplate, final ClientProperties.OAuth properties) {
    }

    public UriComponentsBuilder getBaseURIBuilder() {
        return UriComponentsBuilder.fromUriString(baseUrl);
    }

    /**
     * Override to determine whether we retry, given the HttpStatus associated with a server's response.
     * 
     * @param status http status from response
     * @return true if it is safe to retry on this status
     */
    protected boolean canRetry(HttpStatus status) {
        switch (status) {
        case REQUEST_TIMEOUT:
        case TOO_MANY_REQUESTS:
        case INTERNAL_SERVER_ERROR: // TODO: do we really want to retry this?
        case BAD_GATEWAY: // TODO: do we really want to retry this?
        case SERVICE_UNAVAILABLE: // This happens when the server is down or is being restarted.
        case GATEWAY_TIMEOUT:
            return true;
        default:
            return false;
        }
    }

    /**
     * Override to determine whether we retry on specific exceptions.
     * 
     * @param e throwable
     * @return true if it is safe to retry on this throwable
     */
    protected boolean canRetry(Throwable e) {
        return e instanceof ConnectException || e instanceof SocketTimeoutException;
    }

    @FunctionalInterface
    public interface ResponseEntitySupplier<P> {
        ResponseEntity<? extends P> get() throws IOException;
    }

    /**
     * @param <T> type
     * @param uri this is only used in logging statements
     * @param supplier a ResponseEntitySupplier. This should return a {@code ResponseEntity<? extends T>} each time we retry.
     * @return {@code Page<T>} the response
     * @throws IOException on a non-retryable error or after max retry attempts.
     */
    public <T> T executeWithRetry(URI uri, ResponseEntitySupplier<? extends T> supplier) throws IOException {
        int attempt = 1;
        Throwable cause = null;
        do {
            try {
                final java.time.Instant startTime = java.time.Instant.now();
                ResponseEntity<? extends T> response = supplier.get();
                log.debug("Request [{}]:[{}]:[{} ms]", uri, response.getStatusCode()
                    .value(),
                    Duration.between(startTime, java.time.Instant.now())
                        .toMillis());
                if (response.getHeaders()
                    .containsKey("Location")) {
                    log.debug("Response locations [{}]", response.getHeaders()
                        .get("Location"));
                }
                return response.getBody();
            } catch (HttpStatusCodeException e) {
                cause = e;
                // If RestTemplate throws this exception, it will include the response body. See e.getResponseBodyAsString().
                log.warn("Request [{}] failed with status [{}]: {}, {}", uri, e.getStatusCode(), e.getMessage());
                log.warn("Response: {}", e.getResponseBodyAsString());
                if (!canRetry(e.getStatusCode()) && !canRetry(e))
                    throw new IOException(e);
                log.catching(Level.INFO, e);
            } catch (ResourceAccessException wrapped) {
                cause = wrapped;
                log.warn("Request [{}] failed: {}", uri, wrapped.getMessage());
                // Unwrap the IOException that RestTemplate created
                if (wrapped.getCause() != null && wrapped.getCause() instanceof IOException) {
                    IOException e = (IOException) wrapped.getCause();
                    if (!canRetry(e))
                        throw e;
                    log.catching(Level.DEBUG, e);
                } else {
                    if (log.isDebugEnabled())
                        log.catching(Level.DEBUG, wrapped);
                    else
                        log.warn(wrapped.getMessage());
                    throw wrapped;
                }
            } catch (IOException e) {
                cause = e;
                // RestTemplate throws this if it fails to retrieve the HTTP status or body.
                log.warn("Request [{}] failed: ", uri, e.getMessage());
                if (!canRetry(e))
                    throw e;
                log.catching(Level.INFO, e);
                log.catching(Level.DEBUG, e);
            } catch (UncheckedIOException e) {
                cause = e.getCause();
                log.warn("Request [{}] failed: ", uri, e.getMessage());
                if (!canRetry(e))
                    throw e;
                log.catching(Level.INFO, e);
                log.catching(Level.DEBUG, e);
            }
            long sleepInterval = retrySleepDurations[attempt - 1];
            attempt += 1;
            try {
                log.warn("executeWithRetry: initiating attempt [{}] after sleeping for [{}] seconds", attempt, sleepInterval);
                if (sleepInterval > 0) {
                    Thread.sleep(sleepInterval * 1000);
                }
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        } while (attempt <= retrySleepDurations.length);
        throw new IOException("retries exhausted", cause);
    }
}
