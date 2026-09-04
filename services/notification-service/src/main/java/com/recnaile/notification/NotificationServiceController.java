package com.procurement.platform.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notification")
public class NotificationServiceController {

    private final RecordRepository repository;

    public NotificationServiceController(RecordRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<RecordDocument> findAll() { return repository.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<RecordDocument> findById(@PathVariable String id) {
        return repository.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public RecordDocument create(@Valid @RequestBody CreateRequest request) {
        return repository.save(new RecordDocument(null, request.referenceId(), request.name(),
                request.status() == null ? "PENDING" : request.status(), request.payload(), Instant.now()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecordDocument> update(@PathVariable String id, @Valid @RequestBody CreateRequest request) {
        return repository.findById(id).map(existing -> ResponseEntity.ok(
                repository.save(new RecordDocument(existing.id(), request.referenceId(), request.name(),
                        request.status() == null ? existing.status() : request.status(), request.payload(), existing.createdAt()))
        )).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(
            @NotBlank String referenceId,
            @NotBlank String name,
            String status,
            Map<String,Object> payload) {}
}
