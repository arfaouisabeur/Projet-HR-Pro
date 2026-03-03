package edu.RhPro.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.*;

public class EmailService {

    private static final String FROM_EMAIL = "boudour.zlaoui24@gmail.com";
    private static final String APP_PASSWORD = "podzgjpqnpzwkddf";

    private static final Map<String, String> otpStorage = new HashMap<>();
    private static final Map<String, Long> otpExpiry = new HashMap<>();

    private static final int OTP_VALIDITY = 5 * 60 * 1000; // 5 minutes

    private static jakarta.mail.Session getMailSession(){

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return jakarta.mail.Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }
    public static long getRemainingSeconds(String email) {

        if (!otpExpiry.containsKey(email)) return 0;

        long remaining = otpExpiry.get(email) - System.currentTimeMillis();

        return remaining > 0 ? remaining / 1000 : 0;
    }


    /* ==========================
        ENVOYER OTP
    =========================== */
    public static void sendOtp(String email) throws Exception {

        String otp = String.format("%06d", new Random().nextInt(999999));

        otpStorage.put(email, otp);
        otpExpiry.put(email, System.currentTimeMillis() + OTP_VALIDITY);

        Message message = new MimeMessage(getMailSession());
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(email));

        message.setSubject("RHPro - Code de réinitialisation");

        message.setText(
                "Votre code OTP est : " + otp +
                        "\nValide pendant 5 minutes."
        );

        Transport.send(message);

        System.out.println("OTP envoyé à : " + email);
    }

    /* ==========================
        VERIFIER OTP
    =========================== */
    public static boolean verifyOtp(String email, String inputOtp) {

        if (!otpStorage.containsKey(email)) return false;

        if (System.currentTimeMillis() > otpExpiry.get(email)) {
            otpStorage.remove(email);
            otpExpiry.remove(email);
            return false;
        }

        return otpStorage.get(email).equals(inputOtp);
    }

    public static void clearOtp(String email) {
        otpStorage.remove(email);
        otpExpiry.remove(email);
    }
}
