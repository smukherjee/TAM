package com.utam.asset.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.utam.asset.dto.HeatmapDataDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Service for exporting heatmap data to CSV and PDF formats.
 * <p>
 * Feature: 005-asset-tracking-security (US6 - Heatmap Visualization)
 * Task: T035
 */
@Service
public class HeatmapExportService {

    private static final Logger logger = LoggerFactory.getLogger(HeatmapExportService.class);

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export heatmap data to CSV format.
     * CSV format: Latitude, Longitude, Intensity, ActivityCount, UniqueAssets, AvgSpeed
     *
     * @param heatmapData List of heatmap cells
     * @param mode        Heatmap mode (activity, violations, dwell)
     * @return CSV file as byte array
     */
    public byte[] exportHeatmapDataCSV(List<HeatmapDataDTO> heatmapData, String mode) {
        logger.info("Exporting heatmap data to CSV: mode={}, cells={}", mode, heatmapData.size());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            // Write CSV header
            if ("violations".equalsIgnoreCase(mode)) {
                writer.println("Latitude,Longitude,Intensity,ViolationCount,UniqueAssets,CriticalCount,HighCount,MediumCount,LowCount");
            } else {
                writer.println("Latitude,Longitude,Intensity,ActivityCount,UniqueAssets,AvgSpeed,MaxSpeed");
            }

            // Write data rows
            for (HeatmapDataDTO cell : heatmapData) {
                if ("violations".equalsIgnoreCase(mode)) {
                    writer.printf("%.6f,%.6f,%.4f,%d,%d,%d,%d,%d,%d%n",
                            cell.getLatitude(),
                            cell.getLongitude(),
                            cell.getIntensity(),
                            cell.getMetadata() != null ? cell.getMetadata().getActivityCount() : 0,
                            cell.getMetadata() != null ? cell.getMetadata().getUniqueAssets() : 0,
                            cell.getMetadata() != null ? cell.getMetadata().getCriticalCount() : 0,
                            cell.getMetadata() != null ? cell.getMetadata().getHighCount() : 0,
                            cell.getMetadata() != null ? cell.getMetadata().getMediumCount() : 0,
                            cell.getMetadata() != null ? cell.getMetadata().getLowCount() : 0
                    );
                } else {
                    writer.printf("%.6f,%.6f,%.4f,%d,%d,%.2f,%.2f%n",
                            cell.getLatitude(),
                            cell.getLongitude(),
                            cell.getIntensity(),
                            cell.getMetadata() != null ? cell.getMetadata().getActivityCount() : 0,
                            cell.getMetadata() != null ? cell.getMetadata().getUniqueAssets() : 0,
                            cell.getMetadata() != null && cell.getMetadata().getAvgSpeed() != null ? cell.getMetadata().getAvgSpeed() : 0.0,
                            cell.getMetadata() != null && cell.getMetadata().getMaxSpeed() != null ? cell.getMetadata().getMaxSpeed() : 0.0
                    );
                }
            }

            writer.flush();
            return baos.toByteArray();

        } catch (IOException e) {
            logger.error("Failed to export heatmap CSV", e);
            throw new RuntimeException("Failed to export heatmap data to CSV", e);
        }
    }

    /**
     * Generate heatmap summary report in PDF format.
     * PDF includes: Title, metadata, summary stats, top 10 hotspots table.
     *
     * @param heatmapData List of heatmap cells
     * @param mode        Heatmap mode (activity, violations, dwell)
     * @param tenantCode  Tenant filter
     * @param startDate   Start date
     * @param endDate     End date
     * @param gridSize    Grid resolution
     * @return PDF file as byte array
     */
    public byte[] generateHeatmapReportPDF(
            List<HeatmapDataDTO> heatmapData,
            String mode,
            String tenantCode,
            String startDate,
            String endDate,
            String gridSize) {

        logger.info("Generating heatmap PDF report: mode={}, tenant={}, cells={}",
                mode, tenantCode, heatmapData.size());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.DARK_GRAY);
            Paragraph title = new Paragraph(getModeTitle(mode) + " Heatmap Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Metadata section
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);

            document.add(new Paragraph("Report Details", headerFont));
            document.add(new Paragraph(String.format("Generated: %s", LocalDateTime.now().format(TIMESTAMP_FORMAT)), normalFont));
            document.add(new Paragraph(String.format("Tenant: %s", tenantCode), normalFont));
            document.add(new Paragraph(String.format("Date Range: %s to %s", startDate, endDate), normalFont));
            document.add(new Paragraph(String.format("Grid Resolution: %s", gridSize), normalFont));
            document.add(new Paragraph(String.format("Analysis Mode: %s", getModeTitle(mode)), normalFont));
            document.add(new Paragraph(" ")); // Spacer

            // Summary Statistics
            document.add(new Paragraph("Summary Statistics", headerFont));
            addStatisticsSection(document, heatmapData, mode, normalFont);
            document.add(new Paragraph(" ")); // Spacer

            // Top 10 Hotspots Table
            document.add(new Paragraph("Top 10 Hotspots", headerFont));
            addHotspotsTable(document, heatmapData, mode);

            document.close();
            return baos.toByteArray();

        } catch (DocumentException | IOException e) {
            logger.error("Failed to generate heatmap PDF", e);
            throw new RuntimeException("Failed to generate heatmap PDF report", e);
        }
    }

    /**
     * Get readable title for heatmap mode.
     */
    private String getModeTitle(String mode) {
        return switch (mode.toLowerCase()) {
            case "activity" -> "Activity Density";
            case "violations" -> "Zone Violation";
            case "dwell" -> "Dwell Time";
            default -> "Heatmap";
        };
    }

    /**
     * Add summary statistics section to PDF.
     */
    private void addStatisticsSection(Document document, List<HeatmapDataDTO> data, String mode, Font font)
            throws DocumentException {

        long totalCells = data.size();
        long hotspotCells = data.stream()
                .filter(cell -> cell.getIntensity() != null && cell.getIntensity() >= 0.7)
                .count();

        double maxIntensity = data.stream()
                .filter(cell -> cell.getIntensity() != null)
                .mapToDouble(HeatmapDataDTO::getIntensity)
                .max()
                .orElse(0.0);

        double avgIntensity = data.stream()
                .filter(cell -> cell.getIntensity() != null)
                .mapToDouble(HeatmapDataDTO::getIntensity)
                .average()
                .orElse(0.0);

        long totalActivity = data.stream()
                .filter(cell -> cell.getMetadata() != null && cell.getMetadata().getActivityCount() != null)
                .mapToLong(cell -> cell.getMetadata().getActivityCount())
                .sum();

        int uniqueAssets = data.stream()
                .filter(cell -> cell.getMetadata() != null && cell.getMetadata().getUniqueAssets() != null)
                .mapToInt(cell -> cell.getMetadata().getUniqueAssets())
                .max()
                .orElse(0);

        document.add(new Paragraph(String.format("Total Grid Cells: %d", totalCells), font));
        document.add(new Paragraph(String.format("High Intensity Cells (≥70%%): %d", hotspotCells), font));
        document.add(new Paragraph(String.format("Maximum Intensity: %.2f%%", maxIntensity * 100), font));
        document.add(new Paragraph(String.format("Average Intensity: %.2f%%", avgIntensity * 100), font));

        if ("violations".equalsIgnoreCase(mode)) {
            document.add(new Paragraph(String.format("Total Violations: %d", totalActivity), font));
        } else {
            document.add(new Paragraph(String.format("Total Movement Events: %d", totalActivity), font));
        }
        document.add(new Paragraph(String.format("Unique Assets Detected: %d", uniqueAssets), font));
    }

    /**
     * Add top 10 hotspots table to PDF.
     */
    private void addHotspotsTable(Document document, List<HeatmapDataDTO> data, String mode)
            throws DocumentException {

        // Get top 10 by intensity
        List<HeatmapDataDTO> top10 = data.stream()
                .sorted(Comparator.comparing(HeatmapDataDTO::getIntensity, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .toList();

        // Create table
        PdfPTable table;
        if ("violations".equalsIgnoreCase(mode)) {
            table = new PdfPTable(6); // Rank, Lat, Lng, Intensity, Violations, Critical
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 2, 2, 1.5f, 1.5f, 1.5f});
        } else {
            table = new PdfPTable(6); // Rank, Lat, Lng, Intensity, Activities, Avg Speed
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 2, 2, 1.5f, 1.5f, 1.5f});
        }

        // Header cells
        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Color headerBg = new Color(59, 130, 246); // Blue

        addHeaderCell(table, "Rank", headerFont, headerBg);
        addHeaderCell(table, "Latitude", headerFont, headerBg);
        addHeaderCell(table, "Longitude", headerFont, headerBg);
        addHeaderCell(table, "Intensity", headerFont, headerBg);

        if ("violations".equalsIgnoreCase(mode)) {
            addHeaderCell(table, "Violations", headerFont, headerBg);
            addHeaderCell(table, "Critical", headerFont, headerBg);
        } else {
            addHeaderCell(table, "Events", headerFont, headerBg);
            addHeaderCell(table, "Avg Speed", headerFont, headerBg);
        }

        // Data rows
        Font dataFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
        int rank = 1;
        for (HeatmapDataDTO cell : top10) {
            table.addCell(new PdfPCell(new Phrase(String.valueOf(rank++), dataFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.5f", cell.getLatitude()), dataFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.5f", cell.getLongitude()), dataFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.1f%%", cell.getIntensity() * 100), dataFont)));

            if ("violations".equalsIgnoreCase(mode)) {
                long violations = cell.getMetadata() != null && cell.getMetadata().getActivityCount() != null
                        ? cell.getMetadata().getActivityCount() : 0;
                long critical = cell.getMetadata() != null && cell.getMetadata().getCriticalCount() != null
                        ? cell.getMetadata().getCriticalCount() : 0;
                table.addCell(new PdfPCell(new Phrase(String.valueOf(violations), dataFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(critical), dataFont)));
            } else {
                long events = cell.getMetadata() != null && cell.getMetadata().getActivityCount() != null
                        ? cell.getMetadata().getActivityCount() : 0;
                double avgSpeed = cell.getMetadata() != null && cell.getMetadata().getAvgSpeed() != null
                        ? cell.getMetadata().getAvgSpeed() : 0.0;
                table.addCell(new PdfPCell(new Phrase(String.valueOf(events), dataFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("%.1f km/h", avgSpeed), dataFont)));
            }
        }

        document.add(table);
    }

    /**
     * Add styled header cell to table.
     */
    private void addHeaderCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }
}
