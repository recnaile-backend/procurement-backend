package com.recnaile.indent.repository;

import com.recnaile.indent.domain.enums.IndentStatus;
import com.recnaile.indent.domain.model.Indent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IndentRepositoryCustom {
    Page<Indent> findAllByFilters(IndentStatus status, String departmentName, Pageable pageable);
}
