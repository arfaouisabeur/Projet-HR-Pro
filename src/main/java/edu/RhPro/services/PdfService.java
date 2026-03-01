package edu.RhPro.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.MultiFormatWriter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import edu.RhPro.entities.Service;

public class PdfService {

    public static String genererTicketPDF(Service s) throws Exception {

        String fileName = "ticket_service_" + s.getId() + ".pdf";
        String path = System.getProperty("user.home") + "/Downloads/" + fileName;

        Document doc = new Document();
        PdfWriter.getInstance(doc, new java.io.FileOutputStream(path));
        doc.open();

        // ── Titre ────────────────────────────────
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,
                new BaseColor(109, 34, 105));
        Paragraph titre = new Paragraph("TICKET DE SERVICE #" + s.getId(), titleFont);
        titre.setAlignment(Element.ALIGN_CENTER);
        doc.add(titre);
        doc.add(new Paragraph(" "));

        // ── Séparateur ───────────────────────────
        doc.add(new Paragraph("─────────────────────────────────────────"));
        doc.add(new Paragraph(" "));

        // ── Infos ────────────────────────────────
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,
                new BaseColor(109, 34, 105));
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12,
                Font.NORMAL, BaseColor.DARK_GRAY);

        doc.add(new Paragraph("Employé ID   : " + s.getEmployeeId(),   normalFont));
        doc.add(new Paragraph("Titre        : " + s.getTitre(),        normalFont));
        doc.add(new Paragraph("Date         : " + s.getDateDemande(),  normalFont));
        doc.add(new Paragraph("Priorité     : " + s.getPriorite(),     normalFont));
        doc.add(new Paragraph("Étape        : " + s.getEtapeWorkflow(),normalFont));
        doc.add(new Paragraph("Statut       : " + s.getStatut(),       normalFont));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Description  : " + s.getDescription(),  normalFont));
        doc.add(new Paragraph(" "));

        // ── Séparateur ───────────────────────────
        doc.add(new Paragraph("─────────────────────────────────────────"));
        doc.add(new Paragraph(" "));

        // ── QR Code ──────────────────────────────
        String qrContent = "SERVICE_ID:" + s.getId()
                + "|EMP:" + s.getEmployeeId()
                + "|STATUT:" + s.getStatut()
                + "|ETAPE:" + s.getEtapeWorkflow();

        BitMatrix matrix = new MultiFormatWriter()
                .encode(qrContent, BarcodeFormat.QR_CODE, 150, 150);

        java.awt.image.BufferedImage qrImage =
                MatrixToImageWriter.toBufferedImage(matrix);

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(qrImage, "PNG", baos);

        Image pdfQr = Image.getInstance(baos.toByteArray());
        pdfQr.scaleAbsolute(130, 130);
        pdfQr.setAlignment(Element.ALIGN_CENTER);

        Font qrLabelFont = new Font(Font.FontFamily.HELVETICA, 11,
                Font.ITALIC, BaseColor.GRAY);
        Paragraph qrLabel = new Paragraph("QR Code de vérification", qrLabelFont);
        qrLabel.setAlignment(Element.ALIGN_CENTER);

        doc.add(qrLabel);
        doc.add(pdfQr);
        doc.add(new Paragraph(" "));

        // ── Pied de page ─────────────────────────
        Font footerFont = new Font(Font.FontFamily.HELVETICA, 9,
                Font.ITALIC, BaseColor.GRAY);
        Paragraph footer = new Paragraph(
                "Document généré automatiquement — RhPro", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return path;
    }
}