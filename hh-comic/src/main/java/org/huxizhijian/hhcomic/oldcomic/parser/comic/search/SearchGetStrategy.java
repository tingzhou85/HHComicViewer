/*
 * Copyright 2016-2018 huxizhijian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.huxizhijian.hhcomic.oldcomic.parser.comic.search;

import org.huxizhijian.hhcomic.oldcomic.bean.Comic;
import org.huxizhijian.hhcomic.oldcomic.parser.comic.BaseComicParseStrategy;
import org.huxizhijian.hhcomic.oldcomic.type.RequestFieldType;
import org.huxizhijian.hhcomic.oldcomic.type.ResponseFieldType;
import org.huxizhijian.hhcomic.oldcomic.value.IComicRequest;
import org.huxizhijian.hhcomic.oldcomic.value.IComicResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import okhttp3.Request;

/**
 * Get形式的Comic搜索策略
 *
 * @Author huxizhijian on 2017/10/9.
 */

public abstract class SearchGetStrategy extends BaseComicParseStrategy {

    private String mKey;
    private int mPage;
    private int mSize;

    /**
     * 构建搜索网址(get形式)
     *
     * @param key  搜索关键词
     * @param page 当前请求的页面，默认从0开始请求，如果页码为1开始，请自行+1
     * @param size 每页展示的Comic数量，需要看网站支不支持这个，不一定有用
     * @return 构建出的搜索网址
     */
    protected abstract String getSearchUrl(String key, int page, int size) throws UnsupportedEncodingException;

    /**
     * 解析网络请求结果
     */
    protected abstract List<Comic> parseSearchResult(byte[] data) throws UnsupportedEncodingException;

    /**
     * 获取搜索结果页数
     *
     * @param data html
     * @return 页数，返回-1表示不清楚
     */
    protected abstract int getPageCount(byte[] data);

    @Override
    public Request buildRequest(IComicRequest comicRequest) throws UnsupportedEncodingException {
        //从comicRequest中获取到key
        mKey = comicRequest.getField(RequestFieldType.KEY_WORD);
        mPage = 0;
        mPage = comicRequest.getField(RequestFieldType.PAGE);
        return getRequestGetAndWithUrl(getSearchUrl(mKey, mPage, mSize));
    }

    @Override
    public IComicResponse parseData(IComicResponse comicResponse, byte[] data) throws IOException {
        List<Comic> comics = parseSearchResult(data);
        comicResponse.setComicResponse(comics);
        int pageCount = getPageCount(data);
        comicResponse.addField(ResponseFieldType.PAGE_COUNT, pageCount);
        return comicResponse;
    }

}
