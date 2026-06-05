package com.duoc.ms_administracion_archivos.dto;

import java.time.LocalDate;

/**
 * DTO para crear o actualizar una GuíaDespacho.
 * Separa la capa de presentación del modelo de dominio.
 */
public class GuiaDespachoRequest {

    private String numeroGuia;
    private String transportista;
    private String destinatario;
    private String direccionDestino;
    private String descripcionCarga;
    private Double pesoCarga;
    private LocalDate fechaDespacho;
    private String estado;

    public GuiaDespachoRequest() {}

    // ── Getters y Setters ──────────────────────────────────────────────────────

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
