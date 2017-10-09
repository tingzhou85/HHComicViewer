package org.huxizhijian.hhcomic.comic.parser;

import okhttp3.Request;

/**
 * @Author huxizhijian on 2017/10/9.
 */

public abstract class BaseParseStrategy implements ParseStrategy {

    protected Request getRequestGetAndWithUrl(String url) {
        return new Request.Builder().url(url).get().build();
    }

}
