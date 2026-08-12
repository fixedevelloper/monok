package com.monokek.identity.application;

import com.monokek.common.ApiException;
import com.monokek.identity.domain.Permission;
import com.monokek.identity.domain.Role;
import com.monokek.identity.domain.User;
import com.monokek.identity.domain.PermissionRepository;
import com.monokek.identity.domain.RoleRepository;
import com.monokek.identity.domain.UserRepository;
import com.monokek.identity.web.dto.StaffCreateRequest;
import com.monokek.identity.web.dto.StaffDto;
import com.monokek.identity.web.dto.StaffUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\StaffController}.
 */
@Service
public class StaffService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<StaffDto> list() {
        return userRepository.findAll().stream().map(StaffDto::from).toList();
    }

    @Transactional
    public StaffDto create(StaffCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("Cet email est déjà utilisé.");
        }
        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> ApiException.badRequest("Rôle inconnu: " + request.role()));

        User user = User.register(
                request.name(), request.email(), request.phone(), passwordEncoder.encode(request.password()), role);
        user.setBranchId(request.branchId());

        return StaffDto.from(userRepository.save(user));
    }

    @Transactional
    public StaffDto update(UUID uuid, StaffUpdateRequest request) {
        User staff = findByUuid(uuid);

        if (request.name() != null) {
            staff.setName(request.name());
        }
        if (request.email() != null) {
            staff.setEmail(request.email());
        }
        if (request.phone() != null) {
            staff.setPhone(request.phone());
        }
        if (request.isActive() != null) {
            staff.setActive(request.isActive());
        }
        if (request.role() != null) {
            Role role = roleRepository.findByName(request.role())
                    .orElseThrow(() -> ApiException.badRequest("Rôle inconnu: " + request.role()));
            staff.setRoles(new LinkedHashSet<>(List.of(role)));
        }
        if (request.clearBranchId()) {
            staff.setBranchId(null);
        } else if (request.branchId() != null) {
            staff.setBranchId(request.branchId());
        }

        return StaffDto.from(userRepository.save(staff));
    }

    @Transactional
    public void delete(UUID uuid, Long requesterId) {
        User staff = findByUuid(uuid);
        if (staff.getId().equals(requesterId)) {
            throw ApiException.badRequest("Vous ne pouvez pas vous supprimer vous-même");
        }
        staff.revokeAccess(requesterId);
        userRepository.save(staff);
    }

    @Transactional(readOnly = true)
    public List<StaffDto.RoleOption> listRoles() {
        return roleRepository.findByNameNot("super-admin").stream().map(StaffDto.RoleOption::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StaffDto.PermissionOption> listPermissions() {
        return permissionRepository.findAll().stream().map(StaffDto.PermissionOption::from).toList();
    }

    @Transactional
    public int updatePermissions(UUID uuid, List<String> permissionNames) {
        User staff = findByUuid(uuid);
        List<Permission> permissions = permissionRepository.findByNameIn(permissionNames);
        staff.updateDirectPermissions(new LinkedHashSet<>(permissions));
        userRepository.save(staff);
        return permissions.size();
    }

    private User findByUuid(UUID uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> ApiException.notFound("Membre du staff introuvable"));
    }
}
