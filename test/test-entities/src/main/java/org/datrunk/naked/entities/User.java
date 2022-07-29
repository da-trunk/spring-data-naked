package org.datrunk.naked.entities;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.datrunk.naked.entities.IdClass;
import org.datrunk.naked.entities.bowman.annotation.RemoteResource;
import org.datrunk.naked.entities.random.Randomizer;
import org.datrunk.naked.entities.random.RepeatingRandomizer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.NonNull;

import com.github.javafaker.Name;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@RemoteResource("/users")
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
@ToString
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends IdClass<Integer> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(insertable = false, updatable = false)
  private Integer id;

  @Nonnull
  private String firstName;

  private String lastName;
//
//  @NonNull
//  @Enumerated(EnumType.STRING)
//  private Role role;

//  @NonNull
//  @Column
//  @DateTimeFormat(iso = ISO.DATE_TIME)
//  private LocalDateTime createTime = LocalDateTime.now();
//
//  public void setCreateTime(String str) {
//    if (str == null || str.isEmpty() || str.toLowerCase().equals("null"))
//      createTime = LocalDateTime.now();
//    else
//      createTime = LocalDateTime.parse(str);
//  }
//
//  public void setCreateTime(LocalDateTime dt) {
//    this.createTime = dt;
//  }
}
