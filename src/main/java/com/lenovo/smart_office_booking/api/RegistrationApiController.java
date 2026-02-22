package com.lenovo.smart_office_booking.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lenovo.smart_office_booking.api.dto.RegisterUserRequest;
import com.lenovo.smart_office_booking.service.UserRegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
@Validated
public class RegistrationApiController {

    private final UserRegistrationService userRegistrationService;

    @PostMapping
    public ResponseEntity<String> submit(@Valid @RequestBody RegisterUserRequest request) {
        userRegistrationService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("注册申请已提交，等待管理员审批");
    }
}
