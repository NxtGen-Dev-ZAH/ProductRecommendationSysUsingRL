package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.request.CompanyRequest;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.ICompanyService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.services.interfaces.ISellerUserRoleService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class SellerUserRoleService implements ISellerUserRoleService {

    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final ProductRepository productRepository;

    private final UserMapper userMapper;

    private final IEmailService emailService;
    private final IAuditLogService auditLogService;
    private final ICompanyService companyService;

    private final CompanyRepository companyRepository;
    private final CompanyAdminRightsRepository companyAdminRightsRepository;
    private final ApprovalTokenRepository approvalTokenRepository;

    private final CurrentUserService currentUserService;


    // Base URL for generating approval/denial links
    @Value("${app.base-url}")
    private String BASE_URL;

   /* @Override
    @Deprecated
    @Transactional
    public void requestCompanyAdminSeller(CompanyRequest companyRequest, MultipartFile image) throws MessagingException {
        User user = currentUserService.getCurrentUser();
        Company company = companyService.registerCompany(companyRequest, image, user);
        Roles companyAdminSellerRole = rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("COMPANY_ADMIN_SELLER role not found").build());

        if (companyService.getCompanyAdmins(company).isEmpty()) {
            companyService.assignAdminRights(user, company, true, true);
            user.getUserRoles().add(companyAdminSellerRole);
            userRepository.save(user);
            auditLogService.logAction(user.getEmailAddress(), "BECOME_COMPANY_ADMIN_SELLER",
                    "User assigned COMPANY_ADMIN_SELLER role for new company ID: " + company.getId() + " (" + company.getName() + ")");
            emailService.sendEmail(user.getEmailAddress(),
                    "Company Admin Seller Request Approved",
                    "You are now a company admin seller for " + company.getName() + ".");
        } else {
            CompanyAdminRights rights = companyService.assignAdminRights(user, company, false, false);
            List<CompanyAdminRights> admins = companyService.getCompanyAdmins(company);
            for (CompanyAdminRights admin : admins) {
                String approveLink = BASE_URL + "/approve-company-admin/" + rights.getId();
                String denyLink = BASE_URL + "/deny-company-admin/" + rights.getId();
                emailService.sendEmail(admin.getUser().getEmailAddress(),
                        "New Company Admin Seller Request",
                        "User " + user.getEmailAddress() + " has requested to become a company admin seller for " +
                                company.getName() + ". Please approve or deny this request. " +
                                "Click <a href=\"" + approveLink + "\">here</a> to approve or " +
                                "<a href=\"" + denyLink + "\">here</a> to deny this request.");
            }
            auditLogService.logAction(user.getEmailAddress(), "REQUEST_COMPANY_ADMIN_SELLER",
                    "User " + user.getEmailAddress() + " requested COMPANY_ADMIN_SELLER role for company ID: " + company.getId() + " (" + company.getName() + ")");
            throw BadRequestException.builder().message("Company found. Admin approval required.").build();
        }
    }

    */
//    @Deprecated
//    @Transactional
//    public void requestCompanyAdminSeller(CompanyRequest companyRequest, MultipartFile image) throws MessagingException {
//        User user = getCurrentUser();
//        Company company = companyService.registerCompany(companyRequest, image, user);
//        Roles companyAdminSellerRole = rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)
//                .orElseThrow(() -> ResourceNotFoundException.builder().message("COMPANY_ADMIN_SELLER role not found").build());
//
//        if (companyService.getCompanyAdmins(company).isEmpty()) {
//            // New company: assign admin rights and role
//            companyService.assignAdminRights(user, company, true, true);
//            user.getUserRoles().add(companyAdminSellerRole);
//            userRepository.save(user);
//            auditLogService.logAction(user.getEmailAddress(), "BECOME_COMPANY_ADMIN_SELLER",
//                    "User assigned COMPANY_ADMIN_SELLER role for new company: " + company.getName());
//            emailService.sendEmail(user.getEmailAddress(),
//                    "Company Admin Seller Request Approved",
//                    "You are now a company admin seller for " + company.getName() + ".");
//        } else {
//            // Existing company: request admin rights, notify existing admins
//            CompanyAdminRights rights = companyService.assignAdminRights(user, company, false, false);
//            List<CompanyAdminRights> admins = companyService.getCompanyAdmins(company);
//            for (CompanyAdminRights admin : admins) {
//                String approveLink = BASE_URL + "/buyer/user/role" + "/approve-company-admin/" + rights.getId();
//                String denyLink = BASE_URL + "/buyer/user/role" + "/deny-company-admin/" + rights.getId();
//
//                emailService.sendEmail(admin.getUser().getEmailAddress(),
//                        "New Company Admin Seller Request",
//                        "User " + user.getEmailAddress() + " has requested to become a company admin seller for " +
//                                company.getName() + ". Please approve or deny this request. " +
//                                "If you would like to review this paper, " +
//                                "Click <a href=\"" + approveLink + "\">here</a> to approve or " +
//                                "<a href=\"" + denyLink + "\">here</a> to reject this request.");
//            }
//            auditLogService.logAction(user.getEmailAddress(), "REQUEST_COMPANY_ADMIN_SELLER",
//                    "Requested COMPANY_ADMIN_SELLER role for company: " + company.getName());
//            throw BadRequestException.builder().message("Company found. Admin approval required.").build();
//        }
//    }

    @Override
    @Transactional
    public void requestCompanyAdminSellerV2(@Valid CompanyRequest companyRequest, MultipartFile image) throws MessagingException {
        User user = currentUserService.getCurrentUser();
        Company company = companyService.registerCompany(companyRequest, image, user);
        Roles companyAdminSellerRole = rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)
                .orElseGet(() -> {
                    Roles newRole = Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build();
                    return rolesRepository.saveAndFlush(newRole);
                });
        //.orElseThrow(() -> ResourceNotFoundException.builder().message("COMPANY_ADMIN_SELLER role not found").build());

        if (companyService.getCompanyAdmins(company).isEmpty()) {
            // New company: assign admin rights and role
            companyService.assignAdminRights(user, company, true, true);
            user.getUserRoles().add(companyAdminSellerRole);
            userRepository.save(user);
            auditLogService.logAction(user.getEmailAddress(), "BECOME_COMPANY_ADMIN_SELLER",
                    "User " + user.getEmailAddress() + " assigned COMPANY_ADMIN_SELLER role for new company ID: " + company.getId() + " (" + company.getName() + ")");
            emailService.sendEmail(user.getEmailAddress(),
                    "Company Admin Seller Request Approved",
                    "You are now a company admin seller for " + company.getName() + ".");
        } else {
            // Existing company: request admin rights, notify existing admins
            CompanyAdminRights rights = companyService.assignAdminRights(user, company, false, false);
            String token = UUID.randomUUID().toString();
            ApprovalToken approvalToken = new ApprovalToken();
            approvalToken.setToken(token);
            approvalToken.setRights(rights);
            approvalToken.setExpiryDate(LocalDateTime.now().plusHours(24));
            approvalToken.setRevoked(false);
            approvalTokenRepository.save(approvalToken);

            List<CompanyAdminRights> admins = companyService.getCompanyAdmins(company);
            for (CompanyAdminRights admin : admins) {
                String approveLink = BASE_URL + "/approve-company-admin/company/" + company.getId() + "/user/" + user.getId() + "?token=" + token;
                String denyLink = BASE_URL + "/deny-company-admin/company/" + company.getId() + "/user/" + user.getId() + "?token=" + token;
                emailService.sendEmail(admin.getUser().getEmailAddress(),
                        "New Company Admin Seller Request",
                        "User " + user.getEmailAddress() + " has requested to become a company admin seller for " +
                                company.getName() + ". Please approve or deny this request. " +
                                "Click <a href=\"" + approveLink + "\">here</a> to approve or " +
                                "<a href=\"" + denyLink + "\">here</a> to deny this request.");
            }
            auditLogService.logAction(user.getEmailAddress(), "REQUEST_COMPANY_ADMIN_SELLER",
                    "User " + user.getEmailAddress() + " requested COMPANY_ADMIN_SELLER role for company ID: " + company.getId() + " (" + company.getName() + ")");
            throw ConflictFoundException.builder().message("Company found. An email has been sent for admin approval.").build();
            //throw new ConflictFoundException("Company found. An email has been sent for admin approval.");
        }
    }


