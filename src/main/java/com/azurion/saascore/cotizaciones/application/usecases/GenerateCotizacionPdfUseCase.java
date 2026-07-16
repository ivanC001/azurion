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
import java.time.temporal.ChronoUnit;
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
                drawIntroduction(content, fonts, cotizacion);
                float afterTable = drawDetailsTable(content, fonts, cotizacion);
                float totalsY = drawTotals(content, fonts, cotizacion, afterTable);
                drawCommercialConditions(content, fonts, cotizacion, Math.max(70, totalsY - 112));
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
        drawLogo(document, content, empresa.getLogoPanelUrl(), 52, 754, 68, 48);
        writeScaled(content, fonts.bold, 17, 11, 132, 787, empresa.getRazonSocial(), 190, 5, 48, 91);
        write(content, fonts.bold, 9, 132, 770, "RUC: " + empresa.getRuc(), 180, 30, 64, 105);

        fillRect(content, 333, 750, 214, 52, 5, 48, 91);
        fillRect(content, 373, 742, 150, 20, 24, 137, 255);
        write(content, fonts.bold, 21, 387, 778, "COTIZACION", 148, 255, 255, 255);
        write(content, fonts.bold, 11, 407, 748, quoteCode(cotizacion), 108, 255, 255, 255);

        write(content, fonts.bold, 8, PAGE_LEFT, 731, "SUCURSAL", 70, 0, 96, 190);
        write(content, fonts.regular, 9, 108, 731, cotizacion.getSucursal().getNombre(), 205);
        write(content, fonts.bold, 8, PAGE_LEFT, 714, "DIRECCION", 70, 0, 96, 190);
        write(content, fonts.regular, 9, 108, 714, defaultText(cotizacion.getSucursal().getDireccion(), "Sin direccion registrada"), 205);

        drawLine(content, 320, 696, 320, 736, 203, 213, 225, 0.8f);
        write(content, fonts.bold, 8, 342, 727, "FECHA DE EMISION", 108, 0, 96, 190);
        write(content, fonts.regular, 9, 460, 727, date(cotizacion.getFechaEmision()), 82);
        write(content, fonts.bold, 8, 342, 710, "VALIDA HASTA", 108, 0, 96, 190);
        write(content, fonts.regular, 9, 460, 710, validityDate(cotizacion), 82);
        write(content, fonts.bold, 8, 342, 693, "ASESOR", 108, 0, 96, 190);
        write(content, fonts.regular, 9, 460, 693, cotizacion.getUsuarioNombre(), 82);
        drawLine(content, PAGE_LEFT, 674, PAGE_RIGHT, 674, 0, 96, 190, 1.2f);
    }

    private void drawLogo(PDDocument document, PDPageContentStream content, String logoUrl,
                          float x, float y, float maxWidth, float maxHeight) throws IOException {
        byte[] logoBytes = loadLogoBytes(logoUrl);
        if (logoBytes == null) {
            fillRect(content, x, y, maxWidth, maxHeight, 234, 245, 255);
            return;
        }
        try {
            PDImageXObject image = PDImageXObject.createFromByteArray(document, logoBytes, "empresa-logo");
            float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
            float width = image.getWidth() * scale;
            float height = image.getHeight() * scale;
            content.drawImage(image, x + ((maxWidth - width) / 2), y + ((maxHeight - height) / 2), width, height);
        } catch (Exception ignored) {
            // SVG/WEBP or inaccessible logos are skipped without preventing the PDF.
        }
    }

    private void drawClientBox(PDPageContentStream content, Fonts fonts, Cotizacion cotizacion) throws IOException {
        float y = 650;
        strokeRect(content, PAGE_LEFT, y - 82, PAGE_WIDTH, 82, 191, 205, 222);
        fillRect(content, PAGE_LEFT, y - 24, PAGE_WIDTH, 24, 234, 245, 255);
        fillRect(content, 58, y - 18, 12, 12, 0, 96, 190);
        write(content, fonts.bold, 10, 80, y - 17, "DATOS DEL CLIENTE", 210, 0, 75, 155);

        write(content, fonts.bold, 8, 62, y - 43, "CLIENTE", 58);
        write(content, fonts.regular, 9, 122, y - 43, clientName(cotizacion), 180);
        write(content, fonts.bold, 8, 320, y - 43, "CORREO", 55);
        write(content, fonts.regular, 9, 380, y - 43, clientEmail(cotizacion), 150);
        write(content, fonts.bold, 8, 62, y - 62, "RUC / DNI", 58);
        write(content, fonts.regular, 9, 122, y - 62, clientDocument(cotizacion), 180);
        write(content, fonts.bold, 8, 320, y - 62, "TELEFONO", 55);
        write(content, fonts.regular, 9, 380, y - 62, clientPhone(cotizacion), 150);
        write(content, fonts.bold, 8, 62, y - 78, "DIRECCION", 58);
        write(content, fonts.regular, 8, 122, y - 78, clientAddress(cotizacion), 408);
    }

    private void drawIntroduction(PDPageContentStream content, Fonts fonts, Cotizacion cotizacion) throws IOException {
        write(content, fonts.bold, 9, PAGE_LEFT, 550, "Estimado(a) " + clientFirstName(cotizacion) + ",", PAGE_WIDTH);
        write(content, fonts.regular, 9, PAGE_LEFT, 533,
                "De acuerdo con su solicitud, presentamos la siguiente propuesta comercial.", PAGE_WIDTH);
        write(content, fonts.regular, 9, PAGE_LEFT, 517,
                "Quedamos atentos a cualquier consulta o ajuste que necesite.", PAGE_WIDTH);
        drawLine(content, PAGE_LEFT, 504, PAGE_RIGHT, 504, 0, 96, 190, 1.0f);
    }

    private float drawDetailsTable(PDPageContentStream content, Fonts fonts, Cotizacion cotizacion) throws IOException {
        float top = 470;
        float headerHeight = 24;
        int detailCount = cotizacion.getDetalles().size();
        float rowHeight = detailCount <= 4 ? 40 : 31;
        float[] columns = {PAGE_LEFT, 82, 326, 389, 468, PAGE_RIGHT};

        fillRect(content, PAGE_LEFT, top, PAGE_WIDTH, headerHeight, 0, 75, 155);
        write(content, fonts.bold, 8, 61, top + 8, "#", 18, 255, 255, 255);
        write(content, fonts.bold, 8, 94, top + 8, "DESCRIPCION", 220, 255, 255, 255);
        write(content, fonts.bold, 8, 336, top + 8, "CANTIDAD", 48, 255, 255, 255);
        write(content, fonts.bold, 8, 399, top + 8, "PRECIO UNIT.", 63, 255, 255, 255);
        write(content, fonts.bold, 8, 478, top + 8, "SUBTOTAL", 60, 255, 255, 255);

        float y = top - rowHeight;
        int index = 1;
        for (CotizacionDetalle detalle : cotizacion.getDetalles()) {
            if (index > 7) {
                break;
            }
            if (index == 7 && detailCount > 7) {
                fillRect(content, PAGE_LEFT, y, PAGE_WIDTH, rowHeight, 247, 250, 252);
                write(content, fonts.bold, 8, 63, y + (rowHeight / 2) - 3, "...", 16);
                write(content, fonts.bold, 8, 94, y + (rowHeight / 2) - 3,
                        "+ " + (detailCount - 6) + " items adicionales incluidos", 220);
                drawRowLines(content, columns, y, rowHeight);
                y -= rowHeight;
                break;
            }
            if (index % 2 == 0) {
                fillRect(content, PAGE_LEFT, y, PAGE_WIDTH, rowHeight, 247, 250, 252);
            }
            write(content, fonts.bold, 8, 63, y + (rowHeight / 2) - 3, String.valueOf(index), 16);
            write(content, fonts.bold, 8, 94, y + rowHeight - 14, detailTitle(detalle), 220);
            if (rowHeight >= 36) {
                write(content, fonts.regular, 7, 94, y + 10, detailSecondary(detalle), 220, 71, 85, 105);
            }
            write(content, fonts.regular, 8, 343, y + (rowHeight / 2) - 3, number(detalle.getCantidad()), 40);
            write(content, fonts.regular, 8, 401, y + (rowHeight / 2) - 3, money(detalle.getPrecioUnitario()), 62);
            write(content, fonts.bold, 8, 480, y + (rowHeight / 2) - 3, money(detalle.getTotal()), 58);
            drawRowLines(content, columns, y, rowHeight);
            y -= rowHeight;
            index++;
        }

        float tableBottom = y + rowHeight;
        strokeRect(content, PAGE_LEFT, tableBottom, PAGE_WIDTH, top + headerHeight - tableBottom, 191, 205, 222);
        return y;
    }

    private float drawTotals(PDPageContentStream content, Fonts fonts, Cotizacion cotizacion, float y) throws IOException {
        float boxY = Math.max(183, y - 62);
        BigDecimal gross = grossTotal(cotizacion);
        BigDecimal discount = gross.subtract(cotizacion.getTotal() == null ? BigDecimal.ZERO : cotizacion.getTotal());

        write(content, fonts.bold, 8, 360, boxY + 50, "SUBTOTAL", 90);
        writeRight(content, fonts.regular, 9, 470, boxY + 50, money(gross), 72);
        drawLine(content, 352, boxY + 43, PAGE_RIGHT, boxY + 43, 218, 226, 236, 0.6f);
        write(content, fonts.bold, 8, 360, boxY + 31, "DESCUENTO", 90);
        writeRight(content, fonts.regular, 9, 470, boxY + 31, money(discount.max(BigDecimal.ZERO)), 72);
        fillRect(content, 350, boxY - 1, 197, 24, 234, 245, 255);
        write(content, fonts.bold, 11, 360, boxY + 7, "TOTAL", 80, 0, 75, 155);
        writeRight(content, fonts.bold, 12, 462, boxY + 7, money(cotizacion.getTotal()), 78, 0, 75, 155);
        return boxY;
    }

    private void drawCommercialConditions(PDPageContentStream content, Fonts fonts, Cotizacion cotizacion, float y)
            throws IOException {
        float height = 96;
        strokeRect(content, PAGE_LEFT, y, PAGE_WIDTH, height, 191, 205, 222);
        fillRect(content, PAGE_LEFT, y + height - 25, PAGE_WIDTH, 25, 234, 245, 255);
        fillRect(content, 59, y + height - 18, 12, 12, 0, 96, 190);
        write(content, fonts.bold, 10, 80, y + height - 17, "CONDICIONES COMERCIALES", 250, 0, 75, 155);

        write(content, fonts.bold, 8, 62, y + 50, "VALIDEZ", 65);
        write(content, fonts.regular, 8, 128, y + 50, validityDays(cotizacion) + " dias calendario", 105);
        write(content, fonts.bold, 8, 250, y + 50, "MONEDA", 55);
        write(content, fonts.regular, 8, 310, y + 50, currencyLabel(cotizacion), 95);
        write(content, fonts.bold, 8, 420, y + 50, "ESTADO", 50);
        drawStatusBadge(content, fonts, cotizacion, 475, y + 43);

        write(content, fonts.bold, 8, 62, y + 29, "ASESOR", 65);
        write(content, fonts.regular, 8, 128, y + 29, cotizacion.getUsuarioNombre(), 175);
        write(content, fonts.bold, 8, 320, y + 29, "SUCURSAL", 62);
        write(content, fonts.regular, 8, 386, y + 29, cotizacion.getSucursal().getNombre(), 145);

        write(content, fonts.bold, 8, 62, y + 10, "OBSERVACION", 75);
        write(content, fonts.regular, 8, 142, y + 10,
                defaultText(cotizacion.getObservacion(), "Servicios y condiciones segun el detalle de esta propuesta."), 390);
    }

    private void drawFooter(PDPageContentStream content, Fonts fonts, Empresa empresa, Cotizacion cotizacion) throws IOException {
        fillRect(content, 0, 0, PDRectangle.A4.getWidth(), 52, 5, 48, 91);
        fillRect(content, 0, 0, 72, 52, 24, 137, 255);
        write(content, fonts.bold, 8, 90, 32, empresa.getRazonSocial(), 205, 255, 255, 255);
        write(content, fonts.regular, 7, 90, 18,
                "Propuesta comercial preparada para atender su solicitud.", 260, 218, 235, 255);
        write(content, fonts.bold, 7, 395, 31, "CODIGO " + validationCode(cotizacion), 135, 255, 255, 255);
        write(content, fonts.regular, 7, 455, 17, "Pagina 1 de 1", 75, 218, 235, 255);
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

    private void drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2,
                          int r, int g, int b, float width) throws IOException {
        content.setStrokingColor(new Color(r, g, b));
        content.setLineWidth(width);
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private void drawStatusBadge(PDPageContentStream content, Fonts fonts, Cotizacion cotizacion, float x, float y)
            throws IOException {
        String label = statusLabel(cotizacion);
        fillRect(content, x, y, 61, 16, 0, 96, 190);
        write(content, fonts.bold, 7, x + 7, y + 5, label, 49, 255, 255, 255);
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

    private void writeScaled(PDPageContentStream content, PDType1Font font, int preferredSize, int minimumSize,
                             float x, float y, String text, float maxWidth, int r, int g, int b) throws IOException {
        String value = safe(text);
        int size = preferredSize;
        while (size > minimumSize && (font.getStringWidth(value) / 1000f * size) > maxWidth) {
            size--;
        }
        write(content, font, size, x, y, value, maxWidth, r, g, b);
    }

    private void writeRight(PDPageContentStream content, PDType1Font font, int size, float x, float y,
                            String text, float maxWidth) throws IOException {
        writeRight(content, font, size, x, y, text, maxWidth, 15, 23, 42);
    }

    private void writeRight(PDPageContentStream content, PDType1Font font, int size, float x, float y,
                            String text, float maxWidth, int r, int g, int b) throws IOException {
        String fitted = fitText(font, size, safe(text), maxWidth);
        float textWidth = font.getStringWidth(fitted) / 1000f * size;
        write(content, font, size, x + maxWidth - textWidth, y, fitted, maxWidth, r, g, b);
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

    private String detailTitle(CotizacionDetalle detalle) {
        if (detalle.getProducto() != null && detalle.getProducto().getNombre() != null) {
            return detalle.getProducto().getNombre();
        }
        return detailDescription(detalle);
    }

    private String detailSecondary(CotizacionDetalle detalle) {
        if (detalle.getProducto() != null && detalle.getProducto().getDescripcion() != null
                && !detalle.getProducto().getDescripcion().isBlank()) {
            return detalle.getProducto().getDescripcion();
        }
        String description = detailDescription(detalle);
        return description.equals(detailTitle(detalle)) ? "Incluido en la propuesta comercial." : description;
    }

    private BigDecimal grossTotal(Cotizacion cotizacion) {
        return cotizacion.getDetalles().stream()
                .map(detalle -> (detalle.getCantidad() == null ? BigDecimal.ZERO : detalle.getCantidad())
                        .multiply(detalle.getPrecioUnitario() == null ? BigDecimal.ZERO : detalle.getPrecioUnitario()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    private String clientEmail(Cotizacion cotizacion) {
        return cotizacion.getCliente() == null ? "-" : defaultText(cotizacion.getCliente().getEmail(), "-");
    }

    private String clientPhone(Cotizacion cotizacion) {
        return cotizacion.getCliente() == null ? "-" : defaultText(cotizacion.getCliente().getTelefono(), "-");
    }

    private String clientFirstName(Cotizacion cotizacion) {
        String name = clientName(cotizacion).trim();
        int separator = name.indexOf(' ');
        return separator > 0 ? name.substring(0, separator) : name;
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

    private long validityDays(Cotizacion cotizacion) {
        LocalDate emission = cotizacion.getFechaEmision() == null ? LocalDate.now() : cotizacion.getFechaEmision();
        LocalDate validity = cotizacion.getFechaVencimiento() == null
                ? emission.plusDays(7)
                : cotizacion.getFechaVencimiento();
        return Math.max(0, ChronoUnit.DAYS.between(emission, validity));
    }

    private String currencyLabel(Cotizacion cotizacion) {
        String currency = defaultText(cotizacion.getMoneda(), "PEN").toUpperCase(Locale.ROOT);
        return "PEN".equals(currency) ? "PEN - Soles" : currency;
    }

    private String statusLabel(Cotizacion cotizacion) {
        return defaultText(cotizacion.getEstado(), "BORRADOR").toUpperCase(Locale.ROOT);
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
