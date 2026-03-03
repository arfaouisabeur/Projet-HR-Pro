package edu.RhPro.services;

public class CvSummaryService {

    private PdfTextExtractor pdf = new PdfTextExtractor();
    private GeminiClient gemini = new GeminiClient();

    public String summarize(String path) {

        String text = pdf.extract(path);

        return gemini.summarize(text);
    }
    public String analyze(String path) {

        String text = pdf.extract(path);

        return gemini.analyzeCv(text);
    }
}