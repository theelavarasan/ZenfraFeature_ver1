package com.zenfra.ftp.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zenfra.model.LogModels;

@Repository
public interface LogModelsRepo extends JpaRepository<LogModels, String>{

	List<LogModels> findByActive(boolean active);

}
