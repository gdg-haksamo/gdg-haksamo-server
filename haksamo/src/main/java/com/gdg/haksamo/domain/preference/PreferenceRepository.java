package com.gdg.haksamo.domain.preference;

import com.gdg.haksamo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreferenceRepository extends JpaRepository<Preference, Long> {
    List<Preference> findByUser(User user);

    void deleteByUser(User user);
}