//    @Transactional
//    public void requestCompanyAdminSellerV2(CompanyRequest companyRequest) throws MessagingException {
//        User user = getCurrentUser();
//        Company company = companyService.registerCompany(companyRequest, user);
//        Roles companyAdminSellerRole = rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)
//                .orElseThrow(() -> new IllegalArgumentException("COMPANY_ADMIN_SELLER role not found"));
//
//        if (companyService.getCompanyAdmins(company).isEmpty()) {
//            // New company: assign admin rights and role
//            companyService.assignAdminRights(user, company, true, true);
//            user.getUserRoles().add(companyAdminSellerRole);
//            userRepository.save(user);
//            auditLogService.logAction(user, "BECOME_COMPANY_ADMIN_SELLER",
//                    "User assigned COMPANY_ADMIN_SELLER role for new company: " + company.getName());
//            emailService.sendEmail(user.getEmailAddress(),
//                    "Company Admin Seller Request Approved",
//                    "You are now a company admin seller for " + company.getName() + ".");
//        } else {
//            // Existing company: request admin rights, notify existing admins
//            CompanyAdminRights rights = companyService.assignAdminRights(user, company, false, false);
//            List<CompanyAdminRights> admins = companyService.getCompanyAdmins(company);
//            for (CompanyAdminRights admin : admins) {
//                String approveLink = BASE_URL + "/approve-company-admin/company/" + company.getId() + "/user/" + user.getId();
//                String denyLink = BASE_URL + "/deny-company-admin/company/" + company.getId() + "/user/" + user.getId();
//                emailService.sendEmail(admin.getUser().getEmailAddress(),
//                        "New Company Admin Seller Request",
//                        "User " + user.getEmailAddress() + " has requested to become a company admin seller for " +
//                                company.getName() + ". Please approve or deny this request for user ID " + user.getId() +
//                                " and company ID " + company.getId() + "." +
//                                "Click <a href=\"" + approveLink + "\">here</a> to approve or " +
//                                "<a href=\"" + denyLink + "\">here</a> to reject this request.");
//            }
//            auditLogService.logAction(user, "REQUEST_COMPANY_ADMIN_SELLER",
//                    "Requested COMPANY_ADMIN_SELLER role for company: " + company.getName());
//            throw new IllegalStateException("Company found. Admin approval required.");
//        }
//    }

    @Override
    @Transactional
    public void approveOrDenyCompanyAdminByCompanyAndUser(Long companyId, Long userId, boolean approve, String token) throws MessagingException {
        // Validate token
        ApprovalToken approvalToken = approvalTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> BadRequestException.builder().message("Invalid or expired token").build());
        if (approvalToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw BadRequestException.builder().message("Token has expired").build();
        }

        User approvingUser = currentUserService.getCurrentUser();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Company not found").build());
        User requestingUser = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found").build());

        CompanyAdminRights rights = companyAdminRightsRepository.findByUserAndCompany(requestingUser, company)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Admin rights request not found for user and company").build());

        if (!rights.equals(approvalToken.getRights())) {
            throw UnauthorizedException.builder().message("User not authorized to manage admin rights").build();
        }

        if (!company.getAdminRights().stream()
                .anyMatch(r -> r.getUser().equals(approvingUser) && r.getCanPromoteDemoteAdmins())) {
            throw UnauthorizedException.builder().message("User not authorized to manage admin rights").build();
        }

        // Mark token as used
        approvalToken.setRevoked(true);
        approvalTokenRepository.save(approvalToken);

        companyService.updateAdminRights(rights.getId(), approve, approvingUser);
        if (approve) {
            Roles companyAdminSellerRole = rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)
                    .orElseThrow(() -> ResourceNotFoundException.builder().message("COMPANY_ADMIN_SELLER role not found").build());
            requestingUser.getUserRoles().add(companyAdminSellerRole);
            userRepository.save(requestingUser);
        }

        // Notify all admins and the requesting user
        List<CompanyAdminRights> admins = companyService.getCompanyAdmins(company);
        for (CompanyAdminRights admin : admins) {
            emailService.sendEmail(admin.getUser().getEmailAddress(),
                    "Company Admin Seller Request " + (approve ? "Approved" : "Denied"),
                    "The request for " + requestingUser.getEmailAddress() + " to become a company admin seller for " +
                            company.getName() + " has been " + (approve ? "approved" : "denied") + " by " + approvingUser.getEmailAddress() + ".");
        }
        emailService.sendEmail(requestingUser.getEmailAddress(),
                "Company Admin Seller Request " + (approve ? "Approved" : "Denied"),
                "Your request to become a company admin seller for " + company.getName() +
                        " has been " + (approve ? "approved" : "denied") + " by " + approvingUser.getEmailAddress() + ".");

        auditLogService.logAction(approvingUser.getEmailAddress(), "APPROVE_DENY_COMPANY_ADMIN",
                "User " + requestingUser.getEmailAddress() + " request for COMPANY_ADMIN_SELLER role in company ID: " + company.getId() + " (" + company.getName() +
                        ") was " + (approve ? "approved" : "denied") + " by " + approvingUser.getEmailAddress());
    }

    @Override
    @Transactional
    public void approveOrDenyCompanyAdmin(Long rightsId, boolean approve) throws MessagingException {
        User approvingUser = currentUserService.getCurrentUser();
        CompanyAdminRights rights = companyService.getCompanyAdmins(null)
                .stream()
                .filter(r -> r.getId().equals(rightsId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Admin rights not found").build());

        if (!rights.getCompany().getAdminRights().stream()
                .anyMatch(r -> r.getUser().equals(approvingUser) && r.getCanAddRemoveSellers())) {
            throw UnauthorizedException.builder().message("User not authorized to manage admin rights").build();
        }

        companyService.updateAdminRights(rightsId, approve, approvingUser);
        User requestingUser = rights.getUser();
        if (approve) {
            Roles companyAdminSellerRole = rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)
                    .orElseThrow(() -> ResourceNotFoundException.builder().message("COMPANY_ADMIN_SELLER role not found").build());
            requestingUser.getUserRoles().add(companyAdminSellerRole);
            userRepository.save(requestingUser);
        }

        // Notify all admins and the requesting user
        List<CompanyAdminRights> admins = companyService.getCompanyAdmins(rights.getCompany());
        for (CompanyAdminRights admin : admins) {
            emailService.sendEmail(admin.getUser().getEmailAddress(),
                    "Company Admin Seller Request " + (approve ? "Approved" : "Denied"),
                    "The request for " + requestingUser.getEmailAddress() + " to become a company admin seller for " +
                            rights.getCompany().getName() + " has been " + (approve ? "approved" : "denied") + ".");
        }
        emailService.sendEmail(requestingUser.getEmailAddress(),
                "Company Admin Seller Request " + (approve ? "Approved" : "Denied"),
                "Your request to become a company admin seller for " + rights.getCompany().getName() +
                        " has been " + (approve ? "approved" : "denied") + ".");

        auditLogService.logAction(approvingUser.getEmailAddress(), "APPROVE_DENY_COMPANY_ADMIN",
                "Company admin seller request for " + rights.getCompany().getName() +
                        " has been " + (approve ? "approved" : "denied") + ".");

    }

//    @Transactional
//    public void approveOrDenyCompanyAdminByCompanyAndUser(Long companyId, Long userId, boolean approve) throws MessagingException {
//        User approvingUser = getCurrentUser();
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> ResourceNotFoundException.builder().message("Company not found").build());
//        User requestingUser = userRepository.findById(userId)
//                .orElseThrow(() -> UserNotFoundException.builder().message("User not found").build());
//
//        CompanyAdminRights rights = companyAdminRightsRepository.findByUserAndCompany(requestingUser, company)
//                .orElseThrow(() -> ResourceNotFoundException.builder().message("Admin rights request not found for user and company").build());
//
//        if (!company.getAdminRights().stream()
//                .anyMatch(r -> r.getUser().equals(approvingUser) && r.getCanAddRemoveSellers())) {
//            throw UnauthorizedException.builder().message("User not authorized to manage admin rights").build();
//        }
//
//        companyService.updateAdminRights(rights.getId(), approve, approvingUser);
//        if (approve) {
//            Roles companyAdminSellerRole = rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)
//                    .orElseThrow(() -> ResourceNotFoundException.builder().message("COMPANY_ADMIN_SELLER role not found").build());
//            requestingUser.getUserRoles().add(companyAdminSellerRole);
//            userRepository.save(requestingUser);
//        }
//
//        // Notify all admins and the requesting user
//        List<CompanyAdminRights> admins = companyService.getCompanyAdmins(company);
//        for (CompanyAdminRights admin : admins) {
//            emailService.sendEmail(admin.getUser().getEmailAddress(),
//                    "Company Admin Seller Request " + (approve ? "Approved" : "Denied"),
//                    "The request for " + requestingUser.getEmailAddress() + " to become a company admin seller for " +
//                            company.getName() + " has been " + (approve ? "approved" : "denied") + ".");
//        }
//        emailService.sendEmail(requestingUser.getEmailAddress(),
//                "Company Admin Seller Request " + (approve ? "Approved" : "Denied"),
//                "Your request to become a company admin seller for " + company.getName() +
//                        " has been " + (approve ? "approved" : "denied") + ".");
//    }

    @Override
    @RateLimiter(name = "manageCompany")
    @Transactional
    public void addSellerToCompany(Long companyId, String sellerEmail, String adminEmail) throws MessagingException {
        log.info("addSellerToCompany: {} adding seller {} to company {}", adminEmail, sellerEmail, companyId);
        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Admin not found with email: " + adminEmail).build());
        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Seller not found with email: " + sellerEmail).build());
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
            throw UnauthorizedException.builder()
                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Admin has no rights for company: " + companyId).build());
        if (!adminRights.getCanAddRemoveSellers()) {
            log.error("Admin {} lacks permission to add sellers", adminEmail);
            throw UnauthorizedException.builder()
                    .message("Admin lacks permission to add sellers").build();
        }

        if (!hasRole(seller, RoleTypes.SELLER)) {
            log.error("User {} is not a seller", sellerEmail);
            throw BadRequestException.builder()
                    .message("User is not a seller").build();
        }

        seller.setCompany(company);
        userRepository.save(seller);

        // If no primary admin, set the first COMPANY_ADMIN_SELLER as primary
        if (company.getPrimaryAdmin() == null && hasRole(seller, RoleTypes.COMPANY_ADMIN_SELLER)) {
            company.setPrimaryAdmin(seller);
            companyRepository.save(company);
        }

        emailService.sendEmail(seller.getEmailAddress(), "Added to Company",
                "You have been added as a seller to " + company.getName() + " by " + admin.getEmailAddress() + ".");
        auditLogService.logAction(seller.getEmailAddress(), "ADD_SELLER_TO_COMPANY", adminEmail,
                "Seller " + sellerEmail + " added to company ID: " + companyId + " (" + company.getName() + ") by " + adminEmail);
    }

