package io.mosip.digitalcard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Primary;
@Configuration
public class RestTemplateConfig {

    @Bean("selfTokenRestTemplate")
    @Primary
    public RestTemplate selfTokenRestTemplate() {
        return new RestTemplate();
    }
}