//package com.system.batch.killbatchsystem.jpa.cursor;
//
//import jakarta.persistence.Entity;
//import jakarta.persistence.Id;
//import jakarta.persistence.NamedQuery;
//import jakarta.persistence.OneToMany;
//import jakarta.persistence.Table;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * 게시글 엔티티 - 검열 대상
// */
////@Entity
//@Table(name = "posts")
//@NamedQuery(
//        name = "Post.findTodayUnblockedWithReports",
//        query = "SELECT p FROM Post p JOIN FETCH p.reports r " +
//                "WHERE p.blockedAt IS NULL " +
//                "AND r.reportedAt >= :today"
//)
//@Data
//@NoArgsConstructor
//public class Post {
//    @Id
//    private Long id;
//    private String title;         // 게시물 제목
//    private String content;       // 게시물 내용
//    private String writer;        // 작성자
//
//    @OneToMany(mappedBy = "post")
//    private List<Report> reports = new ArrayList<>();
//
//    private LocalDateTime blockedAt;  // 차단 일시
//}