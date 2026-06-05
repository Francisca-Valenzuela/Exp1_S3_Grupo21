package com.duoc.ms_administracion_archivos.service;

import com.duoc.ms_administracion_archivos.model.GuiaDespacho;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

/**
 * Servicio que gestiona el almacenamiento TEMPORAL de guías en el EFS montado.
 *
 * El EFS se monta en la EC2 en /mnt/efs y se inyecta al contenedor como volumen:
 *   docker run -v /mnt/efs:/app/efs ...
 *
 * Por lo tanto, dentro del contenedor los archivos viven en /app/efs.
 */
@Service
public class EfsService {

    @Value("${efs.mount-path:/app/efs}")
    private String efsMountPath;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Guardar en EFS ─────────────────────────────────────────────────────────

    /**
     * Guarda el contenido de la guía como archivo .txt en el EFS.
     * Ruta: /app/efs/{fecha}/{transportista}/guia{id}.txt
     *
     * @return ruta absoluta del archivo guardado
     */
    public Path guardarEnEfs(GuiaDespacho guia) throws IOException {
        Path directorioGuia = resolverDirectorio(guia);
        Files.createDirectories(directorioGuia);

        Path archivo = directorioGuia.resolve("guia" + guia.getId() + ".txt");
        Files.writeString(archivo, generarContenido(guia), StandardCharsets.UTF_8);

        return archivo;
    }

    /**
     * Verifica si el archivo de una guía existe en el EFS.
     */
    public boolean existeEnEfs(GuiaDespacho guia) {
        Path archivo = resolverDirectorio(guia).resolve("guia" + guia.getId() + ".txt");
        return Files.exists(archivo);
    }

    /**
     * Lee el contenido del archivo de una guía desde el EFS.
     */
    public byte[] leerDesdeEfs(GuiaDespacho guia) throws IOException {
        Path archivo = resolverDirectorio(guia).resolve("guia" + guia.getId() + ".txt");
        if (!Files.exists(archivo)) {
            throw new IllegalStateException(
                "La guía " + guia.getId() + " no existe en el EFS. Genérela primero.");
        }
        return Files.readAllBytes(archivo);
    }

    /**
     * Elimina el archivo de la guía del EFS (si existe).
     */
    public void eliminarDeEfs(GuiaDespacho guia) throws IOException {
        Path archivo = resolverDirectorio(guia).resolve("guia" + guia.getId() + ".txt");
        Files.deleteIfExists(archivo);
    }

    // ── Generación de contenido ────────────────────────────────────────────────

    /**
     * Genera el contenido textual de la guía de despacho.
     */
    public String generarContenido(GuiaDespacho guia) {
        return """
                ================================================
                   GUÍA DE DESPACHO - SISTEMA TRANSPORTISTA
                ================================================

                N° de Guía        : %s
                ID Registro       : %d
                Transportista     : %s
                Destinatario      : %s
                Dirección Destino : %s
                Descripción Carga : %s
                Peso (kg)         : %.2f
                Fecha de Despacho : %s
                Estado            : %s

                ================================================
                """.formatted(
                guia.getNumeroGuia(),
                guia.getId(),
                guia.getTransportista(),
                guia.getDestinatario(),
                guia.getDireccionDestino(),
                guia.getDescripcionCarga(),
                guia.getPesoCarga(),
                guia.getFechaDespacho().format(DISPLAY_FMT),
                guia.getEstado()
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Construye el Path del directorio en EFS para una guía:
     * {efsMountPath}/{yyyyMMdd}/{transportista}/
     */
    public Path resolverDirectorio(GuiaDespacho guia) {
        String fecha        = guia.getFechaDespacho().format(DATE_FMT);
        String transportista = guia.getTransportista()
                                   .replaceAll("[^a-zA-Z0-9_\\-]", "_"); // sanitizar
        return Paths.get(efsMountPath, fecha, transportista);
    }
}
