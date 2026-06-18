package com.orderflow.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    @Value("${notification.from-email:noreply@orderflow.local}")
    private String fromEmail;

    @Value("${notification.from-name:OrderFlow Notifications}")
    private String fromName;

    public void sendOrderPlacedNotification(UUID orderId, UUID customerId, BigDecimal totalAmount, String currency) {
        String subject = "Order Confirmation - Order #" + orderId;
        String body = buildOrderPlacedEmail(orderId, customerId, totalAmount, currency);
        sendEmail("customer@example.com", subject, body);
    }

    public void sendPaymentSuccessNotification(UUID orderId, BigDecimal amount, String currency) {
        String subject = "Payment Confirmed - Order #" + orderId;
        String body = buildPaymentSuccessEmail(orderId, amount, currency);
        sendEmail("customer@example.com", subject, body);
    }

    public void sendPaymentFailedNotification(UUID orderId, String failureReason) {
        String subject = "Payment Failed - Order #" + orderId;
        String body = buildPaymentFailedEmail(orderId, failureReason);
        sendEmail("customer@example.com", subject, body);
    }

    public void sendShipmentCreatedNotification(UUID orderId, String trackingNumber, LocalDate estimatedDelivery) {
        String subject = "Your Order Has Shipped - Tracking #" + trackingNumber;
        String body = buildShipmentCreatedEmail(orderId, trackingNumber, estimatedDelivery);
        sendEmail("customer@example.com", subject, body);
    }

    public void sendShipmentDeliveredNotification(UUID orderId, String trackingNumber, LocalDate deliveryDate) {
        String subject = "Your Order Has Been Delivered - Order #" + orderId;
        String body = buildShipmentDeliveredEmail(orderId, trackingNumber, deliveryDate);
        sendEmail("customer@example.com", subject, body);
    }

    public void sendInventoryInsufficientNotification(UUID orderId) {
        String subject = "Order Cancelled - Out of Stock";
        String body = buildInventoryInsufficientEmail(orderId);
        sendEmail("customer@example.com", subject, body);
    }

    private void sendEmail(String toEmail, String subject, String body) {
        log.info("=".repeat(80));
        log.info("EMAIL NOTIFICATION");
        log.info("=".repeat(80));
        log.info("From: {} <{}>", fromName, fromEmail);
        log.info("To: {}", toEmail);
        log.info("Subject: {}", subject);
        log.info("-".repeat(80));
        log.info(body);
        log.info("=".repeat(80));
    }

    private String buildOrderPlacedEmail(UUID orderId, UUID customerId, BigDecimal totalAmount, String currency) {
        return String.format("""
                Dear Valued Customer,

                Thank you for your order! We're excited to prepare and ship your items.

                ORDER DETAILS:
                Order ID: %s
                Order Amount: %s %s
                Order Date: %s

                Your order has been received and is being processed. You will receive tracking information via email when your order ships.

                If you have any questions, please contact our customer support team.

                Best regards,
                OrderFlow Team
                """, orderId, totalAmount, currency, java.time.LocalDateTime.now());
    }

    private String buildPaymentSuccessEmail(UUID orderId, BigDecimal amount, String currency) {
        return String.format("""
                Dear Valued Customer,

                Great news! Your payment has been successfully processed.

                PAYMENT DETAILS:
                Order ID: %s
                Amount Paid: %s %s
                Payment Date: %s

                Your order will now be prepared for shipment. We'll send you tracking information soon.

                Thank you for your business!

                Best regards,
                OrderFlow Team
                """, orderId, amount, currency, java.time.LocalDateTime.now());
    }

    private String buildPaymentFailedEmail(UUID orderId, String failureReason) {
        return String.format("""
                Dear Valued Customer,

                We encountered an issue processing your payment.

                ORDER DETAILS:
                Order ID: %s
                Failure Reason: %s

                Please try updating your payment method and try again. If the issue persists, please contact our support team.

                Best regards,
                OrderFlow Team
                """, orderId, failureReason);
    }

    private String buildShipmentCreatedEmail(UUID orderId, String trackingNumber, LocalDate estimatedDelivery) {
        return String.format("""
                Dear Valued Customer,

                Your order is on its way! Here's your tracking information:

                SHIPMENT DETAILS:
                Order ID: %s
                Tracking Number: %s
                Carrier: Express Courier
                Estimated Delivery: %s

                Click the tracking number above to monitor your package in real-time.

                Thank you for shopping with us!

                Best regards,
                OrderFlow Team
                """, orderId, trackingNumber, estimatedDelivery);
    }

    private String buildShipmentDeliveredEmail(UUID orderId, String trackingNumber, LocalDate deliveryDate) {
        return String.format("""
                Dear Valued Customer,

                Your order has been successfully delivered!

                DELIVERY DETAILS:
                Order ID: %s
                Tracking Number: %s
                Delivery Date: %s

                Please inspect the package upon receipt. If you have any issues, please contact our support team immediately.

                We'd love your feedback! Please rate your shopping experience.

                Best regards,
                OrderFlow Team
                """, orderId, trackingNumber, deliveryDate);
    }

    private String buildInventoryInsufficientEmail(UUID orderId) {
        return String.format("""
                Dear Valued Customer,

                Unfortunately, we had to cancel your order due to inventory constraints.

                ORDER DETAILS:
                Order ID: %s

                We sincerely apologize for the inconvenience. Your payment has been refunded to your original payment method within 3-5 business days.

                We appreciate your understanding and look forward to serving you in the future.

                Best regards,
                OrderFlow Team
                """, orderId);
    }
}
