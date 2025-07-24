package com.example.media_vault.configuration

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {
    @Bean
    fun imageQueue(): Queue {
        return Queue("imageQueue", false)
    }

    @Bean
    fun fileExchange(): TopicExchange {
        return TopicExchange("fileExchange")
    }

    @Bean
    fun binding(imageQueue: Queue, fileExchange: TopicExchange): Binding {
        return BindingBuilder.bind(imageQueue).to(fileExchange).with("images")
    }
}