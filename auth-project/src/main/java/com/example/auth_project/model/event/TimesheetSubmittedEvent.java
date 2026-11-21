package com.example.auth_project.model.event;

public record TimesheetSubmittedEvent (Long timesheetId, Long employeeId) {
}
