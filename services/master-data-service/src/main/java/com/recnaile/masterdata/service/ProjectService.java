package com.recnaile.masterdata.service;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.masterdata.dto.project.CreateProjectRequest;
import com.recnaile.masterdata.dto.project.ProjectResponse;
import com.recnaile.masterdata.dto.project.UpdateProjectRequest;
import com.recnaile.masterdata.model.MasterDataStatus;
import com.recnaile.masterdata.model.Project;
import com.recnaile.masterdata.repository.ProjectRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {

        String projectName = normalizeName(request.projectName());

        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException(
                    "Project name is required");
        }

        if (projectRepository.existsByProjectName(projectName)) {
            throw new IllegalArgumentException(
                    "Project name already exists");
        }

        Instant now = Instant.now();

        Project project = Project.builder()
                .projectName(projectName)
                .teamLeadId(normalizeValue(request.teamLeadId()))
                .status(MasterDataStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Project saved = projectRepository.save(project);

        return toResponse(saved);
    }

    @Transactional
    public List<ProjectResponse> multiAdd(List<CreateProjectRequest> requests) {
        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("At least one project is required");
        Instant now = Instant.now();
        List<Project> projects = requests.stream().map(request -> {
            String name = normalizeName(request.projectName());
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Project name is required");
            if (projectRepository.existsByProjectName(name)) throw new IllegalArgumentException("Project name already exists: " + name);
            return Project.builder().projectName(name).teamLeadId(normalizeValue(request.teamLeadId())).status(MasterDataStatus.ACTIVE).createdAt(now).updatedAt(now).build();
        }).toList();
        return projectRepository.saveAll(projects).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProjectResponse update(
            String projectId,
            UpdateProjectRequest request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project not found"));

        if (project.getStatus() == MasterDataStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Inactive project cannot be updated");
        }

        if (request.version() == null) {
            throw new IllegalArgumentException(
                    "Project version is required for update");
        }

        if (!request.version().equals(project.getVersion())) {
            throw new OptimisticLockingFailureException(
                    "Project version conflict");
        }

        String projectName = normalizeName(request.projectName());

        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException(
                    "Project name is required");
        }

        projectRepository.findByProjectName(projectName)
                .filter(existing ->
                        !existing.getProjectId().equals(projectId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Project name already exists");
                });

        project.setProjectName(projectName);
        project.setTeamLeadId(
                normalizeValue(request.teamLeadId()));
        project.setUpdatedAt(Instant.now());

        Project saved = projectRepository.save(project);

        return toResponse(saved);
    }

    @Transactional
    public ProjectResponse deactivate(String projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project not found"));

        if (project.getStatus() == MasterDataStatus.INACTIVE) {
            return toResponse(project);
        }

        project.setStatus(MasterDataStatus.INACTIVE);
        project.setUpdatedAt(Instant.now());

        Project saved = projectRepository.save(project);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(String projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project not found"));

        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAll() {

        return projectRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String projectId) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project not found"));

        projectRepository.delete(project);
    }

    private ProjectResponse toResponse(Project project) {

        return new ProjectResponse(
                project.getProjectId(),
                project.getProjectName(),
                project.getTeamLeadId(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getVersion()
        );
    }

    private String normalizeName(String name) {

        if (name == null) {
            return null;
        }

        return name.trim();
    }

    private String normalizeValue(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}