package com.utam.repository;

import com.utam.model.TurnaroundEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnaroundEventRepository extends JpaRepository<TurnaroundEvent, String> {
}