//   v2
//    @RateLimiter(name = "manageCompanySeller")
//    public void addSellerToCompany(Long companyId, String sellerEmail, String adminEmail) {
//        log.info("addSellerToCompany: {} adding seller {} to company {}", adminEmail, sellerEmail, companyId);
//        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Admin not found with email: " + adminEmail).build());
//        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Seller not found with email: " + sellerEmail).build());
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> ResourceNotFoundException.builder()
//                        .message("Company not found with id: " + companyId).build());
//
//        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
//            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
//            throw UnauthorizedException.builder()
//                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
//        }
//
//        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
//                .orElseThrow(() -> UnauthorizedException.builder()
//                        .message("Admin has no rights for company: " + companyId).build());
//        if (!adminRights.getCanAddRemoveSellers()) {
//            log.error("Admin {} lacks permission to add sellers", adminEmail);
//            throw UnauthorizedException.builder()
//                    .message("Admin lacks permission to add sellers").build();
//        }
//
//        if (!hasRole(seller, RoleTypes.SELLER)) {
//            log.error("User {} is not a seller", sellerEmail);
//            throw BadRequestException.builder()
//                    .message("User is not a seller").build();
//        }
//
//        seller.setCompany(company);
//        userRepository.save(seller);
//
//        // If no primary admin, set the first COMPANY_ADMIN_SELLER as primary
//        if (company.getPrimaryAdmin() == null && hasRole(seller, RoleTypes.COMPANY_ADMIN_SELLER)) {
//            company.setPrimaryAdmin(seller);
//            companyRepository.save(company);
//        }
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(sellerEmail)
//                .action("ADD_SELLER_TO_COMPANY")
//                .performedBy(adminEmail)
//                .details("Company ID: " + companyId)
//                .timestamp(LocalDateTime.now())
//                .build());
//    }

