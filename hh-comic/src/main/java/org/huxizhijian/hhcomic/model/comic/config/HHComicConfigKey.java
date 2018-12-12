/*
 * Copyright 2016-2018 huxizhijian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.huxizhijian.hhcomic.model.comic.config;

import androidx.annotation.StringDef;

/**
 * app设置的各项key值
 *
 * @author huxizhijian
 * @date 2018/11/2
 */
public class HHComicConfigKey {

    private HHComicConfigKey() {
    }

    /**
     * 夜间模式
     */
    public static final String NIGHT_MODE = "night_mode";

    /**
     * 在线阅读低清晰度模式，省流量
     */
    public static final String LOW_RESOLUTION_MODE = "low_resolution_mode";

    /**
     * 屏幕方向
     */
    public static final String SCREEN_ORIENTATION = "screen_orientation";

    /**
     * 代替枚举使用StringDef在编译期检查值
     */
    @StringDef({SCREEN_HORIZONTAL, SCREEN_VERTICAL, SCREEN_AUTO})
    public @interface ScreenOrientation {
    }

    /**
     * 横屏
     */
    public static final String SCREEN_HORIZONTAL = "landscape";

    /**
     * 竖屏
     */
    public static final String SCREEN_VERTICAL = "portrait";

    /**
     * 跟随系统
     */
    public static final String SCREEN_AUTO = "auto";

    /**
     * 翻页方向
     */
    public static final String PAGE_TURNING_DIRECTION = "page_turning_direction";

    /**
     * 代替枚举使用StringDef在编译期检查值
     */
    @StringDef({LEFT_TO_RIGHT, RIGHT_TO_LEFT, TOP_TO_BOTTOM, BOTTOM_TO_TOP})
    public @interface PageTurningDirection {
    }

    /**
     * 从左到右
     */
    public static final String LEFT_TO_RIGHT = "left_to_right";

    /**
     * 从右到左
     */
    public static final String RIGHT_TO_LEFT = "right_to_left";

    /**
     * 从上到下
     */
    public static final String TOP_TO_BOTTOM = "top_to_bottom";

    /**
     * 从下到上
     */
    public static final String BOTTOM_TO_TOP = "bottom_to_top";

    /**
     * 使用音量键翻页
     */
    public static final String PAGE_TURNING_USE_VOL_BUTTON = "page_turning_use_vol_button";

    /**
     * 阅读时保持屏幕常亮
     */
    public static final String READING_SCREEN_ALWAYS_ON = "reading_screen_always_on";

    /**
     * 下载位置
     */
    public static final String DOWNLOAD_PATH = "download_path";

    /**
     * 源排序/开启信息
     */
    public static final String SOURCE_CONFIGS = "source_configs";

    /**
     * 上次退出应用时选择的源Key
     */
    public static final String LAST_SOURCE_KEY = "last_source_key";
}
