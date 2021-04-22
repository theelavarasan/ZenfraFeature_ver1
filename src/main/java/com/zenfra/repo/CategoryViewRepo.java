package com.zenfra.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zenfra.model.CategoryView;

@Repository
public interface CategoryViewRepo extends JpaRepository<CategoryView, String>{

}
