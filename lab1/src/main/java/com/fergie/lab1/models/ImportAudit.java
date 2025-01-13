package com.fergie.lab1.models;

import com.fergie.lab1.models.enums.ImportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Table(name = "import_audit")
@Entity
public class ImportAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long authorID;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ImportStatus status;

    @Column(name = "file_hash", nullable = false) //?????? ?? Unique?
    private String fileHash;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "import_date", nullable = false)
    private Date importDate;

    @Column(name = "total_records", nullable = false)
    private int totalRecords;

    @Column(name = "error_records", nullable = false)
    private int errorRecords;

    @Column(name = "success_records", nullable = false)
    private int successRecords;

    @Column
    private String path;

    @Column
    private String fileName;
}
