package com.gdg.haksamo.domain.menu;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MenuCrawlerService {
    private final MenuRepository menuRepository;

    public MenuCrawlerService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @Scheduled(cron = "0 0 1 * * MON")
    void crawl() {
        String[][] targets = {
                {"https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=35", "정보센터"},
                {"https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=36", "복지관"},
                {"https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=37", "첨성"},
                {"https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=46", "글플"},
                {"https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=85", "공식당 교직원"},
                {"https://coop.knu.ac.kr/sub03/sub01_01.html?shop_sqno=86", "공식당 학생"}
        };
        for (String[] target : targets) {
            try {
                restaurant(target[0], target[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void restaurant(String url, String restaurantName) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Elements div = doc.select("div.week_table");

        for (Element di : div) {
            if (di.select("p.title").text().equals("조식")) {
                time(di.select("td"), restaurantName, MealTime.조식);
            }
            if (di.select("p.title").text().equals("중식")) {
                time(di.select("td"), restaurantName, MealTime.중식);
            }
            if (di.select("p.title").text().equals("석식")) {
                time(di.select("td"), restaurantName, MealTime.석식);
            }
        }
    }

    void time(Elements tdList, String restaurantName, MealTime mealTime) {
        Element[] week = new Element[5];
        String[] days = {"월요일", "화요일", "수요일", "목요일", "금요일"};

        for (int i = 0; i < 5; i++) {
            week[i] = tdList.get(i);

            boolean isFirst = true;
            boolean isSpecialRestaurant = restaurantName.equals("정보센터")
                    || restaurantName.equals("첨성")
                    || restaurantName.equals("공식당 학생");

            for (Element menu : week[i].select("li")) {
                Menu menuEntity = new Menu();
                if (menu.select("p").size() == 2) {
                    String name = (menu.ownText() + " " + menu.select("p").get(0).text()).trim();
                    name = name.replaceAll("\\s*★\\s*", "");
                    name = name.replaceAll("\\s*운영시간.*", "");
                    name = name.replaceAll("\\s*정식\\s*", "");
                    name = name.replaceAll("\\s*천원의 아침밥\\s*", "");
                    name = name.replaceAll("\\s*\\(2인\\)\\s*", "");
                    name = name.replaceAll("\\s*\\(\\d:\\d{2}~\\d{2}:\\d{2}, \\d{2}:\\d{2}~\\d{2}:\\d{2}\\)\\s*", "");
                    name = name.replaceAll("\\s*\\(\\d{2}:\\d{2}~\\d{2}:\\d{2}\\)\\s*", "");
                    name = name.trim();
                    menuEntity.setName(name);

                    menuEntity.setRestaurant(restaurantName);
                    menuEntity.setTime(mealTime);
                    menuEntity.setWeek(days[i]);

                    menuEntity.setSpecial(isFirst && isSpecialRestaurant);
                    isFirst = false;

                    String priceStr = menu.select("p").get(1).text().replaceAll("[^0-9]", "");
                    int price = Integer.parseInt(priceStr);
                    menuEntity.setPrice(price);
                    menuRepository.save(menuEntity);
                } else if (menu.text().contains("0원의 저녁밥")) {
                    String name = (menu.ownText() + " " + menu.select("p").get(0).text()).trim();
                    name = name.replaceAll("\\s*\\*0원의 저녁밥\\*\\s*", "");
                    menuEntity.setName(name);

                    menuEntity.setRestaurant(restaurantName);
                    menuEntity.setTime(mealTime);
                    menuEntity.setWeek(days[i]);
                    menuEntity.setSpecial(true);
                    menuEntity.setPrice(0);
                    menuRepository.save(menuEntity);
                }
            }
        }
    }
}
