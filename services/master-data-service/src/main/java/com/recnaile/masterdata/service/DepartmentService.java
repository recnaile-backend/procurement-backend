package com.recnaile.masterdata.service;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.masterdata.dto.department.CreateDepartmentRequest;
import com.recnaile.masterdata.dto.department.DepartmentResponse;
import com.recnaile.masterdata.dto.department.UpdateDepartmentRequest;
import com.recnaile.masterdata.model.Department;
import com.recnaile.masterdata.model.MasterDataStatus;
import com.recnaile.masterdata.repository.DepartmentRepository;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {

        String name = normalizeName(request.name());

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Department name is required");
        }

        if (departmentRepository.existsByName(name)) {
            throw new IllegalArgumentException(
                    "Department name already exists");
        }

        Instant now = Instant.now();

        Department department = Department.builder()
                .name(name)
                .description(normalizeDescription(request.description()))
                .status(MasterDataStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Department saved = departmentRepository.save(department);

        return toResponse(saved);
    }

    @Transactional
    public List<DepartmentResponse> multiAdd(List<CreateDepartmentRequest> requests) {
        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("At least one department is required");
        Instant now = Instant.now();
        List<Department> departments = requests.stream().map(request -> {
            String name = normalizeName(request.name());
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Department name is required");
            if (departmentRepository.existsByName(name)) throw new IllegalArgumentException("Department name already exists: " + name);
            return Department.builder().name(name).description(normalizeDescription(request.description())).status(MasterDataStatus.ACTIVE).createdAt(now).updatedAt(now).build();
        }).toList();
        return departmentRepository.saveAll(departments).stream().map(this::toResponse).toList();
    }

    @Transactional
    public DepartmentResponse update(
            String id,
            UpdateDepartmentRequest request) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found"));

        if (department.getStatus() == MasterDataStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Inactive department cannot be updated");
        }

        /*
         * Optimistic locking:
         * Client must provide the current version.
         */
        if (request.version() == null) {
            throw new IllegalArgumentException(
                    "Department version is required for update");
        }

        if (!request.version().equals(department.getVersion())) {
            throw new OptimisticLockingFailureException(
                    "Department version conflict");
        }

        String name = normalizeName(request.name());

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Department name is required");
        }

        departmentRepository.findByName(name)
                .filter(existing ->
                        !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Department name already exists");
                });

        department.setName(name);
        department.setDescription(
                normalizeDescription(request.description()));
        department.setUpdatedAt(Instant.now());

        Department saved = departmentRepository.save(department);

        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse deactivate(String id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found"));

        if (department.getStatus() == MasterDataStatus.INACTIVE) {
            return toResponse(department);
        }

        department.setStatus(MasterDataStatus.INACTIVE);
        department.setUpdatedAt(Instant.now());

        Department saved = departmentRepository.save(department);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(String id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found"));

        return toResponse(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAll() {

        return departmentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found"));

        departmentRepository.delete(department);
    }

    private DepartmentResponse toResponse(
            Department department) {

        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt(),
                department.getVersion()
        );
    }

    private String normalizeName(String name) {

        if (name == null) {
            return null;
        }

        return name.trim();
    }

    private String normalizeDescription(String description) {

        if (description == null) {
            return null;
        }

        String normalized = description.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}