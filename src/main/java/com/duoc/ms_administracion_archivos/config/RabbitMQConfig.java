package com.duoc.ms_administracion_archivos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de colas RabbitMQ (Semana 8).
 *
 * Define:
 *  - Cola 1 "guias.queue"        -> flujo normal (guía procesada con éxito)
 *  - Cola 2 "guias.error.queue"  -> flujo de error (si falla el envío/procesamiento)
 *  - Exchange tipo Direct "guias.exchange"
 *  - Un binding por cada cola, cada uno con su propia routing key
 *  - Conversor de mensajes a JSON, para enviar/recibir objetos Java (no solo texto)
 */
@Configuration
public class RabbitMQConfig {

    // Nombres de colas
    public static final String QUEUE_GUIAS = "guias.queue";
    public static final String QUEUE_GUIAS_ERROR = "guias.error.queue";

    // Exchange
    public static final String EXCHANGE_GUIAS = "guias.exchange";

    // Routing keys
    public static final String ROUTING_KEY_GUIAS = "guias.routingkey";
    public static final String ROUTING_KEY_GUIAS_ERROR = "guias.error.routingkey";

    // ── Colas ────────────────────────────────────────────────────────────────

    @Bean
    public Queue guiasQueue() {
        // durable = true -> la cola persiste aunque RabbitMQ se reinicie
        return new Queue(QUEUE_GUIAS, true);
    }

    @Bean
    public Queue guiasErrorQueue() {
        return new Queue(QUEUE_GUIAS_ERROR, true);
    }

    // ── Exchange ─────────────────────────────────────────────────────────────

    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(EXCHANGE_GUIAS);
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    @Bean
    public Binding bindingGuias(Queue guiasQueue, DirectExchange guiasExchange) {
        return BindingBuilder.bind(guiasQueue).to(guiasExchange).with(ROUTING_KEY_GUIAS);
    }

    @Bean
    public Binding bindingGuiasError(Queue guiasErrorQueue, DirectExchange guiasExchange) {
        return BindingBuilder.bind(guiasErrorQueue).to(guiasExchange).with(ROUTING_KEY_GUIAS_ERROR);
    }

    // ── Conversor JSON ───────────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}