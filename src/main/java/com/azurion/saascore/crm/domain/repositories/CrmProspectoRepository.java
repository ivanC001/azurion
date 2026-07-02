package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmProspectoRepository extends JpaRepository<CrmProspecto, Long> {

    List<CrmProspecto> findAllByOrderByIdDesc();

    List<CrmProspecto> findByResponsableIdOrderByIdDesc(String responsableId);

    long countByEstado(String estado);

    long countByResponsableIdAndEstado(String responsableId, String estado);

    long countByCanalIngreso(String canalIngreso);

    long countByCanalIngresoNot(String canalIngreso);
}
