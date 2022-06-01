package com.zenfra.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zenfra.model.AwsInstanceCcrData;

@Repository
public interface AwsInstanceCcrDataRepository  extends JpaRepository<AwsInstanceCcrData, Long>{


}
