package com.subBike.server.entity.id;

import java.io.Serializable;
import java.sql.Date;
import java.util.Objects;
public class SubAmountID implements Serializable{
    private Date date;
    private String station;
    private Integer time;

    // 必须提供无参构造函数
    public SubAmountID() {}

    public SubAmountID(Date date, String station, Integer time) {
        this.date = date;
        this.station = station;
        this.time = time;
    }

    // 必须重写equals和hashCode方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubAmountID that = (SubAmountID) o;
        return Objects.equals(date, that.date) &&
                Objects.equals(station, that.station) &&
                Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, station, time);
    }

    // Getters and setters
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
}
