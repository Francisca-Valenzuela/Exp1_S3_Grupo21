package com.duoc.ms_administracion_archivos.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_GUIAS = "guias.queue";
    public static final String QUEUE_GUIAS_ERROR = "guias.error.queue";
    public static final String EXCHANGE_GUIAS = "guias.exchange";
    public static final String DLX_EXCHANGE = "dlx-exchange"; // Exchange para errores

    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(EXCHANGE_GUIAS);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue guiasQueue() {
        // Enlazar la cola principal al DLX[cite: 2]
        return new Queue(QUEUE_GUIAS, true, false, false, 
                Map.of("x-dead-letter-exchange", DLX_EXCHANGE,
                       "x-dead-letter-routing-key", "error.routingkey"));
    }

    @Bean
    public Queue guiasErrorQueue() {
        return new Queue(QUEUE_GUIAS_ERROR, true);
    }

    @Bean
    public Binding bindingGuias(Queue guiasQueue, DirectExchange guiasExchange) {
        return BindingBuilder.bind(guiasQueue).to(guiasExchange).with("guias.routingkey");
    }

    @Bean
    public Binding bindingGuiasError(Queue guiasErrorQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(guiasErrorQueue).to(dlxExchange).with("error.routingkey");
    }
}