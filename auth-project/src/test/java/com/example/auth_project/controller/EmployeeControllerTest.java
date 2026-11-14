package com.example.auth_project.controller;

import com.example.auth_project.model.request.UpdateEmployeeRequest;
import com.example.auth_project.model.response.EmployeeResponse;
import com.example.auth_project.service.EmployeeService;
import com.example.auth_project.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    // Dibutuhkan agar context bisa dimuat (JwtFilter ikut dibuat sebagai bean)
    @MockBean
    private JwtService jwtService;

    // Principal dikirim langsung ke request, sehingga parameter Authentication terisi
    private final UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("budi", null, Collections.emptyList());

    // ==================== GET /me/employee ====================

    @Test
    void shouldReturnCurrentEmployee() throws Exception {
        when(employeeService.getEmployeeByUsername("budi")).thenReturn(new EmployeeResponse());

        mockMvc.perform(get("/me/employee").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee").exists());

        verify(employeeService).getEmployeeByUsername("budi");
    }

    @Test
    void shouldReturnBadRequestWhenEmployeeNotFound() throws Exception {
        when(employeeService.getEmployeeByUsername("budi"))
                .thenThrow(new RuntimeException("Employee not found."));

        mockMvc.perform(get("/me/employee").principal(auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Employee not found."));
    }

    // ==================== PUT /me/employee/edit ====================

    @Test
    void shouldUpdateCurrentEmployee() throws Exception {
        when(employeeService.editEmployee(eq("budi"), any(UpdateEmployeeRequest.class)))
                .thenReturn(new EmployeeResponse());

        mockMvc.perform(put("/me/employee/edit")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee").exists())
                .andExpect(jsonPath("$.message").value("Employee updated successfully."));

        verify(employeeService).editEmployee(eq("budi"), any(UpdateEmployeeRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUpdateFails() throws Exception {
        when(employeeService.editEmployee(anyString(), any(UpdateEmployeeRequest.class)))
                .thenThrow(new RuntimeException("Update failed."));

        mockMvc.perform(put("/me/employee/edit")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Update failed."));
    }

    // ==================== GET /employee/all ====================

    @Test
    void shouldReturnAllEmployees() throws Exception {
        when(employeeService.findAllEmployee()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/employee/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee").isArray())
                .andExpect(jsonPath("$.message").value("Get all employee successfully."));

        verify(employeeService).findAllEmployee();
    }

    @Test
    void shouldReturnBadRequestWhenGetAllEmployeeFails() throws Exception {
        when(employeeService.findAllEmployee())
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/employee/all"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Database error"));
    }

    // ==================== GET /employee/all/summary ====================

    @Test
    void shouldReturnAllEmployeeSummary() throws Exception {
        when(employeeService.findAllInIdAndName()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/employee/all/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee").isArray())
                .andExpect(jsonPath("$.message").value("Get all employee successfully."));

        verify(employeeService).findAllInIdAndName();
    }

    @Test
    void shouldReturnBadRequestWhenSummaryFails() throws Exception {
        when(employeeService.findAllInIdAndName())
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/employee/all/summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Database error"));
    }
}