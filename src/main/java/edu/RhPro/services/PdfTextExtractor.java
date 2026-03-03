package edu.RhPro.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class PdfTextExtractor {

    public String extract(String path) {

        try {

            PDDocument doc = PDDocument.load(new File(path));

            PDFTextStripper stripper = new PDFTextStripper();

            String text = stripper.getText(doc);

            doc.close();

            return text;

        } catch (Exception e) {

            throw new RuntimeException("Erreur lecture PDF");
        }
    }
}