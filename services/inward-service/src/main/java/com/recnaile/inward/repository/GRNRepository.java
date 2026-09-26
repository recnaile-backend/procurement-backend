package com.recnaile.inward.repository;

import com.recnaile.inward.model.GRNDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GRNRepository extends MongoRepository<GRNDocument, String> {

    List<GRNDocument> findByPoId(String poId, Sort sort);

    java.util.Optional<GRNDocument> findByGrnId(String grnId);

    boolean existsByPoIdAndDeliveryNoteNumber(String poId, String deliveryNoteNumber);
}
