package com.azurion.saascore.inventory.domain.repositories;

import com.azurion.saascore.inventory.domain.entities.Producto;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    Optional<Producto> findBySku(String sku);
    Optional<Producto> findBySkuIgnoreCase(String sku);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    @EntityGraph(attributePaths = "almacen")
    List<Producto> findByAlmacenIdOrderByNombreAsc(Long almacenId);

    @EntityGraph(attributePaths = "almacen")
    List<Producto> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = {"almacen", "categoria", "marca", "unidadMedida"})
    @Query("""
            select producto from Producto producto
             where (:almacenId is null or producto.almacen.id = :almacenId)
               and (:query = ''
                    or lower(producto.nombre) like lower(concat('%', :query, '%'))
                    or lower(producto.sku) like lower(concat('%', :query, '%'))
                    or lower(coalesce(producto.codigo, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(producto.codigoBarras, '')) like lower(concat('%', :query, '%')))
            """)
    Page<Producto> search(@Param("query") String query,
                          @Param("almacenId") Long almacenId,
                          Pageable pageable);
}
