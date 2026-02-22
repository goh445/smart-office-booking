package com.lenovo.smart_office_booking.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lenovo.smart_office_booking.domain.AppUser;
import com.lenovo.smart_office_booking.domain.Resource;
import com.lenovo.smart_office_booking.domain.Role;
import com.lenovo.smart_office_booking.domain.enums.ResourceType;
import com.lenovo.smart_office_booking.domain.enums.RoleName;
import com.lenovo.smart_office_booking.repository.AppUserRepository;
import com.lenovo.smart_office_booking.repository.ResourceRepository;
import com.lenovo.smart_office_booking.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final AppUserRepository appUserRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = ensureRole(RoleName.ADMIN);
        Role approverRole = ensureRole(RoleName.APPROVER);
        Role employeeRole = ensureRole(RoleName.EMPLOYEE);

        ensureUser("admin", "System Admin", "admin@lenovo.local", Set.of(adminRole, approverRole, employeeRole));
        ensureUser("approver", "Booking Approver", "approver@lenovo.local", Set.of(approverRole, employeeRole));
        ensureUser("employee", "Office Employee", "employee@lenovo.local", Set.of(employeeRole));

        ensureResource("RM-501", "5F Innovation Room", ResourceType.MEETING_ROOM, "Building A - Floor 5", 12, "display,video conference,whiteboard");
        ensureResource("DS-5A-21", "Desk 5A-21", ResourceType.DESK, "Building A - Floor 5", 1, "monitor,window side");
        ensureResource("PRJ-3F-01", "Portable Projector 3F", ResourceType.OTHER, "Building A - Floor 3", 1, "hdmi,wireless");
        ensureResource("RM-402", "4F Strategy Room", ResourceType.MEETING_ROOM, "Building A - Floor 4", 10, "display,whiteboard");
        ensureResource("RM-302", "3F Sprint Room", ResourceType.MEETING_ROOM, "Building A - Floor 3", 8, "display,conference phone");
        ensureResource("RM-201", "2F Design Lab", ResourceType.MEETING_ROOM, "Building A - Floor 2", 16, "display,video conference,smart board");
        ensureResource("DS-5A-22", "Desk 5A-22", ResourceType.DESK, "Building A - Floor 5", 1, "dual monitor,window side");
        ensureResource("DS-5A-23", "Desk 5A-23", ResourceType.DESK, "Building A - Floor 5", 1, "monitor,ergonomic chair");
        ensureResource("DS-4B-11", "Desk 4B-11", ResourceType.DESK, "Building A - Floor 4", 1, "monitor,docking station");
        ensureResource("DS-3C-07", "Desk 3C-07", ResourceType.DESK, "Building A - Floor 3", 1, "standing desk");
        ensureResource("VC-5F-01", "Video Cart 5F", ResourceType.OTHER, "Building A - Floor 5", 2, "camera,microphone,display");
        ensureResource("PRN-4F-02", "Color Printer 4F", ResourceType.OTHER, "Building A - Floor 4", 1, "a3,duplex,wireless");
        ensureResource("WB-3F-01", "Mobile Whiteboard 3F", ResourceType.OTHER, "Building A - Floor 3", 1, "magnetic,eraser set");
        ensureResource("RM-601", "6F Townhall Room", ResourceType.MEETING_ROOM, "Building A - Floor 6", 24, "projector,stage audio,video conference");
        ensureResource("RM-502", "5F Brainstorm Room", ResourceType.MEETING_ROOM, "Building A - Floor 5", 6, "display,whiteboard");
        ensureResource("RM-503", "5F Focus Room", ResourceType.MEETING_ROOM, "Building A - Floor 5", 4, "display,soundproof");
        ensureResource("RM-403", "4F Planning Room", ResourceType.MEETING_ROOM, "Building A - Floor 4", 12, "display,conference phone");
        ensureResource("RM-303", "3F Collaboration Hub", ResourceType.MEETING_ROOM, "Building A - Floor 3", 14, "video conference,whiteboard");
        ensureResource("RM-202", "2F Quiet Meeting Room", ResourceType.MEETING_ROOM, "Building A - Floor 2", 6, "display,soundproof");
        ensureResource("DS-5A-24", "Desk 5A-24", ResourceType.DESK, "Building A - Floor 5", 1, "monitor,ergonomic chair");
        ensureResource("DS-5A-25", "Desk 5A-25", ResourceType.DESK, "Building A - Floor 5", 1, "dual monitor,window side");
        ensureResource("DS-4B-12", "Desk 4B-12", ResourceType.DESK, "Building A - Floor 4", 1, "monitor,docking station");
        ensureResource("DS-4B-13", "Desk 4B-13", ResourceType.DESK, "Building A - Floor 4", 1, "standing desk,monitor");
        ensureResource("DS-3C-08", "Desk 3C-08", ResourceType.DESK, "Building A - Floor 3", 1, "monitor,quiet zone");
        ensureResource("DS-3C-09", "Desk 3C-09", ResourceType.DESK, "Building A - Floor 3", 1, "monitor,window side");
        ensureResource("PRJ-2F-02", "Portable Projector 2F", ResourceType.OTHER, "Building A - Floor 2", 1, "hdmi,wireless");
        ensureResource("VC-4F-01", "Video Cart 4F", ResourceType.OTHER, "Building A - Floor 4", 2, "camera,microphone,display");
        ensureResource("WB-5F-01", "Mobile Whiteboard 5F", ResourceType.OTHER, "Building A - Floor 5", 1, "magnetic,eraser set");
        ensureResource("TEL-6F-01", "Telepresence Pod 6F", ResourceType.OTHER, "Building A - Floor 6", 2, "4k camera,noise cancellation");
    }

    private Role ensureRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private void ensureUser(String username, String displayName, String email, Set<Role> roles) {
        if (appUserRepository.existsByUsername(username)) {
            return;
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setEnabled(true);
        user.setRoles(roles);
        appUserRepository.save(user);
    }

    private void ensureResource(String code, String name, ResourceType type, String location, int capacity, String features) {
        if (resourceRepository.findByCode(code).isPresent()) {
            return;
        }

        Resource resource = new Resource();
        resource.setCode(code);
        resource.setName(name);
        resource.setType(type);
        resource.setLocation(location);
        resource.setCapacity(capacity);
        resource.setFeatures(features);
        resource.setActive(true);
        resourceRepository.save(resource);
    }
}