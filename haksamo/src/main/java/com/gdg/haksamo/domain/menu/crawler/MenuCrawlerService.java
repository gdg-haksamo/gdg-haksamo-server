package com.gdg.haksamo.domain.menu.crawler;

import com.gdg.haksamo.domain.menu.crawler.parser.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@DependsOn("restaurantService")
public class MenuCrawlerService {
    private final InformationCenterParser informationCenterParser;
    private final WelfareParser welfareParser;
    private final CheomseongParser cheomseongParser;
    private final GpParser gpParser;
    private final GongStudentParser gongStudentParser;
    private final GongStaffParser gongStaffParser;

    private final MenuScheduleSaveService menuScheduleSaveService;

    public MenuCrawlerService(InformationCenterParser informationCenterParser, WelfareParser welfareParser, CheomseongParser cheomseongParser, GpParser gpParser, GongStudentParser gongStudentParser, GongStaffParser gongStaffParser, MenuScheduleSaveService menuScheduleSaveService) {
        this.informationCenterParser = informationCenterParser;
        this.welfareParser = welfareParser;
        this.cheomseongParser = cheomseongParser;
        this.gpParser = gpParser;
        this.gongStudentParser = gongStudentParser;
        this.gongStaffParser = gongStaffParser;

        this.menuScheduleSaveService = menuScheduleSaveService;
    }

    //&selDate=2026-06-15 : 주소 뒤에 붙이면 메뉴 다 있는 주(6/15) 편성표 가져옴
    //@PostConstruct      //실행하면 바로 크롤링 (테스트용)
    @Scheduled(cron = "0 0 1 * * MON")
    void crawl() {
        try {
            //정보센터
            Document infoDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=35").get();
            List<ParsedMenu> infoMenu = informationCenterParser.parse(infoDoc);
            menuScheduleSaveService.saveMenus(infoMenu, "정보센터");
        } catch (Exception e) {
            log.error("정보센터 메뉴 크롤링 실패", e);
        }
        try {
            //복지관
            Document welDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=36").get();
            List<ParsedMenu> welMenu = welfareParser.parse(welDoc);
            menuScheduleSaveService.saveMenus(welMenu, "복지관");
        } catch (Exception e) {
            log.error("복지관 메뉴 크롤링 실패", e);
        }
        try {
            //첨성
            Document cheomDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=37").get();
            List<ParsedMenu> cheomMenu = cheomseongParser.parse(cheomDoc);
            menuScheduleSaveService.saveMenus(cheomMenu, "첨성");
        } catch (Exception e) {
            log.error("첨성 메뉴 크롤링 실패", e);
        }
        try {
            //글플
            Document gpDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=46").get();
            List<ParsedMenu> gpMenu = gpParser.parse(gpDoc);
            menuScheduleSaveService.saveMenus(gpMenu, "글로벌플라자");
        } catch (Exception e) {
            log.error("글로벌플라자 메뉴 크롤링 실패", e);
        }
        try {
            //공식당 교직원
            Document gongStaffDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=85").get();
            List<ParsedMenu> gongStaffMenu = gongStaffParser.parse(gongStaffDoc);
            menuScheduleSaveService.saveMenus(gongStaffMenu, "공식당 교직원식당");
        } catch (Exception e) {
            log.error("공식당 교직원식당 메뉴 크롤링 실패", e);
        }
        try {
            //공식당 학생
            Document gongStudentDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=86").get();
            List<ParsedMenu> gongStudentMenu = gongStudentParser.parse(gongStudentDoc);
            menuScheduleSaveService.saveMenus(gongStudentMenu, "공식당 학생식당");
        } catch (Exception e) {
            log.error("공식당 학생식당 메뉴 크롤링 실패", e);
        }
    }
}
