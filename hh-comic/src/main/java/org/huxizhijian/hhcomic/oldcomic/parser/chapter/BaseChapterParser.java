/*
 * Copyright 2018 huxizhijian
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

package org.huxizhijian.hhcomic.oldcomic.parser.chapter;

import android.util.SparseArray;

import org.huxizhijian.core.app.ConfigKeys;
import org.huxizhijian.core.app.HHEngine;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author huxizhijian
 * @date 2017/10/12
 */

public abstract class BaseChapterParser implements IChapterParser {

    private final SparseArray<String> mImageUrlArray = new SparseArray<>();

    protected abstract String getChapterUrl(String urlKey, int page);

    private BaseChapterParser() {

    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public String moveToNext() {
        return null;
    }

    private byte[] getWebContent(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        OkHttpClient client = HHEngine.getConfiguration(ConfigKeys.OKHTTP_CLIENT);
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException();
        }
        return response.body().bytes();
    }

}