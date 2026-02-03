package com.utam.tracking.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.utam.tracking.dto.MovementDiscrepancyDTO;
import com.utam.tracking.dto.ZoneViolationDTO;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting violation and discrepancy reports to Excel and PDF formats.
 * <p>
 * Feature: 005-asset-tracking-security
 * Task: T055a (Excel Export), T055b (PDF Export)
 * <p>
 * Implements FR5.4: Export functionality for PDF (formatted report), Excel (raw data)
 */
@Service
public class ReportExportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportExportService.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final DateTimeFormatter FILENAME_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // PDF Fonts
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, Color.DARK_GRAY);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, Color.DARK_GRAY);

    // Severity Colors
    private static final Color CRITICAL_COLOR = new Color(220, 38, 38);  // Red
    private static final Color HIGH_COLOR = new Color(234, 88, 12);       // Orange
    private static final Color MEDIUM_COLOR = new Color(234, 179, 8);     // Yellow
    private static final Color LOW_COLOR = new Color(59, 130, 246);       // Blue

    // ============================================================
    // Zone Violations Export
    // ============================================================

    /**
     * Export zone violations to Excel (.xlsx) format.
     *
     * @param violations List of violations to export
     * @param tenantCode Tenant code for the report
     * @param startDate  Report start date
     * @param endDate    Report end date
     * @return Excel file as byte array
     */
    public byte[] exportViolationsToExcel(List<ZoneViolationDTO> violations,
                                           String tenantCode,
                                           String startDate,
                                           String endDate) {
        logger.info("Exporting {} violations to Excel for tenant {}", violations.size(), tenantCode);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Zone Violations");

            // Create header styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle severityStyle = createSeverityStyle(workbook);

            // Title row
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Zone Violations Report - " + tenantCode);
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            // Date range row
            org.apache.poi.ss.usermodel.Row dateRangeRow = sheet.createRow(1);
            dateRangeRow.createCell(0).setCellValue("Period: " + startDate + " to " + endDate);

            // Summary row
            org.apache.poi.ss.usermodel.Row summaryRow = sheet.createRow(2);
            summaryRow.createCell(0).setCellValue("Total Violations: " + violations.size());

            // Header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(4);
            String[] headers = {
                "Violation ID", "Asset Name", "Asset Category", "Zone Name", "Zone Type",
                "Severity", "Entry Time", "Exit Time", "Duration (min)", "Acknowledged",
                "Acknowledged By", "Resolution Notes"
            };

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 5;
            for (ZoneViolationDTO v : violations) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(v.getViolationId());
                row.createCell(1).setCellValue(v.getAssetName());
                row.createCell(2).setCellValue(v.getAssetCategory());
                row.createCell(3).setCellValue(v.getZoneName());
                row.createCell(4).setCellValue(v.getZoneType());
                row.createCell(5).setCellValue(v.getSeverity());
                row.createCell(6).setCellValue(formatDateTime(v.getTimestamp()));
                row.createCell(7).setCellValue(formatDateTime(v.getExitTimestamp()));
                row.createCell(8).setCellValue(v.getDurationSeconds() != null ? v.getDurationSeconds() / 60.0 : 0);
                row.createCell(9).setCellValue(Boolean.TRUE.equals(v.getAcknowledged()) ? "Yes" : "No");
                row.createCell(10).setCellValue(v.getAcknowledgedBy() != null ? v.getAcknowledgedBy() : "");
                row.createCell(11).setCellValue(v.getResolutionNotes() != null ? v.getResolutionNotes() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            logger.error("Error generating Excel report for violations", e);
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }

    /**
     * Export zone violations to PDF format.
     *
     * @param violations List of violations to export
     * @param tenantCode Tenant code for the report
     * @param startDate  Report start date
     * @param endDate    Report end date
     * @return PDF file as byte array
     */
    public byte[] exportViolationsToPdf(List<ZoneViolationDTO> violations,
                                         String tenantCode,
                                         String startDate,
                                         String endDate) {
        logger.info("Exporting {} violations to PDF for tenant {}", violations.size(), tenantCode);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Title
            Paragraph title = new Paragraph("Zone Violations Report", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Metadata
            Paragraph metadata = new Paragraph();
            metadata.add(new Chunk("Tenant: " + tenantCode + "    |    ", CELL_FONT));
            metadata.add(new Chunk("Period: " + startDate + " to " + endDate + "    |    ", CELL_FONT));
            metadata.add(new Chunk("Total: " + violations.size() + " violations", CELL_FONT));
            metadata.setAlignment(Element.ALIGN_CENTER);
            metadata.setSpacingAfter(15);
            document.add(metadata);

            // Summary by severity
            addViolationSummary(document, violations);

            // Table
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setSpacingBefore(15);
            table.setWidths(new float[]{1.5f, 2f, 1.5f, 1.5f, 1f, 2f, 1.5f, 1f});

            // Table headers
            String[] headers = {"Asset Name", "Asset Category", "Zone Name", "Zone Type",
                               "Severity", "Entry Time", "Duration", "Ack"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
                cell.setBackgroundColor(new Color(55, 65, 81));
                cell.setPadding(5);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Table data
            for (ZoneViolationDTO v : violations) {
                addCell(table, v.getAssetName());
                addCell(table, v.getAssetCategory());
                addCell(table, v.getZoneName());
                addCell(table, v.getZoneType());
                addSeverityCell(table, v.getSeverity());
                addCell(table, formatDateTimeShort(v.getTimestamp()));
                addCell(table, formatDuration(v.getDurationSeconds()));
                addCell(table, Boolean.TRUE.equals(v.getAcknowledged()) ? "✓" : "✗");
            }

            document.add(table);

            // Footer
            Paragraph footer = new Paragraph(
                "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY)
            );
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(20);
            document.add(footer);

            document.close();
            return outputStream.toByteArray();

        } catch (DocumentException | java.io.IOException e) {
            logger.error("Error generating PDF report for violations", e);
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // Movement Discrepancies Export
    // ============================================================

    /**
     * Export movement discrepancies to Excel (.xlsx) format.
     *
     * @param discrepancies List of discrepancies to export
     * @param tenantCode    Tenant code for the report
     * @param startDate     Report start date
     * @param endDate       Report end date
     * @return Excel file as byte array
     */
    public byte[] exportDiscrepanciesToExcel(List<MovementDiscrepancyDTO> discrepancies,
                                              String tenantCode,
                                              String startDate,
                                              String endDate) {
        logger.info("Exporting {} discrepancies to Excel for tenant {}", discrepancies.size(), tenantCode);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Movement Discrepancies");

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Title row
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Movement Discrepancies Report - " + tenantCode);
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            // Date range row
            org.apache.poi.ss.usermodel.Row dateRangeRow = sheet.createRow(1);
            dateRangeRow.createCell(0).setCellValue("Period: " + startDate + " to " + endDate);

            // Summary row
            org.apache.poi.ss.usermodel.Row summaryRow = sheet.createRow(2);
            summaryRow.createCell(0).setCellValue("Total Discrepancies: " + discrepancies.size());

            // Header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(4);
            String[] headers = {
                "Discrepancy ID", "Asset Name", "Asset Category", "Type", "Severity",
                "Expected Location", "Actual Location", "Deviation (m)", "Timestamp",
                "Acknowledged", "Resolution Notes"
            };

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 5;
            for (MovementDiscrepancyDTO d : discrepancies) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(d.getDiscrepancyId());
                row.createCell(1).setCellValue(d.getAssetName());
                row.createCell(2).setCellValue(d.getAssetCategory());
                row.createCell(3).setCellValue(d.getDiscrepancyType());
                row.createCell(4).setCellValue(d.getSeverity());
                row.createCell(5).setCellValue(d.getExpectedLocation() != null ? d.getExpectedLocation() : "");
                row.createCell(6).setCellValue(d.getActualLocation() != null ? d.getActualLocation() : "");
                row.createCell(7).setCellValue(d.getDeviationMeters() != null ? d.getDeviationMeters() : 0);
                row.createCell(8).setCellValue(formatDateTime(d.getTimestamp()));
                row.createCell(9).setCellValue(Boolean.TRUE.equals(d.getAcknowledged()) ? "Yes" : "No");
                row.createCell(10).setCellValue(d.getResolutionNotes() != null ? d.getResolutionNotes() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            logger.error("Error generating Excel report for discrepancies", e);
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }

    /**
     * Export movement discrepancies to PDF format.
     *
     * @param discrepancies List of discrepancies to export
     * @param tenantCode    Tenant code for the report
     * @param startDate     Report start date
     * @param endDate       Report end date
     * @return PDF file as byte array
     */
    public byte[] exportDiscrepanciesToPdf(List<MovementDiscrepancyDTO> discrepancies,
                                            String tenantCode,
                                            String startDate,
                                            String endDate) {
        logger.info("Exporting {} discrepancies to PDF for tenant {}", discrepancies.size(), tenantCode);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Title
            Paragraph title = new Paragraph("Movement Discrepancies Report", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Metadata
            Paragraph metadata = new Paragraph();
            metadata.add(new Chunk("Tenant: " + tenantCode + "    |    ", CELL_FONT));
            metadata.add(new Chunk("Period: " + startDate + " to " + endDate + "    |    ", CELL_FONT));
            metadata.add(new Chunk("Total: " + discrepancies.size() + " discrepancies", CELL_FONT));
            metadata.setAlignment(Element.ALIGN_CENTER);
            metadata.setSpacingAfter(15);
            document.add(metadata);

            // Summary by type
            addDiscrepancySummary(document, discrepancies);

            // Table
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setSpacingBefore(15);
            table.setWidths(new float[]{2f, 1.5f, 1.5f, 1f, 2f, 2f, 1f, 0.8f});

            // Table headers
            String[] headers = {"Asset Name", "Category", "Type", "Severity",
                               "Expected", "Actual", "Deviation", "Ack"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
                cell.setBackgroundColor(new Color(55, 65, 81));
                cell.setPadding(5);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Table data
            for (MovementDiscrepancyDTO d : discrepancies) {
                addCell(table, d.getAssetName());
                addCell(table, d.getAssetCategory());
                addCell(table, d.getDiscrepancyType());
                addSeverityCell(table, d.getSeverity());
                addCell(table, d.getExpectedLocation() != null ? d.getExpectedLocation() : "-");
                addCell(table, d.getActualLocation() != null ? d.getActualLocation() : "-");
                addCell(table, d.getDeviationMeters() != null ? String.format("%.0f m", d.getDeviationMeters()) : "-");
                addCell(table, Boolean.TRUE.equals(d.getAcknowledged()) ? "✓" : "✗");
            }

            document.add(table);

            // Footer
            Paragraph footer = new Paragraph(
                "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY)
            );
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(20);
            document.add(footer);

            document.close();
            return outputStream.toByteArray();

        } catch (DocumentException | java.io.IOException e) {
            logger.error("Error generating PDF report for discrepancies", e);
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private void addViolationSummary(Document document, List<ZoneViolationDTO> violations) throws DocumentException {
        long critical = violations.stream().filter(v -> "CRITICAL".equals(v.getSeverity())).count();
        long high = violations.stream().filter(v -> "HIGH".equals(v.getSeverity())).count();
        long medium = violations.stream().filter(v -> "MEDIUM".equals(v.getSeverity())).count();
        long low = violations.stream().filter(v -> "LOW".equals(v.getSeverity())).count();
        long acknowledged = violations.stream().filter(v -> Boolean.TRUE.equals(v.getAcknowledged())).count();

        PdfPTable summaryTable = new PdfPTable(6);
        summaryTable.setWidthPercentage(80);
        summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addSummaryCell(summaryTable, "Critical", String.valueOf(critical), CRITICAL_COLOR);
        addSummaryCell(summaryTable, "High", String.valueOf(high), HIGH_COLOR);
        addSummaryCell(summaryTable, "Medium", String.valueOf(medium), MEDIUM_COLOR);
        addSummaryCell(summaryTable, "Low", String.valueOf(low), LOW_COLOR);
        addSummaryCell(summaryTable, "Acknowledged", String.valueOf(acknowledged), new Color(34, 197, 94));
        addSummaryCell(summaryTable, "Pending", String.valueOf(violations.size() - acknowledged), Color.GRAY);

        document.add(summaryTable);
    }

    private void addDiscrepancySummary(Document document, List<MovementDiscrepancyDTO> discrepancies) throws DocumentException {
        long unexpectedMovement = discrepancies.stream()
            .filter(d -> "UNEXPECTED_MOVEMENT".equals(d.getDiscrepancyType())).count();
        long locationMismatch = discrepancies.stream()
            .filter(d -> "LOCATION_MISMATCH".equals(d.getDiscrepancyType())).count();
        long speedAnomaly = discrepancies.stream()
            .filter(d -> "SPEED_ANOMALY".equals(d.getDiscrepancyType())).count();
        long missingTracking = discrepancies.stream()
            .filter(d -> "MISSING_TRACKING".equals(d.getDiscrepancyType())).count();

        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(70);
        summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addSummaryCell(summaryTable, "Unexpected", String.valueOf(unexpectedMovement), MEDIUM_COLOR);
        addSummaryCell(summaryTable, "Mismatch", String.valueOf(locationMismatch), HIGH_COLOR);
        addSummaryCell(summaryTable, "Speed", String.valueOf(speedAnomaly), CRITICAL_COLOR);
        addSummaryCell(summaryTable, "Missing", String.valueOf(missingTracking), Color.GRAY);

        document.add(summaryTable);
    }

    private void addSummaryCell(PdfPTable table, String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBorder(Rectangle.NO_BORDER);
        
        Paragraph p = new Paragraph();
        p.add(new Chunk(value + "\n", new Font(Font.HELVETICA, 16, Font.BOLD, color)));
        p.add(new Chunk(label, new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY)));
        p.setAlignment(Element.ALIGN_CENTER);
        
        cell.addElement(p);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String content) {
        PdfPCell cell = new PdfPCell(new Phrase(content != null ? content : "", CELL_FONT));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addSeverityCell(PdfPTable table, String severity) {
        Color bgColor = switch (severity) {
            case "CRITICAL" -> CRITICAL_COLOR;
            case "HIGH" -> HIGH_COLOR;
            case "MEDIUM" -> MEDIUM_COLOR;
            case "LOW" -> LOW_COLOR;
            default -> Color.GRAY;
        };

        Font severityFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(severity, severityFont));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }

    private CellStyle createSeverityStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private String formatDateTime(ZonedDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMAT) : "";
    }

    private String formatDateTimeShort(ZonedDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) : "";
    }

    private String formatDuration(Long seconds) {
        if (seconds == null) return "-";
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + " min";
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "h " + remainingMinutes + "m";
    }
}
