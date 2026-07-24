package com.azurion.saascore.messaging.domain.repositories;

import com.azurion.saascore.messaging.domain.entities.PlatformMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformMessageRepository extends JpaRepository<PlatformMessage, Long> {

    List<PlatformMessage> findAllByOrderByPublicadoEnDescIdDesc(Pageable pageable);
}
