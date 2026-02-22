package com.lenovo.smart_office_booking.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lenovo.smart_office_booking.domain.Booking;
import com.lenovo.smart_office_booking.domain.enums.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
            SELECT b
            FROM Booking b
                                                JOIN FETCH b.requester
                                                JOIN FETCH b.resource
            WHERE b.resource.id = :resourceId
              AND b.status IN :activeStatuses
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    List<Booking> findOverlappingBookings(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("activeStatuses") List<BookingStatus> activeStatuses
    );

    @Query("""
            SELECT b
            FROM Booking b
            JOIN FETCH b.resource
            WHERE b.status IN :statuses
              AND b.startTime < :rangeEnd
              AND b.endTime > :rangeStart
            """)
    List<Booking> findByStatusInAndTimeRange(
            @Param("statuses") List<BookingStatus> statuses,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd
    );

    @Query("""
            SELECT b
            FROM Booking b
            JOIN FETCH b.resource
            JOIN FETCH b.requester
            WHERE b.requester.username = :username
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findByRequesterUsernameDetailed(@Param("username") String username);

    @Query("""
            SELECT b
            FROM Booking b
            JOIN FETCH b.resource
            JOIN FETCH b.requester
            WHERE b.id = :id
            """)
    java.util.Optional<Booking> findDetailedById(@Param("id") Long id);
}