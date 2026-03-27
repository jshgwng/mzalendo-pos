package com.joshuaogwang.mzalendopos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.joshuaogwang.mzalendopos.entity.Audit;
import com.joshuaogwang.mzalendopos.entity.Role;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.RoleRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.serviceImpl.RoleServiceImpl;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role;
    private User user;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(1L);
        role.setName("MANAGER");

        user = new User();
        user.setId(1L);
        user.setUsername("admin");
    }

    @Test
    void saveRole_savesAndCreatesAudit() {
        when(roleRepository.save(any(Role.class))).thenReturn(role);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(auditService.createAudit(any(Audit.class))).thenReturn(new Audit());

        Role saved = roleService.saveRole(role, 1L);

        verify(roleRepository).save(role);
        verify(auditService).createAudit(any(Audit.class));
        assertThat(saved.getName()).isEqualTo("MANAGER");
    }

    @Test
    void saveRole_throwsException_whenUserNotFound() {
        when(roleRepository.save(any(Role.class))).thenReturn(role);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.saveRole(role, 99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getRoleById_returnsRole_whenFound() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        Role found = roleService.getRoleById(1L);

        assertThat(found.getName()).isEqualTo("MANAGER");
    }

    @Test
    void getRoleById_throwsException_whenNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllRoles_returnsPaginatedResults() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Role> page = new PageImpl<>(List.of(role), pageable, 1);
        when(roleRepository.findAll(pageable)).thenReturn(page);

        Page<Role> result = roleService.getAllRoles(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void deleteRole_deletesSuccessfully_whenExists() {
        when(roleRepository.existsById(1L)).thenReturn(true);

        roleService.deleteRole(1L);

        verify(roleRepository).deleteById(1L);
    }

    @Test
    void deleteRole_throwsException_whenNotFound() {
        when(roleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> roleService.deleteRole(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateRole_updatesNameAndSaves() {
        Role updates = new Role();
        updates.setName("SENIOR_MANAGER");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        Role updated = roleService.updateRole(1L, updates);

        verify(roleRepository).save(role);
        assertThat(role.getName()).isEqualTo("SENIOR_MANAGER");
    }
}
