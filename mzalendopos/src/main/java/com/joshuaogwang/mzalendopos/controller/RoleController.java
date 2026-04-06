package com.joshuaogwang.mzalendopos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joshuaogwang.mzalendopos.entity.Role;
import com.joshuaogwang.mzalendopos.service.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/role")
@Tag(name = "Roles", description = "Manage user roles and permissions")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @PostMapping("/save-role")
    @Operation(summary = "Create role", description = "Creates a new role and assigns it to the specified user")
    @ApiResponse(responseCode = "201", description = "Role created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Role> saveRole(
            @Valid @RequestBody Role role,
            @Parameter(description = "ID of the user to assign this role to", required = true)
            @RequestParam Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.saveRole(role, userId));
    }

    @GetMapping
    @Operation(summary = "List roles")
    @ApiResponse(responseCode = "200", description = "Roles retrieved successfully")
    public ResponseEntity<Page<Role>> getAllRoles(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(roleService.getAllRoles(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    @ApiResponse(responseCode = "200", description = "Role found")
    @ApiResponse(responseCode = "404", description = "Role not found")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role")
    @ApiResponse(responseCode = "200", description = "Role updated")
    @ApiResponse(responseCode = "404", description = "Role not found")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @Valid @RequestBody Role role) {
        return ResponseEntity.ok(roleService.updateRole(id, role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role")
    @ApiResponse(responseCode = "204", description = "Role deleted")
    @ApiResponse(responseCode = "404", description = "Role not found")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
