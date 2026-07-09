package com.duoc.ms_administracion_archivos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.ms_administracion_archivos.model.GuiaProcesada;
import com.duoc.ms_administracion_archivos.repository.GuiaProcesadaRepository;

/**
 * Controlador que expone las guías procesadas de forma asíncrona
 * vía RabbitMQ (Semana 8). La tabla "guias_procesadas" es poblada
 * automáticamente por ConsumidorService (@RabbitListener), no por
 * este controller — aquí solo se consulta el resultado.
 */
@RestController
@RequestMapping("/guias/procesadas")
public class GuiaProcesadaController {

    private final GuiaProcesadaRepository guiaProcesadaRepository;

    public GuiaProcesadaController(GuiaProcesadaRepository guiaProcesadaRepository) {
        this.guiaProcesadaRepository = guiaProcesadaRepository;
    }

    /**
     * Lista todas las guías que fueron procesadas exitosamente
     * a través de la cola "guias.queue" (Cola 1).
     */
    @PreAuthorize("hasRole('GESTION')")
    @GetMapping
    public ResponseEntity<List<GuiaProcesada>> listarProcesadas() {
        return ResponseEntity.ok(guiaProcesadaRepository.findAll());
    }
}
