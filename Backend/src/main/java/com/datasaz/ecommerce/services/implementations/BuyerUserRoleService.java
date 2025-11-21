package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.repositories.RefreshTokenRepository;
import com.datasaz.ecommerce.repositories.RolesRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.IBuyerUserRoleService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class BuyerUserRoleService implements IBuyerUserRoleService {

    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;

    private final IEmailService emailService;
    private final IAuditLogService auditLogService;

    private final RefreshTokenRepository refreshTokenRepository;

    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public void becomeIndividualSeller() {
        User user = currentUserService.getCurrentUser();
        try {
            Roles sellerRole = rolesRepository.findByRole(RoleTypes.SELLER)
                    .orElseGet(() -> rolesRepository.save(Roles.builder().role(RoleTypes.SELLER).build()));
            if (user.getUserRoles().stream().noneMatch(role -> role.getRole() == RoleTypes.SELLER)) {
                user.getUserRoles().add(sellerRole);
                userRepository.save(user);
                auditLogService.logAction(user.getEmailAddress(), "BECOME_INDIVIDUAL_SELLER", "User assigned SELLER role");
                emailService.sendEmail(user.getEmailAddress(), "Assigned Individual Seller Role",
                        "You have been assigned the SELLER role.");
                refreshTokenRepository.deleteByUserEmail(user.getEmailAddress());
            }
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            throw BadRequestException.builder().message("Error updating user.").build();
        }
    }



}
