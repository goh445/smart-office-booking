package com.lenovo.smart_office_booking.repository;

import java.util.Optional;











import org.springframework.data.jpa.repository.JpaRepository;

import com.lenovo.smart_office_booking.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}