//    v1
//    @RateLimiter(name = "manageCompanySeller")
//    public void addSellerToCompany(Long companyId, String sellerEmail, String adminEmail) {
//        log.info("addSellerToCompany: {} adding seller {} to company {}", adminEmail, sellerEmail, companyId);
//        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Admin not found with email: " + adminEmail).build());
//        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Seller not found with email: " + sellerEmail).build());
//        Company company = companyRepository.findByCompanyId(companyId)
//                .orElseThrow(() -> ResourceNotFoundException.builder()
//                        .message("Company not found with id: " + companyId).build());
//
//        if (!hasRole(admin, "ROLE_COMPANY_ADMIN_SELLER")) {
//            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
//            throw UnauthorizedException.builder()
//                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
//        }
//
//        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
//                .orElseThrow(() -> UnauthorizedException.builder()
//                        .message("Admin has no rights for company: " + companyId).build());
//        if (!adminRights.getCanAddRemoveSellers()) {
//            log.error("Admin {} lacks permission to add sellers", adminEmail);
//            throw UnauthorizedException.builder().message("Admin lacks permission to add sellers").build();
//        }
//
//        if (!hasRole(seller, "ROLE_SELLER")) {
//            log.error("User {} is not a seller", sellerEmail);
//            throw BadRequestException.builder().message("User is not a seller").build();
//        }
//

    /// /        UserCompany userCompany = new UserCompany();
    /// /        userCompany.setUser(seller);
    /// /        userCompany.setCompany(company);
    /// /        userCompanyRepository.save(userCompany);
//
//        seller.setCompany(company);
//        userRepository.save(seller);
//
//        // If no primary admin, set the first COMPANY_ADMIN_SELLER as primary
//        if (company.getPrimaryAdmin() == null && hasRole(seller, "ROLE_COMPANY_ADMIN_SELLER")) {
//            company.setPrimaryAdmin(seller);
//            companyRepository.save(company);
//        }
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(sellerEmail)
//                .action("ADD_SELLER_TO_COMPANY")
//                .performedBy(adminEmail)
//                .details("Company ID: " + companyId)
//                .timestamp(LocalDateTime.now())
//                .build());
//    }
    @Override
    @RateLimiter(name = "manageCompany")
    @Transactional
    public void removeSellerFromCompany(Long companyId, String sellerEmail, String adminEmail) {
        log.info("removeSellerFromCompany: {} removing seller {} from company {}", adminEmail, sellerEmail, companyId);
        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Admin not found with email: " + adminEmail).build());
        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Seller not found with email: " + sellerEmail).build());
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
            throw UnauthorizedException.builder().message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Admin has no rights for company: " + companyId).build());
        if (!adminRights.getCanAddRemoveSellers()) {
            log.error("Admin {} lacks permission to remove sellers", adminEmail);
            throw UnauthorizedException.builder()
                    .message("Admin lacks permission to remove sellers").build();
        }

        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
            long adminCount = userRepository.countCompanyAdminsByCompanyId(companyId);
            if (adminCount <= 1) {
                log.error("Cannot remove the only COMPANY_ADMIN_SELLER for company {}", companyId);
                throw BadRequestException.builder()
                        .message("Cannot remove the only COMPANY_ADMIN_SELLER").build();
            }
        }

        seller.setCompany(null);
        companyAdminRightsRepository.deleteByCompanyIdAndUserId(companyId, seller.getId());
        userRepository.save(seller);

        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
            company.setPrimaryAdmin(null);
            companyRepository.save(company);
        }

        auditLogService.logAction(seller.getEmailAddress(), "REMOVE_SELLER_FROM_COMPANY", adminEmail,
                "Seller " + sellerEmail + " removed from company ID: " + companyId + " (" + company.getName() + ") by " + adminEmail);
    }

