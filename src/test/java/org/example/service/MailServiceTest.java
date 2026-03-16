package org.example.service;

import jakarta.mail.internet.MimeMessage;
import org.example.entity.Organization;
import org.example.entity.OrganizationRole;
import org.example.entity.OrganizationUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private MailService mailService;

    private OrganizationUser mockUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "emailEnabled", true);
        ReflectionTestUtils.setField(mailService, "senderEmail", "noreply@test.com");
        ReflectionTestUtils.setField(mailService, "baseUrl", "http://localhost:8080");

        Organization mockOrg = new Organization();
        mockOrg.setName("Test Org");

        OrganizationRole mockRole = new OrganizationRole();
        mockRole.setRoleCode("ADMIN");
        mockRole.setPermissions(Collections.singletonList("ALL"));

        mockUser = OrganizationUser.builder()
                .email("test@domain.com")
                .firstName("John")
                .lastName("Doe")
                .username("johndoe")
                .employeeId("EMP123")
                .assignedRole(mockRole)
                .organization(mockOrg)
                .build();
    }

    @Test
    void sendOtpEmail_Success() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("otp-mail"), any(Context.class))).thenReturn("<html>OTP</html>");

        mailService.sendOtpEmail("test@domain.com", "John Doe", "123456");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendAppointmentEmail_Success() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("appointment-mail"), any(Context.class))).thenReturn("<html>Appointment</html>");

        mailService.sendAppointmentEmail("test@domain.com", "John Doe", "Colombo", "2026-04-01 10:00", "REF123");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendUserVerificationEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("user-verification-mail"), any(Context.class))).thenReturn("<html>Verification</html>");

        mailService.sendUserVerificationEmail(mockUser);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendUserVerificationEmail_Disabled() {
        ReflectionTestUtils.setField(mailService, "emailEnabled", false);

        mailService.sendUserVerificationEmail(mockUser);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendUserActivationEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("user-activation-mail"), any(Context.class))).thenReturn("<html>Activation</html>");

        mailService.sendUserActivationEmail(mockUser);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendUserSuspensionEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("user-suspension-mail"), any(Context.class))).thenReturn("<html>Suspension</html>");

        mailService.sendUserSuspensionEmail(mockUser, "Policy Violation");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendUserReactivationEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("user-reactivation-mail"), any(Context.class))).thenReturn("<html>Reactivation</html>");

        mailService.sendUserReactivationEmail(mockUser);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("password-reset-mail"), any(Context.class))).thenReturn("<html>Reset</html>");

        mailService.sendPasswordResetEmail(mockUser, "newPass123");

        verify(mailSender).send(mimeMessage);
    }
}
