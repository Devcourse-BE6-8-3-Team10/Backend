package com.back.global.init

import com.back.domain.member.entity.Member
import com.back.domain.member.entity.Role
import com.back.domain.member.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Profile("dev", "prod")
@Component
@Order(1)
class DevDataInitializer(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        log.info("===== DEV 데이터 초기화 시작 =====")
        initAdmin()
        initUsers()
        log.info("===== DEV 데이터 초기화 완료 =====")
    }

    private fun initAdmin() {
        if (!memberRepository.existsByEmail("admin@admin.com")) {
            val admin = Member(
                email = "admin@admin.com",
                password = passwordEncoder.encode("admin1234!"),
                name = "관리자",
                profileUrl = null,
                role = Role.ADMIN
            )
            memberRepository.save(admin)
            log.info("관리자 계정이 생성되었습니다.")
        }
    }

    private fun initUsers() {
        listOf("user1@user.com" to "사용자1", "user2@user.com" to "사용자2")
            .forEach { (email, name) -> createUserIfNotExists(email, name) }
    }

    private fun createUserIfNotExists(email: String, name: String) {
        if (!memberRepository.existsByEmail(email)) {
            val user = Member(
                email = email,
                password = passwordEncoder.encode("user1234!"),
                name = name,
                profileUrl = null,
                role = Role.USER
            )
            memberRepository.save(user)
            log.info("$name 계정이 생성되었습니다.")
        }
    }
}