//    v1
//    @RateLimiter(name = "manageCompanySeller")
//    public void removeSellerFromCompany(Long companyId, String sellerEmail, String adminEmail) {
//        log.info("removeSellerFromCompany: {} removing seller {} from company {}", adminEmail, sellerEmail, companyId);
//        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Admin not found with email: " + adminEmail).build());
//        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Seller not found with email: " + sellerEmail).build());
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> ResourceNotFoundException.builder()
//                        .message("Company not found with id: " + companyId).build());
//
//        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
//            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
//            throw UnauthorizedException.builder().message("User lacks COMPANY_ADMIN_SELLER role").build();
//        }
//
//        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
//                .orElseThrow(() -> UnauthorizedException.builder()
//                        .message("Admin has no rights for company: " + companyId).build());
//        if (!adminRights.getCanAddRemoveSellers()) {
//            log.error("Admin {} lacks permission to remove sellers", adminEmail);
//            throw UnauthorizedException.builder()
//                    .message("Admin lacks permission to remove sellers").build();
//        }
//
//        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
//            long adminCount = userRepository.countCompanyAdminsByCompanyId(companyId);
//            if (adminCount <= 1) {
//                log.error("Cannot remove the only COMPANY_ADMIN_SELLER for company {}", companyId);
//                throw BadRequestException.builder()
//                        .message("Cannot remove the only COMPANY_ADMIN_SELLER").build();
//            }
//        }
//
//        seller.setCompany(null);
//        companyAdminRightsRepository.deleteByCompanyIdAndUserId(companyId, seller.getId());
//        userRepository.save(seller);
//
//        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
//            company.setPrimaryAdmin(null);
//            companyRepository.save(company);
//        }
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(sellerEmail)
//                .action("REMOVE_SELLER_FROM_COMPANY")
//                .performedBy(adminEmail)
//                .details("Company ID: " + companyId)
//                .timestamp(LocalDateTime.now())
//                .build());
//    }

    @Override
    @RateLimiter(name = "manageCompany")
    @Transactional
    public void promoteToCompanyAdmin(Long companyId, String sellerEmail, String adminEmail, boolean canAddRemoveSellers, boolean canPromoteDemoteAdmins, boolean canDelegateAdminRights) throws MessagingException {
        log.info("promoteToCompanyAdmin: {} promoting {} to admin for company {}", adminEmail, sellerEmail, companyId);
        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Admin not found with email: " + adminEmail).build());
        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Seller not found with email: " + sellerEmail).build());
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
            throw UnauthorizedException.builder()
                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Admin has no rights for company: " + companyId).build());
        if (!adminRights.getCanPromoteDemoteAdmins()) {
            log.error("Admin {} lacks permission to promote admins", adminEmail);
            throw UnauthorizedException.builder()
                    .message("Admin lacks permission to promote admins").build();
        }

        if (!hasRole(seller, RoleTypes.SELLER) || seller.getCompany() == null || !seller.getCompany().getId().equals(companyId)) {
            log.error("User {} is not a seller associated with company {}", sellerEmail, companyId);
            throw BadRequestException.builder()
                    .message("User is not a seller associated with the company").build();
        }

        Roles companyAdminSellerRole = rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)
                .orElseGet(() -> rolesRepository.save(Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build()));
        seller.getUserRoles().add(companyAdminSellerRole);
        CompanyAdminRights newAdminRights = CompanyAdminRights.builder()
                .company(company)
                .user(seller)
                .canAddRemoveSellers(canAddRemoveSellers)
                .canPromoteDemoteAdmins(canPromoteDemoteAdmins)
                .canDelegateAdminRights(canDelegateAdminRights)
                .build();
        companyAdminRightsRepository.save(newAdminRights);
        userRepository.save(seller);

        if (company.getPrimaryAdmin() == null) {
            company.setPrimaryAdmin(seller);
            companyRepository.save(company);
        }

        emailService.sendEmail(seller.getEmailAddress(), "Promoted to Company Admin",
                "You have been promoted to company admin for " + company.getName() + " by " + admin.getEmailAddress() + ".");

        auditLogService.logAction(seller.getEmailAddress(), "PROMOTE_TO_COMPANY_ADMIN", adminEmail,
                "Seller " + sellerEmail + " promoted to company admin for company ID: " + companyId + " (" + company.getName() +
                        "), Rights: canAddRemoveSellers=" + canAddRemoveSellers +
                        ", canPromoteDemoteAdmins=" + canPromoteDemoteAdmins +
                        ", canDelegateAdminRights=" + canDelegateAdminRights +
                        ", Performed by: " + adminEmail);
    }

    @Override
    @RateLimiter(name = "manageCompany")
    @Transactional
    public void demoteCompanyAdmin(Long companyId, String sellerEmail, String adminEmail) throws MessagingException {
        log.info("demoteCompanyAdmin: {} demoting {} for company {}", adminEmail, sellerEmail, companyId);
        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Admin not found with email: " + adminEmail).build());
        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Seller not found with email: " + sellerEmail).build());
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
            throw UnauthorizedException.builder()
                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Admin has no rights for company: " + companyId).build());
        if (!adminRights.getCanPromoteDemoteAdmins()) {
            log.error("Admin {} lacks permission to demote admins", adminEmail);
            throw UnauthorizedException.builder()
                    .message("Admin lacks permission to demote admins").build();
        }

        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
            long adminCount = userRepository.countCompanyAdminsByCompanyId(companyId);
            if (adminCount <= 1) {
                log.error("Cannot demote the only COMPANY_ADMIN_SELLER for company {}", companyId);
                throw BadRequestException.builder()
                        .message("Cannot demote the only COMPANY_ADMIN_SELLER").build();
            }
        }

        seller.getUserRoles().removeIf(role -> role.getRole() == RoleTypes.COMPANY_ADMIN_SELLER);
        companyAdminRightsRepository.deleteByCompanyIdAndUserId(companyId, seller.getId());
        userRepository.save(seller);

        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
            company.setPrimaryAdmin(null);
            companyRepository.save(company);
        }

        emailService.sendEmail(seller.getEmailAddress(), "Demoted from Company Admin",
                "You have been demoted from company admin for " + company.getName() + " by " + admin.getEmailAddress() + ".");
        auditLogService.logAction(seller.getEmailAddress(), "DEMOTE_COMPANY_ADMIN",
                "Seller " + sellerEmail + " demoted from company admin for company ID: " + companyId + " (" + company.getName() + ") by " + adminEmail);
    }

    @Override
    @RateLimiter(name = "manageCompany")
    @Transactional
    public void updateAdminRights(Long companyId, String sellerEmail, String adminEmail, boolean canAddRemoveSellers, boolean canPromoteDemoteAdmins, boolean canDelegateAdminRights) {
        log.info("updateAdminRights: {} updating admin rights for {} in company {}", adminEmail, sellerEmail, companyId);
        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Admin not found with email: " + adminEmail).build());
        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
                .orElseThrow(() -> UserNotFoundException.builder()
                        .message("Seller not found with email: " + sellerEmail).build());
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
            log.error("User {} cannot update admin rights for primary admin {} of company {}", adminEmail, sellerEmail, companyId);
            throw UnauthorizedException.builder()
                    .message("User lacks permission to update primary admin").build();
        }

        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
            throw UnauthorizedException.builder()
                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Admin has no rights for company: " + companyId).build());
        if (!adminRights.getCanDelegateAdminRights()) {
            log.error("Admin {} lacks permission to delegate admin rights", adminEmail);
            throw UnauthorizedException.builder()
                    .message("Admin lacks permission to delegate admin rights").build();
        }

        CompanyAdminRights sellerRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())
                .orElseThrow(() -> BadRequestException.builder()
                        .message("Seller is not a company admin for company: " + companyId).build());

        sellerRights.setCanAddRemoveSellers(canAddRemoveSellers);
        sellerRights.setCanPromoteDemoteAdmins(canPromoteDemoteAdmins);
        sellerRights.setCanDelegateAdminRights(canDelegateAdminRights);
        companyAdminRightsRepository.save(sellerRights);

        auditLogService.logAction(seller.getEmailAddress(), "UPDATE_ADMIN_RIGHTS",
                "Admin rights updated for " + sellerEmail + " in company ID " + companyId + " (" + company.getName() +
                        "), Rights: canAddRemoveSellers=" + canAddRemoveSellers +
                        ", canPromoteDemoteAdmins=" + canPromoteDemoteAdmins +
                        ", canDelegateAdminRights=" + canDelegateAdminRights +
                        ", Performed by: " + adminEmail);
    }

    private boolean hasRole(User user, RoleTypes roleType) {
        return user.getUserRoles().stream().anyMatch(role -> role.getRole() == roleType);
    }

//    V1
//    @RateLimiter(name = "manageCompanyAdmin")
//    public void promoteToCompanyAdmin(Long companyId, String sellerEmail, String adminEmail, boolean canAddRemoveSellers, boolean canPromoteDemoteAdmins, boolean canDelegateAdminRights) {
//        log.info("promoteToCompanyAdmin: {} promoting {} to admin for company {}", adminEmail, sellerEmail, companyId);
//        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Admin not found with email: " + adminEmail).build());
//        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Seller not found with email: " + sellerEmail).build());
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> ResourceNotFoundException.builder()
//                        .message("Company not found with id: " + companyId).build());
//
//        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
//            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
//            throw UnauthorizedException.builder()
//                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
//        }
//
//        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
//                .orElseThrow(() -> UnauthorizedException.builder()
//                        .message("Admin has no rights for company: " + companyId).build());
//        if (!adminRights.getCanPromoteDemoteAdmins()) {
//            log.error("Admin {} lacks permission to promote admins", adminEmail);
//            throw UnauthorizedException.builder()
//                    .message("Admin lacks permission to promote admins").build();
//        }
//
//        if (!hasRole(seller, RoleTypes.SELLER) || seller.getCompany() == null || !seller.getCompany().getId().equals(companyId)) {
//            log.error("User {} is not a seller associated with company {}", sellerEmail, companyId);
//            throw BadRequestException.builder()
//                    .message("User is not a seller associated with the company").build();
//        }
//
//        seller.getUserRoles().add(Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build());
//        CompanyAdminRights newAdminRights = CompanyAdminRights.builder()
//                .company(company)
//                .user(seller)
//                .canAddRemoveSellers(canAddRemoveSellers)
//                .canPromoteDemoteAdmins(canPromoteDemoteAdmins)
//                .canDelegateAdminRights(canDelegateAdminRights)
//                .build();
//        companyAdminRightsRepository.save(newAdminRights);
//        userRepository.save(seller);
//
//        if (company.getPrimaryAdmin() == null) {
//            company.setPrimaryAdmin(seller);
//            companyRepository.save(company);
//        }
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(sellerEmail)
//                .action("PROMOTE_TO_COMPANY_ADMIN")
//                .performedBy(adminEmail)
//                .details("Company ID: " + companyId + ", Rights: " + newAdminRights)
//                .timestamp(LocalDateTime.now())
//                .build());
//    }

