package com.fergie.lab1.services;

import com.fergie.lab1.models.Movie;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class CoordinatorComponent implements TransactionParticipant{
    private final StorageService storageService;
    private final MoviesService moviesService;

    public CoordinatorComponent(StorageService storageService, MoviesService moviesService) {
        this.storageService = storageService;
        this.moviesService = moviesService;
    }

    public void execute(String bucketName, String objectName, InputStream inputStream, String contentType, List<Movie> movies){
        Object[] result = prepare(bucketName, objectName, inputStream, contentType, movies);
        boolean status = (Boolean) result[0];
        String message = (String) result[1];

        if (status) {
            commit(bucketName, objectName, inputStream, contentType);
        } else {
            rollback(bucketName, objectName, message);
        }
    }
    @Override
    public Object[] prepare(String bucketName, String objectName, InputStream inputStream, String contentType, List<Movie> movies){

        try {
            moviesService.saveAll(movies);
            storageService.prepareUploadFile(bucketName, objectName, inputStream, contentType);
        } catch (Exception e) {
            return new Object[]{false, e.getMessage()};
        }
        return new Object[]{true, null};
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
    public void rollback(String bucketName, String objectName, String e){
        storageService.deleteFile(bucketName, objectName);
        throw new RuntimeException("Rollback cause of " + e) ;
    }


}
