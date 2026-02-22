package com.lenovo.smart_office_booking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lenovo.smart_office_booking.domain.UserRegistrationRequest;
import com.lenovo.smart_office_booking.domain.enums.RegistrationStatus;

public interface UserRegistrationRequestRepository extends JpaRepository<UserRegistrationRequest, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("""
            SELECT urr
            FROM UserRegistrationRequest urr
            LEFT JOIN FETCH urr.reviewer
            WHERE urr.status = :status
            ORDER BY urr.createdAt ASC
            """)
    List<UserRegistrationRequest> findByStatusDetailed(@Param("status") RegistrationStatus status);
}
