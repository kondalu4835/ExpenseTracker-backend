package com.expensetracker.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Store OTPs temporarily in memory
    private final Map<String, String> otpStore = new HashMap<>();
    private final Map<String, Long> otpExpiry = new HashMap<>();

    public void sendOTP(String toEmail) {
        // Generate 6 digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Store OTP with 5 minute expiry
        otpStore.put(toEmail, otp);
        otpExpiry.put(toEmail, System.currentTimeMillis() + 5 * 60 * 1000);

        // Send email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("ExpenseTracker - Your OTP Verification Code");
        message.setText(
            "Hello!\n\n" +
            "Your OTP for ExpenseTracker registration is:\n\n" +
            "🔐 " + otp + "\n\n" +
            "This OTP is valid for 5 minutes only.\n" +
            "Do not share this OTP with anyone.\n\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "Thanks,\nExpenseTracker Team"
        );

        mailSender.send(message);
        System.out.println("OTP sent to: " + toEmail + " | OTP: " + otp);
    }

    public boolean verifyOTP(String email, String otp) {
        String storedOTP = otpStore.get(email);
        Long expiry = otpExpiry.get(email);

        if (storedOTP == null || expiry == null) {
            return false;
        }

        if (System.currentTimeMillis() > expiry) {
            otpStore.remove(email);
            otpExpiry.remove(email);
            return false;
        }

        if (storedOTP.equals(otp)) {
            otpStore.remove(email);
            otpExpiry.remove(email);
            return true;
        }

        return false;
    }
}