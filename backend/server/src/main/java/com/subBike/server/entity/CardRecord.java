package com.subBike.server.entity;

import jakarta.persistence.*;

import java.sql.Date;
import java.sql.Timestamp;

@Table(name="cardRecord")
@Entity

public class CardRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer recordID;
    private String inStop;
    private Timestamp inTime;
    private String outStop;
    private Timestamp outTime;
    private Date date;

    public Integer getRecordID() {
        return recordID;
    }

    public void setRecordID(Integer recordID) {
        this.recordID = recordID;
    }

    public String getInStop() {
        return inStop;
    }

    public void setInStop(String inStop) {
        this.inStop = inStop;
    }

    public Timestamp getInTime() {
        return inTime;
    }

    public void setInTime(Timestamp inTime) {
        this.inTime = inTime;
    }

    public String getOutStop() {
        return outStop;
    }

    public void setOutStop(String outStop) {
        this.outStop = outStop;
    }

    public Timestamp getOutTime() {
        return outTime;
    }

    public void setOutTime(Timestamp outTime) {
        this.outTime = outTime;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}