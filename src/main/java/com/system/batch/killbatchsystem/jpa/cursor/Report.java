//package com.system.batch.killbatchsystem.jpa.cursor;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.FetchType;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
//import jakarta.persistence.Table;
//import lombok.Data;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.ToString;
//
//import java.time.LocalDateTime;
//
///**
// * 신고 엔티티 - 검열 증거
// */
////@Entity
//@Table(name = "reports")
//@Getter
//@NoArgsConstructor
//@Data
//public class Report {
//    @Id
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "post_id")
//    @ToString.Exclude
//    private Post post;
//
//    private String reportType;     // SPAM, ABUSE, ILLEGAL, FAKE_NEWS ...
//    private int reporterLevel;     // 신고자 신뢰도 (1~5)
//    private String evidenceData;   // 증거 데이터 (URL 등)
//    private LocalDateTime reportedAt;
//}