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

public class GongStaffParser implements MenuParser{

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

                //휴일일때 p가 한 개
                if(pTags.size() < 2) continue;

                String name = pTags.get(0).text().trim();
                name = name.replaceAll("\\s*★\\s*", "");
                name = name.replaceAll("\\s*운영시간\\s*", "");
                name = name.replaceAll("\\s*\\d{2}:\\d{2}~\\d{2}:\\d{2}\\s*", "");

                String operatingTime = null;
                if(mealTime == MealTime.LUNCH) {
                    operatingTime = "11:30~13:30";
                } else if(mealTime == MealTime.DINNER) {
                    operatingTime = "17:00~19:00";
                }

                String priceStr = pTags.get(1).text();
                try{
                    price = Integer.parseInt(priceStr.replace("￦ ", "").replace(",", ""));
                } catch (NumberFormatException e){
                    continue;
                }

                result.add(
                        new ParsedMenu(
                                name,
                                price,
                                operatingTime,
                                mealTime,
                                dayIndex
                        )
                );
            }
        }
    }
}
