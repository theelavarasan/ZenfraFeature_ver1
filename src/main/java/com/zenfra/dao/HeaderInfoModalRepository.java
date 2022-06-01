package com.zenfra.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zenfra.model.HeaderInfoModel;

@Repository
public interface HeaderInfoModalRepository extends JpaRepository<HeaderInfoModel, String> {

	List<HeaderInfoModel> getHeaderInfoModelByDeviceType(String deviceType);

}
