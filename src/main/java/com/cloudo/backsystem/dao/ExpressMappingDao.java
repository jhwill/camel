package com.cloudo.backsystem.dao;

import com.cloudo.backsystem.entity.ExpressMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpressMappingDao extends JpaRepository<ExpressMappingEntity,Object> {

    ExpressMappingEntity findByParcelSnAndSign(String parcelSn,int sign);

    List<ExpressMappingEntity> findByParcelSn(String parcelSn);

    ExpressMappingEntity findByExpressSn(String expressSn);

    List<ExpressMappingEntity> findByStatus(String status);
}
