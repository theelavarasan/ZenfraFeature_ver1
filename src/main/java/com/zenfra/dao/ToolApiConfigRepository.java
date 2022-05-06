package com.zenfra.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.zenfra.model.ToolApiConfigModel;

@Repository
public interface ToolApiConfigRepository extends JpaRepository<ToolApiConfigModel, String> {

	
//	@Query( value = "SELECT first_name, last_name FROM USER_TEMP WHERE user_id=:userId",  nativeQuery = true)
//	Optional<ToolApiConfigModel> findByUserId(String userId);

	List<ToolApiConfigModel> findByIsActive(boolean isActive);

}
