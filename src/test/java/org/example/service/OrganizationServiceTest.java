package org.example.service;

import org.example.dto.CreateOrganizationRequest;
import org.example.dto.OrganizationResponse;
import org.example.entity.Organization;
import org.example.entity.OrganizationUser;
import org.example.entity.PermissionTemplate;
import org.example.enums.OrganizationStatus;
import org.example.enums.OrganizationType;
import org.example.enums.UserStatus;
import org.example.exception.SludiException;
import org.example.repository.OrganizationRepository;
import org.example.repository.OrganizationUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationUserRepository userRepository;

    @Mock
    private PermissionTemplateService permissionTemplateService;

    @Mock
    private FabricOrgAssignmentService fabricOrgAssignmentService;

    @Mock
    private OrganizationUserService userService;

    @InjectMocks
    private OrganizationService organizationService;

    private OrganizationUser mockUser;
    private PermissionTemplate mockTemplate;
    private CreateOrganizationRequest createRequest;

    @BeforeEach
    void setUp() {
        mockUser = OrganizationUser.builder()
                .id(1L)
                .username("admin_user")
                .status(UserStatus.ACTIVE)
                .build();

        mockTemplate = new PermissionTemplate();
        mockTemplate.setId(1L);
        mockTemplate.setName("Default Template");

        createRequest = new CreateOrganizationRequest();
        createRequest.setName("Test Org");
        createRequest.setOrganizationType(OrganizationType.valueOf("FINANCIAL"));
        createRequest.setTemplateId(1L);
        createRequest.setRegistrationNumber("REG123");
    }

    @Test
    void createOrganization_Success() {
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(mockUser));
        when(userService.verifyUserPermission("admin_user", "organization:create")).thenReturn(true);
        when(permissionTemplateService.getTemplateById(1L)).thenReturn(mockTemplate);
        when(organizationRepository.existsByRegistrationNumber("REG123")).thenReturn(false);
        when(organizationRepository.existsByOrgCode(anyString())).thenReturn(false);

        Organization savedOrg = Organization.builder()
                .id(100L)
                .orgCode("ORG-XYZ")
                .name("Test Org")
                .registrationNumber("REG123")
                .status(OrganizationStatus.PENDING)
                .template(mockTemplate)
                .build();

        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrg);

        OrganizationResponse response = organizationService.createOrganization(createRequest, "admin_user");

        assertNotNull(response);
        assertEquals("ORG-XYZ", response.getOrgCode());
        assertEquals("Test Org", response.getName());
        assertEquals(OrganizationStatus.PENDING, response.getStatus());

        verify(organizationRepository).save(any(Organization.class));
        verify(userService).initializeOrganizationRoles(100L);
    }

    @Test
    void createOrganization_UserNotFound() {
        when(userRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

        assertThrows(SludiException.class, () -> 
            organizationService.createOrganization(createRequest, "unknown_user")
        );
    }

    @Test
    void createOrganization_NoPermission() {
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(mockUser));
        when(userService.verifyUserPermission("admin_user", "organization:create")).thenReturn(false);

        assertThrows(SludiException.class, () -> 
            organizationService.createOrganization(createRequest, "admin_user")
        );
    }

    @Test
    void createOrganization_UserInactive() {
        mockUser.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(mockUser));
        when(userService.verifyUserPermission("admin_user", "organization:create")).thenReturn(true);

        assertThrows(SludiException.class, () -> 
            organizationService.createOrganization(createRequest, "admin_user")
        );
    }

    @Test
    void createOrganization_DuplicateRegistrationNumber() {
        when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(mockUser));
        when(userService.verifyUserPermission("admin_user", "organization:create")).thenReturn(true);
        when(permissionTemplateService.getTemplateById(1L)).thenReturn(mockTemplate);
        when(organizationRepository.existsByRegistrationNumber("REG123")).thenReturn(true);

        assertThrows(SludiException.class, () -> 
            organizationService.createOrganization(createRequest, "admin_user")
        );
    }
}
