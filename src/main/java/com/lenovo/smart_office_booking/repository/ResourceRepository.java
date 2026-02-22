package com.lenovo.smart_office_booking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lenovo.smart_office_booking.domain.Resource;
import com.lenovo.smart_office_booking.domain.enums.ResourceType;

public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    Optional<Resource> findByCode(String code);

    List<Resource> findByTypeAndActiveTrue(ResourceType type);
}