package com.example.auth_project.controller;

import com.example.auth_project.model.entity.Timesheet;
import com.example.auth_project.model.request.CreateTimesheetRequest;
import com.example.auth_project.model.request.UpdateTimesheetByMakerRequest;
import com.example.auth_project.model.request.UpdateTimesheetRequest;
import com.example.auth_project.model.response.TimesheetResponse;
import com.example.auth_project.service.JwtService;
import com.example.auth_project.service.TimesheetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimesheetController.class)
@AutoConfigureMockMvc(addFilters = false)
class TimesheetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TimesheetService timesheetService;

    // Dibutuhkan agar context bisa dimuat (JwtFilter ikut dibuat sebagai bean)
    @MockBean
    private JwtService jwtService;

    // Principal dikirim langsung ke request, sehingga parameter Authentication terisi
    private final UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken("budi", null, Collections.emptyList());

    // ==================== GET /timesheet/all ====================

    @Test
    void shouldReturnOwnTimesheetList() throws Exception {
        when(timesheetService.getAllTimesheet("budi")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/timesheet/all").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timesheetList").isArray());

        verify(timesheetService).getAllTimesheet("budi");
    }

    @Test
    void shouldReturnBadRequestWhenGetAllFails() throws Exception {
        when(timesheetService.getAllTimesheet("budi"))
                .thenThrow(new RuntimeException("Failed to load timesheet"));

        mockMvc.perform(get("/timesheet/all").principal(auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to load timesheet"));
    }

    // ==================== GET /timesheet/checker/all ====================

    @Test
    void shouldReturnTimesheetListForChecker() throws Exception {
        when(timesheetService.getAllTimesheetByChecker("budi")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/timesheet/checker/all").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timesheetList").isArray());

        verify(timesheetService).getAllTimesheetByChecker("budi");
    }

    @Test
    void shouldReturnBadRequestWhenCheckerListFails() throws Exception {
        when(timesheetService.getAllTimesheetByChecker("budi"))
                .thenThrow(new RuntimeException("Checker not found."));

        mockMvc.perform(get("/timesheet/checker/all").principal(auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Checker not found."));
    }

    // ==================== GET /timesheet/{id} ====================

    @Test
    void shouldReturnTimesheetById() throws Exception {
        when(timesheetService.getTimesheetById(1L)).thenReturn(new TimesheetResponse());

        mockMvc.perform(get("/timesheet/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timesheet").exists());

        verify(timesheetService).getTimesheetById(1L);
    }

    @Test
    void shouldReturnBadRequestWhenTimesheetNotFound() throws Exception {
        when(timesheetService.getTimesheetById(99L))
                .thenThrow(new RuntimeException("Timesheet not found."));

        mockMvc.perform(get("/timesheet/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Timesheet not found."));
    }

    // ==================== POST /timesheet/create ====================

    @Test
    void shouldCreateTimesheetAndReturnCreated() throws Exception {
        when(timesheetService.createTimesheet(eq("budi"), any(CreateTimesheetRequest.class)))
                .thenReturn(new Timesheet());

        mockMvc.perform(post("/timesheet/create")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Timesheet created successfully."));

        verify(timesheetService).createTimesheet(eq("budi"), any(CreateTimesheetRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenCreateFails() throws Exception {
        when(timesheetService.createTimesheet(anyString(), any(CreateTimesheetRequest.class)))
                .thenThrow(new RuntimeException("Timesheet already exists."));

        mockMvc.perform(post("/timesheet/create")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Timesheet already exists."));
    }

    // ==================== PUT /timesheet/checker/edit/{id} ====================

    @Test
    void shouldUpdateTimesheetByChecker() throws Exception {
        when(timesheetService.editTimeSheetByChecker(eq("budi"), any(UpdateTimesheetRequest.class), eq(1L)))
                .thenReturn(new Timesheet());

        mockMvc.perform(put("/timesheet/checker/edit/1")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "checkerStatus": "APPROVED"
                        }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Timesheet updated successfully."));

        verify(timesheetService).editTimeSheetByChecker(eq("budi"), any(UpdateTimesheetRequest.class), eq(1L));
    }

    @Test
    void shouldReturnBadRequestWhenCheckerEditFails() throws Exception {
        when(timesheetService.editTimeSheetByChecker(anyString(), any(UpdateTimesheetRequest.class), anyLong()))
                .thenThrow(new RuntimeException("Timesheet is already approved."));

        mockMvc.perform(put("/timesheet/checker/edit/1")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "checkerStatus": "APPROVED"
                        }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Timesheet is already approved."));
    }

    // ==================== PUT /timesheet/maker/edit/{id} ====================

    @Test
    void shouldUpdateTimesheetByMaker() throws Exception {
        when(timesheetService.editTimesheetByMaker(eq("budi"), any(UpdateTimesheetByMakerRequest.class), eq(1L)))
                .thenReturn(new Timesheet());

        mockMvc.perform(put("/timesheet/maker/edit/1")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "name": "Budi Santoso",
                            "leadId": 1,
                            "location": "Jakarta",
                            "remarks": "Updated timesheet",
                            "checkerId": 2
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Timesheet updated successfully."));

        verify(timesheetService).editTimesheetByMaker(eq("budi"), any(UpdateTimesheetByMakerRequest.class), eq(1L));
    }

    @Test
    void shouldReturnBadRequestWhenMakerEditFails() throws Exception {
        when(timesheetService.editTimesheetByMaker(anyString(), any(UpdateTimesheetByMakerRequest.class), anyLong()))
                .thenThrow(new RuntimeException("Timesheet not found."));

        mockMvc.perform(put("/timesheet/maker/edit/1")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "name": "Budi Santoso",
                            "leadId": 1,
                            "location": "Jakarta",
                            "remarks": "Updated timesheet",
                            "checkerId": 2
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Timesheet not found."));
    }

    // ==================== DELETE /timesheet/delete/{id} ====================

    @Test
    void shouldDeleteTimesheet() throws Exception {
        mockMvc.perform(delete("/timesheet/delete/1").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Timesheet deleted successfully."));

        verify(timesheetService).deleteTimesheetByMaker("budi", 1L);
    }

    @Test
    void shouldReturnBadRequestWhenDeleteFails() throws Exception {
        doThrow(new RuntimeException("Timesheet not found."))
                .when(timesheetService).deleteTimesheetByMaker(anyString(), anyLong());

        mockMvc.perform(delete("/timesheet/delete/1").principal(auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Timesheet not found."));
    }
}