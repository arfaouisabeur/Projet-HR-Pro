package edu.RhPro.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsService {

    // 🔐 Remplace par TES infos Twilio
    private static final String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String String authToken = System.getenv("TWILIO_AUTH_TOKEN");
    private static final String FROM_NUMBER = "+13639992046";

    static {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    public static void sendSms(String to, String text) {

        Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(FROM_NUMBER),
                text
        ).create();
    }
}