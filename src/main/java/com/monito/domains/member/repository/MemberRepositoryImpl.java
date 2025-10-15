package com.monito.domains.member.repository;

import com.monito.domains.member.domain.Member;
import com.monito.domains.member.dto.request.MemberUpdateRequestDTO;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

@RequiredArgsConstructor
@Slf4j
// 기존 JDBC Template 환경 Repository 구현체
public class MemberRepositoryImpl{
    private final JdbcTemplate jdbcTemplate;

    public Long save(Member member) {
        String sql = "INSERT INTO MEMBERS (ID, USERNAME, PASSWORD, ROLE, EMAIL, CREATED_AT, UPDATED_AT, IS_DELETED) " +
                "VALUES (MEMBERS_SEQ.NEXTVAL, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP, 0)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID"});

            ps.setString(1, member.getUsername());
            ps.setString(2, member.getPassword());
            ps.setString(3, member.getRole().name());
            ps.setString(4, member.getEmail());

            return ps;
        }, keyHolder);

        return keyHolder.getKey().longValue();
    }

    public Optional<Member> findByUsernameAndIsDeleted(String username) {
        String sql = "SELECT * FROM members WHERE username = ? AND is_deleted = 0";
        return jdbcTemplate.query(sql, new MemberRowMapper(), username)
                .stream()
                .findFirst();
    }

    public Optional<Member> findById(Long id) {
        String sql = "SELECT * FROM members WHERE id = ? AND is_deleted = 0";
        return jdbcTemplate.query(sql, new MemberRowMapper(), id)
                .stream()
                .findFirst();
    }

    public List<Member> findAll() {
        String sql = "SELECT * FROM members WHERE is_deleted = 0";
        return jdbcTemplate.query(sql, new MemberRowMapper());
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM members WHERE username = ? AND is_deleted = 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, username);

        return count != null && count > 0;
    }

    public int updateMember(Long id, MemberUpdateRequestDTO memberUpdateRequestDTO) {
        String sql = "UPDATE members SET password = ?, updated_at = SYSTIMESTAMP WHERE id = ? AND is_deleted = 0";

        return jdbcTemplate.update(sql,
                memberUpdateRequestDTO.getPassword(),
                id
        );
    }

    public int deleteMember(Long id) {
        String sql = "UPDATE members SET is_deleted = 1, updated_at = SYSTIMESTAMP WHERE id = ? AND is_deleted = 0";

        return jdbcTemplate.update(sql, id);
    }
}
