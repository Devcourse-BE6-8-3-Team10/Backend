package com.back.domain.member.repository;

import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByName(String name);
    Optional<Member> findByNameAndEmail(String name, String email);
    boolean existsByEmail(String email);
    boolean existsByNameAndEmail(String name, String email);
    Page<Member> findAllByRoleNot(Role role, Pageable pageable);
}
