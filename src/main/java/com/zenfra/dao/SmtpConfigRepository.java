package com.zenfra.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zenfra.model.SmtpConfigModel;

public interface SmtpConfigRepository extends JpaRepository<SmtpConfigModel, String> {

}
