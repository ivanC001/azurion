package com.azurion.saascore.inventory.application.services;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductoSkuGenerator {

    private static final int SKU_WIDTH = 6;

    private final JdbcTemplate jdbcTemplate;

    public String nextSku() {
        jdbcTemplate.update("""
                INSERT INTO producto_sku_counter (id, ultimo_valor)
                VALUES (1, 0)
                ON CONFLICT (id) DO NOTHING
                """);
        Long next = jdbcTemplate.queryForObject("""
                UPDATE producto_sku_counter
                   SET ultimo_valor = ultimo_valor + 1
                 WHERE id = 1
             RETURNING ultimo_valor
                """, Long.class);
        if (next == null) {
            throw new IllegalStateException("No se pudo generar el SKU del producto");
        }
        return String.format(Locale.ROOT, "PRD-%0" + SKU_WIDTH + "d", next);
    }
}
