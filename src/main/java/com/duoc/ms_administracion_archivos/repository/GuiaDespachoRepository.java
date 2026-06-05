package com.duoc.ms_administracion_archivos.repository;

import com.duoc.ms_administracion_archivos.model.GuiaDespacho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long> {

    /**
     * Consulta guías por transportista (criterio 5 de la rúbrica: filtros).
     */
    List<GuiaDespacho> findByTransportista(String transportista);

    /**
     * Consulta guías por fecha de despacho.
     */
    List<GuiaDespacho> findByFechaDespacho(LocalDate fechaDespacho);

    /**
     * Consulta guías por transportista y fecha (filtro combinado requerido).
     */
    List<GuiaDespacho> findByTransportistaAndFechaDespacho(String transportista, LocalDate fechaDespacho);
}
