/*
 * Copyright 2016-2017 huxizhijian
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
package org.huxizhijian.hhcomicviewer2.persenter;

import org.huxizhijian.hhcomicviewer2.model.Comic;

/**
 * Created by wei on 2017/1/3.
 */

public interface IComicDetailsPresenter {

    void getComic(int cid, Comic oldComic);

}