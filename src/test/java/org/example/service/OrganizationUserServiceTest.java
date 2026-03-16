package org.example.service;

import org.example.dto.OrganizationUserRequestDto;
import org.example.dto.OrganizationUserResponseDto;
import org.example.entity.Organization;
import org.example.entity.OrganizationRole;
import org.example.entity.OrganizationUser;
import org.example.enums.OrganizationStatus;
import org.example.enums.UserStatus;
import org.example.enums.VerificationStatus;
import org.example.exception.SludiException;
import org.example.repository.OrganizationOnboardingRepository;
import org.example.repository.OrganizationRepository;
import org.example.repository.OrganizationRoleRepository;
import org.example.repository.OrganizationUserRepository;
import org.example.security.OrganizationJwtService;
import org.example.utils.EmployeeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationUserServiceTest {

    @Mock
    private OrganizationUserRepository userRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationRoleRepository roleRepository;
    @Mock
    private OrganizationOnboardingRepository onboardingRepository;
    @Mock
    private HyperledgerService hyperledgerService;
    @Mock
    private FabricCAService fabricCAService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MailService emailService;
    @Mock
    private EmployeeIdGenerator employeeIdGenerator;
    @Mock
    private OrganizationJwtService jwtService;

    @InjectMocks
    private OrganizationUserService userService;

    private OrganizationUser adminUser;
    private Organization mockOrganization;
    private OrganizationRole mockRole;
    private OrganizationUserRequestDto requestDto;

    @BeforeEach
    void setUp() {
        mockOrganization = Organization.builder()
                .id(1L)
                .name("Test Org")
                .status(OrganizationStatus.ACTIVE)
                .build();

        mockRole = OrganizationRole.builder()
                .id(1L)
                .organization(mockOrganization)
                .roleCode("EMPLOYEE")
                .permissions(Collections.singletonList("ALL"))
                .isActive(true)
                .build();

        adminUser = OrganizationUser.builder()
                .id(1L)
                .username("admin_user")
                .status(UserStatus.ACTIVE)
                .assignedRole(mockRole)
                .build();

        requestDto = new OrganizationUserRequestDto();
        requestDto.setOrganizationId(1L);
        requestDto.setEmail("test@email.com");
        requestDto.setUsername("test_user");
        requestDto.setPassword("Password123");
        requestDto.setRoleId(1L);
        requestDto.setFirstName("Test");
        requestDto.setLastName("User");
    }

    @Test
    void registerEmployeeUser_Success() {
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(adminUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(mockOrganization));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(mockRole));
        when(employeeIdGenerator.generateCode(any(Organization.class))).thenReturn("EMP-001");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");

        OrganizationUser savedUser = OrganizationUser.builder()
                .id(2L)
                .username("test_user")
                .email("test@email.com")
                .organization(mockOrganization)
                .assignedRole(mockRole)
                .status(UserStatus.PENDING)
                .verificationStatus(VerificationStatus.NOT_STARTED)
                .employeeId("EMP-001")
                .build();

        when(userRepository.save(any(OrganizationUser.class))).thenReturn(savedUser);
        
        // Mock method mapToUserResponse indirectly by providing a mock return inside the service
        // However mapToUserResponse needs valid data inside savedUser.

        OrganizationUserResponseDto response = userService.registerEmployeeUser(requestDto, "admin_user");

        assertNotNull(response);
        assertEquals("test_user", response.getUsername());
        assertEquals("test@email.com", response.getEmail());
        assertEquals(UserStatus.PENDING.name(), response.getStatus());
        assertEquals("EMP-001", response.getEmployeeId());

        verify(emailService).sendUserVerificationEmail(any(OrganizationUser.class));
    }

    @Test
    void registerEmployeeUser_InactiveAdmin() {
        adminUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(adminUser));
        assertThrows(SludiException.class, () -> userService.registerEmployeeUser(requestDto, "admin_user"));
    }

    @Test
    void registerEmployeeUser_DuplicateEmail() {
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(adminUser));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(mockOrganization));
        when(userRepository.existsByEmail("test@email.com")).thenReturn(true);

        assertThrows(SludiException.class, () -> userService.registerEmployeeUser(requestDto, "admin_user"));
    }

    @Test
    void approveEmployeeUser_UserNotFound() {
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(SludiException.class, () -> userService.approveEmployeeUser(2L, "admin_user"));
    }
}
