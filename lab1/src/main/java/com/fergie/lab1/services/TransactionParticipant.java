package com.fergie.lab1.services;

import com.fergie.lab1.models.ImportAudit;
import com.fergie.lab1.models.Movie;
import java.io.InputStream;
import java.util.List;

public interface TransactionParticipant {
    boolean prepare(String bucketName, String objectName, InputStream inputStream, String contentType, List<Movie> movies);
    void commit(String bucketName, String objectName, InputStream inputStream, String contentType);
    void rollback(String bucketName, String objectName);
}
