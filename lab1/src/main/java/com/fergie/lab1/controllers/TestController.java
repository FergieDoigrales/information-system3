package com.fergie.lab1.controllers;

import com.fergie.lab1.dto.MovieDTO;
import com.fergie.lab1.models.Coordinates;
import com.fergie.lab1.models.Movie;
import com.fergie.lab1.models.Person;
import com.fergie.lab1.security.CustomUserDetails;
import com.fergie.lab1.services.CoordinatesService;
import com.fergie.lab1.services.LocationService;
import com.fergie.lab1.services.MoviesService;
import com.fergie.lab1.services.PeopleService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/movietest")
public class TestController {
    private MoviesService movieService;
    private LocationService locationService;
    private PeopleService personService;
    private ModelMapper modelMapper;
    private CoordinatesService coordinatesService;


    @Autowired
    public TestController(ModelMapper modelMapper, LocationService locationService, MoviesService movieService, PeopleService personService, CoordinatesService coordinatesService) {
        this.locationService = locationService;
        this.modelMapper = modelMapper;
        this.movieService = movieService;
        this.personService = personService;
        this.coordinatesService = coordinatesService;

    }

    @PostMapping("/save")
    public ResponseEntity<?> saveMovie(@RequestBody MovieDTO movieDTO) {
        try {
            Long userId = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Coordinates coordinates = new Coordinates();
            coordinates.setX(movieDTO.getCoordinates().getX());
            coordinates.setY(movieDTO.getCoordinates().getY());
            coordinatesService.addCoordinates(coordinates, userId);

            Person director = new Person();
            director.setName(movieDTO.getDirector().getName());
            director.setEyeColor(movieDTO.getDirector().getEyeColor());
            director.setHairColor(movieDTO.getDirector().getHairColor());
            director.setPassportID(movieDTO.getDirector().getPassportID());
            director.setNationality(movieDTO.getDirector().getNationality());
            director.setLocation(locationService.findById(movieDTO.getDirector().getLocation().getId()));
            personService.addPerson(director, userId);

            Person screenwriter = new Person();
            screenwriter.setName(movieDTO.getScreenwriter().getName());
            screenwriter.setEyeColor(movieDTO.getScreenwriter().getEyeColor());
            screenwriter.setHairColor(movieDTO.getScreenwriter().getHairColor());
            screenwriter.setPassportID(movieDTO.getScreenwriter().getPassportID());
            screenwriter.setNationality(movieDTO.getScreenwriter().getNationality());
            screenwriter.setLocation(locationService.findById(movieDTO.getScreenwriter().getLocation().getId()));
            personService.addPerson(screenwriter, userId);

            Person operator = new Person();
            operator.setName(movieDTO.getOperator().getName());
            operator.setEyeColor(movieDTO.getOperator().getEyeColor());
            operator.setHairColor(movieDTO.getOperator().getHairColor());
            operator.setPassportID(movieDTO.getOperator().getPassportID());
            operator.setNationality(movieDTO.getOperator().getNationality());
            operator.setLocation(locationService.findById(movieDTO.getOperator().getLocation().getId()));
            personService.addPerson(operator, userId);


            Movie movie = new Movie();
            movie.setName(movieDTO.getName());
            movie.setCoordinates(coordinates);
            movie.setOscarsCount(movieDTO.getOscarsCount());
            movie.setBudget(movieDTO.getBudget());
            movie.setTotalBoxOffice(movieDTO.getTotalBoxOffice());
            movie.setMpaaRating(movieDTO.getMpaaRating());
            movie.setDirector(director);
            movie.setScreenwriter(screenwriter);
            movie.setOperator(operator);
            movie.setLength(movieDTO.getLength());
            movie.setGoldenPalmCount(movieDTO.getGoldenPalmCount());
            movie.setGenre(movieDTO.getGenre());

            movieService.addMovie(movie, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Movie and related entities saved successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

}
