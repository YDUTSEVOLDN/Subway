package com.subBike.server.entity.id;

import jakarta.persistence.Id;

import java.io.Serializable;
import java.sql.Date;
import java.util.Objects;

public class BikeAmountID implements Serializable {

    String station;

    Date date;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BikeAmountID that = (BikeAmountID) o;
        return Objects.equals(station, that.station) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(station, date);
    }

    public BikeAmountID(String station, Date date) {
        this.station = station;
        this.date = date;
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

}
