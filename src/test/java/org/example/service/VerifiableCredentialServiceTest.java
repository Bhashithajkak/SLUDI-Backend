package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.DrivingLicenseRequestResponseDto;
import org.example.dto.PresentationRequestDto;
import org.example.dto.VCBlockChainResult;
import org.example.dto.VerifiableCredentialDto;
import org.example.entity.Organization;
import org.example.entity.OrganizationUser;
import org.example.entity.PresentationRequest;
import org.example.enums.PresentationStatus;
import org.example.enums.UserStatus;
import org.example.exception.SludiException;
import org.example.repository.OrganizationUserRepository;
import org.example.repository.PresentationRequestRepository;
import org.example.security.CryptographyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifiableCredentialServiceTest {

    @Mock
    private HyperledgerService hyperledgerService;
    @Mock
    private CryptographyService cryptographyService;
    @Mock
    private OrganizationUserService organizationUserService;
    @Mock
    private OrganizationUserRepository organizationUserRepository;
    @Mock
    private PresentationRequestRepository presentationRequestRepository;
    @Mock
    private QRCodeService qrCodeService;

    @InjectMocks
    private VerifiableCredentialService verifiableCredentialService;

    private OrganizationUser officer;
    private PresentationRequest presentationRequest;

    @BeforeEach
    void setUp() {
        Organization mockOrg = new Organization();
        mockOrg.setOrgCode("DMT");

        officer = OrganizationUser.builder()
                .username("officer1")
                .status(UserStatus.ACTIVE)
                .organization(mockOrg)
                .build();

        presentationRequest = PresentationRequest.builder()
                .sessionId("session-123")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .status(PresentationStatus.PENDING.name())
                .build();
    }

    @Test
    void getVerifiableCredential_Success() throws Exception {
        VCBlockChainResult mockResult = VCBlockChainResult.builder()
                .id("credential:123")
                .credentialSubjectHash("encryptedHash")
                .build();

        when(hyperledgerService.readCredential("credential:123")).thenReturn(mockResult);
        when(cryptographyService.decryptData("encryptedHash")).thenReturn("{\"id\":\"did:subject:123\"}");

        VerifiableCredentialDto response = verifiableCredentialService.getVerifiableCredential("credential:123");

        assertNotNull(response);
        assertEquals("credential:123", response.getId());
    }

    @Test
    void getVerifiableCredential_NullId() {
        assertThrows(SludiException.class, () -> verifiableCredentialService.getVerifiableCredential(null));
    }

    @Test
    void getVerifiableCredential_NotFound() throws Exception {
        when(hyperledgerService.readCredential("credential:123")).thenReturn(null);
        
        assertThrows(SludiException.class, () -> verifiableCredentialService.getVerifiableCredential("credential:123"));
    }

    @Test
    void initiateDrivingLicenseRequest_Success() {
        when(organizationUserRepository.findByUsername("officer1")).thenReturn(Optional.of(officer));
        when(organizationUserService.verifyUserPermission("officer1", "license:request_citizen_data")).thenReturn(true);
        when(qrCodeService.generateQRCode(anyString(), anyInt(), anyInt())).thenReturn("mockQRCodeImage".getBytes());

        DrivingLicenseRequestResponseDto response = verifiableCredentialService.initiateDrivingLicenseRequest("officer1");

        assertNotNull(response);
        assertNotNull(response.getSessionId());
        assertNotNull(response.getQrCode());
    }

    @Test
    void initiateDrivingLicenseRequest_UserNotFound() {
        when(organizationUserRepository.findByUsername("officer1")).thenReturn(Optional.empty());

        assertThrows(SludiException.class, () -> verifiableCredentialService.initiateDrivingLicenseRequest("officer1"));
    }

    @Test
    void initiateDrivingLicenseRequest_NoPermission() {
        when(organizationUserRepository.findByUsername("officer1")).thenReturn(Optional.of(officer));
        when(organizationUserService.verifyUserPermission("officer1", "license:request_citizen_data")).thenReturn(false);

        assertThrows(SludiException.class, () -> verifiableCredentialService.initiateDrivingLicenseRequest("officer1"));
    }

    @Test
    void getPresentationRequest_Success() {
        when(presentationRequestRepository.findBySessionId("session-123")).thenReturn(Optional.of(presentationRequest));

        PresentationRequestDto response = verifiableCredentialService.getPresentationRequest("session-123");

        assertNotNull(response);
        assertEquals("session-123", response.getSessionId());
    }

    @Test
    void getPresentationRequest_Expired() {
        presentationRequest.setExpiresAt(LocalDateTime.now().minusMinutes(15));
        when(presentationRequestRepository.findBySessionId("session-123")).thenReturn(Optional.of(presentationRequest));

        assertThrows(SludiException.class, () -> verifiableCredentialService.getPresentationRequest("session-123"));
    }
}
