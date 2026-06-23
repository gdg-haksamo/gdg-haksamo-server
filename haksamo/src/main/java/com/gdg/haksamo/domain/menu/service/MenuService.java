package com.gdg.haksamo.domain.menu.service;

import com.gdg.haksamo.domain.menu.dto.MenuResponse;
import com.gdg.haksamo.domain.menu.entity.MenuSchedule;
import com.gdg.haksamo.domain.menu.repository.MenuScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuScheduleRepository menuScheduleRepository;

    public List<MenuResponse> getMenus(LocalDate date) {
        List<MenuSchedule> schedules = menuScheduleRepository.findByDate(date);

        List<MenuResponse> result = new ArrayList<>();
        for (MenuSchedule schedule : schedules) {
            result.add(
                    new MenuResponse(
                            schedule.getMenu().getRestaurant().getName(),
                            schedule.getMenu().getName(),
                            schedule.getMenu().getPrice(),
                            schedule.getMenu().getOperatingTime(),
                            schedule.getTime()
                    )
            );
        }
        return result;
    }
}