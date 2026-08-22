package com.monokek.accounting.application;

import com.monokek.settings.StoreSettings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders any {@link ReportTable} to a branded tabular PDF (PDFBox) — one
 * generic writer for all four accounting reports, same reasoning as
 * {@link ExcelReportWriter}. No layout library: a store-identity header
 * (name/address/phone from {@link StoreSettings}, same source the receipt
 * printer uses), an accent-colored table header + light zebra striping for
 * readability, and a page footer (generation timestamp + page numbers) —
 * "official document for the expert-comptable" territory, not a raw data
 * dump. Pagination is automatic when a page fills up.
 */
@Component
public class PdfReportWriter {

    private static final float MARGIN = 40f;
    private static final float ROW_HEIGHT = 18f;
    private static final float FONT_SIZE = 9f;
    private static final float TITLE_FONT_SIZE = 14f;
    private static final float STORE_NAME_FONT_SIZE = 15f;
    private static final float STORE_INFO_FONT_SIZE = 8.5f;
    private static final float FOOTER_FONT_SIZE = 7.5f;
    private static final float FOOTER_HEIGHT = 24f;

    /** Matches the accent color already used for the analytics report PDF (useExport.tsx) and
     * the app's own UI — one consistent brand color across every exported document. */
    private static final Color ACCENT = new Color(79, 70, 229);
    private static final Color ACCENT_TINT = new Color(238, 237, 252);
    private static final Color ZEBRA_TINT = new Color(247, 247, 252);
    private static final Color GRAY = new Color(110, 110, 110);
    private static final Color RULE_GRAY = new Color(225, 225, 230);

    private final StoreSettings storeSettings;

    public PdfReportWriter(StoreSettings storeSettings) {
        this.storeSettings = storeSettings;
    }

