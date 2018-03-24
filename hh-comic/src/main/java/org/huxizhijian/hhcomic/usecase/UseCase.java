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

package org.huxizhijian.hhcomic.usecase;

import org.huxizhijian.hhcomic.comic.value.IComicRequest;
import org.huxizhijian.hhcomic.comic.value.IComicResponse;

/**
 * 用例（UseCase）类的基础类，这是DomianLayer的基本
 *
 * @author huxizhijian
 * @date 2017/9/17
 */
public abstract class UseCase<Q extends UseCase.RequestValues, P extends UseCase.ResponseValue> {

    private Q mRequestValues;

    private UseCaseCallback<P> mUseCaseCallback;

    public void setRequestValues(Q requestValues) {
        mRequestValues = requestValues;
    }

    public Q getRequestValues() {
        return mRequestValues;
    }

    public UseCaseCallback<P> getUseCaseCallback() {
        return mUseCaseCallback;
    }

    public void setUseCaseCallback(UseCaseCallback<P> useCaseCallback) {
        mUseCaseCallback = useCaseCallback;
    }

    void run() {
        executeUseCase(mRequestValues);
    }

    /**
     * 执行use case的方法
     *
     * @param requestValues 请求参数类
     */
    protected abstract void executeUseCase(Q requestValues);

    /**
     * 传入参数，IComicRequest
     */
    public interface RequestValues {
        /**
         * 获取请求参数
         *
         * @return 请求参数的集合类
         */
        IComicRequest getValues();
    }

    /**
     * 返回参数，IComicResponse
     */
    public interface ResponseValue {
        /**
         * 获取结果
         *
         * @return 结果的集合类，可以取出结果
         */
        IComicResponse getValues();
    }

    public interface UseCaseCallback<R> {

        /**
         * use case调用成功并返回数据
         *
         * @param responseValue 结果类
         */
        void onSuccess(R responseValue);

        /**
         * 出现了异常
         */
        void onError();
    }

}
