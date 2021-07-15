package com.zenfra.ftp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.zenfra.model.ResourceModel;

@Repository
@Transactional
public interface ResourceRepo extends JpaRepository<ResourceModel, String>{

}
