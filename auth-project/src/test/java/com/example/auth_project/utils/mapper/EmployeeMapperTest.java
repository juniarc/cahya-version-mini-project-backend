package com.example.auth_project.utils.mapper;

import com.example.auth_project.model.entity.Employee;
import com.example.auth_project.model.entity.User;
import com.example.auth_project.model.response.EmployeeResponse;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeMapperTest {

    private Employee createEmployee(Long empId, Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("budi");

        Employee employee = new Employee();
        employee.setEmpId(empId);
        employee.setEmpName("Budi Santoso");
        employee.setEmpEmail("budi@mail.com");
        employee.setEmpPhone("08123456789");
        employee.setEmpRole("Staff");
        employee.setDateOfJoin(new Date(0));
        employee.setCreated_at(new Date(1000));
        employee.setUpdated_at(new Date(2000));
        employee.setUser(user);
        return employee;
    }

    @Test
    void shouldReturnNullWhenEmployeeIsNull() {
        assertThat(EmployeeMapper.toResponse(null)).isNull();
    }

    @Test
    void shouldMapAllFieldsFromEmployee() {
        Employee employee = createEmployee(1L, 10L);

        EmployeeResponse response = EmployeeMapper.toResponse(employee);

        assertThat(response.getEmpId()).isEqualTo(1L);
        assertThat(response.getEmpName()).isEqualTo("Budi Santoso");
        assertThat(response.getEmpEmail()).isEqualTo("budi@mail.com");
        assertThat(response.getEmpPhone()).isEqualTo("08123456789");
        assertThat(response.getEmpRole()).isEqualTo("Staff");
        assertThat(response.getDateOfJoin()).isEqualTo(new Date(0));
        assertThat(response.getCreated_at()).isEqualTo(new Date(1000));
        assertThat(response.getUpdated_at()).isEqualTo(new Date(2000));
        assertThat(response.getUserId()).isEqualTo(10L);
    }

    @Test
    void shouldLeaveManagerNullWhenEmployeeHasNoManager() {
        Employee employee = createEmployee(1L, 10L);

        EmployeeResponse response = EmployeeMapper.toResponse(employee);

        assertThat(response.getManager()).isNull();
    }

    @Test
    void shouldMapManagerWhenEmployeeHasManager() {
        Employee manager = createEmployee(2L, 20L);
        manager.setEmpName("Andi Manager");

        Employee employee = createEmployee(1L, 10L);
        employee.setManager(manager);

        EmployeeResponse response = EmployeeMapper.toResponse(employee);

        assertThat(response.getManager()).isNotNull();
        assertThat(response.getManager().getEmpId()).isEqualTo(2L);
        assertThat(response.getManager().getEmpName()).isEqualTo("Andi Manager");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenUserIsNull() {
        Employee employee = createEmployee(1L, 10L);
        employee.setUser(null);

        // Mapper tidak melakukan null-check pada user, jadi data tanpa user akan gagal.
        // Test ini mengunci perilaku tersebut agar perubahan ke depan disadari.
        assertThatThrownBy(() -> EmployeeMapper.toResponse(employee))
                .isInstanceOf(NullPointerException.class);
    }
}