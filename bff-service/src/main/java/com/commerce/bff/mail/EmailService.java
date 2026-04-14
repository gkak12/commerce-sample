package com.commerce.bff.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 이메일 발송 서비스 (HTML 템플릿 인라인 구성)
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");

    private final JavaMailSender mailSender;

    /**
     * 주문 완료 메일
     */
    public void sendOrderCompletedMail(String to, String orderId, BigDecimal totalAmount, LocalDateTime completedAt) {
        String subject = "[Commerce] 주문이 완료되었습니다 - " + orderId;
        String content = buildOrderCompletedHtml(orderId, totalAmount, completedAt);
        send(to, subject, content);
    }

    /**
     * 주문 취소 메일
     */
    public void sendOrderCancelledMail(String to, String orderId, String reason) {
        String subject = "[Commerce] 주문이 취소되었습니다 - " + orderId;
        String content = buildOrderCancelledHtml(orderId, reason);
        send(to, subject, content);
    }

    // ── private ──────────────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("[Mail] 발송 완료. to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("[Mail] 발송 실패. to={}, subject={}, error={}", to, subject, e.getMessage());
        }
    }

    private String buildOrderCompletedHtml(String orderId, BigDecimal totalAmount, LocalDateTime completedAt) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #4CAF50;">✅ 주문이 완료되었습니다</h2>
                    <hr style="border: 1px solid #eee;">
                    <table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
                        <tr>
                            <td style="padding: 10px; color: #666;">주문 번호</td>
                            <td style="padding: 10px; font-weight: bold;">%s</td>
                        </tr>
                        <tr style="background-color: #f9f9f9;">
                            <td style="padding: 10px; color: #666;">결제 금액</td>
                            <td style="padding: 10px; font-weight: bold; color: #4CAF50;">%s원</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; color: #666;">완료 일시</td>
                            <td style="padding: 10px;">%s</td>
                        </tr>
                    </table>
                    <p style="color: #888; font-size: 12px;">본 메일은 발신 전용입니다.</p>
                </div>
                """.formatted(
                orderId,
                String.format("%,d", totalAmount.longValue()),
                completedAt.format(FORMATTER)
        );
    }

    private String buildOrderCancelledHtml(String orderId, String reason) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #f44336;">❌ 주문이 취소되었습니다</h2>
                    <hr style="border: 1px solid #eee;">
                    <table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
                        <tr>
                            <td style="padding: 10px; color: #666;">주문 번호</td>
                            <td style="padding: 10px; font-weight: bold;">%s</td>
                        </tr>
                        <tr style="background-color: #f9f9f9;">
                            <td style="padding: 10px; color: #666;">취소 사유</td>
                            <td style="padding: 10px; color: #f44336;">%s</td>
                        </tr>
                    </table>
                    <p style="color: #888; font-size: 12px;">결제가 진행되지 않았습니다. 문의 사항은 고객센터를 이용해 주세요.</p>
                </div>
                """.formatted(orderId, reason);
    }
}
