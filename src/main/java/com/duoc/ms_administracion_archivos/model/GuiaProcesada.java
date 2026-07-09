package com.duoc.ms_administracion_archivos.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad que registra las guías que pasaron por el flujo asíncrono de
 * RabbitMQ (Semana 8). Es una tabla NUEVA, distinta de "guias_despacho",
 * que actúa como bitácora/confirmación de procesamiento vía cola.
 */
@Entity
@Table(name = "guias_procesadas")
public class GuiaProcesada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long guiaId; // referencia al ID de GuiaDespacho original

    @Column(nullable = false)
    private String numeroGuia;

    @Column(nullable = false)
    private String transportista;

    @Column(nullable = false)
    private LocalDate fechaDespacho;

    @Column(nullable = false)
    private LocalDateTime fechaProcesamiento;

    public GuiaProcesada() {}

    public GuiaProcesada(Long guiaId, String numeroGuia, String transportista, LocalDate fechaDespacho) {
        this.guiaId             = guiaId;
        this.numeroGuia         = numeroGuia;
        this.transportista      = transportista;
        this.fechaDespacho      = fechaDespacho;
        this.fechaProcesamiento = LocalDateTime.now();
    }

    // ── Getters y Setters ──────────────────────────────────────────────────

    public Long getId() { 
        return id; 
    }

    public void setId(Long id) { 
        this.id = id; 
    }

    public Long getGuiaId() { 
        return guiaId; 
    }

    public void setGuiaId(Long guiaId) { 
        this.guiaId = guiaId; 
    }

    public String getNumeroGuia() { 
        return numeroGuia; 
    }

    public void setNumeroGuia(String numeroGuia)  { 
        this.numeroGuia = numeroGuia; 
    }

    public String getTransportista() { 
        return transportista; 
    }

    public void setTransportista(String transportista)   { 
        this.transportista = transportista; 
    }

    public LocalDate getFechaDespacho()  { 
        return fechaDespacho; 
    }

    public void setFechaDespacho(LocalDate fechaDespacho) { 
        this.fechaDespacho = fechaDespacho; 
    }

    public LocalDateTime getFechaProcesamiento()  { 
        return fechaProcesamiento; 
    }

    public void setFechaProcesamiento(LocalDateTime f)  { 
        this.fechaProcesamiento = f; 
    }
}