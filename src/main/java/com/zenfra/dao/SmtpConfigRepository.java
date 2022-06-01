package com.zenfra.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zenfra.model.SmtpConfigModel;

@Repository
public interface SmtpConfigRepository extends JpaRepository<SmtpConfigModel, String> {

	Optional<SmtpConfigModel> findByTenantId(String tenantId);

	void deleteByTenantId(String tenantId);

}
