package com.azurion.saascore.inventory.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azurion.shared.exception.BusinessException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ProductoPhotoValidatorTest {

    @Test
    void acceptsValidPngDataUrl() {
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01
        };
        String value = "data:image/png;base64," + Base64.getEncoder().encodeToString(png);

        assertThat(ProductoPhotoValidator.validateNewPhoto(value)).isEqualTo(value);
    }

    @Test
    void rejectsExternalUrl() {
        assertThatThrownBy(() -> ProductoPhotoValidator.validateNewPhoto("https://example.com/photo.png"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no se permiten URL externas");
    }

    @Test
    void rejectsContentOverOneMegabyte() {
        byte[] content = new byte[ProductoPhotoValidator.MAX_PHOTO_BYTES + 1];
        content[0] = (byte) 0xFF;
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;
        String value = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(content);

        assertThatThrownBy(() -> ProductoPhotoValidator.validateNewPhoto(value))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1 MB");
    }

    @Test
    void preservesHistoricalUrlWhenItWasNotChanged() {
        String historicalUrl = "https://legacy.example.com/product.png";

        assertThat(ProductoPhotoValidator.preserveOrValidate(historicalUrl, historicalUrl))
                .isEqualTo(historicalUrl);
    }
}
