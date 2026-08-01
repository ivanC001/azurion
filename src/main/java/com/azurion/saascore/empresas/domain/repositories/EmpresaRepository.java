package com.azurion.saascore.empresas.domain.repositories;

import com.azurion.saascore.empresas.domain.entities.Empresa;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByTenantId(String tenantId);
    Optional<Empresa> findByTenantIdIgnoreCase(String tenantId);
    Optional<Empresa> findByRuc(String ruc);
    Optional<Empresa> findByRucIgnoreCase(String ruc);
    Optional<Empresa> findBySchemaName(String schemaName);
    List<Empresa> findAllByOrderByRazonSocialAsc();
    List<Empresa> findByActivoTrueOrderByRazonSocialAsc();

    List<Empresa> findTop20ByFacturadorStatusInAndFacturadorNextAttemptAtLessThanEqualOrderByIdAsc(
            List<String> statuses,
            OffsetDateTime now
    );

    Optional<Empresa> findByIdAndFacturadorStatusAndFacturadorLeaseOwner(
            Long id,
            String status,
            String leaseOwner
    );

    @Modifying
    @Transactional
    @Query("""
            update Empresa empresa
               set empresa.facturadorStatus = 'PROVISIONANDO',
                   empresa.facturadorAttempts = empresa.facturadorAttempts + 1,
                   empresa.facturadorLeaseOwner = :owner,
                   empresa.facturadorLeaseUntil = :leaseUntil,
                   empresa.updatedAt = :updatedAt,
                   empresa.version = empresa.version + 1
             where empresa.id = :id
               and empresa.facturadorStatus in ('PENDIENTE', 'REINTENTO')
               and empresa.facturadorNextAttemptAt <= :now
            """)
    int claimFacturadorProvisioning(@Param("id") Long id,
                                    @Param("owner") String owner,
                                    @Param("now") OffsetDateTime now,
                                    @Param("leaseUntil") OffsetDateTime leaseUntil,
                                    @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
            update Empresa empresa
               set empresa.facturadorStatus = 'REINTENTO',
                   empresa.facturadorNextAttemptAt = :now,
                   empresa.facturadorLastError = 'Tarea recuperada despues de una interrupcion',
                   empresa.facturadorLeaseOwner = null,
                   empresa.facturadorLeaseUntil = null,
                   empresa.updatedAt = :updatedAt,
                   empresa.version = empresa.version + 1
             where empresa.facturadorStatus = 'PROVISIONANDO'
               and empresa.facturadorLeaseUntil < :now
            """)
    int recoverExpiredFacturadorLeases(@Param("now") OffsetDateTime now,
                                       @Param("updatedAt") LocalDateTime updatedAt);
}
