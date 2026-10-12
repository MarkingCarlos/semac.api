package com.semac.java_api.repository;

import com.semac.java_api.model.Time;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeRepository extends JpaRepository<Time, Integer> {

    List<Time> findAllByOrderByIdAsc();
}
