package com.subBike.server.entity;

import com.subBike.server.entity.id.SubAmountID;
import jakarta.persistence.*;

import java.sql.Date;
@Table(name="subAmount")
@IdClass(SubAmountID.class)
@Entity
public class SubAmount {
    @Id
    private Date date;
    @Id
    private String station;
    @Id
    private Integer time;
    private Integer inNum;
    private Integer outNum;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }







    public Integer getInNum() {
        return inNum;
    }

    public void setInNum(Integer inNum) {
        this.inNum = inNum;
    }

    public Integer getOutNum() {
        return outNum;
    }

    public void setOutNum(Integer outNum) {
        this.outNum = outNum;
    }

    public SubAmount() {}
}