//    V0
//    @RateLimiter(name = "manageCompanySeller")
//    public void removeSellerFromCompany(Long companyId, String sellerEmail, String adminEmail) {
//        log.info("removeSellerFromCompany: {} removing seller {} from company {}", adminEmail, sellerEmail, companyId);
//        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Admin not found with email: " + adminEmail).build());
//        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Seller not found with email: " + sellerEmail).build());
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> ResourceNotFoundException.builder()
//                        .message("Company not found with id: " + companyId).build());
//
//        if (!hasRole(admin, "ROLE_COMPANY_ADMIN_SELLER")) {
//            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
//            throw UnauthorizedException.builder().message("User lacks COMPANY_ADMIN_SELLER role").build();
//        }
//
//        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
//                .orElseThrow(() -> UnauthorizedException.builder().message("Admin has no rights for company: " + companyId).build());
//        if (!adminRights.getCanAddRemoveSellers()) {
//            log.error("Admin {} lacks permission to remove sellers", adminEmail);
//            throw UnauthorizedException.builder().message("Admin lacks permission to remove sellers").build();
//        }
//
//        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
//            long adminCount = userRepository.countCompanyAdminsByCompanyId(companyId);
//            if (adminCount <= 1) {
//                log.error("Cannot remove the only COMPANY_ADMIN_SELLER for company {}", companyId);
//                throw BadRequestException.builder()
//                        .message("Cannot remove the only COMPANY_ADMIN_SELLER").build();
//            }
//        }
//
//        seller.setCompany(null);
//        companyAdminRightsRepository.deleteByCompanyIdAndUserId(companyId, seller.getId());
//        userRepository.save(seller);
//
//        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
//            company.setPrimaryAdmin(null);
//            companyRepository.save(company);
//        }
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(sellerEmail)
//                .action("REMOVE_SELLER_FROM_COMPANY")
//                .performedBy(adminEmail)
//                .details("Company ID: " + companyId)
//                .timestamp(LocalDateTime.now())
//                .build());
//    }

//    @RateLimiter(name = "manageCompanyAdmin")
//    public void promoteToCompanyAdmin(Long companyId, String sellerEmail, String adminEmail, boolean canAddRemoveSellers, boolean canPromoteDemoteAdmins, boolean canDelegateAdminRights) {
//        log.info("promoteToCompanyAdmin: {} promoting {} to admin for company {}", adminEmail, sellerEmail, companyId);
//        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Admin not found with email: " + adminEmail).build());
//        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Seller not found with email: " + sellerEmail).build());
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> ResourceNotFoundException.builder()
//                        .message("Company not found with id: " + companyId).build());
//
//        if (!hasRole(admin, "ROLE_COMPANY_ADMIN_SELLER")) {
//            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
//            throw UnauthorizedException.builder().message("User lacks COMPANY_ADMIN_SELLER role").build();
//        }
//
//        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
//                .orElseThrow(() -> UnauthorizedException.builder()
//                        .message("Admin has no rights for company: " + companyId).build());
//        if (!adminRights.getCanPromoteDemoteAdmins()) {
//            log.error("Admin {} lacks permission to promote admins", adminEmail);
//            throw UnauthorizedException.builder().message("Admin lacks permission to promote admins").build();
//        }
//
//        if (!hasRole(seller, "ROLE_SELLER") || seller.getCompany() == null || !seller.getCompany().getId().equals(companyId)) {
//            log.error("User {} is not a seller associated with company {}", sellerEmail, companyId);
//            throw BadRequestException.builder().message("User is not a seller associated with the company").build();
//        }
//
//        seller.getUserRoles().add(new Roles("ROLE_COMPANY_ADMIN_SELLER"));
//        CompanyAdminRights newAdminRights = CompanyAdminRights.builder()
//                .company(company)
//                .user(seller)
//                .canAddRemoveSellers(canAddRemoveSellers)
//                .canPromoteDemoteAdmins(canPromoteDemoteAdmins)
//                .canDelegateAdminRights(canDelegateAdminRights)
//                .build();
//        companyAdminRightsRepository.save(newAdminRights);
//        userRepository.save(seller);
//
//        if (company.getPrimaryAdmin() == null) {
//            company.setPrimaryAdmin(seller);
//            companyRepository.save(company);
//        }
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(sellerEmail)
//                .action("PROMOTE_TO_COMPANY_ADMIN")
//                .performedBy(adminEmail)
//                .details("Company ID: " + companyId + ", Rights: " + newAdminRights)
//                .timestamp(LocalDateTime.now())
//                .build());
//    }

//    v2
//    @RateLimiter(name = "manageCompanyAdmin")
//    public void demoteCompanyAdmin(Long companyId, String sellerEmail, String adminEmail) {
//        log.info("demoteCompanyAdmin: {} demoting {} for company {}", adminEmail, sellerEmail, companyId);
//        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Admin not found with email: " + adminEmail).build());
//        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Seller not found with email: " + sellerEmail).build());
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> ResourceNotFoundException.builder()
//                        .message("Company not found with id: " + companyId).build());
//
//        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
//            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
//            throw UnauthorizedException.builder()
//                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
//        }
//
//        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
//                .orElseThrow(() -> UnauthorizedException.builder()
//                        .message("Admin has no rights for company: " + companyId).build());
//        if (!adminRights.getCanPromoteDemoteAdmins()) {
//            log.error("Admin {} lacks permission to demote admins", adminEmail);
//            throw UnauthorizedException.builder()
//                    .message("Admin lacks permission to demote admins").build();
//        }
//
//        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
//            long adminCount = userRepository.countCompanyAdminsByCompanyId(companyId);
//            if (adminCount <= 1) {
//                log.error("Cannot demote the only COMPANY_ADMIN_SELLER for company {}", companyId);
//                throw BadRequestException.builder()
//                        .message("Cannot demote the only COMPANY_ADMIN_SELLER").build();
//            }
//        }
//
//        seller.getUserRoles().removeIf(role -> role.getRole() == RoleTypes.COMPANY_ADMIN_SELLER);
//        companyAdminRightsRepository.deleteByCompanyIdAndUserId(companyId, seller.getId());
//        userRepository.save(seller);
//
//        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
//            company.setPrimaryAdmin(null);
//            companyRepository.save(company);
//        }
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(sellerEmail)
//                .action("DEMOTE_COMPANY_ADMIN")
//                .performedBy(adminEmail)
//                .details("Company ID: " + companyId)
//                .timestamp(LocalDateTime.now())
//                .build());
//    }

