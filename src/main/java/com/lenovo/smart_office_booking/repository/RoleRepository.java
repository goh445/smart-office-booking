package com.lenovo.smart_office_booking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lenovo.smart_office_booking.domain.Role;
import com.lenovo.smart_office_booking.domain.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}