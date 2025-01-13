package com.fergie.lab1.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "movie_audit")
@Setter
@Getter
public class MovieAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id")
    private Long movieId;

    private String action;

    private String field;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at")

    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date changedAt;

    public MovieAudit() {}

}