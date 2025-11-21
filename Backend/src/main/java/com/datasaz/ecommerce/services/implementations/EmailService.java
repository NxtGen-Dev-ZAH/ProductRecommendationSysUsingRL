package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.repositories.entities.ReturnStatus;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;


    @Override
    @Retryable(
            value = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void sendResetEmail(String toEmail, String resetToken) {
        log.info("sendResetEmail by {} and {}",toEmail,resetToken);
        String subject = "Password Reset Email";
        String body = "Clicke on the following link in order to re-initialize the Password: " +
                "https://www.shopora.fr/reset-password?token=" + resetToken;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void sendActivationEmail(String email, String activationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Activate Your Account");
            String activationUrl = "https://api.shopora.fr/ecommerce/auth/activate-account?token=" + activationToken;
            helper.setText("Dear User,\n\nPlease activate your account by clicking the following link:\n" + activationUrl + "\n\nIf you did not register, please ignore this email.\n\nBest regards,\nE-Commerce Team", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send activation email to " + email, e);
        }
    }

    @Override
    public void sendBlockNotification(String email, String reason, LocalDateTime blockDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Account Blocked");
            helper.setText("Dear User,\n\nYour account was blocked on " + blockDate + " for the following reason: " + reason + ".\n\nPlease contact support for assistance.\n\nBest regards,\nE-Commerce Team", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send block notification to " + email, e);
        }
    }

    @Override
    public void sendUnblockNotification(String email, LocalDateTime unblockDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Account Unblocked");
            helper.setText("Dear User,\n\nYour account was unblocked on " + unblockDate + ". You can now log in to your account.\n\nBest regards,\nE-Commerce Team", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send unblock notification to " + email, e);
        }
    }

    @Override
    @Retryable(
            value = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void sendInvoiceEmail(String toEmail, File invoiceFile) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(toEmail);
        helper.setSubject("Your Invoice");
        helper.setText("Please find the attached invoice.");
        helper.addAttachment("invoice.pdf", invoiceFile);

        mailSender.send(message);
    }

    @Override
    @Async
    @Retryable(
            value = MessagingException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void sendRoleChangeNotification(String toEmail, String roleName, String action, LocalDateTime timestamp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject("Role Change Notification");
            String actionText = "ASSIGN_ROLE".equals(action) ? "assigned to" : "removed from";
            String htmlContent = String.format(
                    "<h2>Role Change Notification</h2>" +
                            "<p>Dear User,</p>" +
                            "<p>Your account role has been updated. The role <strong>%s</strong> was %s your account on %s.</p>" +
                            "<p>If you have any questions, please contact our support team.</p>" +
                            "<p>Best regards,<br></p>",
                    roleName, actionText, timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("EmailService: Sent role change notification to {} for role {} with action {}", toEmail, roleName, action);
        } catch (MessagingException e) {
            log.error("EmailService: Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendUserDeletionNotification(String email, LocalDateTime timestamp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Account Deletion Notification");
            String htmlContent = String.format(
                    "<h1>Account Deletion Notification</h1>" +
                            "<p>Your account has been deleted on %s.</p>" +
                            "<p>Please contact support if you believe this is an error.</p>",
                    timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Sent deletion notification to {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send deletion notification to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendUserRestorationNotification(String email, LocalDateTime timestamp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Account Restoration Notification");
            String htmlContent = String.format(
                    "<h1>Account Restoration Notification</h1>" +
                            "<p>Your account has been restored on %s.</p>" +
                            "<p>You can now log in to your account.</p>",
                    timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Sent restoration notification to {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send restoration notification to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendDeletionConfirmationEmail(String email, String deletionToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Confirm Account Deletion");
            String deletionUrl = "https://api.shopora.fr/ecommerce/buyer/user/confirm-delete-account?deletionToken=" + deletionToken;
            String htmlContent = String.format(
                    "<h1>Confirm Account Deletion</h1>" +
                            "<p>You have requested to delete your account. Please use the following token to confirm:</p>" +
                            "<p><strong>%s</strong></p>" +
                            "<p>or click on the link : " + deletionUrl + " </p>" +
                            "<p>This token is valid for 24 hours. If you did not request this, please contact support.</p>",
                    deletionToken);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Sent deletion confirmation email to {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send deletion confirmation email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendEmailChangeVerification(String email, String activationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Confirm Email Change Verification");
            String activationUrl = "https://api.shopora.fr/ecommerce/auth/activate-account?token=" + activationToken;
            helper.setText("Dear User,\n\nPlease activate your account by clicking the following link:\n" + activationUrl + "\n\nIf you did not register, please ignore this email.\n\nBest regards,\nE-Commerce Team", true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send activation email to " + email, e);
        }
    }


    public void sendEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
    }


    //@Override
    public void sendInvoiceEmailVdel(String recipient, File invoiceFile) {
        log.info("Sending invoice email to {}", recipient);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(recipient);
            helper.setSubject("Your Order Invoice");
            helper.setText(
                    "<h2>Thank You for Your Purchase!</h2>" +
                            "<p>Your order has been successfully processed. Please find the invoice attached.</p>" +
                            "<p>If you have any questions, contact our support team.</p>",
                    true
            );
            helper.addAttachment(invoiceFile.getName(), invoiceFile);
            mailSender.send(message);
            log.info("Invoice email sent to {}", recipient);
        } catch (MessagingException e) {
            log.error("Failed to send invoice email to {}: {}", recipient, e.getMessage());
            throw new RuntimeException("Failed to send invoice email: " + e.getMessage());
        }
    }

    @Override
    public void sendTrackingUpdateEmail(String recipient, Long orderId, String trackingNumber, String carrierStatus) {
        log.info("Sending tracking update email to {} for order {}", recipient, orderId);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(recipient);
            helper.setSubject("Shipping Update for Order #" + orderId);
            helper.setText(
                    "<h2>Shipping Update</h2>" +
                            "<p>Your order #" + orderId + " has a new shipping status.</p>" +
                            "<p><strong>Tracking Number:</strong> " + trackingNumber + "</p>" +
                            "<p><strong>Carrier Status:</strong> " + carrierStatus + "</p>" +
                            "<p>Track your order at the carrier's website or contact our support for assistance.</p>",
                    true
            );
            mailSender.send(message);
            log.info("Tracking update email sent to {} for order {}", recipient, orderId);
        } catch (MessagingException e) {
            log.error("Failed to send tracking update email to {} for order {}: {}", recipient, orderId, e.getMessage());
            throw new RuntimeException("Failed to send tracking update email: " + e.getMessage());
        }
    }

    @Override
    public void sendReturnRequestEmail(String recipient, Long orderId, Long returnRequestId, String reason) {
        log.info("Sending return request email to {} for order {} and return request {}", recipient, orderId, returnRequestId);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(recipient);
            helper.setSubject("Return Request Confirmation for Order #" + orderId);
            helper.setText(
                    "<h2>Return Request Submitted</h2>" +
                            "<p>Your return request for order #" + orderId + " has been received.</p>" +
                            "<p><strong>Return Request ID:</strong> " + returnRequestId + "</p>" +
                            "<p><strong>Reason:</strong> " + reason + "</p>" +
                            "<p>We will review your request and notify you of the status soon.</p>",
                    true
            );
            mailSender.send(message);
            log.info("Return request email sent to {} for order {}", recipient, orderId);
        } catch (MessagingException e) {
            log.error("Failed to send return request email to {} for order {}: {}", recipient, orderId, e.getMessage());
            throw new RuntimeException("Failed to send return request email: " + e.getMessage());
        }
    }

    @Override
    public void sendReturnStatusEmail(String recipient, Long orderId, Long returnRequestId, ReturnStatus status) {
        log.info("Sending return status email to {} for order {} and return request {}", recipient, orderId, returnRequestId);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(recipient);
            helper.setSubject("Return Request Update for Order #" + orderId);
            helper.setText(
                    "<h2>Return Request Update</h2>" +
                            "<p>Your return request #" + returnRequestId + " for order #" + orderId + " has been updated.</p>" +
                            "<p><strong>Status:</strong> " + status.name() + "</p>" +
                            "<p>Please contact our support team for further details.</p>",
                    true
            );
            mailSender.send(message);
            log.info("Return status email sent to {} for order {} with status {}", recipient, orderId, status);
        } catch (MessagingException e) {
            log.error("Failed to send return status email to {} for order {}: {}", recipient, orderId, e.getMessage());
            throw new RuntimeException("Failed to send return status email: " + e.getMessage());
        }
    }

    @Override
    public void sendRefundEmail(String recipient, File refundDocument, BigDecimal refundAmount) {
        log.info("Sending refund email to {} with refund amount {}", recipient, refundAmount);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(recipient);
            helper.setSubject("Refund Processed");
            helper.setText(
                    "<h2>Refund Processed</h2>" +
                            "<p>Your refund of " + refundAmount + " has been processed successfully.</p>" +
                            "<p>Please find the refund document attached.</p>" +
                            "<p>Contact our support team for any questions.</p>",
                    true
            );
            helper.addAttachment(refundDocument.getName(), refundDocument);
            mailSender.send(message);
            log.info("Refund email sent to {} with amount {}", recipient, refundAmount);
        } catch (MessagingException e) {
            log.error("Failed to send refund email to {}: {}", recipient, e.getMessage());
            throw new RuntimeException("Failed to send refund email: " + e.getMessage());
        }
    }

    @Override
    public void sendPasswordChangeNotification(String email, LocalDateTime timestamp) {
        log.info("Sending password change notification to {} at {}", email, timestamp);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Password Updated");
            helper.setText(
                    "<h2>Password updated</h2>" +
                            "<p>Your Password has been processed successfully.</p>" +
                            "<p>Contact our support team for any questions.</p>",
                    true
            );
            mailSender.send(message);
            log.info("Password update email sent to {}.", email);
        } catch (MessagingException e) {
            log.error("Failed to send password update email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send password update email: " + e.getMessage());
        }
    }

    @Override
    public void sendProfileUpdateNotification(String email, LocalDateTime timestamp) {
        log.info("Sending profile update notification to {} at {}", email, timestamp);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Profile Updated");
            helper.setText(
                    "<h2>Profile updated</h2>" +
                            "<p>Your Profile has been processed successfully.</p>" +
                            "<p>Contact our support team for any questions.</p>",
                    true
            );
            mailSender.send(message);
            log.info("Profile update email sent to {}.", email);
        } catch (MessagingException e) {
            log.error("Failed to send profile update email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send profile update email: " + e.getMessage());
        }
    }

    @Override
    public void sendWelcomeNotification(String email, LocalDateTime timestamp) {
        log.info("Sending welcome notification to {} at {}", email, timestamp);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Welcome Notification");
            String htmlContent = String.format(
                    "<h1>Welcome Notification</h1>" +
                            "<p>You have been successfully registered at %s.</p>" +
                            "<p>You can now log in to your account.</p>",
                    timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Sent welcome notification to {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send welcome notification to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

}
