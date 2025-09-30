package com.system.batch.killbatchsystem.multi_thread_step;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Entity
@Table(name = "humans")
@Data
public class Human {
    @Id
    private Long id;
    private String name;
    private String rank;        // 💀 저항군 내 계급 (COMMANDER, OFFICER, SOLDIER, CIVILIAN 등) 💀
    private Boolean terminated; // 💀 전사 여부 💀

    @OneToMany(mappedBy = "human", fetch = FetchType.EAGER)
    @BatchSize(size = 100)
    private List<Activity> activities;
}