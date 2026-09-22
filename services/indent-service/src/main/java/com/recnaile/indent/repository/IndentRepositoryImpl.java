package com.recnaile.indent.repository;

import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.Indent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class IndentRepositoryImpl implements IndentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public IndentRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Indent> findAllByFilters(
            IndentStatus status,
            String departmentName,
            Pageable pageable) {

        Criteria criteria = new Criteria();
        if (status != null) {
            criteria.and("status").is(status);
        }
        if (departmentName != null && !departmentName.isBlank()) {
            criteria.and("departmentName").is(departmentName.trim());
        }

        Query query = Query.query(criteria).with(pageable);
        List<Indent> content = mongoTemplate.find(query, Indent.class);
        long total = mongoTemplate.count(Query.query(criteria), Indent.class);

        return new PageImpl<>(content, pageable, total);
    }
}
