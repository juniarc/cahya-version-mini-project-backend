package com.example.auth_project.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class UpdateEmployeeRequest {
    @NotBlank(message = "Employee Name is required")
    private String empName;

    @NotBlank(message = "Employee Email is required")
    @Email
    private String empEmail;

    @NotBlank(message = "Employee Role is required")
    private String empRole;

    @NotBlank(message = "Employee Phone is required")
    private String empPhone;

    @NotNull
    private Date dateOfJoin;

    @NotNull(message = "Manager is required")
    private Long managerId;
}
