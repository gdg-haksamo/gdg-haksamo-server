package com.gdg.haksamo.domain.menu.crawler.parser;

import com.gdg.haksamo.domain.menu.crawler.ParsedMenu;
import com.gdg.haksamo.domain.menu.entity.MealTime;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
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

                //휴일일때 p가 한 개
                if(pTags.size() < 2) continue;

                //br 나눠서 처리 했는데
                String name = menu.html();
                if (name.contains("<p")) name = name.substring(0, name.indexOf("<p"));
                name = name.replace("<br>", " ");
                name = name.replaceAll("<[^>]*>", "");
                name = Parser.unescapeEntities(name, false);
                name = name.replaceAll("\\s*★\\s*", "");
                name = name.replaceAll("\\s*\\*2000원의 저녁밥\\*\\s*", "");
                name = name.replaceAll("\\s*\\(.*?\\)", "");
                name = name.trim();

                String operatingTime = null;
                if(name.equals("라면") || name.equals("우동") || name.equals("우동밥")) {
                    operatingTime = "13:00~17:00";
                } else if(mealTime == MealTime.LUNCH) {
                    operatingTime = "11:00~13:30";
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
