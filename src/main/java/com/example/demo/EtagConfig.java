package com.example.demo;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class EtagConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ShallowEtagHeaderFilter());

        // Only apply to your API
        registration.addUrlPatterns("/items/*");

        // Ensures this filter runs LAST (important for ETag correctness)
        registration.setOrder(Integer.MAX_VALUE);

        return registration;
    }
}
