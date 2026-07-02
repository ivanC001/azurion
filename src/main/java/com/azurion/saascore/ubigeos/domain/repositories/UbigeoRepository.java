package com.azurion.saascore.ubigeos.domain.repositories;

import com.azurion.saascore.ubigeos.domain.entities.Ubigeo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UbigeoRepository extends JpaRepository<Ubigeo, Long> {

    Optional<Ubigeo> findByCodigo(String codigo);

    @Query("""
            SELECT u
            FROM Ubigeo u
            WHERE :query IS NULL
               OR LOWER(u.codigo) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.departamento) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.provincia) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.distrito) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY u.departamento ASC, u.provincia ASC, u.distrito ASC
            """)
    List<Ubigeo> search(@Param("query") String query, Pageable pageable);
}
