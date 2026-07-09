package com.duoc.ms_administracion_archivos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.duoc.ms_administracion_archivos.config.RabbitMQConfig;
import com.duoc.ms_administracion_archivos.model.GuiaDespacho;
import com.duoc.ms_administracion_archivos.model.GuiaProcesada;
import com.duoc.ms_administracion_archivos.repository.GuiaProcesadaRepository;

/**
 * Consumidor (Semana 8): escucha "guias.queue" y, por cada mensaje recibido,
 * guarda un registro en la tabla NUEVA "guias_procesadas".
 *
 * También escucha "guias.error.queue" solo para dejar log de las guías
 * que fallaron (no las persiste como procesadas).
 */
@Service
public class ConsumidorService {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorService.class);

    private final GuiaProcesadaRepository guiaProcesadaRepository;

    public ConsumidorService(GuiaProcesadaRepository guiaProcesadaRepository) {
        this.guiaProcesadaRepository = guiaProcesadaRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_GUIAS)
    public void recibirGuia(GuiaDespacho guia) {
        log.info("Mensaje recibido desde '{}': guía ID {}",
                RabbitMQConfig.QUEUE_GUIAS, guia.getId());

        GuiaProcesada procesada = new GuiaProcesada(
                guia.getId(),
                guia.getNumeroGuia(),
                guia.getTransportista(),
                guia.getFechaDespacho()
        );

        GuiaProcesada guardada = guiaProcesadaRepository.save(procesada);

        log.info("Guía {} registrada en 'guias_procesadas' con ID {}",
                guia.getId(), guardada.getId());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_GUIAS_ERROR)
    public void recibirGuiaConError(GuiaDespacho guia) {
        log.warn("Mensaje recibido desde '{}': la guía ID {} no pudo procesarse correctamente",
                RabbitMQConfig.QUEUE_GUIAS_ERROR, guia.getId());
        // No se persiste como procesada; queda solo como evidencia en el log
        // y visible en la consola de administración de RabbitMQ (15672).
    }
}