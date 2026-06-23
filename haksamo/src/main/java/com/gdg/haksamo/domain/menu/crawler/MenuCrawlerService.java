package com.gdg.haksamo.domain.menu.crawler;

import com.gdg.haksamo.domain.menu.crawler.parser.*;
import com.gdg.haksamo.domain.menu.entity.Menu;
import com.gdg.haksamo.domain.menu.entity.MenuSchedule;
import com.gdg.haksamo.domain.menu.repository.MenuRepository;
import com.gdg.haksamo.domain.menu.repository.MenuScheduleRepository;
import com.gdg.haksamo.domain.restaurant.Restaurant;
import com.gdg.haksamo.domain.restaurant.RestaurantRepository;
import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@DependsOn("restaurantService")
public class MenuCrawlerService {
    private final MenuRepository menuRepository;
    private final InformationCenterParser informationCenterParser;
    private final WelfareParser welfareParser;
    private final CheomseongParser cheomseongParser;
    private final GpParser gpParser;
    private final GongStudentParser gongStudentParser;
    private final GongStaffParser gongStaffParser;

    private final RestaurantRepository restaurantRepository;
    private final MenuScheduleRepository menuScheduleRepository;


    public MenuCrawlerService(MenuRepository menuRepository, InformationCenterParser informationCenterParser, WelfareParser welfareParser, CheomseongParser cheomseongParser, GpParser gpParser, GongStudentParser gongStudentParser, GongStaffParser gongStaffParser, RestaurantRepository restaurantRepository, MenuScheduleRepository menuScheduleRepository) {
        this.menuRepository = menuRepository;
        this.informationCenterParser = informationCenterParser;
        this.welfareParser = welfareParser;
        this.cheomseongParser = cheomseongParser;
        this.gpParser = gpParser;
        this.gongStudentParser = gongStudentParser;
        this.gongStaffParser = gongStaffParser;

        this.restaurantRepository = restaurantRepository;
        this.menuScheduleRepository = menuScheduleRepository;
    }

    //&selDate=2026-06-15 : 주소 뒤에 붙이면 메뉴 다 있는 주(6/15) 편성표 가져옴
    @PostConstruct      //실행하면 바로 크롤링 (테스트용)
    //@Scheduled(cron = "0 0 1 * * MON")
    void crawl() {
        try {
            //정보센터
            Document infoDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=35").get();
            List<ParsedMenu> infoMenu = informationCenterParser.parse(infoDoc);
            saveMenus(infoMenu, "정보센터");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //복지관
            Document welDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=36").get();
            List<ParsedMenu> welMenu = welfareParser.parse(welDoc);
            saveMenus(welMenu, "복지관");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //첨성
            Document cheomDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=37").get();
            List<ParsedMenu> cheomMenu = cheomseongParser.parse(cheomDoc);
            saveMenus(cheomMenu, "첨성");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //글플
            Document gpDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=46").get();
            List<ParsedMenu> gpMenu = gpParser.parse(gpDoc);
            saveMenus(gpMenu, "글로벌플라자");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //공식당 교직원
            Document gongStaffDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=85").get();
            List<ParsedMenu> gongStaffMenu = gongStaffParser.parse(gongStaffDoc);
            saveMenus(gongStaffMenu, "공식당 교직원식당");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //공식당 학생
            Document gongStudentDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=86").get();
            List<ParsedMenu> gongStudentMenu = gongStudentParser.parse(gongStudentDoc);
            saveMenus(gongStudentMenu, "공식당 학생식당");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void saveMenus(
            List<ParsedMenu> parsedMenus,
            String restaurantName
    ) {

        Restaurant restaurant = restaurantRepository.findByName(restaurantName)
                .orElseThrow(() ->
                        new IllegalArgumentException("식당을 찾을 수 없습니다: " + restaurantName));

        LocalDate monday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

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
                                .operatingTime(parsedMenu.operatingTime())
                                .build()
                );
            }

            LocalDate date = monday.plusDays(parsedMenu.dayIndex());

            MenuSchedule existingSchedule =
                    menuScheduleRepository
                            .findByMenuAndDateAndTime(
                                    menu,
                                    date,
                                    parsedMenu.time()
                            )
                            .orElse(null);

            if (existingSchedule == null) {
                MenuSchedule schedule = new MenuSchedule();

                schedule.setMenu(menu);
                schedule.setDate(date);
                schedule.setTime(parsedMenu.time());
                schedule.setSoldOut(false);

                menuScheduleRepository.save(schedule);
            }
        }
    }
}
