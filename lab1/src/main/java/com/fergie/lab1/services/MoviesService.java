package com.fergie.lab1.services;

import com.fergie.lab1.dto.MovieDTO;
import com.fergie.lab1.models.*;
import com.fergie.lab1.models.enums.*;
import com.fergie.lab1.repositories.MoviesRepository;
import com.fergie.lab1.specification.MovieSpecification;
import com.fergie.lab1.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
@Service
public class MoviesService {
    private final MoviesRepository moviesRepository;
    private final MovieAuditService movieAuditService;
    private final CoordinatesService coordinatesService;
    private final PeopleService peopleService;
    private final LocationService locationService;
    private final LocationDetailsService locationDetailsService;

    @Autowired
    public MoviesService(MoviesRepository moviesRepository, MovieAuditService movieAuditService, CoordinatesService coordinatesService,
                         PeopleService peopleService, LocationService locationService, LocationDetailsService locationDetailsService) {
        this.moviesRepository = moviesRepository;
        this.movieAuditService = movieAuditService;
        this.coordinatesService = coordinatesService;
        this.peopleService = peopleService;
        this.locationService = locationService;
        this.locationDetailsService = locationDetailsService;
    }

    @Cacheable(value = "moviesCache", key = "#Id")
    public Movie findById(Long Id){
        Optional<Movie> movie = moviesRepository.findById(Id);
        return movie.orElse(null);
    }

    public Page<Movie> searchMovies(String query, Pageable pageable) {
        MovieSpecification specification = new MovieSpecification(query);
        return moviesRepository.findAll(specification, pageable);
    }


    public int getMovieCountByGenre(String genre) {
        MovieGenre mGenre = MovieGenre.valueOf(genre);
        return moviesRepository.countMoviesByGenre(mGenre);
    }


    public List<Integer> getUniqueGoldenPalmCounts() {
        return moviesRepository.findUniqueGoldenPalmCounts();
    }

    public String getMovieWithMinDirector() {
        return moviesRepository.findMovieWithMinDirector();
    }

    @Cacheable(value = "moviesCache", key = "#name")
    public Movie findByName(String name){
        Optional<Movie> movie = moviesRepository.findByName(name);
        return movie.orElse(null);
    }

    @Cacheable(value = "moviesCache", key = "#pageable")
    public Page<Movie> findAll(Pageable pageable) {
        return moviesRepository.findAll(pageable);
    }

    public void saveAll(List<Movie> movies) {
        for (Movie movie : movies) {
            if (!isUnique(movie.getName(), movie.getOperator())) {
                throw new IllegalArgumentException("Movie with name " + movie.getName() + " and operator " + movie.getOperator().getName() + " already exists");
            }
        }
        moviesRepository.saveAll(movies);
    }

    @CacheEvict(value = "moviesCache", allEntries = true)
    @Transactional
    public void addMovie(Movie movie, Long authorID) {
        if (!isUnique(movie.getName(), movie.getDirector())) {
            throw new IllegalArgumentException("Movie with such name and director already exists");
        }
        Movie addedMovie = moviesRepository.save(enrichMovie(movie, authorID));
        movieAuditService.recordMovieChange(addedMovie, "CREATE", "name", movie.getName(), movie.getName(), String.valueOf(authorID));
    }

    @CacheEvict(value = "moviesCache", allEntries = true)
    @Transactional
    public Movie updateMovie(AccessRole userRole, Long authorID, Long id, Movie updatedMovie) {
        Optional<Movie> existingMovie = moviesRepository.findById(id);

        if (existingMovie.isPresent()) {
            Movie movie = existingMovie.get();
            System.out.println("Author ID: " + authorID);
            System.out.println("Movie author ID: " + movie.getAuthorID());
            if (!authorID.equals(movie.getAuthorID()) && !userRole.equals(AccessRole.ADMIN)) {
                throw new IllegalArgumentException("Author ID does not match and user is not an admin");
            }
            movieAuditService.recordMovieChange(movie, "UPDATE", "name", movie.getName(), updatedMovie.getName(), String.valueOf(authorID));
            movie.setName(updatedMovie.getName());
            movie.setName(updatedMovie.getName());
            movie.setCoordinates(updatedMovie.getCoordinates());
            movie.setOscarsCount(updatedMovie.getOscarsCount());
            movie.setBudget(updatedMovie.getBudget());
            movie.setTotalBoxOffice(updatedMovie.getTotalBoxOffice());
            movie.setMpaaRating(updatedMovie.getMpaaRating());
            movie.setDirector(updatedMovie.getDirector());
            movie.setScreenwriter(updatedMovie.getScreenwriter());
            movie.setOperator(updatedMovie.getOperator());
            movie.setLength(updatedMovie.getLength());
            movie.setGoldenPalmCount(updatedMovie.getGoldenPalmCount());
            movie.setGenre(updatedMovie.getGenre());

            return moviesRepository.save(movie);
        } else {
            return null;
        }
    }

