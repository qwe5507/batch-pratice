package com.system.batch.killbatchsystem.rabbitmq_integration;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
    private final RabbitMQProperties properties;

    @Bean
    public Queue jobRequestsQueue() {
        return QueueBuilder.durable(properties.getQueue().get("job-requests")).build();
    }

    @Bean
    public DirectExchange jobRequestsExchange() {
        return new DirectExchange(properties.getExchange().get("job-requests"));
    }

    @Bean
    public Binding jobRequestsBinding() {
        return BindingBuilder
                .bind(jobRequestsQueue())
                .to(jobRequestsExchange())
                .with(properties.getRoutingKey().get("job-requests"));
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}