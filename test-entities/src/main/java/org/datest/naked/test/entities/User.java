package org.datest.naked.test.entities;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.datrunk.naked.entities.IdClass;
import org.datrunk.naked.entities.bowman.annotation.RemoteResource;
import org.springframework.lang.NonNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(schema="PUBLIC")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RemoteResource("/users")
public class User extends IdClass<Long> {
    @Id
    @GeneratedValue(generator = "ACTIVITY_SEQ")
    @Column(insertable = false, updatable = false)
    private Long id;
    
    private String name;
    
    @NonNull
    @Enumerated(EnumType.STRING)
    private Role role;

    @NonNull
    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant createTime = Instant.now();
}
