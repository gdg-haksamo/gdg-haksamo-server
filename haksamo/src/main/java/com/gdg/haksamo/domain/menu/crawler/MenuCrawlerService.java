package com.gdg.haksamo.domain.menu.crawler;

import com.gdg.haksamo.domain.menu.crawler.parser.*;
import com.gdg.haksamo.domain.menu.repository.MenuRepository;
import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class MenuCrawlerService {
    private final MenuRepository menuRepository;
    private final InformationCenterParser informationCenterParser;
    private final WelfareParser welfareParser;
    private final CheomseongParser cheomseongParser;
    private final GpParser gpParser;
    private final GongStudentParser gongStudentParser;
    private final GongStaffParser gongStaffParser;


    public MenuCrawlerService(MenuRepository menuRepository, InformationCenterParser informationCenterParser, WelfareParser welfareParser, CheomseongParser cheomseongParser, GpParser gpParser, GongStudentParser gongStudentParser, GongStaffParser gongStaffParser) {
        this.menuRepository = menuRepository;
        this.informationCenterParser = informationCenterParser;
        this.welfareParser = welfareParser;
        this.cheomseongParser = cheomseongParser;
        this.gpParser = gpParser;
        this.gongStudentParser = gongStudentParser;
        this.gongStaffParser = gongStaffParser;
    }

    //&selDate=2026-06-15 : 주소 뒤에 붙이면 메뉴 다 있는 주(6/15) 편성표 가져옴
    //@PostConstruct      //실행하면 바로 크롤링 (테스트용)
    @Scheduled(cron = "0 0 1 * * MON")
    void crawl() {
        try {
            //정보센터
            Document infoDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=35").get();
            List<ParsedMenu> infoMenu = informationCenterParser.parse(infoDoc);
            infoMenu.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //복지관
            Document welDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=36").get();
            List<ParsedMenu> welMenu = welfareParser.parse(welDoc);
            welMenu.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //첨성
            Document cheomDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=37").get();
            List<ParsedMenu> cheomMenu = cheomseongParser.parse(cheomDoc);
            cheomMenu.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //글플
            Document gpDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=46").get();
            List<ParsedMenu> gpMenu = gpParser.parse(gpDoc);
            gpMenu.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //공식당 교직원
            Document gongStaffDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=85").get();
            List<ParsedMenu> gongStaffMenu = gongStaffParser.parse(gongStaffDoc);
            gongStaffMenu.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //공식당 학생
            Document gongStudentDoc = Jsoup.connect("https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=86").get();
            List<ParsedMenu> gongStudentMenu = gongStudentParser.parse(gongStudentDoc);
            gongStudentMenu.forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