    public byte[] write(ReportTable table) {
        StoreSettings.StoreInfo store = storeSettings.current();
        LocalDateTime generatedAt = LocalDateTime.now();

        try (PDDocument document = new PDDocument()) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float usableWidth = pageWidth - 2 * MARGIN;
            float[] columnWidths = columnWidths(table.headers().size(), usableWidth);
            boolean[] rightAligned = rightAlignedColumns(table);

            PageCursor cursor = new PageCursor(document, pageWidth, pageHeight);
            cursor.newPage();
            drawStoreHeader(cursor, regular, bold, store);
            drawTitle(cursor, regular, bold, table);
            drawHeaderRow(cursor, bold, table.headers(), columnWidths, rightAligned);

            int lastIndex = table.rows().size() - 1;
            for (int r = 0; r < table.rows().size(); r++) {
                boolean isTotalRow = r == lastIndex && "TOTAL".equals(String.valueOf(table.rows().get(r).get(0)));
                if (cursor.y - ROW_HEIGHT < MARGIN + FOOTER_HEIGHT) {
                    cursor.newPage();
                    drawHeaderRow(cursor, bold, table.headers(), columnWidths, rightAligned);
                }
                if (!isTotalRow && r % 2 == 1) {
                    cursor.fillRect(MARGIN, cursor.y - ROW_HEIGHT + 4, sum(columnWidths), ROW_HEIGHT, ZEBRA_TINT);
                }
                drawDataRow(cursor, isTotalRow ? bold : regular, isTotalRow, table.rows().get(r), columnWidths, rightAligned);
            }
            cursor.close();

            drawFooters(document, regular, pageWidth, generatedAt);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de générer le PDF", e);
        }
    }

    private void drawStoreHeader(PageCursor cursor, PDFont regular, PDFont bold, StoreSettings.StoreInfo store) throws IOException {
        String name = store.name() == null || store.name().isBlank() ? "Mono-Kek" : store.name();
        cursor.text(bold, STORE_NAME_FONT_SIZE, ACCENT, MARGIN, cursor.y, name);
        cursor.y -= STORE_NAME_FONT_SIZE + 4;

        String contact = String.join("  ·  ",
                java.util.stream.Stream.of(store.address(), store.phone()).filter(s -> s != null && !s.isBlank()).toList());
        if (!contact.isEmpty()) {
            cursor.text(regular, STORE_INFO_FONT_SIZE, GRAY, MARGIN, cursor.y, contact);
            cursor.y -= STORE_INFO_FONT_SIZE + 8;
        } else {
            cursor.y -= 4;
        }

        cursor.fillRect(MARGIN, cursor.y, cursor.pageWidth - 2 * MARGIN, 2f, ACCENT);
        cursor.y -= 16;
    }

    private void drawTitle(PageCursor cursor, PDFont regular, PDFont bold, ReportTable table) throws IOException {
        cursor.text(bold, TITLE_FONT_SIZE, Color.BLACK, MARGIN, cursor.y, table.title());
        cursor.y -= TITLE_FONT_SIZE + 6;
        cursor.text(regular, FONT_SIZE, GRAY, MARGIN, cursor.y, "Période : " + table.startDate() + " au " + table.endDate());
        cursor.y -= FONT_SIZE + 14;
    }

    private void drawHeaderRow(PageCursor cursor, PDFont bold, List<String> headers, float[] columnWidths, boolean[] rightAligned) throws IOException {
        cursor.fillRect(MARGIN, cursor.y - ROW_HEIGHT + 5, sum(columnWidths), ROW_HEIGHT, ACCENT);
        float x = MARGIN;
        for (int col = 0; col < headers.size(); col++) {
            drawCell(cursor, bold, Color.WHITE, headers.get(col), x, columnWidths[col], rightAligned[col]);
            x += columnWidths[col];
        }
        cursor.y -= ROW_HEIGHT;
    }

    private void drawDataRow(PageCursor cursor, PDFont font, boolean isTotalRow, List<Object> values, float[] columnWidths, boolean[] rightAligned) throws IOException {
        if (isTotalRow) {
            cursor.rule(RULE_GRAY, MARGIN, MARGIN + sum(columnWidths), cursor.y + 4);
        }
        float x = MARGIN;
        Color textColor = isTotalRow ? ACCENT : Color.BLACK;
        for (int col = 0; col < values.size(); col++) {
            drawCell(cursor, font, textColor, formatCell(values.get(col)), x, columnWidths[col], rightAligned[col]);
            x += columnWidths[col];
        }
        cursor.y -= ROW_HEIGHT;
    }

    private void drawCell(PageCursor cursor, PDFont font, Color color, String value, float x, float width, boolean rightAlign) throws IOException {
        String truncated = truncate(font, value, width - 6);
        float textX = x + 4;
        if (rightAlign) {
            try {
                float textWidth = font.getStringWidth(truncated) / 1000 * FONT_SIZE;
                textX = x + width - textWidth - 6;
            } catch (IOException ignored) {
                // fall back to left padding
            }
        }
        cursor.text(font, FONT_SIZE, color, textX, cursor.y, truncated);
    }

    private void drawFooters(PDDocument document, PDFont regular, float pageWidth, LocalDateTime generatedAt) throws IOException {
        String generatedLabel = "Généré le " + generatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));
        int total = document.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream stream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                stream.setStrokingColor(RULE_GRAY);
                stream.setLineWidth(0.5f);
                stream.moveTo(MARGIN, FOOTER_HEIGHT);
                stream.lineTo(pageWidth - MARGIN, FOOTER_HEIGHT);
                stream.stroke();

                stream.setNonStrokingColor(GRAY);
                stream.beginText();
                stream.setFont(regular, FOOTER_FONT_SIZE);
                stream.newLineAtOffset(MARGIN, FOOTER_HEIGHT - 12);
                stream.showText(sanitize(generatedLabel));
                stream.endText();

                String pageLabel = "Page " + (i + 1) + " / " + total;
                float labelWidth = regular.getStringWidth(pageLabel) / 1000 * FOOTER_FONT_SIZE;
                stream.beginText();
                stream.setFont(regular, FOOTER_FONT_SIZE);
                stream.newLineAtOffset(pageWidth - MARGIN - labelWidth, FOOTER_HEIGHT - 12);
                stream.showText(sanitize(pageLabel));
                stream.endText();
            }
        }
    }

    private String truncate(PDFont font, String value, float maxWidth) throws IOException {
        if (value == null) {
            return "";
        }
        String text = value;
        while (!text.isEmpty() && font.getStringWidth(text) / 1000 * FONT_SIZE > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text.length() < value.length() && text.length() > 1 ? text.substring(0, text.length() - 1) + "…" : text;
    }

    private String formatCell(Object value) {
        return switch (value) {
            case null -> "";
            case BigDecimal bd -> formatNumber(bd);
            case Long l -> String.valueOf(l);
            default -> String.valueOf(value);
        };
    }

    private String formatNumber(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols).format(value);
    }

    private float[] columnWidths(int columnCount, float usableWidth) {
        float[] widths = new float[columnCount];
        float each = usableWidth / columnCount;
        for (int i = 0; i < columnCount; i++) {
            widths[i] = each;
        }
        return widths;
    }

    /** Numeric-looking columns (amounts, quantities) read better right-aligned; the rest stay left-aligned. */
    private boolean[] rightAlignedColumns(ReportTable table) {
        int columnCount = table.headers().size();
        boolean[] result = new boolean[columnCount];
        if (table.rows().isEmpty()) {
            return result;
        }
        List<Object> sample = table.rows().get(0);
        for (int col = 0; col < columnCount; col++) {
            Object v = sample.get(col);
            result[col] = v instanceof BigDecimal || v instanceof Long;
        }
        return result;
    }

    private float sum(float[] values) {
        float total = 0;
        for (float v : values) {
            total += v;
        }
        return total;
    }

    /** WinAnsi (PDFBox's Standard14 base encoding) can't encode every Unicode char — drop what it can't. */
    private static String sanitize(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            sb.append(c <= 0xFF ? c : '?');
        }
        return sb.toString();
    }

    /** Owns the current page/content-stream and the running Y position, and opens a fresh page on demand. */
    private static final class PageCursor {
        private final PDDocument document;
        final float pageWidth;
        private final float pageHeight;
        private PDPageContentStream stream;
        float y;

        PageCursor(PDDocument document, float pageWidth, float pageHeight) {
            this.document = document;
            this.pageWidth = pageWidth;
            this.pageHeight = pageHeight;
        }

        void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = pageHeight - MARGIN;
        }

        void text(PDFont font, float size, Color color, float x, float yPos, String value) throws IOException {
            stream.setNonStrokingColor(color);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, yPos);
            stream.showText(sanitize(value));
            stream.endText();
        }

        void rule(Color color, float x1, float x2, float yPos) throws IOException {
            stream.setStrokingColor(color);
            stream.setLineWidth(0.5f);
            stream.moveTo(x1, yPos);
            stream.lineTo(x2, yPos);
            stream.stroke();
        }

        void fillRect(float x, float y, float width, float height, Color color) throws IOException {
            stream.setNonStrokingColor(color);
            stream.addRect(x, y, width, height);
            stream.fill();
        }

        void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
