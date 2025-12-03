package nl.wilcokas.luckystackworker;

import static nl.wilcokas.luckystackworker.constants.Constants.VERSION_REQUEST_TIMEOUT;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class LswConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return createObjectMapper();
    }

    @Bean
    public ObjectMapper snakeCaseObjectMapper() {
        return createSnakeCaseObjectMapper();
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestFactory(getClientHttpRequestFactory())
                .build();
    }

    public static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public static ObjectMapper createSnakeCaseObjectMapper() {
        final ObjectMapper mapper = createObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }

    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(VERSION_REQUEST_TIMEOUT);
        clientHttpRequestFactory.setConnectionRequestTimeout(VERSION_REQUEST_TIMEOUT);
        return clientHttpRequestFactory;
    }
}
