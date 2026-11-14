package com.example.auth_project.service;

import com.example.auth_project.model.entity.Employee;
import com.example.auth_project.model.entity.Timesheet;
import com.example.auth_project.model.entity.User;
import com.example.auth_project.model.request.CreateTimesheetRequest;
import com.example.auth_project.model.request.UpdateTimesheetByMakerRequest;
import com.example.auth_project.model.request.UpdateTimesheetRequest;
import com.example.auth_project.model.response.AdminResponse;
import com.example.auth_project.model.response.TimesheetResponse;
import com.example.auth_project.repository.EmployeeRepository;
import com.example.auth_project.repository.TimesheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceTest {

    @Mock
    private TimesheetRepository timesheetRepo;

    @Mock
    private EmployeeRepository employeeRepo;

    @InjectMocks
    private TimesheetService timesheetService;

    private Employee maker;
    private Employee checker;
    private Employee pLead;
    private Timesheet timesheet;

    @BeforeEach
    void setUp() {
        maker = createEmployee(1L, "budi", "Budi Santoso");
        checker = createEmployee(2L, "siti", "Siti Checker");
        pLead = createEmployee(3L, "andi", "Andi Lead");

        timesheet = new Timesheet();
        timesheet.setId(10L);
        timesheet.setPName("Project A");
        timesheet.setMaker(maker);
        timesheet.setChecker(checker);
        timesheet.setPLead(pLead);
        timesheet.setLocation("Jakarta");
        timesheet.setRemarks("Remarks awal");
        timesheet.setCheckerStatus("Pending");
    }

    // Helper: membuat Employee lengkap dengan User, seperti data normal di database
    private Employee createEmployee(Long empId, String username, String name) {
        User user = new User();
        user.setId(empId);
        user.setUsername(username);

        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setEmpName(name);
        employee.setUser(user);
        return employee;
    }

    // ==================== createTimesheet ====================

    @Test
    void shouldCreateTimesheetWithPendingStatus() {
        CreateTimesheetRequest request = new CreateTimesheetRequest();
        request.setName("Project A");
        request.setLeadId(3L);
        request.setCheckerId(2L);
        request.setLocation("Jakarta");
        request.setRemarks("Remarks");

        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.of(maker));
        when(employeeRepo.findById(3L)).thenReturn(Optional.of(pLead));
        when(employeeRepo.findById(2L)).thenReturn(Optional.of(checker));

        Timesheet result = timesheetService.createTimesheet("budi", request);

        ArgumentCaptor<Timesheet> captor = ArgumentCaptor.forClass(Timesheet.class);
        verify(timesheetRepo).save(captor.capture());
        Timesheet saved = captor.getValue();

        assertThat(result).isSameAs(saved);
        assertThat(saved.getPName()).isEqualTo("Project A");
        assertThat(saved.getMaker()).isSameAs(maker);
        assertThat(saved.getPLead()).isSameAs(pLead);
        assertThat(saved.getChecker()).isSameAs(checker);
        assertThat(saved.getCheckerStatus()).isEqualTo("Pending");
        assertThat(saved.getCheckerRemark()).isNull();
        assertThat(saved.getSubmitDate()).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenMakerNotFoundOnCreate() {
        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timesheetService.createTimesheet("budi", new CreateTimesheetRequest()))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Employee not found.");

        verify(timesheetRepo, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenLeaderNotFoundOnCreate() {
        CreateTimesheetRequest request = new CreateTimesheetRequest();
        request.setLeadId(99L);

        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.of(maker));
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timesheetService.createTimesheet("budi", request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Employee leader not found.");

        verify(timesheetRepo, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCheckerNotFoundOnCreate() {
        CreateTimesheetRequest request = new CreateTimesheetRequest();
        request.setLeadId(3L);
        request.setCheckerId(99L);

        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.of(maker));
        when(employeeRepo.findById(3L)).thenReturn(Optional.of(pLead));
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timesheetService.createTimesheet("budi", request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Employee checker not found.");

        verify(timesheetRepo, never()).save(any());
    }

    // ==================== editTimeSheetByChecker ====================

    @Test
    void shouldUpdateRemarkAndStatusByAssignedChecker() {
        UpdateTimesheetRequest request = new UpdateTimesheetRequest();
        request.setCheckerRemark("Sudah sesuai");
        request.setCheckerStatus("Approved");

        when(employeeRepo.findByUserUsername("siti")).thenReturn(Optional.of(checker));
        when(timesheetRepo.findById(10L)).thenReturn(Optional.of(timesheet));

        Timesheet result = timesheetService.editTimeSheetByChecker("siti", request, 10L);

        assertThat(result.getCheckerRemark()).isEqualTo("Sudah sesuai");
        assertThat(result.getCheckerStatus()).isEqualTo("Approved");
        assertThat(result.getCheckerSubmitDate()).isNotNull();
        verify(timesheetRepo).save(timesheet);
    }

    @Test
    void shouldThrowExceptionWhenCheckerEditNotFoundInTimesheet() {
        when(employeeRepo.findByUserUsername("siti")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                timesheetService.editTimeSheetByChecker("siti", new UpdateTimesheetRequest(), 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Employee not found.");

        verify(timesheetRepo, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenTimesheetNotFoundOnCheckerEdit() {
        when(employeeRepo.findByUserUsername("siti")).thenReturn(Optional.of(checker));
        when(timesheetRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                timesheetService.editTimeSheetByChecker("siti", new UpdateTimesheetRequest(), 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet not found.");

        verify(timesheetRepo, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenNonAssignedCheckerEdits() {
        Employee otherChecker = createEmployee(5L, "rina", "Rina Lain");

        when(employeeRepo.findByUserUsername("rina")).thenReturn(Optional.of(otherChecker));
        when(timesheetRepo.findById(10L)).thenReturn(Optional.of(timesheet));

        assertThatThrownBy(() ->
                timesheetService.editTimeSheetByChecker("rina", new UpdateTimesheetRequest(), 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You do not have permission to edit this data.");

        verify(timesheetRepo, never()).save(any());
    }

    // ==================== editTimesheetByMaker ====================

    @Test
    void shouldUpdateTimesheetByOwnerWithoutChangingCheckerAndLead() {
        UpdateTimesheetByMakerRequest request = new UpdateTimesheetByMakerRequest();
        request.setName("Project B");
        request.setRemarks("Remarks baru");
        request.setLocation("Bandung");

        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.of(maker));
        when(timesheetRepo.findById(10L)).thenReturn(Optional.of(timesheet));

        Timesheet result = timesheetService.editTimesheetByMaker("budi", request, 10L);

        assertThat(result.getPName()).isEqualTo("Project B");
        assertThat(result.getRemarks()).isEqualTo("Remarks baru");
        assertThat(result.getLocation()).isEqualTo("Bandung");
        assertThat(result.getChecker()).isSameAs(checker);
        assertThat(result.getPLead()).isSameAs(pLead);
        verify(timesheetRepo).save(timesheet);
        verify(employeeRepo, never()).findById(any());
    }

    @Test
    void shouldUpdateCheckerAndLeadWhenIdsProvided() {
        Employee newChecker = createEmployee(6L, "dewi", "Dewi Checker");
        Employee newLead = createEmployee(7L, "eko", "Eko Lead");

        UpdateTimesheetByMakerRequest request = new UpdateTimesheetByMakerRequest();
        request.setName("Project A");
        request.setCheckerId(6L);
        request.setLeadId(7L);

        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.of(maker));
        when(timesheetRepo.findById(10L)).thenReturn(Optional.of(timesheet));
        when(employeeRepo.findById(6L)).thenReturn(Optional.of(newChecker));
        when(employeeRepo.findById(7L)).thenReturn(Optional.of(newLead));

        Timesheet result = timesheetService.editTimesheetByMaker("budi", request, 10L);

        assertThat(result.getChecker()).isSameAs(newChecker);
        assertThat(result.getPLead()).isSameAs(newLead);
        verify(timesheetRepo).save(timesheet);
    }

    @Test
    void shouldThrowExceptionWhenNonOwnerEditsTimesheet() {
        Employee otherMaker = createEmployee(5L, "rina", "Rina Lain");

        UpdateTimesheetByMakerRequest request = new UpdateTimesheetByMakerRequest();
        request.setName("Hacked");

        when(employeeRepo.findByUserUsername("rina")).thenReturn(Optional.of(otherMaker));
        when(timesheetRepo.findById(10L)).thenReturn(Optional.of(timesheet));

        assertThatThrownBy(() -> timesheetService.editTimesheetByMaker("rina", request, 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You do not have permission to edit this data.");

        verify(timesheetRepo, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenCheckerNotFoundOnMakerEdit() {
        UpdateTimesheetByMakerRequest request = new UpdateTimesheetByMakerRequest();
        request.setCheckerId(99L);

        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.of(maker));
        when(timesheetRepo.findById(10L)).thenReturn(Optional.of(timesheet));
        when(employeeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timesheetService.editTimesheetByMaker("budi", request, 10L))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Employee checker not found.");

        verify(timesheetRepo, never()).save(any());
    }

    // ==================== getTimesheetById ====================

    @Test
    void shouldReturnTimesheetDetailById() {
        when(timesheetRepo.findById(10L)).thenReturn(Optional.of(timesheet));
        when(employeeRepo.findById(2L)).thenReturn(Optional.of(checker));
        when(employeeRepo.findById(3L)).thenReturn(Optional.of(pLead));

        TimesheetResponse result = timesheetService.getTimesheetById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getPName()).isEqualTo("Project A");
        assertThat(result.getLocation()).isEqualTo("Jakarta");
        assertThat(result.getChecker()).isNotNull();
        assertThat(result.getPLead()).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenTimesheetNotFoundById() {
        when(timesheetRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timesheetService.getTimesheetById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet not found.");
    }

    @Test
    void shouldThrowExceptionWhenCheckerNotFoundById() {
        when(timesheetRepo.findById(10L)).thenReturn(Optional.of(timesheet));
        when(employeeRepo.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timesheetService.getTimesheetById(10L))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Employee checker not found.");
    }

    // ==================== getAllTimesheet ====================

    @Test
    void shouldReturnTimesheetsCreatedByUser() {
        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.of(maker));
        when(timesheetRepo.findByMakerId(1L)).thenReturn(List.of(timesheet));

        List<TimesheetResponse> result = timesheetService.getAllTimesheet("budi");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getPName()).isEqualTo("Project A");
        verify(timesheetRepo).findByMakerId(1L);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundOnGetAll() {
        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timesheetService.getAllTimesheet("budi"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    // ==================== getAllTimesheetByChecker ====================

    @Test
    void shouldReturnTimesheetsAssignedToChecker() {
        when(employeeRepo.findByUserUsername("siti")).thenReturn(Optional.of(checker));
        when(timesheetRepo.findByCheckerId(2L)).thenReturn(List.of(timesheet));

        List<TimesheetResponse> result = timesheetService.getAllTimesheetByChecker("siti");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        verify(timesheetRepo).findByCheckerId(2L);
    }

    @Test
    void shouldThrowExceptionWhenCheckerNotFoundOnGetAll() {
        when(employeeRepo.findByUserUsername("siti")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timesheetService.getAllTimesheetByChecker("siti"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    // ==================== deleteTimesheetByMaker ====================

    @Test
    void shouldDeleteTimesheetByOwner() {
        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.of(maker));
        when(timesheetRepo.findById(10L)).thenReturn(Optional.of(timesheet));

        timesheetService.deleteTimesheetByMaker("budi", 10L);

        verify(timesheetRepo).delete(timesheet);
    }

    @Test
    void shouldThrowExceptionWhenNonOwnerDeletesTimesheet() {
        Employee otherMaker = createEmployee(5L, "rina", "Rina Lain");

        when(employeeRepo.findByUserUsername("rina")).thenReturn(Optional.of(otherMaker));
        when(timesheetRepo.findById(10L)).thenReturn(Optional.of(timesheet));

        assertThatThrownBy(() -> timesheetService.deleteTimesheetByMaker("rina", 10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("You do not have permission to edit this data.");

        verify(timesheetRepo, never()).delete(any());
    }

    @Test
    void shouldThrowExceptionWhenTimesheetNotFoundOnDelete() {
        when(employeeRepo.findByUserUsername("budi")).thenReturn(Optional.of(maker));
        when(timesheetRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timesheetService.deleteTimesheetByMaker("budi", 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timesheet not found.");

        verify(timesheetRepo, never()).delete(any());
    }

    // ==================== getAllTimesheetByAdmin ====================

    @Test
    void shouldReturnAllTimesheetsForAdmin() {
        when(timesheetRepo.findAll()).thenReturn(List.of(timesheet));

        List<AdminResponse> result = timesheetService.getAllTimesheetByAdmin();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaker()).isEqualTo("Budi Santoso");
        assertThat(result.get(0).getChecker()).isEqualTo("Siti Checker");
        assertThat(result.get(0).getStatus()).isEqualTo("Pending");
    }

    @Test
    void shouldReturnEmptyListForAdminWhenNoTimesheet() {
        when(timesheetRepo.findAll()).thenReturn(Collections.emptyList());

        assertThat(timesheetService.getAllTimesheetByAdmin()).isEmpty();
    }
}