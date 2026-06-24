package com.gdg.haksamo.domain.notification.repository;

import com.gdg.haksamo.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
