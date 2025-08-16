package com.example.backend.specification;

import com.example.backend.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {
    public static Specification<User> base(Boolean includeDeleted) {
        return (root, query, cb) -> {
            if (Boolean.TRUE.equals(includeDeleted)) return cb.conjunction();
            return cb.isFalse(root.get("isDeleted"));
        };
    }

    public static Specification<User> ageEquals(Integer age) {
        return (root, query, cb) -> age == null ? cb.conjunction() : cb.equal(root.get("age"), age);
    }

    public static Specification<User> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.toLowerCase() + "%";
            Predicate p1 = cb.like(cb.lower(root.get("name")), like);
            Predicate p2 = cb.like(cb.lower(root.get("email")), like);
            return cb.or(p1, p2);
        };
    }
}
