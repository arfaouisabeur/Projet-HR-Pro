package edu.RhPro.services;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private final String username;
    private final String password;
    private final String host;
    private final int port;

    public EmailService() {
        this.username = "Nayssenk@gmail.com";
        this.password = "trji cybt rqbf tnxw";
        this.host     = "smtp.gmail.com";
        this.port     = 587;
    }

    public void sendEmail(String to, String subject, String htmlContent)
            throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.host",              host);
        props.put("mail.smtp.port",              String.valueOf(port));
        props.put("mail.smtp.auth",              "true");
        props.put("mail.smtp.starttls.enable",   "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust",         "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols",     "TLSv1.2");
        props.put("mail.smtp.connectiontimeout", "30000");
        props.put("mail.smtp.timeout",           "30000");
        props.put("mail.smtp.writetimeout",      "30000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(username, "RH Pro", "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(username));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, "UTF-8");
        MimeMultipart multipart = new MimeMultipart("alternative");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(htmlContent.replaceAll("<[^>]+>", ""), "UTF-8");
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlContent, "text/html; charset=UTF-8");
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);
        message.setContent(multipart);

        Transport.send(message);
        System.out.println("Email envoye a : " + to);
    }

    public void sendEmailAsync(String to, String subject, String htmlContent) {
        new Thread(() -> {
            try {
                sendEmail(to, subject, htmlContent);
            } catch (MessagingException e) {
                System.err.println("Erreur envoi email a " + to + " : " + e.getMessage());
            }
        }, "email-sender").start();
    }

    public void sendTaskAssignedEmail(String employeeEmail, String employeeName,
                                      String taskTitle, String projectName) {
        sendEmailAsync(employeeEmail, "Nouvelle tache assignee - " + taskTitle,
                buildTaskEmail(employeeName, taskTitle, projectName));
    }

    public void sendProjectAssignedEmail(String employeeEmail, String employeeName,
                                         String projectTitle, String role) {
        sendEmailAsync(employeeEmail, "Nouveau projet assigne - " + projectTitle,
                buildProjectEmail(employeeName, role, projectTitle));
    }

    public void sendCongeDecisionEmail(String employeeEmail, String employeeName,
                                       String decision, String commentaire) {
        boolean accepted = "ACCEPTEE".equalsIgnoreCase(decision);
        String subject = accepted ? "Votre conge a ete accepte" : "Votre conge a ete refuse";
        sendEmailAsync(employeeEmail, subject,
                buildCongeEmail(employeeName, decision, commentaire, accepted));
    }

    public void sendDeadlineEmail(String email, String nom, String projetTitre,
                                  java.time.LocalDate dateFin, String cas) {
        String subject, badgeColor, badgeText, message;
        switch (cas) {
            case "today" -> {
                subject = "Projet [" + projetTitre + "] - Echeance aujourd\'hui";
                badgeColor = "#f59e0b"; badgeText = "ECHEANCE AUJOURD\'HUI";
                message = "La date de fin est aujourd\'hui.";
            }
            case "3days" -> {
                subject = "Projet [" + projetTitre + "] - Plus que 3 jours";
                badgeColor = "#6B2D67"; badgeText = "3 JOURS RESTANTS";
                message = "Il vous reste 3 jours.";
            }
            default -> {
                subject = "Projet [" + projetTitre + "] - Date depassee";
                badgeColor = "#ef4444"; badgeText = "DATE DEPASSEE";
                message = "La date de fin est depassee.";
            }
        }
        String dateStr = dateFin != null
                ? dateFin.format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH))
                : "Non definie";
        String html = "<!DOCTYPE html><html><head><meta charset=\'UTF-8\'></head>"
                + "<body style=\'margin:0;padding:0;background:#f4f4f4;font-family:Arial,sans-serif;\'>"
                + "<div style=\'max-width:560px;margin:32px auto;background:white;border-radius:16px;overflow:hidden;\'>"
                + "<div style=\'background:linear-gradient(135deg,#5A2A80,#3B1A56);padding:28px 32px;text-align:center;\'>"
                + "<h1 style=\'color:white;margin:0;font-size:20px;\'>Rappel Projet</h1></div>"
                + "<div style=\'padding:32px;\'>"
                + "<p>Bonjour <strong>" + nom + "</strong>,</p>"
                + "<div style=\'border:2px solid " + badgeColor + ";border-radius:12px;padding:14px;text-align:center;margin:20px 0;\'>"
                + "<p style=\'margin:0;color:" + badgeColor + ";font-weight:700;\'>" + badgeText + "</p></div>"
                + "<p><strong>Projet :</strong> " + projetTitre + "</p>"
                + "<p><strong>Date de fin :</strong> " + dateStr + "</p>"
                + "<p>" + message + "</p>"
                + "</div></div></body></html>";
        sendEmailAsync(email, subject, html);
    }

    private String buildTaskEmail(String name, String task, String project) {
        return "<html><body><p>Bonjour <strong>" + name + "</strong>,</p>"
                + "<p>Tache assignee : <strong>" + task + "</strong></p>"
                + "<p>Projet : <strong>" + project + "</strong></p></body></html>";
    }

    private String buildProjectEmail(String name, String role, String project) {
        return "<html><body><p>Bonjour <strong>" + name + "</strong>,</p>"
                + "<p>Role : <strong>" + role + "</strong></p>"
                + "<p>Projet : <strong>" + project + "</strong></p></body></html>";
    }

    private String buildCongeEmail(String name, String decision, String commentaire, boolean accepted) {
        String color = accepted ? "#10b981" : "#ef4444";
        String label = accepted ? "ACCEPTEE" : "REFUSEE";
        return "<html><body><p>Bonjour <strong>" + name + "</strong>,</p>"
                + "<p style=\'color:" + color + ";font-weight:bold;\'>Demande " + label + "</p>"
                + (commentaire != null && !commentaire.isBlank() ? "<p>Commentaire : " + commentaire + "</p>" : "")
                + "</body></html>";
    }




}