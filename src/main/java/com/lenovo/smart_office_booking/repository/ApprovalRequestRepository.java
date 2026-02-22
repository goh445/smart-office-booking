package com.lenovo.smart_office_booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lenovo.smart_office_booking.domain.ApprovalRequest;
import com.lenovo.smart_office_booking.domain.enums.ApprovalStatus;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByStatusOrderByCreatedAtAsc(ApprovalStatus status);

    Optional<ApprovalRequest> findByBookingId(Long bookingId);

    @Query("""
            SELECT ar
            FROM ApprovalRequest ar
            WHERE ar.booking.id IN :bookingIds
            """)
    List<ApprovalRequest> findByBookingIdIn(@Param("bookingIds") List<Long> bookingIds);

    @Query("""
            SELECT ar
            FROM ApprovalRequest ar
            JOIN FETCH ar.booking b
            JOIN FETCH b.requester
            JOIN FETCH b.resource
            LEFT JOIN FETCH ar.approver
            WHERE ar.status = :status
            ORDER BY ar.createdAt ASC
            """)
    List<ApprovalRequest> findDetailedByStatus(@Param("status") ApprovalStatus status);

    @Query("""
            SELECT ar
            FROM ApprovalRequest ar
            JOIN FETCH ar.booking b
            JOIN FETCH b.requester
            JOIN FETCH b.resource
            LEFT JOIN FETCH ar.approver
            WHERE ar.id = :id
            """)
    Optional<ApprovalRequest> findDetailedById(@Param("id") Long id);
}