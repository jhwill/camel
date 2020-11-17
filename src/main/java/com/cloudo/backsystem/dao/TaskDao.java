package com.cloudo.backsystem.dao;

import com.cloudo.backsystem.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskDao extends JpaRepository<TaskEntity,Object> {
    List<TaskEntity> findAllByParcelSnIn(List<String> parcelSnList);
    TaskEntity findByParcelSn(String parcelSn);
    List<TaskEntity> findByStatus(String status);

}
