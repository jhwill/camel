package com.cloudo.backsystem.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExpressContent implements Serializable {

    private static final long serialVersionUID = 8589835559483231840L;

    private String msg;
    private String sign;
    private String location;
    private String time;
}
