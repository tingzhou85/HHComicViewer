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

package org.huxizhijian.sdk.imagedownload.listener;

/**
 * @author huxizhijian 2017/3/15
 */
public interface RequestProgressListener {

    void onProgress(long chid, int progress, int size);

    void onFailure(long chid, Throwable throwable, int progress, int size);

    void onCompleted(long chid, int progress, int size);

    void onPaused(long chid, int progress, int size);

}
