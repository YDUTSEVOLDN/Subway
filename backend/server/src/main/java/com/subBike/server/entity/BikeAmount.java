package com.subBike.server.entity;

import jakarta.persistence.Id;

import java.sql.Date;

public class BikeAmount {
    @Id
    String station;
    @Id
    Date date;
    Long numBer;

    public BikeAmount(String station, Date date, Long numBer) {
        this.station = station;
        this.date = date;
        this.numBer = numBer;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getNumBer() {
        return numBer;
    }

    public void setNumBer(Long numBer) {
        this.numBer = numBer;
    }
}
