package com.joshuaogwang.mzalendopos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.joshuaogwang.mzalendopos.entity.Role;
import com.joshuaogwang.mzalendopos.entity.User;
import com.joshuaogwang.mzalendopos.repository.RoleRepository;
import com.joshuaogwang.mzalendopos.repository.UserRepository;
import com.joshuaogwang.mzalendopos.service.serviceImpl.UserServiceImpl;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setId(1L);
        role.setName("USER");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setRole(role);
    }

    @Test
    void saveUser_encodesPasswordAndAssignsRole() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(roleRepository.findByName("USER")).thenReturn(role);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User saved = userService.saveUser(user);

        verify(passwordEncoder).encode("password123");
        verify(roleRepository).findByName("USER");
        verify(userRepository).save(user);
        assertThat(saved).isNotNull();
    }

    @Test
    void getUserById_returnsUser_whenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User found = userService.getUserById(1L);

        assertThat(found.getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUserById_throwsException_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllUsers_returnsPaginatedResults() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.getAllUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void deleteUser_deletesSuccessfully_whenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_throwsException_whenUserNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateUser_updatesFieldsAndSaves() {
        User updates = new User();
        updates.setFirstName("Updated");
        updates.setLastName("Name");
        updates.setEmail("updated@example.com");
        updates.setPhoneNumber("0712345678");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.updateUser(1L, updates);

        verify(userRepository).save(user);
        assertThat(user.getFirstName()).isEqualTo("Updated");
        assertThat(user.getLastName()).isEqualTo("Name");
        assertThat(user.getEmail()).isEqualTo("updated@example.com");
    }

    @Test
    void updateUser_encodesPassword_whenProvided() {
        User updates = new User();
        updates.setFirstName("Test");
        updates.setLastName("User");
        updates.setEmail("test@example.com");
        updates.setPassword("newpassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword")).thenReturn("encoded_new");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUser(1L, updates);

        verify(passwordEncoder).encode("newpassword");
        assertThat(user.getPassword()).isEqualTo("encoded_new");
    }
}
