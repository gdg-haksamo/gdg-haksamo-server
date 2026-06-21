package com.gdg.haksamo.domain.menu.repository;

import com.gdg.haksamo.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
}
