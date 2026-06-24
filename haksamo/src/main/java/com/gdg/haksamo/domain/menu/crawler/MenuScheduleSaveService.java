package com.gdg.haksamo.domain.menu.crawler;

import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.menu.entity.MenuSchedule;
import com.gdg.haksamo.domain.menu.repository.MenuRepository;
import com.gdg.haksamo.domain.menu.repository.MenuScheduleRepository;
import com.gdg.haksamo.domain.restaurant.Restaurant;
import com.gdg.haksamo.domain.restaurant.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MenuScheduleSaveService {
    private final MenuRepository menuRepository;
    private final MenuScheduleRepository menuScheduleRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public void saveMenus(List<ParsedMenu> parsedMenus, String restaurantName) {
        Restaurant restaurant = restaurantRepository.findByName(restaurantName)
                .orElseThrow(() ->
                        new IllegalArgumentException("식당을 찾을 수 없습니다: " + restaurantName));

        LocalDate monday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        menuScheduleRepository.deleteByMenu_Restaurant(restaurant);

        for (ParsedMenu parsedMenu : parsedMenus) {

            Menu menu = menuRepository.findByRestaurantAndName(
                    restaurant,
                    parsedMenu.name()
            ).orElse(null);

            if (menu == null) {
                menu = menuRepository.save(
                        Menu.builder()
                                .restaurant(restaurant)
                                .name(parsedMenu.name())
                                .price(parsedMenu.price())
                                .build()
                );
            } else if (!Objects.equals(menu.getPrice(), parsedMenu.price())) {
                menu.setPrice(parsedMenu.price());
                menu = menuRepository.save(menu);
            }

            LocalDate date = monday.plusDays(parsedMenu.dayIndex());

            MenuSchedule schedule = new MenuSchedule();

            schedule.setMenu(menu);
            schedule.setDate(date);
            schedule.setTime(parsedMenu.time());
            schedule.setOperatingTime(parsedMenu.operatingTime());
            schedule.setSoldOut(false);

            menuScheduleRepository.save(schedule);
        }
    }
}
