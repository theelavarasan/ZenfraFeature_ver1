package com.zenfra.ftp.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.zenfra.model.PolicyModel;

@Repository
@Transactional
public interface PolicyRepo extends JpaRepository<PolicyModel, String> {

}
