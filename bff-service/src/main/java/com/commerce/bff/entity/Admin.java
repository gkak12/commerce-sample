package com.commerce.bff.entity;

import com.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admins")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Admin extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String adminId;  // UUID

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    /**
     * 임시 비밀번호 변경 필요 여부
     * - 관리자 계정 생성 시 true (임시 비밀번호 발급)
     * - 비밀번호 변경 완료 시 false
     */
    @Column(nullable = false)
    private boolean passwordChangeRequired;

    /**
     * 생성한 관리자 adminId
     * - Flyway migration으로 생성된 최초 관리자는 null
     */
    private String createdBy;

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordChangeRequired = false;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
