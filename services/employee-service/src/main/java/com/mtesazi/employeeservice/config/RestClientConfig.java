package com.mtesazi.employeeservice.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Load-balanced {@link RestClient.Builder}: logical service ids such as
     * {@code http://department-service} are resolved by Spring Cloud LoadBalancer, which
     * takes its instance list from the Eureka registry instead of a hard-coded host:port.
     *
     * <p>Prototype-scoped, like Spring Boot's own auto-configured builder, because builder
     * methods mutate in place: a shared singleton would leak one client's base URL and
     * timeouts into every other client built from it.
     *
     * <p>{@link RestClientCustomizer} beans are applied so that the customizations Boot's
     * builder would have contributed - most importantly observability/tracing propagation -
     * are not lost by replacing it.
     */
    @Bean
    @LoadBalanced
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    RestClient.Builder loadBalancedRestClientBuilder(ObjectProvider<RestClientCustomizer> customizers) {
        RestClient.Builder builder = RestClient.builder();
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder;
    }
}
