package com.dreamcatcher.backend.member.domain;

import com.dreamcatcher.backend.common.auditing.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "members")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="member_id")
    private Long memberId;

    @Column(name="nickname", nullable = false)
    private String nickname;

    @Column(name="point", nullable = false)
    private Long point;

    @Version
    @Column(name="version")
    private Integer version; // 동시에 포인트 결제 시도시 낙관적 락 구현 위함(결제 버튼을 빠르게 두 번 누르는 경우 무거운 분산락보다 가볍게 낙관락 처리하는 게 좋음)

} // end of Member
