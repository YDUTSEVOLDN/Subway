package com.subBike.server.entity;

import com.subBike.server.entity.id.SubAmountID;
import jakarta.persistence.*;

import java.sql.Date;
@Table(name="subAmount")
@IdClass(SubAmountID.class)
@Entity
public class SubAmount {
    // 联合主键（使用@EmbeddedId）
    @EmbeddedId
    private SubAmountID id;
    Integer inNum;
    Integer outNum;

    public SubAmountID getId() {
        return id;
    }

    public void setId(SubAmountID id) {
        this.id = id;
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

}
