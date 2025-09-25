package com.system.batch.killbatchsystem.rabbitmq_integration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {
    private Map<String, String> queue;
    private Map<String, String> exchange;
    private Map<String, String> routingKey;
}
