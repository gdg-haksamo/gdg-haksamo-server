package com.gdg.haksamo.domain.menu.crawler;

import com.gdg.haksamo.domain.menu.crawler.parser.*;
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
    private final MenuInfoEnrichmentService menuInfoEnrichmentService;

    public MenuCrawlerService(InformationCenterParser informationCenterParser, WelfareParser welfareParser, CheomseongParser cheomseongParser, GpParser gpParser, GongStudentParser gongStudentParser, GongStaffParser gongStaffParser, MenuScheduleSaveService menuScheduleSaveService, MenuInfoEnrichmentService menuInfoEnrichmentService) {
        this.informationCenterParser = informationCenterParser;
        this.welfareParser = welfareParser;
        this.cheomseongParser = cheomseongParser;
        this.gpParser = gpParser;
        this.gongStudentParser = gongStudentParser;
        this.gongStaffParser = gongStaffParser;

        this.menuScheduleSaveService = menuScheduleSaveService;
        this.menuInfoEnrichmentService = menuInfoEnrichmentService;
    }

    //&selDate=2026-06-15 : 주소 뒤에 붙이면 메뉴 다 있는 주(6/15) 편성표 가져옴
    @Scheduled(cron = "0 0 1 * * MON")
    public void crawl() {
        crawl(null);
    }

    /**
     * 메뉴 크롤 실행. selDate(yyyy-MM-dd)를 주면 학교 사이트의 해당 주 편성표를 가져온다(생략 시 현재 주).
     * 저장 날짜는 항상 이번 주 월요일+dayIndex로 매겨지고, 식당별 wipe &amp; re-insert라 멱등(중복 없음).
     * 운영팀(SUPER_ADMIN)이 수동 트리거로도 호출한다.
     */
    public void crawl(String selDate) {
        String dateParam = (selDate != null && !selDate.isBlank()) ? "&selDate=" + selDate : "";
        try {
            //정보센터
            Document infoDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=35" + dateParam).get();
            List<ParsedMenu> infoMenu = informationCenterParser.parse(infoDoc);
            menuScheduleSaveService.saveMenus(infoMenu, "정보센터");
        } catch (Exception e) {
            log.error("정보센터 메뉴 크롤링 실패", e);
        }
        try {
            //복지관
            Document welDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=36" + dateParam).get();
            List<ParsedMenu> welMenu = welfareParser.parse(welDoc);
            menuScheduleSaveService.saveMenus(welMenu, "복지관");
        } catch (Exception e) {
            log.error("복지관 메뉴 크롤링 실패", e);
        }
        try {
            //첨성
            Document cheomDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=37" + dateParam).get();
            List<ParsedMenu> cheomMenu = cheomseongParser.parse(cheomDoc);
            menuScheduleSaveService.saveMenus(cheomMenu, "첨성");
        } catch (Exception e) {
            log.error("첨성 메뉴 크롤링 실패", e);
        }
        try {
            //글플
            Document gpDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=46" + dateParam).get();
            List<ParsedMenu> gpMenu = gpParser.parse(gpDoc);
            menuScheduleSaveService.saveMenus(gpMenu, "글로벌플라자");
        } catch (Exception e) {
            log.error("글로벌플라자 메뉴 크롤링 실패", e);
        }
        try {
            //공식당 교직원
            Document gongStaffDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=85" + dateParam).get();
            List<ParsedMenu> gongStaffMenu = gongStaffParser.parse(gongStaffDoc);
            menuScheduleSaveService.saveMenus(gongStaffMenu, "공식당 교직원식당");
        } catch (Exception e) {
            log.error("공식당 교직원식당 메뉴 크롤링 실패", e);
        }
        try {
            //공식당 학생
            Document gongStudentDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=86" + dateParam).get();
            List<ParsedMenu> gongStudentMenu = gongStudentParser.parse(gongStudentDoc);
            menuScheduleSaveService.saveMenus(gongStudentMenu, "공식당 학생식당");
        } catch (Exception e) {
            log.error("공식당 학생식당 메뉴 크롤링 실패", e);
        }

        // 편성 저장 후, 신규 메뉴(설명·탄단지 비어있음)만 골라 AI로 정보 생성·저장. 크롤 본체와 분리.
        try {
            menuInfoEnrichmentService.enrichMissing();
        } catch (Exception e) {
            log.error("신규 메뉴 정보 생성 실패", e);
        }
    }
}
