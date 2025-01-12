package com.fergie.lab1.controllers;
import com.fergie.lab1.dto.LocationDTO;
import com.fergie.lab1.dto.PersonDTO;
import com.fergie.lab1.models.Location;
import com.fergie.lab1.models.Person;
import com.fergie.lab1.security.CustomUserDetails;
import com.fergie.lab1.services.LocationService;
import com.fergie.lab1.services.PeopleService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@RequestMapping("/location")
@Controller
public class LocationController {
    private final LocationService locationService;
    private final ModelMapper modelMapper;


    @Autowired
    public LocationController(ModelMapper modelMapper, LocationService locationService) {
        this.locationService = locationService;
        this.modelMapper = modelMapper;
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveLocation(@ModelAttribute("location") LocationDTO locationDTO) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Location location = convertToLocation(locationDTO);
            locationService.addLocation(location, userDetails.getId());
            List<Location> updatedLocationsList = locationService.findAll();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "updatedLocationsList", updatedLocationsList
        ));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
                ));
        }
    }

    private Location convertToLocation(LocationDTO locationDTO) {
        return modelMapper.map(locationDTO, Location.class);
    }

    private LocationDTO convertToLocationDTO(Location location) {
        return modelMapper.map(location, LocationDTO.class);
    }
}
