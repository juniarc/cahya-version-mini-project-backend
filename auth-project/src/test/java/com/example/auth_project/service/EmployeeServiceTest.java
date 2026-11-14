package com.example.auth_project.service;

import com.example.auth_project.model.entity.Employee;
import com.example.auth_project.model.entity.User;
import com.example.auth_project.model.projection.EmployeeSummary;
import com.example.auth_project.model.request.UpdateEmployeeRequest;
import com.example.auth_project.model.response.EmployeeResponse;
import com.example.auth_project.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private User user;
    private Employee employee;

    private Employee createEmployee(Long userId, String username) {
        User u = new User();
        u.setId(userId);
        u.setUsername(username);

        Employee e = new Employee();
        e.setUser(u);
        return e;
    }

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("budi");

        employee = new Employee();
        employee.setUser(user);
        employee.setEmpName("Budi Santoso");
        employee.setEmpEmail("budi@mail.com");
        employee.setEmpPhone("08123456789");
        employee.setEmpRole("Staff");
    }

    // ==================== getEmployeeByUsername ====================

    @Test
    void shouldReturnEmployeeByUsername() {
        when(employeeRepository.findByUserUsername("budi")).thenReturn(Optional.of(employee));

        EmployeeResponse result = employeeService.getEmployeeByUsername("budi");

        assertThat(result).isNotNull();
        verify(employeeRepository).findByUserUsername("budi");
    }

    @Test
    void shouldThrowExceptionWhenEmployeeByUsernameNotFound() {
        when(employeeRepository.findByUserUsername("budi")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeByUsername("budi"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Employee not found.");
    }

    // ==================== editEmployee ====================

    @Test
    void shouldUpdateEmployeeFieldsAndSave() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest();
        request.setEmpName("Budi Baru");
        request.setEmpEmail("budibaru@mail.com");
        request.setEmpPhone("08987654321");
        request.setEmpRole("Supervisor");
        request.setDateOfJoin(new Date());
        request.setManagerId(null);

        when(employeeRepository.findByUserUsername("budi")).thenReturn(Optional.of(employee));

        EmployeeResponse result = employeeService.editEmployee("budi", request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();

        assertThat(result).isNotNull();
        assertThat(saved.getEmpName()).isEqualTo("Budi Baru");
        assertThat(saved.getEmpEmail()).isEqualTo("budibaru@mail.com");
        assertThat(saved.getEmpPhone()).isEqualTo("08987654321");
        assertThat(saved.getEmpRole()).isEqualTo("Supervisor");
        assertThat(saved.getDateOfJoin()).isEqualTo(request.getDateOfJoin());
        assertThat(saved.getUpdated_at()).isNotNull();
    }

    @Test
    void shouldSetManagerWhenManagerIdProvided() {
        Employee manager = new Employee();
        manager.setEmpName("Manager");

        UpdateEmployeeRequest request = new UpdateEmployeeRequest();
        request.setEmpName("Budi Santoso");
        request.setManagerId(2L);

        when(employeeRepository.findByUserUsername("budi")).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(manager));

        employeeService.editEmployee("budi", request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getManager()).isSameAs(manager);
    }

    @Test
    void shouldClearManagerWhenManagerIdIsNull() {
        Employee oldManager = new Employee();
        employee.setManager(oldManager);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest();
        request.setEmpName("Budi Santoso");
        request.setManagerId(null);

        when(employeeRepository.findByUserUsername("budi")).thenReturn(Optional.of(employee));

        employeeService.editEmployee("budi", request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getManager()).isNull();
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    void shouldThrowExceptionWhenEditTargetEmployeeNotFound() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest();

        when(employeeRepository.findByUserUsername("budi")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.editEmployee("budi", request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Employee not found.");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenManagerNotFound() {
        UpdateEmployeeRequest request = new UpdateEmployeeRequest();
        request.setManagerId(99L);

        when(employeeRepository.findByUserUsername("budi")).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.editEmployee("budi", request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Manager not found.");

        verify(employeeRepository, never()).save(any());
    }

    // ==================== findAllEmployee ====================

    @Test
    void shouldReturnAllEmployeesOrderedById() {
        Employee second = createEmployee(2L, "siti");

        when(employeeRepository.findAllByOrderByEmpIdAsc()).thenReturn(List.of(employee, second));

        List<EmployeeResponse> result = employeeService.findAllEmployee();

        assertThat(result).hasSize(2);
        verify(employeeRepository).findAllByOrderByEmpIdAsc();
    }

    @Test
    void shouldReturnEmptyListWhenNoEmployee() {
        when(employeeRepository.findAllByOrderByEmpIdAsc()).thenReturn(Collections.emptyList());

        List<EmployeeResponse> result = employeeService.findAllEmployee();

        assertThat(result).isEmpty();
    }

    // ==================== findAllInIdAndName ====================

    @Test
    void shouldReturnEmployeeSummaryList() {
        EmployeeSummary summary = mock(EmployeeSummary.class);
        when(employeeRepository.findAllProjectedByOrderByEmpIdAsc()).thenReturn(List.of(summary));

        List<EmployeeSummary> result = employeeService.findAllInIdAndName();

        assertThat(result).containsExactly(summary);
        verify(employeeRepository).findAllProjectedByOrderByEmpIdAsc();
    }
}