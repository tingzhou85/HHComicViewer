package org.huxizhijian.hhcomic.comic.source;

import org.huxizhijian.hhcomic.comic.bean.Chapter;
import org.huxizhijian.hhcomic.comic.bean.Comic;
import org.huxizhijian.hhcomic.comic.parser.detail.DetailStrategy;
import org.huxizhijian.hhcomic.comic.parser.rank.RankStrategy;
import org.huxizhijian.hhcomic.comic.parser.search.SearchGetStrategy;
import org.huxizhijian.hhcomic.comic.type.ComicDataSourceType;
import org.huxizhijian.hhcomic.comic.type.RankType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;

/**
 * 汗汗漫画网站解析类
 *
 * @Author huxizhijian on 2017/10/9.
 */
public class HHManHua extends ComicSource {

    private static final String HH_NAME = "汗汗漫画";
    private static final String HH_BASE_URL = "http://www.hhmmoo.com";
    private static final String HH_COMIC_PRE = "/manhua";
    private static final String HH_SEARCH_URL = "http://ssooff.com/"; //搜索网站

    private static final int SOURCE_TYPE = Source.HHManHua;

    @Override
    public String setSourceName() {
        return HH_NAME;
    }

    @Override
    public String setBaseUrl() {
        return HH_BASE_URL;
    }

    @Override
    public int getSourceType() {
        return SOURCE_TYPE;
    }

    public HHManHua() {
        // 支持的排行类别
        RANK_TYPE_MAP.put(RankType.HH_NEW_RATING, new TypeContent("最近更新", "/top/newrating.aspx"));
        RANK_TYPE_MAP.put(RankType.HH_TOP_READER, new TypeContent("最多人看", "/top/hotrating.aspx"));
        RANK_TYPE_MAP.put(RankType.HH_TOP_RATING, new TypeContent("评分最高", "/top/toprating.aspx"));
        RANK_TYPE_MAP.put(RankType.HH_HOT_COMMIT, new TypeContent("最多人评论", "/top/hoorating.aspx"));
    }

    /**
     * 默认添加所有策略，也可以自行添加
     */
    public ComicSource newDefaltConfig() {
        return new HHManHua()
                .addStragegy(ComicDataSourceType.WEB_DETAIL, new HHDetailStrategy())
                .addStragegy(ComicDataSourceType.WEB_SEARCH, new HHSearchStrategy())
                .addStragegy(ComicDataSourceType.WEB_RANK, new HHRankStrategy());
    }

    /**
     * 获取Comic详情策略
     */
    public class HHDetailStrategy extends DetailStrategy {

        @Override
        protected String getDetailUrl(String comicId) {
            return HH_BASE_URL + HH_COMIC_PRE + comicId + ".html";
        }

        @Override
        protected Comic parseComic(byte[] data, String comicId) throws UnsupportedEncodingException {
            //转换成UTF-8
            String content = new String(data, "utf-8");
            //初始化comic
            Comic comic = new Comic();
            comic.setSource(SOURCE_TYPE);
            comic.setCid(comicId);
            //获取到网页内容时自动完善内容
            Document doc = Jsoup.parse(content);
            Element comicInfoDiv = doc.select("div[class=product]").first();

            comic.setTitle(comicInfoDiv.getElementsByTag("h1").first().text());
            comic.setCover(comicInfoDiv.select("div[id=about_style]").first()
                    .getElementsByTag("img").first().attr("src"));

            Element about_kit = comicInfoDiv.select("div[id=about_kit]").first();
            Elements comicInfoList = about_kit.select("li");
            comicInfoList.remove(0);
            for (Element comicInfo : comicInfoList) {
                switch (comicInfo.getElementsByTag("b").first().text()) {
                    case "作者:":
                        comic.setAuthor(comicInfo.text().split(":")[1]);
                        break;
                    case "状态:":
                        String state = comicInfo.text();
                        if (state.contains("完结")) {
                            comic.setFinish(true);
                        } else if (state.contains("连载")) {
                            comic.setFinish(false);
                        }
                        break;
                    case "更新:":
                        comic.setUpdate(comicInfo.text());
                        break;
                    case "收藏:":
                        comic.setFavoriteCount(Float.valueOf(comicInfo.text()));
                        break;
                    case "评价:":
                        comic.setRate(Float.valueOf(comicInfo.getElementsByTag("span").first().text()));
                        comic.setRatePeopleCount(Integer.valueOf(comicInfo.text().split("\\(")[1].split("人")[0]));
                        break;
                    case "简介":
                        comic.setIntro(comicInfo.text());
                        break;
                }
            }
            return comic;
        }

