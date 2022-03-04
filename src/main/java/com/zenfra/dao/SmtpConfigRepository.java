package com.zenfra.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zenfra.model.SmtpConfigModel;

@Repository
public interface SmtpConfigRepository extends JpaRepository<SmtpConfigModel, String> {

}
