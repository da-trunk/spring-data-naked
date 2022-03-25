package org.datrunk.naked.client;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(of = { "location" })
@NoArgsConstructor
public abstract class ClientProperties {
    private String location;
    @Getter
    private String batchPath = "/batch";
    private List<String> retrySleepDurations = Lists.newArrayList("0");
    private OAuth oauth = null;

    @Data
    public static class OAuth {
        private String accessTokenEndpoint;
        private String key;
        private String secret;
    }

    public ClientProperties(String location) {
        this.location = location;
        this.retrySleepDurations = Lists.newArrayList("0");
    }

    public long[] getRetrySleepMillis() {
        return getRetrySleepDurations().stream()
            .mapToLong(str -> Long.valueOf(str))
            .toArray();
    }

    /**
     * This overrides the method created by {@link Data} and is identical to the one Lombok would create. It is only here for safety, since
     * a public method with this signature must exist or Spring will stop initializing this value from a {@code @ConfigurationProperties}
     * annotated sub-class.
     * 
     * @return OAuth
     */
    public OAuth getOauth() {
        return oauth;
    }

    @Nonnull
    public Optional<OAuth> getOAuth() {
        if (oauth == null || Strings.nullToEmpty(oauth.getAccessTokenEndpoint())
            .trim()
            .isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(oauth);
    }
}
