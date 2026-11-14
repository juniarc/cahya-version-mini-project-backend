package com.example.auth_project.utils.mapper;

import com.example.auth_project.model.dto.SimpleEmployeeDTO;
import com.example.auth_project.model.entity.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {

    @Test
    void shouldReturnNullWhenEmployeeIsNull() {
        assertThat(Mapper.toSimple(null)).isNull();
    }

    @Test
    void shouldMapEmpIdAndEmpNameOnly() {
        Employee employee = new Employee();
        employee.setEmpId(1L);
        employee.setEmpName("Budi Santoso");
        employee.setEmpEmail("budi@mail.com");
        employee.setEmpPhone("08123456789");

        SimpleEmployeeDTO result = Mapper.toSimple(employee);

        assertThat(result).isNotNull();
        assertThat(result.getEmpId()).isEqualTo(1L);
        assertThat(result.getEmpName()).isEqualTo("Budi Santoso");
    }

    @Test
    void shouldNotRequireUserToMapSimpleEmployee() {
        // Mapper ini tidak mengakses User, jadi employee tanpa user tetap aman dipetakan
        Employee employee = new Employee();
        employee.setEmpId(2L);
        employee.setEmpName("Siti");

        SimpleEmployeeDTO result = Mapper.toSimple(employee);

        assertThat(result.getEmpId()).isEqualTo(2L);
        assertThat(result.getEmpName()).isEqualTo("Siti");
    }
}