package com.subBike.server.security.services;

import com.subBike.server.entity.VerificationCode;
import com.subBike.server.mapper.VerificationCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class VerificationCodeService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeService.class);
    private static final int EXPIRY_TIME_IN_MINUTES = 10;
    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private VerificationCodeRepository codeRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Transactional
    public String generateAndSendCode(String email) {
        String code = String.format("%06d", random.nextInt(999999));

        // Invalidate previous code if exists
        codeRepository.deleteByEmail(email);
        
        VerificationCode verificationCode = new VerificationCode(email, code, EXPIRY_TIME_IN_MINUTES);
        codeRepository.save(verificationCode);

        // Send email
        sendEmail(email, "Your Verification Code", "Your verification code is: " + code);

        return code; // In a real app, we might not return the code
    }

    public boolean validateCode(String email, String code) {
        Optional<VerificationCode> verificationCodeOpt = codeRepository.findByEmail(email);

        if (verificationCodeOpt.isPresent()) {
            VerificationCode verificationCode = verificationCodeOpt.get();
            if (verificationCode.getExpiryDate().isAfter(LocalDateTime.now()) && verificationCode.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
    
    @Transactional
    public void invalidateCode(String email) {
        codeRepository.deleteByEmail(email);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            logger.info("Attempting to send email to: {}", to);
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);

        } catch (MailException e) {
            logger.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
            throw e; // Propagate exception to be handled by the controller
        }
    }
} 