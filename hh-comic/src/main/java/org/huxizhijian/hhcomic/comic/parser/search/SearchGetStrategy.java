package org.huxizhijian.hhcomic.comic.parser.search;

import org.huxizhijian.hhcomic.comic.bean.Comic;
import org.huxizhijian.hhcomic.comic.parser.BaseParseStrategy;
import org.huxizhijian.hhcomic.comic.type.RequestFieldType;
import org.huxizhijian.hhcomic.comic.type.ResponseFieldType;
import org.huxizhijian.hhcomic.comic.value.IHHComicRequest;
import org.huxizhijian.hhcomic.comic.value.IHHComicResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import okhttp3.Request;

/**
 * Get形式的Comic搜索策略
 *
 * @Author huxizhijian on 2017/10/9.
 */

public abstract class SearchGetStrategy extends BaseParseStrategy {

    private String mKey;
    private int mPage;
    private int mSize;

    /**
     * 构建搜索网址(get形式)
     *
     * @param key  搜索关键词
     * @param page 当前请求的页面
     * @param size 每页展示的Comic数量，需要看网站支不支持这个，不一定有用
     * @return 构建出的搜索网址
     */
    protected abstract String getSearchUrl(String key, int page, int size) throws UnsupportedEncodingException;

    /**
     * -
     * 解析网络请求结果
     */
    protected abstract List<Comic> parseSearchResult(byte[] data) throws UnsupportedEncodingException;

    /**
     * 获取搜索结果页数
     *
     * @return 页数
     */
    protected abstract int getPageCount(byte[] data);

    @Override
    public Request buildRequest(IHHComicRequest comicRequest) throws UnsupportedEncodingException {
        //从comicRequest中获取到key
        mKey = comicRequest.getField(RequestFieldType.KEY_WORD);
        mPage = 0;
        mPage = comicRequest.getField(RequestFieldType.PAGE);
        mSize = comicRequest.getField(RequestFieldType.SIZE);
        return getRequestGetAndWithUrl(getSearchUrl(mKey, mPage, mSize));
    }

    @Override
    public IHHComicResponse parseData(IHHComicResponse comicResponse, byte[] data) throws IOException {
        List<Comic> comics = parseSearchResult(data);
        comicResponse.setResponse(comics);
        int pageCount = getPageCount(data);
        comicResponse.addField(ResponseFieldType.PAGE_COUNT, pageCount);
        return comicResponse;
    }

}