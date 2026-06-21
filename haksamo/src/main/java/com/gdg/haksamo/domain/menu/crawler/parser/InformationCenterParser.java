package com.gdg.haksamo.domain.menu.crawler.parser;

import com.gdg.haksamo.domain.menu.crawler.ParsedMenu;
import com.gdg.haksamo.domain.menu.entity.MealTime;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.jsoup.nodes.Document;
import java.util.ArrayList;
import java.util.List;

@Component

public class InformationCenterParser implements MenuParser{

    @Override
    public List<ParsedMenu> parse(Document doc) {
        List<ParsedMenu> result = new ArrayList<>();

        Elements div = doc.select("div.week_table");
        for (Element di : div) {
            if (di.select("p.title").text().equals("조식")) {
                parseMenu(di.select("td"), MealTime.BREAKFAST, result);
            }
            if (di.select("p.title").text().equals("중식")) {
                parseMenu(di.select("td"), MealTime.LUNCH, result);
            }
            if (di.select("p.title").text().equals("석식")) {
                parseMenu(di.select("td"), MealTime.DINNER, result);
            }
        }
        return result;
    }
    private void parseMenu(Elements tdList, MealTime mealTime, List<ParsedMenu> result) {
        //월~금 고정
        for (int i = 0; i < 5; i++) {
            Element dayMenu = tdList.get(i);

            for (Element menu : dayMenu.select("li")) {
                Integer price = null;
                int dayIndex = i;       //dayIndex 명시하기 위해 바로 i를 쓰지 않음

                Elements pTags = menu.select("p");
                if(pTags.size() < 2){
                    continue;
                }

                String name = (menu.ownText() + " " + pTags.get(0).text()).trim();
                name = name.replaceAll("\\s*★\\s*", "");
                //예외 메뉴(라면) : 중식 메뉴 리스트에 등장하지만 시간대가 달라 이름과 함께 운영시간 제공
                if (!menu.text().contains("(13:00~17:00)")) {
                    name = name.replaceAll("\\s*\\(\\d{2}:\\d{2}~\\d{2}:\\d{2}\\)\\s*", "");
                }
                name = name.replaceAll("\\s*\\*2000원의 저녁밥\\*\\s*", "");
                name = name.trim();

                String priceStr = pTags.get(1).text();
                try{
                    price = Integer.parseInt(priceStr.replace("￦ ", "").replace(",", ""));
                } catch (NumberFormatException e){
                }

                result.add(
                        new ParsedMenu(
                                name,
                                price,
                                mealTime,
                                dayIndex
                        )
                );
            }
        }
    }
}
