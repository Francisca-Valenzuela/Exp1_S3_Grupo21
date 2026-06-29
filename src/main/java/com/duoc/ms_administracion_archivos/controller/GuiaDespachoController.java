package com.duoc.ms_administracion_archivos.controller;

import com.duoc.ms_administracion_archivos.dto.GuiaDespachoRequest;
import com.duoc.ms_administracion_archivos.model.GuiaDespacho;
import com.duoc.ms_administracion_archivos.service.GuiaDespachoService;
import com.duoc.ms_administracion_archivos.service.S3Service;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST que expone todos los endpoints requeridos por la actividad S3:
 *
 * CRUD de guías (en H2):
 *   POST   /guias                          → crear guía
 *   GET    /guias                          → listar todas
 *   GET    /guias/{id}                     → obtener por ID
 *   PUT    /guias/{id}                     → actualizar guía
 *   DELETE /guias/{id}                     → eliminar guía
 *
 * Operaciones S3 / EFS (rúbrica):
 *   POST   /guias/{id}/subir               → guardar en EFS + subir a S3
 *   GET    /guias/{id}/descargar           → descargar desde S3
 *   PUT    /guias/{id}/actualizar-archivo  → reemplazar archivo en S3
 *   DELETE /guias/{id}/eliminar-archivo    → eliminar archivo de S3
 *
 * Consulta / historial (rúbrica criterio 5):
 *   GET    /guias/historial                → listar todo el bucket
 *   GET    /guias/historial?fecha=yyyyMMdd → filtrar por fecha
 *   GET    /guias/historial?transportista= → filtrar por transportista
 *   GET    /guias/historial?fecha=&transportista= → filtro combinado
 *   GET    /guias/consulta?transportista=&fecha=yyyy-MM-dd → consulta en BD
 */
@RestController
@RequestMapping("/guias")
public class GuiaDespachoController {

    private final GuiaDespachoService guiaService;
    private final S3Service s3Service;

