package com.example.auth_project.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTimesheetByMakerRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull
    private Long leadId;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Remark is required")
    private String remarks;

    @NotNull
    private Long checkerId;
}
