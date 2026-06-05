package com.duoc.ms_administracion_archivos.service;

import com.duoc.ms_administracion_archivos.model.GuiaDespacho;
import com.duoc.ms_administracion_archivos.repository.GuiaDespachoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio que encapsula todas las operaciones sobre AWS S3 para las guías de despacho.
 *
 * Estructura de keys en S3 (requerida por la rúbrica):
 *   /{yyyyMMdd}/{transportista}/guia{id}.txt
 *
 * Ejemplo: /20240315/TransportistaX/guia123.txt
 */
@Service
public class S3Service {

    private final S3Client s3Client;
    private final GuiaDespachoRepository guiaRepository;
    private final EfsService efsService;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    public S3Service(S3Client s3Client,
                     GuiaDespachoRepository guiaRepository,
                     EfsService efsService) {
        this.s3Client       = s3Client;
        this.guiaRepository = guiaRepository;
        this.efsService     = efsService;
    }

    // ── Key S3 ─────────────────────────────────────────────────────────────────

    /**
     * Construye la key S3 para una guía de despacho.
     * Formato requerido: /{fecha}/{transportista}/guia{id}.txt
     */
    public String buildS3Key(GuiaDespacho guia) {
        String fecha         = guia.getFechaDespacho().format(DATE_FMT);
        String transportista = guia.getTransportista()
                                   .replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return String.format("%s/%s/guia%d.txt", fecha, transportista, guia.getId());
    }

    // ── Subir a S3 ─────────────────────────────────────────────────────────────

    /**
     * Genera el contenido de la guía y lo sube a S3.
     * Antes de subir, guarda temporalmente en EFS (criterio 1 de la rúbrica).
     * Key en S3: {fecha}/{transportista}/guia{id}.txt
     */
    public String subirGuia(Long id) throws IOException {
        GuiaDespacho guia = obtenerGuia(id);

        // Paso 1: Guardar en EFS temporalmente
        efsService.guardarEnEfs(guia);

        // Paso 2: Subir a S3 desde el contenido generado
        String contenido = efsService.generarContenido(guia);
        String key       = buildS3Key(guia);
        byte[] bytes     = contenido.getBytes(StandardCharsets.UTF_8);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("text/plain; charset=utf-8")
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        return String.format(
            "Guía %d subida exitosamente a S3. Key: %s | EFS: guardada temporalmente.",
            id, key);
    }

    // ── Descargar desde S3 ─────────────────────────────────────────────────────

    /**
     * Descarga el archivo de la guía desde S3 y retorna sus bytes.
     * Lanza IllegalStateException (→ 404) si el archivo no existe.
     */
    public byte[] descargarGuia(Long id) throws IOException {
        GuiaDespacho guia = obtenerGuia(id);
        String key        = buildS3Key(guia);
        validarExistencia(key, id);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            return response.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(
                "Error al leer el archivo desde S3 para la guía " + id, e);
        }
    }

    // ── Actualizar en S3 ───────────────────────────────────────────────────────

    /**
     * Reemplaza el archivo de la guía en S3 con el contenido del MultipartFile recibido.
     * Usa la misma key para sobreescribir el objeto existente.
     */
    public String actualizarGuia(Long id, MultipartFile archivo) throws IOException {
        GuiaDespacho guia = obtenerGuia(id);
        String key        = buildS3Key(guia);

        byte[] bytes = archivo.getBytes();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("text/plain; charset=utf-8")
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        return String.format(
            "Guía %d actualizada exitosamente en S3. Key: %s", id, key);
    }

    // ── Eliminar de S3 ─────────────────────────────────────────────────────────

    /**
     * Elimina el archivo de la guía de S3.
     * Valida primero que el objeto exista; si no, lanza IllegalStateException (→ 404).
     */
    public String eliminarGuia(Long id) {
        GuiaDespacho guia = obtenerGuia(id);
        String key        = buildS3Key(guia);
        validarExistencia(key, id);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);

        return String.format(
            "Guía %d eliminada exitosamente del bucket S3.", id);
    }

    // ── Consultar historial ────────────────────────────────────────────────────

    /**
     * Lista todos los archivos existentes en el bucket S3.
     * Cumple el criterio 5 de la rúbrica: historial de archivos generados.
     */
    public List<String> listarTodoElBucket() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        return s3Client.listObjectsV2(request).contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * Lista archivos en S3 filtrados por transportista usando prefijo de carpeta.
     * Criterio 5: consulta por transportista.
     */
    public List<String> listarPorTransportista(String transportista) {
        String transportistaSanitizado = transportista
                .replaceAll("[^a-zA-Z0-9_\\-]", "_");

        // Listar todo el bucket y filtrar por el nombre del transportista en la key
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        return s3Client.listObjectsV2(request).contents().stream()
                .map(S3Object::key)
                .filter(key -> key.contains("/" + transportistaSanitizado + "/"))
                .collect(Collectors.toList());
    }

    /**
     * Lista archivos en S3 filtrados por fecha (prefijo yyyyMMdd/).
     * Criterio 5: consulta por fecha.
     */
    public List<String> listarPorFecha(String fecha) {
        // fecha en formato yyyyMMdd
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(fecha + "/")
                .build();

        return s3Client.listObjectsV2(request).contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * Lista archivos en S3 filtrados por fecha y transportista.
     * Criterio 5: consulta combinada (requerida por los endpoints REST).
     */
    public List<String> listarPorFechaYTransportista(String fecha, String transportista) {
        String transportistaSanitizado = transportista
                .replaceAll("[^a-zA-Z0-9_\\-]", "_");
        String prefijo = fecha + "/" + transportistaSanitizado + "/";

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefijo)
                .build();

        return s3Client.listObjectsV2(request).contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    // ── Mover objeto en S3 ─────────────────────────────────────────────────────

    /**
     * Verifica si un objeto existe en S3 sin lanzar excepción.
     * Usado por GuiaDespachoService para decidir si mover el archivo al actualizar.
     */
    public boolean existeEnS3(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }

    /**
     * Mueve un objeto dentro del mismo bucket: copia a la nueva key y elimina la antigua.
     * Se invoca automáticamente desde GuiaDespachoService.actualizar() cuando
     * el transportista o la fecha cambian y el archivo ya existía en S3.
     *
     * @param oldKey key S3 de origen (antes de la actualización)
     * @param newKey key S3 de destino (después de la actualización)
     * @return mensaje descriptivo del resultado
     */
    public String moverArchivoEnS3(String oldKey, String newKey) {
        // Paso 1: copiar al nuevo destino
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(oldKey)
                .destinationBucket(bucketName)
                .destinationKey(newKey)
                .build();
        s3Client.copyObject(copyRequest);

        // Paso 2: eliminar el origen
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(oldKey)
                .build());

        return String.format("Archivo movido en S3: [%s] → [%s]", oldKey, newKey);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private GuiaDespacho obtenerGuia(Long id) {
        return guiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                    "No existe una guía de despacho con ID: " + id));
    }

    private void validarExistencia(String key, Long id) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new IllegalStateException(
                    "La guía " + id + " no existe en S3. " +
                    "Suba el archivo primero con POST /guias/" + id + "/subir");
            }
            throw e;
        }
    }
}
