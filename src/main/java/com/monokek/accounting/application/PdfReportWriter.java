package com.monokek.accounting.application;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

/**
 * Renders any {@link ReportTable} to a plain, functional tabular PDF
 * (PDFBox) — one generic writer for all four accounting reports, same
 * reasoning as {@link ExcelReportWriter}. No layout library: a title, a
 * bold header row, left-aligned text / right-aligned numbers, and automatic
 * pagination when a page fills up.
 */
@Component
public class PdfReportWriter {

    private static final float MARGIN = 40f;
    private static final float ROW_HEIGHT = 16f;
    private static final float FONT_SIZE = 9f;
    private static final float TITLE_FONT_SIZE = 14f;

    public byte[] write(ReportTable table) {
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
            drawTitle(cursor, regular, bold, table);
            drawHeaderRow(cursor, bold, table.headers(), columnWidths, rightAligned);

            int lastIndex = table.rows().size() - 1;
            for (int r = 0; r < table.rows().size(); r++) {
                boolean isTotalRow = r == lastIndex && "TOTAL".equals(String.valueOf(table.rows().get(r).get(0)));
                if (cursor.y - ROW_HEIGHT < MARGIN) {
                    cursor.newPage();
                    drawHeaderRow(cursor, bold, table.headers(), columnWidths, rightAligned);
                }
                drawDataRow(cursor, isTotalRow ? bold : regular, table.rows().get(r), columnWidths, rightAligned);
            }
            cursor.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de générer le PDF", e);
        }
    }

    private void drawTitle(PageCursor cursor, PDFont regular, PDFont bold, ReportTable table) throws IOException {
        cursor.text(bold, TITLE_FONT_SIZE, MARGIN, cursor.y, table.title());
        cursor.y -= TITLE_FONT_SIZE + 6;
        cursor.text(regular, FONT_SIZE, MARGIN, cursor.y, "Période : " + table.startDate() + " au " + table.endDate());
        cursor.y -= FONT_SIZE + 14;
    }

    private void drawHeaderRow(PageCursor cursor, PDFont bold, List<String> headers, float[] columnWidths, boolean[] rightAligned) throws IOException {
        float x = MARGIN;
        for (int col = 0; col < headers.size(); col++) {
            drawCell(cursor, bold, headers.get(col), x, columnWidths[col], rightAligned[col]);
            x += columnWidths[col];
        }
        cursor.rule(MARGIN, MARGIN + sum(columnWidths), cursor.y - 3);
        cursor.y -= ROW_HEIGHT;
    }

    private void drawDataRow(PageCursor cursor, PDFont font, List<Object> values, float[] columnWidths, boolean[] rightAligned) throws IOException {
        float x = MARGIN;
        for (int col = 0; col < values.size(); col++) {
            drawCell(cursor, font, formatCell(values.get(col)), x, columnWidths[col], rightAligned[col]);
            x += columnWidths[col];
        }
        cursor.y -= ROW_HEIGHT;
    }

    private void drawCell(PageCursor cursor, PDFont font, String value, float x, float width, boolean rightAlign) throws IOException {
        String truncated = truncate(font, value, width - 6);
        float textX = x + 2;
        if (rightAlign) {
            try {
                float textWidth = font.getStringWidth(truncated) / 1000 * FONT_SIZE;
                textX = x + width - textWidth - 4;
            } catch (IOException ignored) {
                // fall back to left padding
            }
        }
        cursor.text(font, FONT_SIZE, textX, cursor.y, truncated);
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

    /** Owns the current page/content-stream and the running Y position, and opens a fresh page on demand. */
    private static final class PageCursor {
        private final PDDocument document;
        private final float pageWidth;
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

        void text(PDFont font, float size, float x, float yPos, String value) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, yPos);
            stream.showText(sanitize(value));
            stream.endText();
        }

        void rule(float x1, float x2, float yPos) throws IOException {
            stream.setLineWidth(0.5f);
            stream.moveTo(x1, yPos);
            stream.lineTo(x2, yPos);
            stream.stroke();
        }

        void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }

        /** WinAnsi (PDFBox's Standard14 base encoding) can't encode every Unicode char — drop what it can't. */
        private String sanitize(String value) {
            StringBuilder sb = new StringBuilder(value.length());
            for (char c : value.toCharArray()) {
                sb.append(c <= 0xFF ? c : '?');
            }
            return sb.toString();
        }
    }
}
