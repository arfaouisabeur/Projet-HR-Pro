package edu.RhPro.services;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import edu.RhPro.entities.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Service QR Code amélioré :
 *  - Contenu JSON enrichi
 *  - QR coloré avec logo RH au centre
 *  - Scanner depuis fichier image
 */
public class QrCodeService {

    private static final int QR_SIZE   = 300;
    private static final int QR_COLOR  = 0xFF6d2269; // violet #6d2269
    private static final int BG_COLOR  = 0xFFFFFFFF; // blanc

    /**
     * Génère un QR Code coloré depuis n'importe quel texte (URL, etc.)
     * avec logo RH au centre — retourne les bytes PNG.
     */
    public static byte[] genererQRDepuisTexte(String texte) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(texte, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

        MatrixToImageConfig config = new MatrixToImageConfig(QR_COLOR, BG_COLOR);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix, config);
        qrImage = ajouterLogo(qrImage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        return baos.toByteArray();
    }

    // ══════════════════════════════════════════════
    //  1. Générer QR enrichi → BufferedImage
    // ══════════════════════════════════════════════

    /**
     * Génère un QR Code coloré avec logo RH au centre.
     * Le contenu est un JSON complet de la demande.
     */
    public static BufferedImage genererQR(Service s) throws Exception {

        String contenu = buildJsonContent(s);

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // H = 30% correction (pour le logo)
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(contenu, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

        // QR coloré violet sur blanc
        MatrixToImageConfig config = new MatrixToImageConfig(QR_COLOR, BG_COLOR);
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(matrix, config);

        // Ajouter le logo RH au centre
        qrImage = ajouterLogo(qrImage);

        return qrImage;
    }

    /**
     * Génère le QR et retourne les bytes PNG (pour affichage JavaFX)
     */
    public static byte[] genererQRBytes(Service s) throws Exception {
        BufferedImage img = genererQR(s);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Génère le QR et sauvegarde en fichier PNG dans Downloads
     */
    public static String sauvegarderQR(Service s) throws Exception {
        BufferedImage img = genererQR(s);
        String path = System.getProperty("user.home") + "/Downloads/qr_service_" + s.getId() + ".png";
        ImageIO.write(img, "PNG", new File(path));
        return path;
    }

    // ══════════════════════════════════════════════
    //  2. Scanner QR depuis un fichier image
    // ══════════════════════════════════════════════

    /**
     * Lit un fichier image contenant un QR code et retourne le contenu décodé.
     * Lance une exception si aucun QR n'est trouvé.
     */
    public static String scannerDepuisFichier(File imageFile) throws Exception {
        BufferedImage img = ImageIO.read(imageFile);
        if (img == null) throw new Exception("Impossible de lire l'image : " + imageFile.getName());

        LuminanceSource source = new BufferedImageLuminanceSource(img);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        Result result = new MultiFormatReader().decode(bitmap, hints);
        return result.getText();
    }

    /**
     * Parse le contenu JSON du QR et extrait l'ID de la demande.
     * Retourne -1 si non trouvé.
     */
    public static long extraireIdDepuisQR(String contenuQR) {
        try {
            // Cherche "id": 12
            String marker = "\"id\":";
            int start = contenuQR.indexOf(marker);
            if (start == -1) {
                // Format ancien : SERVICE_ID:12|...
                if (contenuQR.startsWith("SERVICE_ID:")) {
                    String[] parts = contenuQR.split("\\|");
                    return Long.parseLong(parts[0].replace("SERVICE_ID:", "").trim());
                }
                return -1;
            }
            start += marker.length();
            // Sauter les espaces
            while (start < contenuQR.length() && contenuQR.charAt(start) == ' ') start++;
            int end = start;
            while (end < contenuQR.length() && Character.isDigit(contenuQR.charAt(end))) end++;
            return Long.parseLong(contenuQR.substring(start, end));
        } catch (Exception e) {
            return -1;
        }
    }

    // ══════════════════════════════════════════════
    //  3. Construire le contenu JSON enrichi
    // ══════════════════════════════════════════════
    private static String buildJsonContent(Service s) {
        return "{"
                + "\"id\":" + s.getId() + ","
                + "\"titre\":\"" + escape(s.getTitre()) + "\","
                + "\"employe_id\":" + s.getEmployeeId() + ","
                + "\"statut\":\"" + nvl(s.getStatut()) + "\","
                + "\"priorite\":\"" + nvl(s.getPriorite()) + "\","
                + "\"etape\":\"" + nvl(s.getEtapeWorkflow()) + "\","
                + "\"date\":\"" + (s.getDateDemande() != null ? s.getDateDemande() : "-") + "\","
                + "\"deadline\":\"" + (s.getDeadlineReponse() != null ? s.getDeadlineReponse() : "-") + "\","
                + "\"sla_depasse\":" + s.isSlaDepasse()
                + "}";
    }

    // ══════════════════════════════════════════════
    //  4. Logo RH au centre du QR
    // ══════════════════════════════════════════════
    private static BufferedImage ajouterLogo(BufferedImage qrImage) {
        int qrW = qrImage.getWidth();
        int qrH = qrImage.getHeight();

        // Taille du logo = 18% du QR
        int logoSize = (int) (qrW * 0.18);
        int x = (qrW - logoSize) / 2;
        int y = (qrH - logoSize) / 2;

        // Créer le logo : cercle violet avec "RH" en blanc
        BufferedImage logo = new BufferedImage(logoSize, logoSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = logo.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fond blanc (marge)
        g.setColor(Color.WHITE);
        g.fillOval(0, 0, logoSize, logoSize);

        // Cercle violet
        int pad = 3;
        g.setColor(new Color(109, 34, 105)); // #6d2269
        g.fillOval(pad, pad, logoSize - pad * 2, logoSize - pad * 2);

        // Texte "RH"
        int fontSize = Math.max(10, logoSize / 3);
        g.setFont(new Font("Arial", Font.BOLD, fontSize));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int tx = (logoSize - fm.stringWidth("RH")) / 2;
        int ty = (logoSize - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString("RH", tx, ty);
        g.dispose();

        // Fusionner sur le QR
        Graphics2D qrG = qrImage.createGraphics();
        qrG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        qrG.drawImage(logo, x, y, logoSize, logoSize, null);
        qrG.dispose();

        return qrImage;
    }

    private static String nvl(String s) { return s != null ? s : ""; }
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}