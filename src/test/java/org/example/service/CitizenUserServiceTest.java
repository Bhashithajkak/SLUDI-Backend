package org.example.service;

import org.example.dto.*;
import org.example.entity.CitizenUser;
import org.example.entity.SupportingDocument;
import org.example.enums.UserStatus;
import org.example.enums.VerificationStatus;
import org.example.exception.SludiException;
import org.example.integration.IPFSIntegration;
import org.example.repository.AuthenticationLogRepository;
import org.example.repository.CitizenUserRepository;
import org.example.repository.IPFSContentRepository;
import org.example.utils.CitizenCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitizenUserServiceTest {

    @Mock
    private IPFSIntegration ipfsIntegration;
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private CitizenCodeGenerator citizenCodeGenerator;
    @Mock
    private CitizenUserRepository citizenUserRepository;
    @Mock
    private AuthenticationLogRepository authLogRepository;
    @Mock
    private IPFSContentRepository ipfsContentRepository;

    @InjectMocks
    private CitizenUserService citizenUserService;

    private CitizenUserRegistrationRequestDto registrationRequest;
    private CitizenUser mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        AddressDto addressDto = AddressDto.builder()
                .street("123 Main St")
                .city("Colombo")
                .district("Colombo")
                .province("Western")
                .postalCode("00100")
                .build();

        MockMultipartFile mockPhoto = new MockMultipartFile(
                "profilePhoto", "photo.jpg", "image/jpeg", "photo content".getBytes()
        );

        PersonalInfoDto personalInfo = PersonalInfoDto.builder()
                .nic("199012345678")
                .fullName("John Doe")
                .address(addressDto)
                .profilePhoto(mockPhoto)
                .build();

        ContactInfoDto contactInfo = ContactInfoDto.builder()
                .email("john@example.com")
                .phone("+94771234567")
                .build();

        registrationRequest = CitizenUserRegistrationRequestDto.builder()
                .personalInfo(personalInfo)
                .contactInfo(contactInfo)
                .build();

        mockUser = CitizenUser.builder()
                .id(userId)
                .nic("199012345678")
                .email("john@example.com")
                .status(UserStatus.ACTIVE)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
    }

    @Test
    void registerCitizenUser_Success() throws Exception {
        when(citizenUserRepository.existsByNicHash(anyString())).thenReturn(false);
        when(citizenUserRepository.existsByEmailHash(anyString())).thenReturn(false);
        when(citizenCodeGenerator.generateCitizenCode()).thenReturn("CIT-1234");
        when(ipfsIntegration.storeFile(anyString(), any(byte[].class))).thenReturn("QmHash123");
        when(citizenUserRepository.save(any(CitizenUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CitizenUserRegistrationResponseDto response = citizenUserService.registerCitizenUser(registrationRequest);

        assertNotNull(response);
        assertEquals("CIT-1234", response.getCitizenCode());
        assertEquals("User registered successfully", response.getMessage());

        verify(citizenUserRepository).save(any(CitizenUser.class));
        verify(ipfsIntegration).storeFile(anyString(), any(byte[].class));
        verify(authLogRepository).save(any());
    }

    @Test
    void registerCitizenUser_MissingNic() {
        registrationRequest.getPersonalInfo().setNic(null);

        assertThrows(SludiException.class, () -> citizenUserService.registerCitizenUser(registrationRequest));
    }

    @Test
    void getUserProfile_Success() {
        GetCitizenUserProfileRequestDto request = new GetCitizenUserProfileRequestDto();
        request.setId(userId);

        when(citizenUserRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        GetCitizenUserProfileResponseDto response = citizenUserService.getUserProfile(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("199012345678", response.getNic());
        assertEquals("john@example.com", response.getEmail());

        verify(authLogRepository).save(any());
    }

    @Test
    void getUserProfile_UserNotFound() {
        GetCitizenUserProfileRequestDto request = new GetCitizenUserProfileRequestDto();
        request.setId(userId);

        when(citizenUserRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(SludiException.class, () -> citizenUserService.getUserProfile(request));
    }
}
