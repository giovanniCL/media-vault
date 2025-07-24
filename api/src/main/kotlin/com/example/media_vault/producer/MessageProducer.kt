package com.example.media_vault.producer

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class MessageProducer(private val rabbitTemplate: RabbitTemplate) {
    fun sendMessage(message: String, exchange: String, routingKey: String) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message)
    }
}