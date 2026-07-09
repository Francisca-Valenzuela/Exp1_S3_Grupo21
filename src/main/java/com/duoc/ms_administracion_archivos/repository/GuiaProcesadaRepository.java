package com.duoc.ms_administracion_archivos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duoc.ms_administracion_archivos.model.GuiaProcesada;

@Repository
public interface GuiaProcesadaRepository extends JpaRepository<GuiaProcesada, Long> {
}