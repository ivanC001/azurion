package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappAutoReplySchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmWhatsappAutoReplyScheduleRepository extends JpaRepository<CrmWhatsappAutoReplySchedule, Long> {
    List<CrmWhatsappAutoReplySchedule> findAllByConfig_IdOrderByDiaSemanaAsc(Long configId);
    void deleteAllByConfig_Id(Long configId);
}
