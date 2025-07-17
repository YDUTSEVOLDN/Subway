package com.subBike.server.entity;

import com.subBike.server.entity.id.BikeAmountID;
import com.subBike.server.entity.id.SubAmountID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.sql.Date;
@Table(name="bikeAmount")
@IdClass(BikeAmountID.class)
@Entity
public class BikeAmount {
    @Id
    String station;
    @Id
    Date date;
    Long number;

    public BikeAmount(String station, Date date, Long number) {
        this.station = station;
        this.date = date;
        this.number = number;
    }

    public BikeAmount() {
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

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long numBer) {
        this.number = numBer;
    }
}
