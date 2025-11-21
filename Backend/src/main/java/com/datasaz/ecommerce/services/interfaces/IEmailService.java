package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.repositories.entities.ReturnStatus;
import jakarta.mail.MessagingException;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IEmailService {

    void sendResetEmail(String toEmail, String resetToken);
    void sendInvoiceEmail(String toEmail, File invoiceFile) throws MessagingException;
    void sendRoleChangeNotification(String toEmail, String roleName, String action, LocalDateTime timestamp);
    void sendUserDeletionNotification(String email, LocalDateTime timestamp);
    void sendUserRestorationNotification(String email, LocalDateTime timestamp);

    void sendDeletionConfirmationEmail(String email, String deletionToken);

    void sendEmailChangeVerification(String email, String activationToken);
    void sendEmail(String to, String subject, String content) throws MessagingException;

    //void sendInvoiceEmail(String recipient, File invoiceFile);
    void sendTrackingUpdateEmail(String recipient, Long orderId, String trackingNumber, String carrierStatus);

    void sendReturnRequestEmail(String recipient, Long orderId, Long returnRequestId, String reason);
    void sendReturnStatusEmail(String recipient, Long orderId, Long returnRequestId, ReturnStatus status);
    void sendRefundEmail(String recipient, File refundDocument, BigDecimal refundAmount);

    void sendPasswordChangeNotification(String email, LocalDateTime timestamp);
    void sendProfileUpdateNotification(String email, LocalDateTime timestamp);

    void sendWelcomeNotification(String email, LocalDateTime timestamp);
    //void sendUserRestorationNotification(String email, LocalDateTime timestamp);

    void sendActivationEmail(String email, String activationToken);

    void sendBlockNotification(String email, String reason, LocalDateTime blockDate);

    void sendUnblockNotification(String email, LocalDateTime unblockDate);
}
