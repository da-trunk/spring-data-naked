//package org.datrunk.naked.entities;
//
//import javax.annotation.Nonnull;
//import javax.persistence.Column;
//import javax.persistence.Entity;
//import javax.persistence.GeneratedValue;
//import javax.persistence.GenerationType;
//import javax.persistence.Id;
//import javax.persistence.OneToOne;
//
//import org.datrunk.naked.entities.bowman.annotation.RemoteResource;
//
//import lombok.AccessLevel;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.RequiredArgsConstructor;
//import lombok.Setter;
//import lombok.ToString;
//
//@Entity
//@RemoteResource("/jobs")
//@Getter
//@Setter
//@ToString
//@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//public class Job extends IdClass<Integer> {
//  @Id
//  @GeneratedValue(strategy = GenerationType.IDENTITY)
//  @Column(insertable = false, updatable = false)
//  private Integer id;
//
//  @Nonnull
//  private String name;
//
//  @OneToOne
//  private User user;
//}