//    v1
//    @RateLimiter(name = "manageCompanyAdmin")
//    public void demoteCompanyAdmin(Long companyId, String sellerEmail, String adminEmail) {
//        log.info("demoteCompanyAdmin: {} demoting {} for company {}", adminEmail, sellerEmail, companyId);
//        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
//                .orElseThrow(() -> new UserNotFoundException("Admin not found with email: " + adminEmail));
//        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
//                .orElseThrow(() -> new UserNotFoundException("Seller not found with email: " + sellerEmail));
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> new CompanyNotFoundException("Company not found with id: " + companyId));
//
//        if (!hasRole(admin, "ROLE_COMPANY_ADMIN_SELLER")) {
//            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
//            throw new ForbiddenException("User lacks COMPANY_ADMIN_SELLER role");
//        }
//
//        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
//                .orElseThrow(() -> new ForbiddenException("Admin has no rights for company: " + companyId));
//        if (!adminRights.getCanPromoteDemoteAdmins()) {
//            log.error("Admin {} lacks permission to demote admins", adminEmail);
//            throw new ForbiddenException("Admin lacks permission to demote admins");
//        }
//
//        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
//            long adminCount = userRepository.countCompanyAdminsByCompanyId(companyId);
//            if (adminCount <= 1) {
//                log.error("Cannot demote the only COMPANY_ADMIN_SELLER for company {}", companyId);
//                throw new BadRequestException("Cannot demote the only COMPANY_ADMIN_SELLER");
//            }
//        }
//
//        seller.getUserRoles().removeIf(role -> role.getRole().equals("ROLE_COMPANY_ADMIN_SELLER"));
//        companyAdminRightsRepository.deleteByCompanyIdAndUserId(companyId, seller.getId());
//        userRepository.save(seller);
//
//        if (company.getPrimaryAdmin() != null && company.getPrimaryAdmin().getId().equals(seller.getId())) {
//            company.setPrimaryAdmin(null);
//            companyRepository.save(company);
//        }
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(sellerEmail)
//                .action("DEMOTE_COMPANY_ADMIN")
//                .performedBy(adminEmail)
//                .details("Company ID: " + companyId)
//                .timestamp(LocalDateTime.now())
//                .build());
//    }

//    v2
//    @RateLimiter(name = "manageCompanyAdmin")
//    public void updateAdminRights(Long companyId, String sellerEmail, String adminEmail, boolean canAddRemoveSellers, boolean canPromoteDemoteAdmins, boolean canDelegateAdminRights) {
//        log.info("updateAdminRights: {} updating admin rights for {} in company {}", adminEmail, sellerEmail, companyId);
//        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Admin not found with email: " + adminEmail).build());
//        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
//                .orElseThrow(() -> UserNotFoundException.builder()
//                        .message("Seller not found with email: " + sellerEmail).build());
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> ResourceNotFoundException.builder()
//                        .message("Company not found with id: " + companyId).build());
//
//        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
//            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
//            throw UnauthorizedException.builder()
//                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
//        }
//
//        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
//                .orElseThrow(() -> UnauthorizedException.builder()
//                        .message("Admin has no rights for company: " + companyId).build());
//        if (!adminRights.getCanDelegateAdminRights()) {
//            log.error("Admin {} lacks permission to delegate admin rights", adminEmail);
//            throw UnauthorizedException.builder()
//                    .message("Admin lacks permission to delegate admin rights").build();
//        }
//
//        CompanyAdminRights sellerRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())
//                .orElseThrow(() -> BadRequestException.builder()
//                        .message("Seller is not a company admin for company: " + companyId).build());
//
//        sellerRights.setCanAddRemoveSellers(canAddRemoveSellers);
//        sellerRights.setCanPromoteDemoteAdmins(canPromoteDemoteAdmins);
//        sellerRights.setCanDelegateAdminRights(canDelegateAdminRights);
//        companyAdminRightsRepository.save(sellerRights);
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(sellerEmail)
//                .action("UPDATE_ADMIN_RIGHTS")
//                .performedBy(adminEmail)
//                .details("Company ID: " + companyId + ", Rights: " + sellerRights)
//                .timestamp(LocalDateTime.now())
//                .build());
//    }

//    v1
//    @RateLimiter(name = "manageCompanyAdmin", key = "#root.methodName + ':' + #adminEmail")
//    public void updateAdminRights(Long companyId, String sellerEmail, String adminEmail, boolean canAddRemoveSellers, boolean canPromoteDemoteAdmins, boolean canDelegateAdminRights) {
//        log.info("updateAdminRights: {} updating admin rights for {} in company {}", adminEmail, sellerEmail, companyId);
//        User admin = userRepository.findByEmailAddressAndDeletedFalse(adminEmail)
//                .orElseThrow(() -> new UserNotFoundException("Admin not found with email: " + adminEmail));
//        User seller = userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)
//                .orElseThrow(() -> new UserNotFoundException("Seller not found with email: " + sellerEmail));
//        Company company = companyRepository.findById(companyId)
//                .orElseThrow(() -> new CompanyNotFoundException("Company not found with id: " + companyId));
//
//        if (!hasRole(admin, "ROLE_COMPANY_ADMIN_SELLER")) {
//            log.error("User {} lacks COMPANY_ADMIN_SELLER role", adminEmail);
//            throw new ForbiddenException("User lacks COMPANY_ADMIN_SELLER role");
//        }
//
//        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
//                .orElseThrow(() -> new ForbiddenException("Admin has no rights for company: " + companyId));
//        if (!adminRights.getCanDelegateAdminRights()) {
//            log.error("Admin {} lacks permission to delegate admin rights", adminEmail);
//            throw new ForbiddenException("Admin lacks permission to delegate admin rights");
//        }
//
//        CompanyAdminRights sellerRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())
//                .orElseThrow(() -> new BadRequestException("Seller is not a company admin for company: " + companyId));
//
//        sellerRights.setCanAddRemoveSellers(canAddRemoveSellers);
//        sellerRights.setCanPromoteDemoteAdmins(canPromoteDemoteAdmins);
//        sellerRights.setCanDelegateAdminRights(canDelegateAdminRights);
//        companyAdminRightsRepository.save(sellerRights);
//
//        auditLogRepository.save(AuditLog.builder()
//                .userEmail(sellerEmail)
//                .action("UPDATE_ADMIN_RIGHTS")
//                .performedBy(adminEmail)
//                .details("Company ID: " + companyId + ", Rights: " + sellerRights)
//                .timestamp(LocalDateTime.now())
//                .build());
//    }

