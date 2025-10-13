package com.monito.domians.member.repository;

import com.monito.domians.member.domain.Member;
import com.monito.domians.member.domain.Role;
import com.monito.domians.member.dto.request.MemberCreateRequestDTO;
import com.monito.domians.member.dto.request.MemberUpdateRequestDTO;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MemberRepositoryImpl implements MemberRepository{
    private final JdbcTemplate jdbcTemplate;


    @Override
    public int insertMember(MemberCreateRequestDTO dto) {
        String sql = "INSERT INTO MEMBERS (ID, USERNAME, PASSWORD, ROLE, EMAIL, CREATED_AT, UPDATED_AT, IS_DELETED) " +
                "VALUES (MEMBERS_SEQ.NEXTVAL, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP, 0)";

        return jdbcTemplate.update(sql,
                dto.getUsername(),
                dto.getPassword(),
                dto.getEmail(),
                dto.getRole().name().trim());
    }

    @Override
    public void save(Member member) {
        // BaseEntity의 createdAt, updatedAt 필드가 AOP에 의해 채워져 있다고 가정
        String sql = "INSERT INTO members (username, password, role, created_at, updated_at, is_deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                member.getUsername(),
                member.getPassword(),
                member.getRole().name(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.getIsDeleted()
        );
    }

    @Override
    public Optional<Member> findByUsername(String username) {
        String sql = "SELECT * FROM members WHERE username = ? AND is_deleted = 0";
        return jdbcTemplate.query(sql, new MemberRowMapper(), username)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Member> findById(Long id) {
        String sql = "SELECT * FROM members WHERE id = ? AND is_deleted = 0";
        return jdbcTemplate.query(sql, new MemberRowMapper(), id)
                .stream()
                .findFirst();
    }

    @Override
    public List<Member> findAll() {
        String sql = "SELECT * FROM members WHERE is_deleted = 0";
        return jdbcTemplate.query(sql, new MemberRowMapper());
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM members WHERE username = ? AND is_deleted = 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, username);

        return count != null && count > 0;
    }

    @Override
    public int updateMember(Long id, MemberUpdateRequestDTO memberUpdateRequestDTO) {
        String sql = "UPDATE members SET password = ?, updated_at = NOW() WHERE id = ? AND is_deleted = 0";

        return jdbcTemplate.update(sql,
                memberUpdateRequestDTO.getPassword(), // Service 계층에서 해시된 비밀번호를 DTO에 담아 넘겨야 함
                id
        );
    }

    @Override
    public int deleteMember(Long id) {
        // 논리적 삭제(Soft Delete) 구현: is_deleted 필드를 1로 설정하고 updated_at을 갱신
        String sql = "UPDATE members SET is_deleted = 1, updated_at = NOW() WHERE id = ? AND is_deleted = 0";

        return jdbcTemplate.update(sql, id);
    }
}
