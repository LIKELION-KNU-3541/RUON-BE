package com.springboot.ruon.auth.data.repository;

import com.springboot.ruon.auth.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
