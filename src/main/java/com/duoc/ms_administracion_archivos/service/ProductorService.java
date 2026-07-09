package com.duoc.ms_administracion_archivos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.duoc.ms_administracion_archivos.config.RabbitMQConfig;
import com.duoc.ms_administracion_archivos.model.GuiaDespacho;

/**
 * Productor (Semana 8): publica la guía de despacho en RabbitMQ.
 *
 * Flujo:
 *  - Si la publicación es exitosa -> mensaje va a "guias.queue" (Cola 1)
 *  - Si ocurre un error al publicar -> mensaje va a "guias.error.queue" (Cola 2)
 */
@Service
public class ProductorService {

    private static final Logger log = LoggerFactory.getLogger(ProductorService.class);

    private final RabbitTemplate rabbitTemplate;

    public ProductorService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviarGuia(GuiaDespacho guia) {
        try {
            // Validación de negocio: fuerza el flujo de error si el peso es inválido
            if (guia.getPesoCarga() == null || guia.getPesoCarga() <= 0) {
                throw new IllegalArgumentException(
                    "Peso de carga inválido: " + guia.getPesoCarga());
            }

            log.info("Publicando en '{}' la guía con ID {}",
                    RabbitMQConfig.QUEUE_GUIAS, guia.getId());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_GUIAS,
                    RabbitMQConfig.ROUTING_KEY_GUIAS,
                    guia
            );
        } catch (Exception e) {
            log.error("Error al publicar la guía {} en la cola principal, enviando a cola de error: {}",
                    guia.getId(), e.getMessage());

            enviarAColaError(guia, e.getMessage());
        }
    }

    private void enviarAColaError(GuiaDespacho guia, String motivoError) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_GUIAS,
                    RabbitMQConfig.ROUTING_KEY_GUIAS_ERROR,
                    guia
            );
            log.warn("Guía {} enviada a '{}'. Motivo: {}",
                    guia.getId(), RabbitMQConfig.QUEUE_GUIAS_ERROR, motivoError);
        } catch (Exception ex) {
            log.error("No se pudo enviar la guía {} ni siquiera a la cola de error: {}",
                    guia.getId(), ex.getMessage());
        }
    }
}