package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmPublicLeadSubmission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmPublicLeadSubmissionRepository extends JpaRepository<CrmPublicLeadSubmission, Long> {

    Optional<CrmPublicLeadSubmission> findByIdempotencyHash(String idempotencyHash);
}
