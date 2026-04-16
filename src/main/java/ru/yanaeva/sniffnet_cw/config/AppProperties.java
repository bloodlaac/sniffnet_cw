package ru.yanaeva.sniffnet_cw.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Storage storage = new Storage();
    private final Integration integration = new Integration();

    public Jwt getJwt() {
        return jwt;
    }

    public Storage getStorage() {
        return storage;
    }

    public Integration getIntegration() {
        return integration;
    }

    public static class Jwt {
        @NotBlank
        private String secret;

        private Duration expiration = Duration.ofHours(12);
        private Duration refreshExpiration = Duration.ofDays(7);

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getExpiration() {
            return expiration;
        }

        public void setExpiration(Duration expiration) {
            this.expiration = expiration;
        }

        public Duration getRefreshExpiration() {
            return refreshExpiration;
        }

        public void setRefreshExpiration(Duration refreshExpiration) {
            this.refreshExpiration = refreshExpiration;
        }
    }

    public static class Storage {
        @NotBlank
        private String imageDirectory;

        @Min(1)
        private long maxImageSizeBytes = 5 * 1024 * 1024;

        public String getImageDirectory() {
            return imageDirectory;
        }

        public void setImageDirectory(String imageDirectory) {
            this.imageDirectory = imageDirectory;
        }

        public long getMaxImageSizeBytes() {
            return maxImageSizeBytes;
        }

        public void setMaxImageSizeBytes(long maxImageSizeBytes) {
            this.maxImageSizeBytes = maxImageSizeBytes;
        }
    }

    public static class Integration {
        @NotBlank
        private String trainingServiceUrl;
        @NotBlank
        private String classificationServiceUrl;
        private Duration timeout = Duration.ofSeconds(30);

        public String getTrainingServiceUrl() {
            return trainingServiceUrl;
        }

        public void setTrainingServiceUrl(String trainingServiceUrl) {
            this.trainingServiceUrl = trainingServiceUrl;
        }

        public String getClassificationServiceUrl() {
            return classificationServiceUrl;
        }

        public void setClassificationServiceUrl(String classificationServiceUrl) {
            this.classificationServiceUrl = classificationServiceUrl;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
}
