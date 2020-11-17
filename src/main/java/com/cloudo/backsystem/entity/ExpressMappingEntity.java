package com.cloudo.backsystem.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "express_mapping")
@EntityListeners(AuditingEntityListener.class)
public class ExpressMappingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String parcelSn;
    private String expressCode;
    private String expressSn;
    private String status;
    private int sign;
    @CreatedDate
    private Date createTime;
}
