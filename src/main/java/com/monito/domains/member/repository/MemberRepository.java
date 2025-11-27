package com.monito.domains.member.repository;

import com.monito.domains.member.domain.Member;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
/**
 작성자: 백승준
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("""
        SELECT m 
        FROM Member m
        WHERE (:keyword IS NULL OR :keyword = '' OR 
            LOWER(m.name) LIKE LOWER(CONCAT('%', TRIM(:keyword), '%')) OR
            LOWER(m.companyName) LIKE LOWER(CONCAT('%', TRIM(:keyword), '%')) OR
            LOWER(m.position) LIKE LOWER(CONCAT('%', TRIM(:keyword), '%')) OR
            LOWER(m.mobileNumber) LIKE LOWER(CONCAT('%', TRIM(:keyword), '%')) OR
            LOWER(m.officePhone) LIKE LOWER(CONCAT('%', TRIM(:keyword), '%')) OR
            LOWER(m.email) LIKE LOWER(CONCAT('%', TRIM(:keyword), '%'))
        )
        """)
    List<Member> findAllWithSearch(@Param("keyword") String keyword);
}
