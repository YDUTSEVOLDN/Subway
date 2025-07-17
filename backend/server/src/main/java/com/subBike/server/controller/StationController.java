package com.subBike.server.controller;

import com.subBike.server.entity.Station;
import com.subBike.server.repository.StationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/stations")
@Tag(name = "Stations", description = "API for station data")
public class StationController {

    @Autowired
    private StationRepository stationRepository;

    @GetMapping("/all")
    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }
} 