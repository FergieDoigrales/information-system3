package com.fergie.lab1.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fergie.lab1.dto.MovieDTO;
import com.fergie.lab1.models.Movie;
import com.fergie.lab1.security.CustomUserDetails;
import com.fergie.lab1.services.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/movie")
public class MoviesController {
    private final MoviesService moviesService;
    private final PeopleService peopleService;

    private final CoordinatesService coordinatesService;

    private final ModelMapper modelMapper;
    private final PreparePageService preparePageService;

    private final SimpMessagingTemplate messagingTemplate;


    @Autowired
    public MoviesController(MoviesService moviesService, ModelMapper modelMapper, PeopleService peopleService,
                            CoordinatesService coordinatesService, SimpMessagingTemplate messagingTemplate,
                            PreparePageService preparePageService) {
        this.moviesService = moviesService;
        this.modelMapper = modelMapper;
        this.peopleService = peopleService;
        this.coordinatesService = coordinatesService;
        this.preparePageService = preparePageService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveMovie(@ModelAttribute("movie") MovieDTO movieDTO,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(defaultValue = "name") String sort) {
        try {
            Movie movie = convertToMovie(movieDTO);
            setRelatedEntities(movie);
            CustomUserDetails userDetails = getUserInfo();
            moviesService.addMovie(movie, userDetails.getId());
            Page<Movie> moviePage = preparePageService.getMoviePage(page, size, sort);
            messagingTemplate.convertAndSend("/topic/movies", Map.of(
                    "moviePage", moviePage,
                    "currentUserId", userDetails.getId(),
                    "userRole", userDetails.getRole().name(),
                    "action", "save",
                    "currentPage", moviePage.getNumber()
            ));
            System.out.println("Sending moviePage: " + moviePage);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "moviePage", moviePage.getContent(),
                    "totalPages", moviePage.getTotalPages(),
                    "currentPage", moviePage.getNumber(),
                    "currentUserId", userDetails.getId(),
                    "userRole", userDetails.getRole().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        Movie movie = moviesService.findById(id);
        messagingTemplate.convertAndSend("/topic/movies", id);
        if (movie != null) {
            return ResponseEntity.ok(movie);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateMovie(@ModelAttribute("movie") MovieDTO movieDTO,
                                         @RequestParam(value = "file", required = false) MultipartFile file,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(defaultValue = "name") String sort) throws IOException {

        CustomUserDetails userDetails = getUserInfo();

        if (movieDTO == null) {
            String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            movieDTO = objectMapper.readValue(fileContent, MovieDTO.class);
        }

        Movie movie = convertToMovie(movieDTO);

        try {
            setRelatedEntities(movie);
            Movie updatedMovie = moviesService.updateMovie(userDetails.getRole(), userDetails.getId(), movie.getMovieId(), movie);
            Page<Movie> moviePage = preparePageService.getMoviePage(page, size, sort);
            messagingTemplate.convertAndSend("/topic/movies", Map.of(
                    "moviePage", moviePage,
                    "action", "update",
                    "currentPage", moviePage.getNumber()
            ));
            if (updatedMovie != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "updatedMovie", updatedMovie,
                        "moviePage", moviePage.getContent(),
                        "totalPages", moviePage.getTotalPages(),
                        "currentPage", moviePage.getNumber()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", "Movie not found"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMovie(@PathVariable Long id,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(defaultValue = "name") String sort) {
        CustomUserDetails userDetails = getUserInfo();
        try {
            moviesService.deleteMovie(userDetails.getRole(), userDetails.getId(), id);
            Page<Movie> moviePage = preparePageService.getMoviePage(page, size, sort);
            messagingTemplate.convertAndSend("/topic/movies", Map.of(
                    "moviePage", moviePage,
                    "currentUserId", userDetails.getId(),
                    "userRole", userDetails.getRole().name(),
                    "action", "delete",
                    "currentPage", moviePage.getNumber()
            ));
            return ResponseEntity.ok(Map.of("success", true,
                    "moviePage", moviePage.getContent(),
                    "totalPages", moviePage.getTotalPages(),
                    "currentPage", moviePage.getNumber()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Access denied"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchMovies(@RequestParam("query") String query,
                                          @RequestParam int page,
                                          @RequestParam int size,
                                          @RequestParam String sort,
                                          @RequestParam String sortOrder) {
        Sort.Order sortOrderObj = getSortObject(sort, sortOrder);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrderObj));
        Page<Movie> moviePage = moviesService.searchMovies(query, pageable);
        return ResponseEntity.ok(Map.of(
                "movies", moviePage,
                "currentPage", moviePage.getNumber(),
                "totalPages", moviePage.getTotalPages()
        ));
    }

    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestParam("query") String query,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size,
                                                              @RequestParam String sort,
                                                              @RequestParam String sortOrder) {
        Sort.Order sortOrderObj = getSortObject(sort, sortOrder);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOrderObj));
        Page<Movie> moviePage = moviesService.searchMovies(query, pageable);
        CustomUserDetails userDetails = getUserInfo();
        return ResponseEntity.ok(Map.of(
                "movies", moviePage,
                "currentUserId", userDetails.getId(),
                "userRole", userDetails.getRole().name(),
                "currentPage", moviePage.getNumber(),
                "totalPages", moviePage.getTotalPages()
        ));
    }

    @GetMapping("/count-by-genre")
    public ResponseEntity<?> getMovieCountByGenre(@RequestParam String genre) {
        try {
            int count = moviesService.getMovieCountByGenre(genre);

            return ResponseEntity.ok()
                    .body(Map.of("genre", genre, "count", count));

        } catch (Exception e) {
            return ResponseEntity.ok()
                    .body(Map.of("genre", genre, "count", 0));
        }
    }

    @GetMapping("/unique-golden-palm-counts")
    public ResponseEntity<List<Integer>> getUniqueGoldenPalmCounts() {
        try {
            List<Integer> uniquePalmCounts = moviesService.getUniqueGoldenPalmCounts();
            return ResponseEntity.ok(uniquePalmCounts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }

    }

    @GetMapping("/min-director")
    public ResponseEntity<String> getMovieWithMinDirector() {
        try {
            String movie = moviesService.getMovieWithMinDirector();
            return ResponseEntity.ok(movie);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }


    private Sort.Order getSortObject(String sortField, String sortOrder) {
        return "desc".equalsIgnoreCase(sortOrder)
                ? Sort.Order.desc(sortField)
                : Sort.Order.asc(sortField);
    }

    private CustomUserDetails getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) authentication.getPrincipal();
    }
    private void setRelatedEntities(Movie movie) {
        movie.setCoordinates(coordinatesService.findById(movie.getCoordinates().getId()));
        movie.setOperator(peopleService.findById(movie.getOperator().getId()));
        movie.setScreenwriter(peopleService.findById(movie.getScreenwriter().getId()));
        movie.setDirector(peopleService.findById(movie.getDirector().getId()));
    }
    private Movie convertToMovie(MovieDTO movieDTO) {
        return modelMapper.map(movieDTO, Movie.class);
    }

    private MovieDTO convertToMovieDTO(Movie movie) {
        return modelMapper.map(movie, MovieDTO.class);
    }

}

