package com.recnaile.masterdata.service;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.masterdata.dto.vendor.CreateVendorRequest;
import com.recnaile.masterdata.dto.vendor.UpdateVendorRequest;
import com.recnaile.masterdata.dto.vendor.VendorResponse;
import com.recnaile.masterdata.model.MasterDataStatus;
import com.recnaile.masterdata.model.Vendor;
import com.recnaile.masterdata.repository.VendorRepository;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Transactional
    public VendorResponse create(CreateVendorRequest request) {

        String name = normalizeName(request.name());

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Vendor name is required");
        }

        if (vendorRepository.existsByName(name)) {
            throw new IllegalArgumentException(
                    "Vendor name already exists");
        }

        Instant now = Instant.now();

        Vendor vendor = Vendor.builder()
                .name(name)
                .email(normalizeValue(request.email()))
                .category(normalizeValue(request.category()))
                .gstNumber(normalizeValue(request.gstNumber()))
                .notes(normalizeValue(request.notes()))
                .orderSource(normalizeValue(request.orderSource()))
                .webUrl(normalizeValue(request.webUrl()))
                .status(MasterDataStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Vendor saved = vendorRepository.save(vendor);

        return toResponse(saved);
    }

    @Transactional
    public List<VendorResponse> multiAdd(List<CreateVendorRequest> requests) {
        if (requests == null || requests.isEmpty()) throw new IllegalArgumentException("At least one vendor is required");
        Instant now = Instant.now();
        List<Vendor> vendors = requests.stream().map(request -> {
            String name = normalizeName(request.name());
            if (name == null || name.isBlank()) throw new IllegalArgumentException("Vendor name is required");
            if (vendorRepository.existsByName(name)) throw new IllegalArgumentException("Vendor name already exists: " + name);
            return Vendor.builder().name(name).email(normalizeValue(request.email())).category(normalizeValue(request.category())).gstNumber(normalizeValue(request.gstNumber())).notes(normalizeValue(request.notes())).orderSource(normalizeValue(request.orderSource())).webUrl(normalizeValue(request.webUrl())).status(MasterDataStatus.ACTIVE).createdAt(now).updatedAt(now).build();
        }).toList();
        return vendorRepository.saveAll(vendors).stream().map(this::toResponse).toList();
    }

    @Transactional
    public VendorResponse update(
            String id,
            UpdateVendorRequest request) {

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Vendor not found"));

        if (vendor.getStatus() == MasterDataStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Inactive vendor cannot be updated");
        }

        if (request.version() == null) {
            throw new IllegalArgumentException(
                    "Vendor version is required for update");
        }

        if (!request.version().equals(vendor.getVersion())) {
            throw new OptimisticLockingFailureException(
                    "Vendor version conflict");
        }

        String name = normalizeName(request.name());

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Vendor name is required");
        }

        vendorRepository.findByName(name)
                .filter(existing ->
                        !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Vendor name already exists");
                });

        vendor.setName(name);
        vendor.setEmail(
                normalizeValue(request.email()));
        vendor.setCategory(
                normalizeValue(request.category()));
        vendor.setGstNumber(
                normalizeValue(request.gstNumber()));
        vendor.setNotes(
                normalizeValue(request.notes()));
        vendor.setOrderSource(
                normalizeValue(request.orderSource()));
        vendor.setWebUrl(
                normalizeValue(request.webUrl()));
        vendor.setUpdatedAt(Instant.now());

        Vendor saved = vendorRepository.save(vendor);

        return toResponse(saved);
    }

    @Transactional
    public VendorResponse deactivate(String id) {

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Vendor not found"));

        if (vendor.getStatus() == MasterDataStatus.INACTIVE) {
            return toResponse(vendor);
        }

        vendor.setStatus(MasterDataStatus.INACTIVE);
        vendor.setUpdatedAt(Instant.now());

        Vendor saved = vendorRepository.save(vendor);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VendorResponse getById(String id) {

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Vendor not found"));

        return toResponse(vendor);
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> getAll() {

        return vendorRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String id) {

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Vendor not found"));

        vendorRepository.delete(vendor);
    }

    private VendorResponse toResponse(Vendor vendor) {

        return new VendorResponse(
                vendor.getId(),
                vendor.getName(),
                vendor.getEmail(),
                vendor.getCategory(),
                vendor.getGstNumber(),
                vendor.getNotes(),
                vendor.getOrderSource(),
                vendor.getWebUrl(),
                vendor.getStatus(),
                vendor.getCreatedAt(),
                vendor.getUpdatedAt(),
                vendor.getVersion()
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