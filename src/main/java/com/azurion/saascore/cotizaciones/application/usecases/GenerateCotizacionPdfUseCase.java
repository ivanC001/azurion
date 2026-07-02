package com.azurion.saascore.cotizaciones.application.usecases;

import com.azurion.saascore.cotizaciones.application.dto.CotizacionPdfResponse;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.entities.CotizacionDetalle;
import com.azurion.saascore.empresas.application.usecases.GetCurrentEmpresaUseCase;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.shared.exception.BusinessException;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GenerateCotizacionPdfUseCase {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final float PAGE_LEFT = 48;
    private static final float PAGE_RIGHT = 547;
    private static final float PAGE_WIDTH = PAGE_RIGHT - PAGE_LEFT;

    private final GetCotizacionUseCase getCotizacionUseCase;
    private final GetCurrentEmpresaUseCase getCurrentEmpresaUseCase;

    @Value("${azurion.storage.public-files.root-dir:${user.dir}/storage/public-files}")
    private String publicFilesRootDir;

    @Transactional(readOnly = true)
    public CotizacionPdfResponse execute(Long id) {
        Cotizacion cotizacion = getCotizacionUseCase.find(id);
        Empresa empresa = getCurrentEmpresaUseCase.resolveCurrentEmpresa();

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            Fonts fonts = new Fonts();

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawHeader(document, content, fonts, empresa, cotizacion);
                drawClientBox(content, fonts, cotizacion);
                float afterTable = drawDetailsTable(content, fonts, cotizacion);
                drawTotals(content, fonts, cotizacion, afterTable);
                drawValidity(content, fonts, cotizacion);
                drawFooter(content, fonts, empresa, cotizacion);
            }

            document.save(output);
            return new CotizacionPdfResponse(
                    "cotizacion-" + cotizacion.getId() + ".pdf",
                    "application/pdf",
                    Base64.getEncoder().encodeToString(output.toByteArray())
            );
        } catch (IOException exception) {
            throw new BusinessException("COTIZACION_PDF_ERROR", "No se pudo generar el PDF de cotizacion");
        }
    }

    private void drawHeader(PDDocument document, PDPageContentStream content, Fonts fonts, Empresa empresa, Cotizacion cotizacion)
            throws IOException {
        fillRect(content, PAGE_LEFT, 708, PAGE_WIDTH, 86, 15, 23, 42);
        drawLogo(document, content, empresa.getLogoPanelUrl());

        write(content, fonts.bold, 16, 135, 763, empresa.getRazonSocial(), 255, 255, 255, 255);
        write(content, fonts.regular, 9, 135, 745, "RUC: " + empresa.getRuc(), 255, 226, 232, 240);
        write(content, fonts.regular, 9, 135, 730, safe(cotizacion.getSucursal().getDireccion()), 255, 226, 232, 240);
        write(content, fonts.regular, 8, 135, 716, cotizacion.getSucursal().getNombre(), 255, 226, 232, 240);

        fillRect(content, 415, 724, 116, 51, 255, 255, 255);
        write(content, fonts.bold, 15, 431, 755, "COTIZACION", 95, 15, 23, 42);
        write(content, fonts.bold, 12, 431, 738, quoteCode(cotizacion), 95, 15, 23, 42);
    }

    private void drawLogo(PDDocument document, PDPageContentStream content, String logoUrl) {
        byte[] logoBytes = loadLogoBytes(logoUrl);
        if (logoBytes == null) {
            return;
        }
        try {
            PDImageXObject image = PDImageXObject.createFromByteArray(document, logoBytes, "empresa-logo");
            float maxWidth = 70;
            float maxHeight = 60;
            float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            content.drawImage(image, 58 + ((maxWidth - width) / 2), 722 + ((maxHeight - height) / 2), width, height);
        } catch (Exception ignored) {
            // SVG/WEBP or inaccessible logos are skipped without preventing the PDF.
        }
    }

    private void drawClientBox(PDPageContentStream content, Fonts fonts, Cotizacion cotizacion) throws IOException {
        float y = 675;
        fillRect(content, PAGE_LEFT, y, PAGE_WIDTH, 22, 15, 118, 110);
        write(content, fonts.bold, 10, 58, y + 7, "DATOS DEL CLIENTE", 220, 255, 255, 255);

        strokeRect(content, PAGE_LEFT, y - 76, PAGE_WIDTH, 76, 218, 226, 236);
        write(content, fonts.bold, 9, 58, y - 18, "Cliente", 70);
        write(content, fonts.regular, 9, 118, y - 18, clientName(cotizacion), 205);
        write(content, fonts.bold, 9, 344, y - 18, "Documento", 70);
        write(content, fonts.regular, 9, 415, y - 18, clientDocument(cotizacion), 110);
        write(content, fonts.bold, 9, 58, y - 38, "Direccion", 70);
        write(content, fonts.regular, 9, 118, y - 38, clientAddress(cotizacion), 205);
        write(content, fonts.bold, 9, 344, y - 38, "Emision", 70);
        write(content, fonts.regular, 9, 415, y - 38, date(cotizacion.getFechaEmision()), 110);
        write(content, fonts.bold, 9, 58, y - 58, "Atendido por", 70);
        write(content, fonts.regular, 9, 118, y - 58, cotizacion.getUsuarioNombre(), 205);
        write(content, fonts.bold, 9, 344, y - 58, "Valida hasta", 70);
        write(content, fonts.bold, 9, 415, y - 58, validityDate(cotizacion), 110, 15, 118, 110);
    }

    private float drawDetailsTable(PDPageContentStream content, Fonts fonts, Cotizacion cotizacion) throws IOException {
        float top = 565;
        float rowHeight = 24;
        float[] columns = {PAGE_LEFT, 78, 330, 386, 463, PAGE_RIGHT};

        fillRect(content, PAGE_LEFT, top, PAGE_WIDTH, rowHeight, 71, 85, 105);
        write(content, fonts.bold, 8, 57, top + 8, "#", 18, 255, 255, 255);
        write(content, fonts.bold, 8, 88, top + 8, "DESCRIPCION", 230, 255, 255, 255);
        write(content, fonts.bold, 8, 340, top + 8, "CANT.", 42, 255, 255, 255);
        write(content, fonts.bold, 8, 396, top + 8, "PRECIO", 60, 255, 255, 255);
        write(content, fonts.bold, 8, 475, top + 8, "TOTAL", 62, 255, 255, 255);

        float y = top - rowHeight;
        int index = 1;
        for (CotizacionDetalle detalle : cotizacion.getDetalles()) {
            if (y < 210) {
                break;
            }
            if (index % 2 == 0) {
                fillRect(content, PAGE_LEFT, y, PAGE_WIDTH, rowHeight, 241, 245, 249);
            }
            write(content, fonts.regular, 8, 58, y + 8, String.valueOf(index), 17);
            write(content, fonts.regular, 8, 88, y + 8, detailDescription(detalle), 232);
            write(content, fonts.regular, 8, 340, y + 8, number(detalle.getCantidad()), 38);
            write(content, fonts.regular, 8, 396, y + 8, money(detalle.getPrecioUnitario()), 60);
            write(content, fonts.bold, 8, 475, y + 8, money(detalle.getTotal()), 62);
            drawRowLines(content, columns, y, rowHeight);
            y -= rowHeight;
            index++;
        }
        strokeRect(content, PAGE_LEFT, y + rowHeight, PAGE_WIDTH, top - y, 203, 213, 225);
        return y;
    }

    private void drawTotals(PDPageContentStream content, Fonts fonts, Cotizacion cotizacion, float y) throws IOException {
        float boxY = Math.max(145, y - 68);
        write(content, fonts.bold, 9, 365, boxY + 49, "SUBTOTAL", 80);
        write(content, fonts.regular, 9, 462, boxY + 49, money(cotizacion.getSubtotal()), 72);
        fillRect(content, 350, boxY, 197, 35, 15, 118, 110);
        write(content, fonts.bold, 13, 365, boxY + 12, "TOTAL", 80, 255, 255, 255);
        write(content, fonts.bold, 13, 462, boxY + 12, money(cotizacion.getTotal()), 72, 255, 255, 255);

        write(content, fonts.bold, 9, PAGE_LEFT, boxY + 49, "Observaciones", 110);
        write(content, fonts.regular, 8, PAGE_LEFT, boxY + 32, defaultText(cotizacion.getObservacion(), "Sin observaciones."), 270);
    }

    private void drawValidity(PDPageContentStream content, Fonts fonts, Cotizacion cotizacion) throws IOException {
        fillRect(content, PAGE_LEFT, 92, PAGE_WIDTH, 38, 236, 253, 245);
        strokeRect(content, PAGE_LEFT, 92, PAGE_WIDTH, 38, 167, 243, 208);
        write(content, fonts.bold, 8, 58, 116, "VALIDEZ DE LA COTIZACION", 180, 6, 95, 70);
        write(content, fonts.regular, 8, 58, 101,
                "Documento comercial valido hasta " + validityDate(cotizacion)
                        + ". Codigo de validacion: " + validationCode(cotizacion) + ".", 465, 6, 95, 70);
    }

    private void drawFooter(PDPageContentStream content, Fonts fonts, Empresa empresa, Cotizacion cotizacion) throws IOException {
        fillRect(content, 0, 0, PDRectangle.A4.getWidth(), 61, 15, 23, 42);
        write(content, fonts.bold, 8, PAGE_LEFT, 38, empresa.getRazonSocial(), 210, 255, 255, 255);
        write(content, fonts.regular, 7, PAGE_LEFT, 23,
                "Cotizacion comercial. No constituye comprobante de pago ni reserva stock.", 330, 203, 213, 225);
        write(content, fonts.regular, 7, 435, 31, "Pagina 1 de 1", 100, 203, 213, 225);
    }

    private void drawRowLines(PDPageContentStream content, float[] columns, float y, float rowHeight) throws IOException {
        content.setStrokingColor(new Color(226, 232, 240));
        content.setLineWidth(0.5f);
        for (float column : columns) {
            content.moveTo(column, y);
            content.lineTo(column, y + rowHeight);
        }
        content.stroke();
    }

    private void fillRect(PDPageContentStream content, float x, float y, float width, float height, int r, int g, int b)
            throws IOException {
        content.setNonStrokingColor(new Color(r, g, b));
        content.addRect(x, y, width, height);
        content.fill();
    }

    private void strokeRect(PDPageContentStream content, float x, float y, float width, float height, int r, int g, int b)
            throws IOException {
        content.setStrokingColor(new Color(r, g, b));
        content.setLineWidth(0.7f);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void write(PDPageContentStream content, PDType1Font font, int size, float x, float y, String text, float maxWidth)
            throws IOException {
        write(content, font, size, x, y, text, maxWidth, 15, 23, 42);
    }

    private void write(PDPageContentStream content, PDType1Font font, int size, float x, float y, String text, float maxWidth,
                       int r, int g, int b) throws IOException {
        content.beginText();
        content.setNonStrokingColor(new Color(r, g, b));
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(fitText(font, size, safe(text), maxWidth));
        content.endText();
    }

    private String fitText(PDType1Font font, int size, String text, float maxWidth) throws IOException {
        if (text.isBlank()) {
            return "-";
        }
        String value = text;
        while (value.length() > 3 && (font.getStringWidth(value) / 1000f * size) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value.equals(text) ? value : value.substring(0, Math.max(0, value.length() - 3)) + "...";
    }

    private byte[] loadLogoBytes(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(logoUrl.trim());
            String path = uri.getPath();
            int filesIndex = path == null ? -1 : path.indexOf("/files/");
            if (filesIndex >= 0) {
                Path localPath = Paths.get(publicFilesRootDir).toAbsolutePath().normalize()
                        .resolve(path.substring(filesIndex + "/files/".length())).normalize();
                if (localPath.startsWith(Paths.get(publicFilesRootDir).toAbsolutePath().normalize()) && Files.isRegularFile(localPath)) {
                    return Files.readAllBytes(localPath);
                }
            }
            if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
                HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
                HttpResponse<byte[]> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
                return response.statusCode() >= 200 && response.statusCode() < 300 ? response.body() : null;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String detailDescription(CotizacionDetalle detalle) {
        return defaultText(detalle.getDescripcion(), detalle.getProducto() == null ? "Oferta CRM" : detalle.getProducto().getNombre());
    }

    private String clientName(Cotizacion cotizacion) {
        return cotizacion.getCliente() == null ? "Cliente no especificado" : cotizacion.getCliente().getNombre();
    }

    private String clientDocument(Cotizacion cotizacion) {
        return cotizacion.getCliente() == null ? "-" : defaultText(cotizacion.getCliente().getNumeroDocumento(), "-");
    }

    private String clientAddress(Cotizacion cotizacion) {
        return cotizacion.getCliente() == null ? "-" : defaultText(cotizacion.getCliente().getDireccion(), "-");
    }

    private String quoteCode(Cotizacion cotizacion) {
        return String.format(Locale.ROOT, "COT-%06d", cotizacion.getId());
    }

    private String validationCode(Cotizacion cotizacion) {
        long datePart = cotizacion.getFechaEmision() == null ? 0 : cotizacion.getFechaEmision().toEpochDay();
        return String.format(Locale.ROOT, "AZ-%06d-%X", cotizacion.getId(), datePart + cotizacion.getId()).toUpperCase(Locale.ROOT);
    }

    private String validityDate(Cotizacion cotizacion) {
        LocalDate validity = cotizacion.getFechaVencimiento() == null
                ? cotizacion.getFechaEmision().plusDays(7)
                : cotizacion.getFechaVencimiento();
        return date(validity);
    }

    private String date(LocalDate value) {
        return value == null ? "-" : DATE_FORMAT.format(value);
    }

    private String money(BigDecimal value) {
        return "S/ " + (value == null ? BigDecimal.ZERO : value).setScale(2).toPlainString();
    }

    private String number(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").replaceAll("[^\\x20-\\x7E]", "");
    }

    private record Fonts(PDType1Font regular, PDType1Font bold) {
        private Fonts() {
            this(
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
            );
        }
    }
}
