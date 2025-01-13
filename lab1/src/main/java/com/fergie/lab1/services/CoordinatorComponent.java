package com.fergie.lab1.services;

import com.fergie.lab1.models.ImportAudit;
import com.fergie.lab1.models.Movie;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.util.List;

@Component
public class CoordinatorComponent implements TransactionParticipant{
    private final StorageService storageService;
    private final MoviesService moviesService;

    public CoordinatorComponent(StorageService storageService, MoviesService moviesService) {
        this.storageService = storageService;
        this.moviesService = moviesService;
    }

    public void execute(String bucketName, String objectName, InputStream inputStream, String contentType, List<Movie> movies){
        if (prepare(bucketName, objectName, inputStream, contentType, movies)) {
            commit(bucketName, objectName, inputStream, contentType);
        } else {
            rollback(bucketName, objectName);
        }
    }
    @Override
    public boolean prepare(String bucketName, String objectName, InputStream inputStream, String contentType, List<Movie> movies){
        try {
            moviesService.saveAll(movies);
            storageService.prepareUploadFile(bucketName, objectName, inputStream, contentType);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void commit(String bucketName, String objectName, InputStream inputStream, String contentType){
        try {
            storageService.uploadFile(bucketName, objectName, inputStream, contentType);
        } catch (Exception e) {
            throw new RuntimeException("Error during commit occurred: " + e.getMessage());
        }
    }
    @Override
    public void rollback(String bucketName, String objectName){
        try {
            storageService.deleteFile(bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException("Error during rollback occurred: " + e.getMessage());
        }
    }


}
