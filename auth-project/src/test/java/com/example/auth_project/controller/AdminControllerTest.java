package com.example.auth_project.controller;

import com.example.auth_project.service.JwtService;
import com.example.auth_project.service.TimesheetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TimesheetService timesheetService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnTimesheetListWhenUserIsAdmin() throws Exception {
        when(timesheetService.getAllTimesheetByAdmin()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/timesheet/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timesheetList").isArray())
                .andExpect(jsonPath("$.timesheetList").isEmpty());

        verify(timesheetService).getAllTimesheetByAdmin();
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenServiceThrowsException() throws Exception {
        when(timesheetService.getAllTimesheetByAdmin())
                .thenThrow(new RuntimeException("Failed to load timesheet"));

        mockMvc.perform(get("/timesheet/admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to load timesheet"));
    }
}