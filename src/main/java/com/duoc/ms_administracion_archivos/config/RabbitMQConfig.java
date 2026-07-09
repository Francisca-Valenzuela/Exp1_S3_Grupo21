package com.duoc.ms_administracion_archivos.config;

import java.util.Map;

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

@Configuration
public class RabbitMQConfig {

    // Nombres de colas
    public static final String QUEUE_GUIAS = "guias.queue";
    public static final String QUEUE_GUIAS_ERROR = "guias.error.queue";

    // Exchanges
    public static final String EXCHANGE_GUIAS = "guias.exchange";
    public static final String DLX_EXCHANGE = "dlx-exchange"; // Exchange para errores

    // Routing keys (¡Las que causaban el error de compilación!)
    public static final String ROUTING_KEY_GUIAS = "guias.routingkey";
    public static final String ROUTING_KEY_GUIAS_ERROR = "guias.error.routingkey";

    // ── Colas ────────────────────────────────────────────────────────────────

    @Bean
    public Queue guiasQueue() {
        // Enlazamos la cola principal al DLX para que derive los errores automáticamente
        return new Queue(QUEUE_GUIAS, true, false, false, 
                Map.of("x-dead-letter-exchange", DLX_EXCHANGE,
                       "x-dead-letter-routing-key", ROUTING_KEY_GUIAS_ERROR));
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

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    // ── Bindings ─────────────────────────────────────────────────────────────

    @Bean
    public Binding bindingGuias(Queue guiasQueue, DirectExchange guiasExchange) {
        return BindingBuilder.bind(guiasQueue).to(guiasExchange).with(ROUTING_KEY_GUIAS);
    }

    @Bean
    public Binding bindingGuiasError(Queue guiasErrorQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(guiasErrorQueue).to(dlxExchange).with(ROUTING_KEY_GUIAS_ERROR);
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