        @Override
        protected boolean shouldNotRequestToParseChapter() {
            return false;
        }

        @Override
        protected Request buildChapterRequest(String comicId) {
            return null;
        }

        @Override
        protected List<Chapter> parseChapter(byte[] data) throws UnsupportedEncodingException {
            // 转换成UTF-8
            String content = new String(data, "utf-8");
            // 初始化Chapter
            List<Chapter> chapters = new ArrayList<>();
            Chapter chapter = null;
            // 获取到网页内容时自动完善内容
            Document doc = Jsoup.parse(content);
            // 章节目录解析
            Element volListSrc = doc.select("div[class=cVolList]").first();
            Elements tagsSrc = volListSrc.select("div[class=cVolTag]");
            Elements tagChapterSrc = volListSrc.select("ul[class=cVolUl]");

            for (int i = 0; i < tagsSrc.size(); i++) {
                Elements chaptersSrc = tagChapterSrc.get(i).select("a[class=l_s]");
                for (int j = chaptersSrc.size() - 1; j > -1; j--) {
                    //这个倒数循环把原本的倒序的章节顺序变为正序
                    String title = chaptersSrc.get(j).attr("title");
                    //地址
                    String url = chaptersSrc.get(j).attr("href");
                    chapter = new Chapter(title, url);
                    chapters.add(chapter);
                }
            }
            return chapters;
        }
    }

    /**
     * 搜索策略
     */
    public class HHSearchStrategy extends SearchGetStrategy {

        @Override
        protected String getSearchUrl(String key, int page, int size) throws UnsupportedEncodingException {
            String getKey = null;
            getKey = "?key=" + URLEncoder.encode(key, "GB2312");
            getKey += "&button=%CB%D1%CB%F7%C2%FE%BB%AD";
            return HH_SEARCH_URL + getKey;
        }

        @Override
        protected List<Comic> parseSearchResult(byte[] data) throws UnsupportedEncodingException {
            String content = null;
            content = new String(data, "gb2312");
            Document doc = Jsoup.parse(content);
            Element comicSrcs = doc.select("div[class=dSHtm]").first();
            Elements comicUrls = comicSrcs.select("div");
            comicUrls.remove(0);

            List<Comic> comics = new ArrayList<>();
            Comic comic = null;

            for (int i = 0; i < comicUrls.size(); i++) {
                comic = new Comic();
                comic.setSource(SOURCE_TYPE);
                Element comicSrc = comicUrls.get(i).select("a").first();
                String url = comicSrc.attr("href");
                String[] urlSplit = url.split("/");
                String end = urlSplit[urlSplit.length - 1];
                comic.setCid(end.split("\\.")[0]);
                comic.setTitle(comicSrc.text());
                Element imgUrl = comicUrls.get(i).select("img").first();
                comic.setCover(imgUrl.attr("src"));
                Elements desc = comicUrls.get(i).getElementsByTag("br");
                comic.setIntro(desc.get(2).text());
                comics.add(comic);
            }
            return comics;
        }

        @Override
        protected int getPageCount(byte[] data) {
            // 由于网站限制，永远只有一页
            return 1;
        }
    }

    /**
     * 排行策略
     */
    public class HHRankStrategy extends RankStrategy {

        @Override
        protected String getRankUrl(Enum<RankType> rankType, int page, int size) {
            TypeContent content = RANK_TYPE_MAP.get(rankType);
            return HH_BASE_URL + content.urlKey;
        }

        @Override
        protected int getPageCount(byte[] data) {
            // 排行会在一页中展示
            return 1;
        }

        @Override
        protected List<Comic> parseRankComics(byte[] data) throws UnsupportedEncodingException {
            String content = new String(data, "utf-8");
            Document doc = Jsoup.parse(content);
            Elements comicSrcs = doc.select("div[class=cComicItem]");
            List<Comic> comics = new ArrayList<>();
            for (Element comicSrc : comicSrcs) {
                Comic comic = new Comic();
                String comicUrl = comicSrc.select("a").first().attr("href");
                String end = comicUrl.substring(HH_COMIC_PRE.length());
                comic.setCid(end.split("\\.")[0]);
                comic.setCover(comicSrc.select("img").first().attr("src"));
                comic.setTitle(comicSrc.select("span[class=cComicTitle]").first().text());
                comic.setAuthor(comicSrc.select("span[class=cComicAuthor").first().text());
                comic.setIntro(comicSrc.select("span[class=cComicRating").first().text());
                comics.add(comic);
            }
            return comics;
        }
    }

}
