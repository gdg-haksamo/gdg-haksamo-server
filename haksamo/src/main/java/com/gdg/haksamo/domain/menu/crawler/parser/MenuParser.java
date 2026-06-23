package com.gdg.haksamo.domain.menu.crawler.parser;

import com.gdg.haksamo.domain.menu.crawler.ParsedMenu;
import org.jsoup.nodes.Document;
import java.util.List;

//파서 공통 규칙
public interface MenuParser {
    List<ParsedMenu> parse(Document doc);
}