    public GuiaDespachoController(GuiaDespachoService guiaService,
                                  S3Service s3Service) {
        this.guiaService = guiaService;
        this.s3Service   = s3Service;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CRUD de guías (en base de datos)
    // ════════════════════════════════════════════════════════════════════════════

    /** Crea una nueva guía de despacho. */
    @PreAuthorize("hasRole('GESTION')")
    @PostMapping
    public ResponseEntity<?> crearGuia(@RequestBody GuiaDespachoRequest request) {
        try {
            GuiaDespacho guia = guiaService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(guia);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear la guía: " + e.getMessage());
        }
    }

    /** Lista todas las guías de despacho registradas. */
    @PreAuthorize("hasRole('GESTION')")
    @GetMapping
    public ResponseEntity<List<GuiaDespacho>> listarGuias() {
        return ResponseEntity.ok(guiaService.listarTodas());
    }

    /** Obtiene una guía por su ID. */
    @PreAuthorize("hasRole('GESTION')")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerGuia(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(guiaService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza los datos de una guía en la base de datos.
     * Si el transportista o la fecha cambiaron y el archivo ya existía en S3,
     * lo mueve automáticamente a la nueva key para mantener la organización sincronizada.
     */
    @PreAuthorize("hasRole('GESTION')")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarGuia(@PathVariable Long id,
                                            @RequestBody GuiaDespachoRequest request) {
        try {
            GuiaDespachoService.GuiaActualizadaResponse response =
                    guiaService.actualizar(id, request);

            // Construir respuesta enriquecida con info del movimiento S3 (si aplica)
            var body = new java.util.LinkedHashMap<String, Object>();
            body.put("guia", response.guia());
            if (response.mensajeS3() != null) {
                body.put("archivoS3Movido", true);
                body.put("mensajeS3", response.mensajeS3());
            } else {
                body.put("archivoS3Movido", false);
                body.put("mensajeS3", "No había archivo en S3 o la key no cambió.");
            }
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar: " + e.getMessage());
        }
    }

    /** Elimina una guía de la base de datos. */
    @PreAuthorize("hasRole('GESTION')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarGuia(@PathVariable Long id) {
        try {
            guiaService.eliminar(id);
            return ResponseEntity.ok("Guía " + id + " eliminada de la base de datos.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Operaciones S3 / EFS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Guarda la guía temporalmente en EFS y luego la sube a S3.
     * Key en S3: {yyyyMMdd}/{transportista}/guia{id}.txt
     * Criterios 1 y 2 de la rúbrica.
     */
    @PreAuthorize("hasRole('GESTION')")
    @PostMapping("/{id}/subir")
    public ResponseEntity<?> subirGuia(@PathVariable Long id) {
        try {
            String mensaje = s3Service.subirGuia(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(mensaje);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (S3Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Descarga el archivo de la guía desde S3.
     * Criterio 4 de la rúbrica.
     * Único endpoint accesible con el rol "DESCARGA" (rol exclusivo del caso).
     */
    @PreAuthorize("hasRole('DESCARGA') or hasRole('GESTION')")
    @GetMapping("/{id}/descargar")
    public ResponseEntity<?> descargarGuia(@PathVariable Long id) {
        try {
            byte[] bytes   = s3Service.descargarGuia(id);
            String filename = "guia_despacho_" + id + ".txt";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
            headers.setContentLength(bytes.length);

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (S3Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Actualiza/reemplaza el archivo de la guía en S3 con un MultipartFile.
     * Criterio 3 de la rúbrica.
     */
    @PreAuthorize("hasRole('GESTION')")
    @PutMapping("/{id}/actualizar-archivo")
    public ResponseEntity<?> actualizarArchivoS3(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            if (archivo.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("El archivo enviado está vacío.");
            }
            String mensaje = s3Service.actualizarGuia(id, archivo);
            return ResponseEntity.ok(mensaje);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No se pudo leer el archivo: " + e.getMessage());
        } catch (S3Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Elimina el archivo de la guía de S3.
     * Criterio 3 (eliminación) de la rúbrica.
     */
    @PreAuthorize("hasRole('GESTION')")
    @DeleteMapping("/{id}/eliminar-archivo")
    public ResponseEntity<?> eliminarArchivoS3(@PathVariable Long id) {
        try {
            String mensaje = s3Service.eliminarGuia(id);
            return ResponseEntity.ok(mensaje);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (S3Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Consulta / historial (criterio 5 de la rúbrica)
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Lista el historial de archivos en S3.
     * Acepta filtros opcionales:
     *   ?fecha=yyyyMMdd
     *   ?transportista=NombreTransportista
     *   ?fecha=yyyyMMdd&transportista=NombreTransportista
     */
    @PreAuthorize("hasRole('GESTION') or hasRole('DESCARGA')")
    @GetMapping("/historial")
    public ResponseEntity<?> listarHistorial(
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false) String transportista) {
        try {
            List<String> archivos;

            if (fecha != null && transportista != null) {
                archivos = s3Service.listarPorFechaYTransportista(fecha, transportista);
            } else if (fecha != null) {
                archivos = s3Service.listarPorFecha(fecha);
            } else if (transportista != null) {
                archivos = s3Service.listarPorTransportista(transportista);
            } else {
                archivos = s3Service.listarTodoElBucket();
            }

            return ResponseEntity.ok(archivos);
        } catch (S3Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado: " + e.getMessage());
        }
    }

    /**
     * Consulta guías en la base de datos (H2) por transportista y/o fecha.
     * Permite demostrar los filtros usando datos de BD.
     *   ?transportista=TransportistaX
     * 
     *   ?fecha=2024-03-15
     *   ?transportista=TransportistaX&fecha=2024-03-15
     */
    @PreAuthorize("hasRole('GESTION') or hasRole('DESCARGA')")
    @GetMapping("/consulta")
    public ResponseEntity<?> consultarGuias(
            @RequestParam(required = false) String transportista,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            List<GuiaDespacho> guias;

            if (transportista != null && fecha != null) {
                guias = guiaService.buscarPorTransportistaYFecha(transportista, fecha);
            } else if (transportista != null) {
                guias = guiaService.buscarPorTransportista(transportista);
            } else if (fecha != null) {
                guias = guiaService.buscarPorFecha(fecha);
            } else {
                guias = guiaService.listarTodas();
            }

            return ResponseEntity.ok(guias);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado: " + e.getMessage());
        }
    }
}