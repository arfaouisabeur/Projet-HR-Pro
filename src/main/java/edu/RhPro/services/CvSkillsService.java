package edu.RhPro.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class CvSkillsService {

    public String extractTextFromPdf(File pdf) throws Exception {
        try (PDDocument doc = PDDocument.load(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    public List<String> loadSkills() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/skills.txt")) {
            if (is == null) throw new RuntimeException("skills.txt introuvable dans resources");
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Arrays.stream(content.split("\\R"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    public List<String> extractSkills(String cvText, List<String> skillsList) {
        String text = normalize(cvText);

        // ✅ Variantes pour mieux détecter
        Map<String, String[]> variants = new LinkedHashMap<>();
        variants.put("Spring Boot", new String[]{"springboot", "spring boot"});
        variants.put("Java EE", new String[]{"java ee", "jee", "j2ee"});
        variants.put("REST", new String[]{"rest", "rest api", "api rest"});
        variants.put("Microservices", new String[]{"microservices", "micro-service", "micro service"});
        variants.put("MySQL", new String[]{"mysql"});
        variants.put("PostgreSQL", new String[]{"postgres", "postgresql"});

        Set<String> found = new LinkedHashSet<>();

        // 1) chercher les variantes (plus intelligent)
        for (Map.Entry<String, String[]> e : variants.entrySet()) {
            for (String v : e.getValue()) {
                if (containsWord(text, normalize(v))) {
                    found.add(e.getKey());
                    break;
                }
            }
        }

        // 2) chercher la liste skills.txt
        for (String skill : skillsList) {
            String s = normalize(skill);
            if (containsWord(text, s)) found.add(skill);
        }

        return new ArrayList<>(found);
    }

    private String normalize(String s) {
        if (s == null) return "";
        String x = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        x = x.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#.\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return x;
    }

    private boolean containsWord(String text, String word) {
        return (" " + text + " ").contains(" " + word + " ");
    }
}