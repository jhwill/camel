package com.cloudo.backsystem.dao;

import com.cloudo.backsystem.entity.ExpressValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpressValueDao extends JpaRepository<ExpressValueEntity,Object> {

    ExpressValueEntity findByExpressSn(String expressSn);

}
