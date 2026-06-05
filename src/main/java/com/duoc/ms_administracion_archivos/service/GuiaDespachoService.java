package com.duoc.ms_administracion_archivos.service;

import com.duoc.ms_administracion_archivos.dto.GuiaDespachoRequest;
import com.duoc.ms_administracion_archivos.model.GuiaDespacho;
import com.duoc.ms_administracion_archivos.repository.GuiaDespachoRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de negocio para gestión de guías de despacho (CRUD en H2).
 *
 * Al actualizar una guía, si el transportista o la fecha cambian y el archivo
 * ya fue subido a S3, se mueve automáticamente a la nueva key para mantener
 * la organización {fecha}/{transportista}/guia{id}.txt sincronizada.
 */
@Service
public class GuiaDespachoService {

    private final GuiaDespachoRepository repository;
    private final S3Service s3Service;

    // @Lazy rompe la dependencia circular GuiaDespachoService ↔ S3Service
    public GuiaDespachoService(GuiaDespachoRepository repository,
                                @Lazy S3Service s3Service) {
        this.repository = repository;
        this.s3Service  = s3Service;
    }

    public List<GuiaDespacho> listarTodas() {
        return repository.findAll();
    }

    public GuiaDespacho obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                    "No existe una guía con ID: " + id));
    }

    public GuiaDespacho crear(GuiaDespachoRequest req) {
        GuiaDespacho guia = new GuiaDespacho(
            req.getNumeroGuia(),
            req.getTransportista(),
            req.getDestinatario(),
            req.getDireccionDestino(),
            req.getDescripcionCarga(),
            req.getPesoCarga(),
            req.getFechaDespacho() != null ? req.getFechaDespacho() : LocalDate.now(),
            req.getEstado() != null ? req.getEstado() : "PENDIENTE"
        );
        return repository.save(guia);
    }

    /**
     * Actualiza los datos de la guía en BD y, si el transportista o la fecha
     * cambiaron, mueve automáticamente el archivo en S3 a la nueva key.
     *
     * Flujo:
     *  1. Calcular la key S3 ANTES del cambio (keyAntigua)
     *  2. Aplicar los cambios en el objeto
     *  3. Calcular la key DESPUÉS del cambio (keyNueva)
     *  4. Guardar en BD
     *  5. Si la key cambió y el archivo existe en S3 → mover a la nueva key
     *
     * @return GuiaActualizadaResponse con la guía actualizada y un mensaje
     *         indicando si se movió el archivo en S3.
     */
    public GuiaActualizadaResponse actualizar(Long id, GuiaDespachoRequest req) {
        GuiaDespacho guia = obtenerPorId(id);

        // Paso 1: key actual en S3 (antes del cambio)
        String keyAntigua = s3Service.buildS3Key(guia);

        // Paso 2: aplicar cambios
        if (req.getNumeroGuia()       != null) guia.setNumeroGuia(req.getNumeroGuia());
        if (req.getTransportista()    != null) guia.setTransportista(req.getTransportista());
        if (req.getDestinatario()     != null) guia.setDestinatario(req.getDestinatario());
        if (req.getDireccionDestino() != null) guia.setDireccionDestino(req.getDireccionDestino());
        if (req.getDescripcionCarga() != null) guia.setDescripcionCarga(req.getDescripcionCarga());
        if (req.getPesoCarga()        != null) guia.setPesoCarga(req.getPesoCarga());
        if (req.getFechaDespacho()    != null) guia.setFechaDespacho(req.getFechaDespacho());
        if (req.getEstado()           != null) guia.setEstado(req.getEstado());

        // Paso 3: key nueva (después del cambio)
        String keyNueva = s3Service.buildS3Key(guia);

        // Paso 4: guardar en BD
        GuiaDespacho guardada = repository.save(guia);

        // Paso 5: si la key cambió y el archivo existe en S3, moverlo
        String mensajeS3 = null;
        if (!keyAntigua.equals(keyNueva) && s3Service.existeEnS3(keyAntigua)) {
            mensajeS3 = s3Service.moverArchivoEnS3(keyAntigua, keyNueva);
        }

        return new GuiaActualizadaResponse(guardada, mensajeS3);
    }

    public void eliminar(Long id) {
        obtenerPorId(id); // valida que exista
        repository.deleteById(id);
    }

    public List<GuiaDespacho> buscarPorTransportista(String transportista) {
        return repository.findByTransportista(transportista);
    }

    public List<GuiaDespacho> buscarPorFecha(LocalDate fecha) {
        return repository.findByFechaDespacho(fecha);
    }

    public List<GuiaDespacho> buscarPorTransportistaYFecha(String transportista, LocalDate fecha) {
        return repository.findByTransportistaAndFechaDespacho(transportista, fecha);
    }

    // ── DTO interno para la respuesta de actualizar ────────────────────────────

    /**
     * Wrappea la guía actualizada con un mensaje opcional sobre el movimiento en S3.
     */
    public record GuiaActualizadaResponse(
            GuiaDespacho guia,
            String mensajeS3
    ) {}
}
