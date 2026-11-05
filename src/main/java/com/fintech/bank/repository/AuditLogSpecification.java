package com.fintech.bank.repository;

import com.fintech.bank.dto.AuditLogFilterRequest;
import com.fintech.bank.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic JPA Specification for filtering audit logs by actor, action, resource, result, date range, and correlation ID.
 */
public class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> withFilter(AuditLogFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filter.actorId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorId"), filter.actorId()));
            }

            if (filter.actorUsername() != null && !filter.actorUsername().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("actorUsername")),
                        filter.actorUsername().toLowerCase().trim()
                ));
            }

            if (filter.actorRole() != null && !filter.actorRole().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("actorRole"), filter.actorRole().trim()));
            }

            if (filter.action() != null && !filter.action().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("action"), filter.action().trim()));
            }

            if (filter.resourceType() != null && !filter.resourceType().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("resourceType"), filter.resourceType().trim()));
            }

            if (filter.resourceId() != null && !filter.resourceId().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("resourceId"), filter.resourceId().trim()));
            }

            if (filter.result() != null) {
                predicates.add(criteriaBuilder.equal(root.get("result"), filter.result()));
            }

            if (filter.correlationId() != null && !filter.correlationId().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("correlationId"), filter.correlationId().trim()));
            }

            if (filter.fromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.fromDate()));
            }

            if (filter.toDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.toDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
