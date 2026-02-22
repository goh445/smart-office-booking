package com.lenovo.smart_office_booking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lenovo.smart_office_booking.api.dto.RegisterUserRequest;
import com.lenovo.smart_office_booking.domain.AppUser;
import com.lenovo.smart_office_booking.domain.Role;
import com.lenovo.smart_office_booking.domain.UserRegistrationRequest;
import com.lenovo.smart_office_booking.domain.enums.RegistrationStatus;
import com.lenovo.smart_office_booking.domain.enums.RoleName;
import com.lenovo.smart_office_booking.repository.AppUserRepository;
import com.lenovo.smart_office_booking.repository.RoleRepository;
import com.lenovo.smart_office_booking.repository.UserRegistrationRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserRegistrationService {

    private final UserRegistrationRequestRepository requestRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationRequest submit(RegisterUserRequest request) {
        if (request.requestedRole() != RoleName.APPROVER && request.requestedRole() != RoleName.EMPLOYEE) {
            throw new IllegalArgumentException("仅支持申请 APPROVER 或 EMPLOYEE 角色");
        }

        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }

        if (appUserRepository.existsByUsername(request.username()) || requestRepository.existsByUsername(request.username())) {
            throw new IllegalStateException("用户名已存在");
        }

        if (appUserRepository.existsByEmail(request.email()) || requestRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("邮箱已存在");
        }

        UserRegistrationRequest entity = new UserRegistrationRequest();
        entity.setUsername(request.username());
        entity.setDisplayName(request.displayName());
        entity.setEmail(request.email());
        entity.setRequestedRole(request.requestedRole());
        entity.setEncodedPassword(passwordEncoder.encode(request.password()));
        entity.setStatus(RegistrationStatus.PENDING);

        return requestRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<UserRegistrationRequest> getPending() {
        return requestRepository.findByStatusDetailed(RegistrationStatus.PENDING);
    }

    public UserRegistrationRequest approve(Long requestId, String reviewerUsername, String comment) {
        UserRegistrationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("注册申请不存在: " + requestId));

        if (request.getStatus() != RegistrationStatus.PENDING) {
            throw new IllegalStateException("申请已处理");
        }

        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("用户名已被占用");
        }
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("邮箱已被占用");
        }

        Role role = roleRepository.findByName(request.getRequestedRole())
                .orElseThrow(() -> new IllegalStateException("角色不存在: " + request.getRequestedRole()));

        AppUser reviewer = appUserRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new IllegalArgumentException("审核人不存在"));

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setDisplayName(request.getDisplayName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getEncodedPassword());
        user.setEnabled(true);
        user.getRoles().add(role);
        appUserRepository.save(user);

        request.setStatus(RegistrationStatus.APPROVED);
        request.setReviewer(reviewer);
        request.setReviewComment(comment);
        request.setDecidedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    public UserRegistrationRequest reject(Long requestId, String reviewerUsername, String comment) {
        UserRegistrationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("注册申请不存在: " + requestId));

        if (request.getStatus() != RegistrationStatus.PENDING) {
            throw new IllegalStateException("申请已处理");
        }

        AppUser reviewer = appUserRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new IllegalArgumentException("审核人不存在"));

        request.setStatus(RegistrationStatus.REJECTED);
        request.setReviewer(reviewer);
        request.setReviewComment(comment);
        request.setDecidedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }
}
