package com.recnaile.inward.service;

import com.recnaile.inward.dto.GRNResponse;
import com.recnaile.inward.exception.GRNNotFoundException;
import com.recnaile.inward.model.GRNDocument;
import com.recnaile.inward.repository.GRNRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GRNQueryService {

    private final GRNRepository repository;

    public GRNQueryService(GRNRepository repository) {
        this.repository = repository;
    }

    public List<GRNResponse> list(String poId) {
        List<GRNDocument> grns;

        if (poId != null && !poId.isBlank()) {
            grns = repository.findByPoId(
                    poId.trim(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        } else {
            grns = repository.findAll(
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        return grns.stream().map(GRNResponse::from).toList();
    }

    public GRNResponse getByGrnId(String grnId) {
        return repository.findByGrnId(grnId)
                .map(GRNResponse::from)
                .orElseThrow(() -> new GRNNotFoundException(grnId));
    }
}
