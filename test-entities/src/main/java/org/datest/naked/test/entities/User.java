package org.datest.naked.test.entities;

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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.NonNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@RemoteResource("/users")
//@Table(schema = "PUBLIC")
@Inheritance(strategy = InheritanceType.JOINED)
@ToString
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends IdClass<Long> {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(insertable = false, updatable = false)
  private Long id;  
  
//  public String getId() {
//    return String.format("%s %s", firstName, lastName);
//  }
//  
//  public void setId(String id) {
//    String[] names = id.split(" ");
//    firstName = names[0];
//    lastName = names[1];
//  }

  @Nonnull
  private String firstName;
  
  @Nonnull
  private String lastName;

  @NonNull
  @Enumerated(EnumType.STRING)
  private Role role;

//    @Column //(columnDefinition = "TIMESTAMP WITH TIME ZONE")
//    private Instant createTime = Instant.now();
  @NonNull
  @Column // (columnDefinition = "TIMESTAMP")
  @DateTimeFormat(iso = ISO.DATE_TIME)
  // @JsonFormat(pattern = "YYYY-MM-dd HH:mm")
  private LocalDateTime createTime = LocalDateTime.now();
//    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
//    private OffsetDateTime offsetDateTime;

  public void setCreateTime(String str) {
    if (str == null || str.isEmpty() || str.toLowerCase().equals("null"))
      createTime = LocalDateTime.now();
    else
      createTime = LocalDateTime.parse(str);
  }

  public void setCreateTime(LocalDateTime dt) {
    this.createTime = dt;
  }
}
