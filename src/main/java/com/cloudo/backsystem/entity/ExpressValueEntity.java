package com.cloudo.backsystem.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "express_value")
@EntityListeners(AuditingEntityListener.class)
public class ExpressValueEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String expressSn;
    private String code;
    private String info;
    @CreatedDate
    private Date createTime;
}
