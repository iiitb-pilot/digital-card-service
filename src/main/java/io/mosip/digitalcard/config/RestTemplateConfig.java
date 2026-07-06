package io.mosip.digitalcard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Primary
    @Bean("selfTokenRestTemplate")
    public RestTemplate selfTokenRestTemplate() {
        return new RestTemplate();
    }
}