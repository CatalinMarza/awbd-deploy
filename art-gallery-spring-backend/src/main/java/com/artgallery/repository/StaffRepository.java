package com.artgallery.repository;

import com.artgallery.model.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Page<Staff> findByNameContainingIgnoreCaseOrRoleContainingIgnoreCase(
            String name, String role, Pageable pageable);
}
