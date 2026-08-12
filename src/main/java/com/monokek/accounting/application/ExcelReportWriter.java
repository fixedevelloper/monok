package com.monokek.accounting.application;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;

/**
 * Renders any {@link ReportTable} to real {@code .xlsx} bytes (Apache POI) —
 * one generic writer for all four accounting reports, since every one of
 * them is just a titled table. Numeric cells (BigDecimal/Long) are written
 * as real Excel numbers, not text, so the expert-comptable can SUM() them
 * directly instead of re-parsing strings.
 */
@Component
public class ExcelReportWriter {

    public byte[] write(ReportTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(safeSheetName(table.title()));

            CellStyle titleStyle = titleStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle numberStyle = numberStyle(workbook);
            CellStyle totalStyle = totalStyle(workbook);
            CellStyle totalNumberStyle = totalNumberStyle(workbook);

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(table.title() + "  (" + table.startDate() + " au " + table.endDate() + ")");
            titleCell.setCellStyle(titleStyle);

            rowIndex++; // blank separator row

            Row headerRow = sheet.createRow(rowIndex++);
            for (int col = 0; col < table.headers().size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(table.headers().get(col));
                cell.setCellStyle(headerStyle);
            }

            int lastDataRow = table.rows().size() - 1;
            for (int r = 0; r < table.rows().size(); r++) {
                Row row = sheet.createRow(rowIndex++);
                boolean isTotalRow = r == lastDataRow && lastDataRow >= 0
                        && "TOTAL".equals(String.valueOf(table.rows().get(r).get(0)));
                var values = table.rows().get(r);
                for (int col = 0; col < values.size(); col++) {
                    Cell cell = row.createCell(col);
                    writeCell(cell, values.get(col), isTotalRow ? totalNumberStyle : numberStyle, isTotalRow ? totalStyle : null);
                }
            }

            for (int col = 0; col < table.headers().size(); col++) {
                sheet.autoSizeColumn(col);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de générer le fichier Excel", e);
        }
    }

    private void writeCell(Cell cell, Object value, CellStyle numberStyle, CellStyle textStyle) {
        switch (value) {
            case BigDecimal bd -> {
                cell.setCellValue(bd.doubleValue());
                cell.setCellStyle(numberStyle);
            }
            case Long l -> {
                cell.setCellValue(l);
                cell.setCellStyle(numberStyle);
            }
            case null -> cell.setCellValue("");
            default -> {
                cell.setCellValue(String.valueOf(value));
                if (textStyle != null) {
                    cell.setCellStyle(textStyle);
                }
            }
        }
    }

    private String safeSheetName(String title) {
        String cleaned = title.replaceAll("[\\\\/*?\\[\\]:]", " ");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    private CellStyle titleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle numberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private CellStyle totalStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }

    private CellStyle totalNumberStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }
}
