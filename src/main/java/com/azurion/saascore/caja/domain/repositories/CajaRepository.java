package com.azurion.saascore.caja.domain.repositories;

import com.azurion.saascore.caja.domain.entities.Caja;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    @EntityGraph(attributePaths = "sucursal")
    Optional<Caja> findFirstBySucursalIdAndCodigoIgnoreCaseAndEstado(Long sucursalId, String codigo, String estado);

    @EntityGraph(attributePaths = "sucursal")
    Optional<Caja> findFirstByResponsableAperturaIdAndEstadoOrderByFechaAperturaDesc(String responsableAperturaId, String estado);

    @EntityGraph(attributePaths = "sucursal")
    List<Caja> findByEstadoOrderByFechaAperturaDesc(String estado);

    @EntityGraph(attributePaths = "sucursal")
    List<Caja> findAllByOrderByFechaAperturaDesc();

    @EntityGraph(attributePaths = "sucursal")
    List<Caja> findBySucursalIdOrderByFechaAperturaDesc(Long sucursalId);

    @EntityGraph(attributePaths = "sucursal")
    List<Caja> findBySucursalIdAndEstadoOrderByFechaAperturaDesc(Long sucursalId, String estado);

    @Override
    @EntityGraph(attributePaths = "sucursal")
    Optional<Caja> findById(Long id);
}
