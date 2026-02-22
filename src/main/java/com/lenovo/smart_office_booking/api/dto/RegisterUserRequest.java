package com.lenovo.smart_office_booking.api.dto;

import com.lenovo.smart_office_booking.domain.enums.RoleName;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 80, message = "用户名长度需在3-80之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 120, message = "密码长度需在8-120之间")
        String password,

        @NotBlank(message = "确认密码不能为空")
        @Size(min = 8, max = 120, message = "确认密码长度需在8-120之间")
        String confirmPassword,

        @NotBlank(message = "显示名不能为空")
        @Size(max = 100, message = "显示名最长100字符")
        String displayName,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 150, message = "邮箱最长150字符")
        String email,

        @NotNull(message = "申请角色不能为空")
        RoleName requestedRole
) {
}
