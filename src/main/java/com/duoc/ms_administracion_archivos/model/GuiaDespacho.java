package com.duoc.ms_administracion_archivos.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entidad que representa una Guía de Despacho.
 * Se persiste en H2 y se usa como origen de datos para los archivos en S3/EFS.
 */
@Entity
@Table(name = "guias_despacho")
public class GuiaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String numeroGuia;

    @Column(nullable = false)
    private String transportista;

    @Column(nullable = false)
    private String destinatario;

    @Column(nullable = false)
    private String direccionDestino;

    @Column(nullable = false)
    private String descripcionCarga;

    @Column(nullable = false)
    private Double pesoCarga;

    @Column(nullable = false)
    private LocalDate fechaDespacho;

    @Column(nullable = false)
    private String estado; // PENDIENTE, EN_TRANSITO, ENTREGADO

    // ── Constructores ──────────────────────────────────────────────────────────

    public GuiaDespacho() {}

    public GuiaDespacho(String numeroGuia, String transportista, String destinatario,
                        String direccionDestino, String descripcionCarga,
                        Double pesoCarga, LocalDate fechaDespacho, String estado) {
        this.numeroGuia       = numeroGuia;
        this.transportista    = transportista;
        this.destinatario     = destinatario;
        this.direccionDestino = direccionDestino;
        this.descripcionCarga = descripcionCarga;
        this.pesoCarga        = pesoCarga;
        this.fechaDespacho    = fechaDespacho;
        this.estado           = estado;
    }

    // ── Getters y Setters ──────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getNumeroGuia()              { return numeroGuia; }
    public void setNumeroGuia(String n)        { this.numeroGuia = n; }

    public String getTransportista()           { return transportista; }
    public void setTransportista(String t)     { this.transportista = t; }

    public String getDestinatario()            { return destinatario; }
    public void setDestinatario(String d)      { this.destinatario = d; }

    public String getDireccionDestino()        { return direccionDestino; }
    public void setDireccionDestino(String d)  { this.direccionDestino = d; }

    public String getDescripcionCarga()        { return descripcionCarga; }
    public void setDescripcionCarga(String d)  { this.descripcionCarga = d; }

    public Double getPesoCarga()               { return pesoCarga; }
    public void setPesoCarga(Double p)         { this.pesoCarga = p; }

    public LocalDate getFechaDespacho()        { return fechaDespacho; }
    public void setFechaDespacho(LocalDate f)  { this.fechaDespacho = f; }

    public String getEstado()                  { return estado; }
    public void setEstado(String e)            { this.estado = e; }
}
