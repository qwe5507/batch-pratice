package com.system.batch.killbatchsystem.multi_thread_step;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "activities")
@Data
@ToString(exclude = "human")
public class Activity {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "human_id")
    private Human human;

    private double severityIndex;
    private LocalDate detectionDate;
    @Enumerated(EnumType.STRING)
    private ActivityType activityType;     // 💀 활동 유형 (COMBAT, SABOTAGE, RECRUITMENT, SUPPLY, INTELLIGENCE) 💀
    private String location;         // 💀 활동 발생 위치 💀

    public enum ActivityType {
        COMBAT,
        SABOTAGE,
        MEDICAL,
        HACKING
    }
}