//    private boolean hasRole(User user, String roleName) {
//        return user.getUserRoles().stream().anyMatch(role -> role.getRole().equals(roleName));
//    }


    @Override
    @RateLimiter(name = "manageCompany")
    @Transactional
    public void deleteCompany(Long companyId) throws MessagingException {
        log.info("deleteCompany: deleting company {}", companyId);
        User admin = currentUserService.getCurrentUser();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (company.isDeleted()) {
            log.error("Company {} is already deleted", companyId);
            throw BadRequestException.builder()
                    .message("Company is already deleted").build();
        }

        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", admin.getEmailAddress());
            throw UnauthorizedException.builder()
                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Admin has no rights for company: " + companyId).build());
        if (!adminRights.getCanDelegateAdminRights()) {
            log.error("Admin {} lacks permission to delete company", admin.getEmailAddress());
            throw UnauthorizedException.builder()
                    .message("Admin lacks permission to delete company").build();
        }


        // Remove associated users
        List<User> associatedUsers = userRepository.findByCompanyId(companyId);
        for (User user : associatedUsers) {
            user.setCompany(null);
            user.getUserRoles().removeIf(role -> role.getRole() == RoleTypes.COMPANY_ADMIN_SELLER);
            userRepository.save(user);
            companyAdminRightsRepository.deleteByCompanyIdAndUserId(companyId, user.getId());
            emailService.sendEmail(user.getEmailAddress(),
                    "Company Deletion Notification",
                    "The company " + company.getName() + " has been deleted. Your association with this company has been removed.");

            auditLogService.logAction(admin.getEmailAddress(), "DELETE_COMPANY - REMOVE USER ASSOCIATION", user.getEmailAddress(),
                    "Company ID " + companyId + " (" + company.getName() + ") deleted by " + admin.getEmailAddress() + ", affected user: " + user.getEmailAddress());
        }

        // Delete associated admin rights
        companyAdminRightsRepository.deleteByCompanyId(companyId);

        // Remove associated products
        List<Product> associatedProducts = productRepository.findByCompanyId(companyId);
        for (Product product : associatedProducts) {
            product.setCompany(null);
            productRepository.save(product);
            auditLogService.logAction(admin.getEmailAddress(), "DELETE_COMPANY - REMOVE PRODUCT ASSOCIATION",
                    "Company ID " + companyId + " (" + company.getName() + ") deleted by " + admin.getEmailAddress() +
                            ", affected product: " + product.getName() + ", affected productId: " + product.getId());
        }

        auditLogService.logAction(admin.getEmailAddress(), "DELETE_COMPANY",
                "Company ID: " + companyId + " (" + company.getName() + ") having primary admin " +
                        (company.getPrimaryAdmin() != null ? company.getPrimaryAdmin().getEmailAddress() + "(userId: " + company.getPrimaryAdmin().getId() + ")" : "none") +
                        ", deleted by " + admin.getEmailAddress() + ", affected users: " + associatedUsers.size());
        // Mark company as deleted
        company.setDeleted(true);
        company.setPrimaryAdmin(null);
        company.setDeletedByUserId(admin.getId());
        companyRepository.save(company);

    }

    @Deprecated
    @RateLimiter(name = "manageCompany")
    @Transactional
    public void revokeCompanyV0(Long companyId) throws MessagingException {
        log.info("revokeCompany: revoking company {}", companyId);
        User admin = currentUserService.getCurrentUser();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (!company.isDeleted()) {
            log.error("Company {} is not deleted and cannot be revoked", companyId);
            throw BadRequestException.builder()
                    .message("Company is not deleted").build();
        }

        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", admin.getEmailAddress());
            throw UnauthorizedException.builder()
                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        // Check if admin has rights for this company (assuming they were previously associated)
        // but in deleteCompany, deleted associated admin rights
        CompanyAdminRights adminRights = companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())
                .orElseThrow(() -> UnauthorizedException.builder()
                        .message("Admin has no rights for company: " + companyId).build());
        if (!adminRights.getCanDelegateAdminRights()) {
            log.error("Admin {} lacks permission to revoke company", admin.getEmailAddress());
            throw UnauthorizedException.builder()
                    .message("Admin lacks permission to revoke company").build();
        }

        // Check associated users
        List<User> associatedUsers = userRepository.findByCompanyId(companyId);

        // should proceed with revocation
        // Verify, logically the following code-block should never execute as the users, in deleteCompany method, were de-associated while company deletion
        for (User user : associatedUsers) {
            if (user.getDeleted()) {
                log.error("Revoking company {}: but associated user {} is deleted", companyId, user.getEmailAddress());
                //throw BadRequestException.builder().message("Cannot revoke company: associated user " + user.getEmailAddress() + " is deleted").build();
            }
        }

        // Mark company as not deleted
        company.setDeleted(false);
        company.setPrimaryAdmin(admin);
        companyRepository.save(company);

        // Verify, logically the following code-block should never execute as the users, in deleteCompany method, were de-associated while company deletion
        // Notify associated users
        for (User user : associatedUsers) {
            if (!user.getDeleted()) {
                emailService.sendEmail(user.getEmailAddress(),
                        "Company Revocation Notification",
                        "The company " + company.getName() + " has been restored. Your association with this company remains active.");

                auditLogService.logAction(admin.getEmailAddress(), "REVOKE_COMPANY - RESTORE USER ASSOCIATION", user.getEmailAddress(),
                        "Company ID " + companyId + " (" + company.getName() + ") revoked by " + admin.getEmailAddress() + ", affected user: " + user.getEmailAddress());

            }
        }

        auditLogService.logAction(admin.getEmailAddress(), "REVOKE_COMPANY",
                "Company ID " + companyId + " (" + company.getName() + ") revoked by " + admin.getEmailAddress() + ", affected users: " + associatedUsers.size());
    }

    @Override
    @RateLimiter(name = "manageCompany")
    @Transactional
    public void revokeCompany(Long companyId) throws MessagingException {
        log.info("revokeCompany: revoking company {}", companyId);
        User admin = currentUserService.getCurrentUser();
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.builder()
                        .message("Company not found with id: " + companyId).build());

        if (!company.isDeleted()) {
            log.error("Company {} is not deleted and cannot be revoked", companyId);
            throw BadRequestException.builder()
                    .message("Company is not deleted").build();
        }

        if (!hasRole(admin, RoleTypes.COMPANY_ADMIN_SELLER)) {
            log.error("User {} lacks COMPANY_ADMIN_SELLER role", admin.getEmailAddress());
            throw UnauthorizedException.builder()
                    .message("User lacks COMPANY_ADMIN_SELLER role").build();
        }

        if (!admin.getId().equals(company.getDeletedByUserId())) {
            log.error("User {} is not authorized to revoke company {} (only the deleting admin can revoke)", admin.getEmailAddress(), companyId);
            throw UnauthorizedException.builder()
                    .message("Only the admin who deleted the company can revoke it").build();
        }

        company.setDeleted(false);
        company.setPrimaryAdmin(admin);
        companyRepository.save(company);

        auditLogService.logAction(admin.getEmailAddress(), "REVOKE_COMPANY",
                "Company ID: " + companyId + " (" + company.getName() + ") revoked by " + admin.getEmailAddress());
    }

}
