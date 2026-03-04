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
        // Configure with your email settings
        this.username = "Nayssenk@gmail.com";
        this.password = "hrha oalw ezzl qizs"; // Use App Password for Gmail
        this.host = "smtp.gmail.com";
        this.port = 587;
    }

    public void sendEmail(String to, String subject, String content) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(content);

        Transport.send(message);
        System.out.println("✅ Email sent to: " + to);
    }

    public void sendTaskAssignedEmail(String employeeEmail, String employeeName,
                                      String taskTitle, String projectName) {
        String subject = "🔔 Nouvelle tâche assignée";
        String content = String.format(
                "Bonjour %s,\n\n" +
                        "Une nouvelle tâche vous a été assignée :\n\n" +
                        "📋 Tâche: %s\n" +
                        "📁 Projet: %s\n\n" +
                        "Connectez-vous à l'application pour plus de détails.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe RH Pro",
                employeeName, taskTitle, projectName
        );

        try {
            sendEmail(employeeEmail, subject, content);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
        }
    }

    public void sendProjectAssignedEmail(String employeeEmail, String employeeName,
                                         String projectTitle, String role) {
        String subject = "📊 Nouveau projet assigné";
        String content = String.format(
                "Bonjour %s,\n\n" +
                        "Vous avez été assigné comme %s au projet :\n\n" +
                        "📁 Projet: %s\n\n" +
                        "Connectez-vous à l'application pour voir les détails.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe RH Pro",
                employeeName, role, projectTitle
        );

        try {
            sendEmail(employeeEmail, subject, content);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
        }
    }
}