    @CacheEvict(value = "moviesCache", allEntries = true)
    @Transactional
    public void deleteMovie(AccessRole userRole, Long authorID, Long id) {
        Optional<Movie> existingMovie = moviesRepository.findById(id);
        if (existingMovie.isPresent()) {
            Movie movie = existingMovie.get();
            if (!authorID.equals(movie.getAuthorID()) && !userRole.equals(AccessRole.ADMIN)) {
                throw new IllegalArgumentException("Author ID does not match and user is not an admin");
            }
            movieAuditService.recordMovieChange(movie, "DELETE", "name", movie.getName(), "DELETED", String.valueOf(authorID));
        }
        moviesRepository.deleteById(id);
    }

    public Movie processMovieDTO(MovieDTO movieDTO, Long userId) {
        try {
            Movie movie = new Movie();
            movie.setName(movieDTO.getName());
            movie.setOscarsCount(movieDTO.getOscarsCount());
            movie.setBudget(movieDTO.getBudget());
            movie.setTotalBoxOffice(movieDTO.getTotalBoxOffice());
            movie.setMpaaRating(MpaaRating.valueOf(String.valueOf(movieDTO.getMpaaRating())));
            movie.setLength(movieDTO.getLength());
            movie.setGoldenPalmCount(movieDTO.getGoldenPalmCount());
            movie.setGenre(MovieGenre.valueOf(String.valueOf(movieDTO.getGenre())));
            movie.setAuthorID(userId);

            if (movieDTO.getCoordinates().getId() != null) {
                movie.setCoordinates(coordinatesService.findById(movieDTO.getCoordinates().getId()));
            } else {
                Coordinates coordinates = new Coordinates();
                coordinates.setX(movieDTO.getCoordinates().getX());
                coordinates.setY(movieDTO.getCoordinates().getY());
                if (CoordinatesValidator.validateCoordinates(coordinates)) {
                    movie.setCoordinates(coordinates);
                } else {
                    throw new IllegalArgumentException("Invalid coordinates data");
                }
            }
            if (movieDTO.getDirector() != null) {
                if (movieDTO.getDirector().getId() != null) {
                    movie.setDirector(peopleService.findById(movieDTO.getDirector().getId()));
                } else {
                    Person director = new Person();
                    director.setName(movieDTO.getDirector().getName());
                    director.setPassportID(movieDTO.getDirector().getPassportID());
                    director.setHairColor(Color.valueOf(String.valueOf(movieDTO.getDirector().getHairColor())));
                    director.setEyeColor(Color.valueOf(String.valueOf(movieDTO.getDirector().getEyeColor())));
                    director.setNationality(Country.valueOf(String.valueOf(movieDTO.getDirector().getNationality())));
                    if (movieDTO.getDirector().getLocation().getId() != null) {
                        director.setLocation(locationService.findById(movieDTO.getDirector().getLocation().getId()));
                    } else {
                        Location newLocation = new Location();
                        newLocation.setY(movieDTO.getDirector().getLocation().getY());
                        newLocation.setX(movieDTO.getDirector().getLocation().getX());
                        newLocation.setZ(movieDTO.getDirector().getLocation().getZ());
                        if (movieDTO.getDirector().getLocation().getLocationDetails().getId() != null) {
                            System.out.println("Не налл");
                            newLocation.setLocationDetails(locationDetailsService.findById(movieDTO.getDirector().getLocation().getLocationDetails().getId()));
                        } else {
                            LocationDetails locationDetails = new LocationDetails();
                            locationDetails.setAddress(movieDTO.getDirector().getLocation().getLocationDetails().getAddress());
                            locationDetails.setDescription(movieDTO.getDirector().getLocation().getLocationDetails().getDescription());
                            if (LocationDetailsValidator.isValidLocationDetails(locationDetails)) {
                                newLocation.setLocationDetails(locationDetails);
                            } else {
                                throw new IllegalArgumentException("Invalid location details data");
                            }
                        }
                        if (LocationValidator.validateLocation(newLocation)) {
                            director.setLocation(newLocation);
                        } else {
                            throw new IllegalArgumentException("Invalid location data");
                        }

                    }
                    if (PersonValidator.isPersonValid(director)) {
                        movie.setDirector(director);
                    } else {
                        throw new IllegalArgumentException("Invalid person data");
                    }

                }
            } else {
                movie.setDirector(null);
            }
            if (movieDTO.getScreenwriter() != null) {
                if (movieDTO.getScreenwriter().getId() != null) {
                    movie.setScreenwriter(peopleService.findById(movieDTO.getScreenwriter().getId()));
                } else {
                    Person screenwriter = new Person();
                    screenwriter.setName(movieDTO.getScreenwriter().getName());
                    screenwriter.setPassportID(movieDTO.getScreenwriter().getPassportID());
                    screenwriter.setHairColor(Color.valueOf(String.valueOf(movieDTO.getScreenwriter().getHairColor())));
                    screenwriter.setEyeColor(Color.valueOf(String.valueOf(movieDTO.getScreenwriter().getEyeColor())));
                    screenwriter.setNationality(Country.valueOf(String.valueOf(movieDTO.getScreenwriter().getNationality())));
                    if (movieDTO.getScreenwriter().getLocation().getId() != null) {
                        screenwriter.setLocation(locationService.findById(movieDTO.getScreenwriter().getLocation().getId()));
                    } else {
                        Location newLocation = new Location();
                        newLocation.setY(movieDTO.getScreenwriter().getLocation().getY());
                        newLocation.setX(movieDTO.getScreenwriter().getLocation().getX());
                        newLocation.setZ(movieDTO.getScreenwriter().getLocation().getZ());
                        if (movieDTO.getScreenwriter().getLocation().getLocationDetails().getId() != null) {
                            newLocation.setLocationDetails(locationDetailsService.findById(movieDTO.getScreenwriter().getLocation().getLocationDetails().getId()));
                        } else {
                            LocationDetails locationDetails = new LocationDetails();
                            locationDetails.setAddress(movieDTO.getScreenwriter().getLocation().getLocationDetails().getAddress());
                            locationDetails.setDescription(movieDTO.getScreenwriter().getLocation().getLocationDetails().getDescription());
                            if (LocationDetailsValidator.isValidLocationDetails(locationDetails)) {
                                newLocation.setLocationDetails(locationDetails);
                            } else {
                                throw new IllegalArgumentException("Invalid location details data");
                            }
                        }
                        if (LocationValidator.validateLocation(newLocation)) {
                            screenwriter.setLocation(newLocation);
                        } else {
                            throw new IllegalArgumentException("Invalid location data");
                        }
                    }
                    if (PersonValidator.isPersonValid(screenwriter)) {
                        movie.setScreenwriter(screenwriter);

                    } else {
                        throw new IllegalArgumentException("Invalid person data");
                    }
                }
            } else {
                movie.setScreenwriter(null);
            }
            if (movieDTO.getOperator().getId() != null) {
                movie.setOperator(peopleService.findById(movieDTO.getOperator().getId()));
            } else {
                Person operator = new Person();
                operator.setName(movieDTO.getOperator().getName());
                operator.setPassportID(movieDTO.getOperator().getPassportID());
                operator.setHairColor(Color.valueOf(String.valueOf(movieDTO.getOperator().getHairColor())));
                operator.setEyeColor(Color.valueOf(String.valueOf(movieDTO.getOperator().getEyeColor())));
                operator.setNationality(Country.valueOf(String.valueOf(movieDTO.getOperator().getNationality())));
                if (movieDTO.getOperator().getLocation().getId() != null) {
                    operator.setLocation(locationService.findById(movieDTO.getOperator().getLocation().getId()));
                } else {
                    Location newLocation = new Location();
                    newLocation.setY(movieDTO.getOperator().getLocation().getY());
                    newLocation.setX(movieDTO.getOperator().getLocation().getX());
                    newLocation.setZ(movieDTO.getOperator().getLocation().getZ());
                    if (movieDTO.getOperator().getLocation().getLocationDetails().getId() != null) {
                        newLocation.setLocationDetails(locationDetailsService.findById(movieDTO.getOperator().getLocation().getLocationDetails().getId()));
                    } else {
                        LocationDetails locationDetails = new LocationDetails();
                        locationDetails.setAddress(movieDTO.getOperator().getLocation().getLocationDetails().getAddress());
                        locationDetails.setDescription(movieDTO.getOperator().getLocation().getLocationDetails().getDescription());
                        if (LocationDetailsValidator.isValidLocationDetails(locationDetails)) {
                            newLocation.setLocationDetails(locationDetails);
                        } else {
                            throw new IllegalArgumentException("Invalid location details data");
                        }
                    }
                    if (LocationValidator.validateLocation(newLocation)) {
                        operator.setLocation(newLocation);
                    } else {
                        throw new IllegalArgumentException("Invalid location data");
                    }
                }
                if (PersonValidator.isPersonValid(operator)) {
                    movie.setOperator(operator);
                } else {
                    throw new IllegalArgumentException("Invalid person data");
                }
            }

            if (MovieValidator.validateMovie(movie)) {
                return movie;
            } else {
                throw new IllegalArgumentException("Invalid movie data");
            }
        } catch (Exception e ) {
            return null;
        }
    }

    private Boolean isUnique(String name, Person operator) {
        return moviesRepository.findFirstByNameAndOperator(name, operator).isEmpty();
    }


    private Movie enrichMovie(Movie movie, Long authorId) { //как получить Id автора
        movie.setCreationDate(new java.util.Date());
        movie.setAuthorID(authorId);
        return movie;
    }

    private Sort.Order getSortObject(String sortField, String sortOrder) {
        return "desc".equalsIgnoreCase(sortOrder)
                ? Sort.Order.desc(sortField)
                : Sort.Order.asc(sortField);
    }



}