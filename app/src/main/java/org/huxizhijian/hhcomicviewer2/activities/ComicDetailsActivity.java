/*
 * Copyright 2016 huxizhijian
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

package org.huxizhijian.hhcomicviewer2.activities;

import android.Manifest;
import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Fade;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.rey.material.app.BottomSheetDialog;

import org.huxizhijian.hhcomicviewer2.R;
import org.huxizhijian.hhcomicviewer2.adapter.VolDownloadSelectorAdapter;
import org.huxizhijian.hhcomicviewer2.adapter.VolRecyclerViewAdapter;
import org.huxizhijian.hhcomicviewer2.app.HHApplication;
import org.huxizhijian.hhcomicviewer2.databinding.ActivityComicDetailsBinding;
import org.huxizhijian.hhcomicviewer2.db.ComicChapterDBHelper;
import org.huxizhijian.hhcomicviewer2.db.ComicDBHelper;
import org.huxizhijian.hhcomicviewer2.enities.Comic;
import org.huxizhijian.hhcomicviewer2.enities.ComicChapter;
import org.huxizhijian.hhcomicviewer2.service.DownloadManagerService;
import org.huxizhijian.hhcomicviewer2.utils.BaseUtils;
import org.huxizhijian.hhcomicviewer2.utils.Constants;
import org.huxizhijian.hhcomicviewer2.view.FullyGridLayoutManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.sharesdk.onekeyshare.OnekeyShare;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class ComicDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityComicDetailsBinding mBinding = null;

    //数据操作
    private ComicDBHelper mComicDBHelper;
    private ComicChapterDBHelper mComicChapterDBHelper;

    //数据
    private List<ComicChapter> mDownloadedComicChapters; //开始下载的章节列表
    private List<String> mFinishedComicChapters; //下载完成的章节名列表
    private Comic mComic;
    private List<String> mSelectedChapters;
    private String mUrl;
    private boolean isDescriptionOpen = false;
    private boolean isDescriptionCanOpen = false;
    private int mDetailsHeigth;
    private int mBackHeigth;

    //章节列表adapter
    private VolRecyclerViewAdapter mVolAdapter;

    //Handler，用于开启下载任务
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == Constants.MSG_DOWNLOAD) {
                ComicChapter Chapter = (ComicChapter) msg.obj;
                Intent intent = new Intent(ComicDetailsActivity.this, DownloadManagerService.class);
                intent.setAction(DownloadManagerService.ACTION_START);
                intent.putExtra("comicChapter", Chapter);
                startService(intent);
            }
        }
    };

    //广播接收器
    ComicChapterDownloadUpdateReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_comic_details);
        initView();
        preLoadingImageAndTitle();
        initDBValues();
        initData();
        setupAppBarListener();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            setupEnterAnimations();
            setupExitAnimations();
        }
    }

    private CollapsingToolbarLayoutState state;

    private enum CollapsingToolbarLayoutState {
        EXPANDED, //展开
        COLLAPSED, //收起
        INTERNEDIATE //中间
    }

    private void setupAppBarListener() {
        //设置监听事件，控制readButton的显隐
        mBinding.appBarComicDetails.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset == 0) {
                    if (state != CollapsingToolbarLayoutState.EXPANDED) {
                        state = CollapsingToolbarLayoutState.EXPANDED;//修改状态标记为展开
                    }
                } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
                    if (state != CollapsingToolbarLayoutState.COLLAPSED) {
                        mBinding.readButton.setVisibility(View.VISIBLE);//隐藏播放按钮
                        state = CollapsingToolbarLayoutState.COLLAPSED;//修改状态标记为折叠
                    }
                } else {
                    if (state != CollapsingToolbarLayoutState.INTERNEDIATE) {
                        if (state == CollapsingToolbarLayoutState.COLLAPSED) {
                            mBinding.readButton.setVisibility(View.GONE);//由折叠变为中间状态时隐藏播放按钮
                        }
                        state = CollapsingToolbarLayoutState.INTERNEDIATE;//修改状态标记为中间
                    }
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animateRevealShow(View viewRoot) {
        //获取中心点坐标
        int cx = (viewRoot.getLeft() + viewRoot.getRight()) / 2;
        int cy = (viewRoot.getTop() + viewRoot.getBottom()) / 2;
        //获取宽高的中的最大值
        int finalRadius = Math.max(viewRoot.getWidth(), viewRoot.getHeight());

        Animator anim = ViewAnimationUtils.createCircularReveal(viewRoot, cx, cy, 0, finalRadius);
        viewRoot.setVisibility(View.VISIBLE); //设为可见
        anim.setDuration(getResources().getInteger(R.integer.anim_duration_medium));//设置时间
        anim.setInterpolator(new AccelerateInterpolator());//设置插补器 一开始慢后来快
        anim.start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupEnterAnimations() {
        Fade enterTransition = new Fade();  //淡入淡出
        getWindow().setEnterTransition(enterTransition);
        enterTransition.setDuration(getResources().getInteger(R.integer.anim_duration_medium));//时间
        enterTransition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                transition.removeListener(this);
                animateRevealShow(mBinding.appBarComicDetails);//toolbar的缩放动画
            }

            @Override
            public void onTransitionEnd(Transition transition) {

            }

            @Override
            public void onTransitionCancel(Transition transition) {

            }

            @Override
            public void onTransitionPause(Transition transition) {

            }

            @Override
            public void onTransitionResume(Transition transition) {

            }
        });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupExitAnimations() {
        Fade returnTransition = new Fade();  //淡入淡出
        getWindow().setReturnTransition(returnTransition);
        returnTransition.setDuration(getResources().getInteger(R.integer.anim_duration_medium));//时间
        returnTransition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                //移除监听
                transition.removeListener(this);
            }

            @Override
            public void onTransitionEnd(Transition transition) {
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        });
    }

    private void preLoadingImageAndTitle() {
        Intent intent = getIntent();
        mUrl = intent.getStringExtra("url");
        String thumbnailUrl = intent.getStringExtra("thumbnailUrl");
        String title = intent.getStringExtra("title");

        //加载缩略图
        Glide.with(this)
                .load(thumbnailUrl)
                .placeholder(R.mipmap.blank)
                .error(R.mipmap.blank)
                .fitCenter()
                .into(mBinding.comicThumbnailComicDetails);

        mBinding.comicTitleComicDetails.setText(title);
    }

    private void initData() {
        mComic = mComicDBHelper.findByUrl(mUrl);
        Request request = new Request.Builder().url(mUrl).build();
        OkHttpClient client = ((HHApplication) getApplication()).getClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                if (BaseUtils.getAPNType(ComicDetailsActivity.this) == BaseUtils.NONEWTWORK) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), Constants.NO_NETWORK, Toast.LENGTH_SHORT).show();
                            //没有网络，读取数据库中的信息
                            if (mComic != null) {
                                if (mComic.isMark() || mComic.isDownload()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateViews();
                                        }
                                    });
                                }
                            }
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "出错！", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    byte[] result = response.body().bytes();
                    final String content = new String(result, "utf-8");
                    //初始化
                    if (mComic == null) {
                        mComic = new Comic(mUrl, content);
                    } else {
                        mComic.checkUpdate(content);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateViews();
                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        mReceiver = new ComicChapterDownloadUpdateReceiver();
    }

    private void updateViews() {
        //加载其他信息
        mBinding.comicAuthorComicDetails.setText(mComic.getAuthor());
        mBinding.comicVolStatusComicDetails.setText(mComic.getComicStatus());
        mBinding.comicFavoriteNumberComicDetails.setText(mComic.getComicFavorite());
        mBinding.comicUpdateTimeComicDetails.setText(mComic.getComicUpdateTime());

        //加载简介
        mBinding.comicDescriptionComicDetails.setText(mComic.getDescription());
        //测量高度
        mBinding.comicDescriptionComicDetailsBack.setVisibility(View.VISIBLE);
        mBinding.comicDescriptionComicDetailsBack.setText(mComic.getDescription());
        final ViewTreeObserver vto1 = mBinding.comicDescriptionComicDetails.getViewTreeObserver();
        vto1.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mDetailsHeigth = mBinding.comicDescriptionComicDetails.getHeight();
                //获得高度之后，移除监听
                vto1.removeGlobalOnLayoutListener(this);
            }
        });
        //测量tv_back 的高度
        ViewTreeObserver vto = mBinding.comicDescriptionComicDetailsBack.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                mBackHeigth = mBinding.comicDescriptionComicDetailsBack.getHeight();
                mBinding.comicDescriptionComicDetailsBack.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                //比较高度：
                if (mBackHeigth > mDetailsHeigth) {
                    //说明有展开的内容：
                    mBinding.ivArrow.setVisibility(View.VISIBLE);
                    //默认是关闭状态：
                    mBinding.comicDescriptionComicDetails.setTag(true);
                    isDescriptionCanOpen = true;
                } else {
                    mBinding.ivArrow.setVisibility(View.GONE);
                    isDescriptionCanOpen = false;
                }

                mBinding.comicDescriptionComicDetailsBack.setVisibility(View.GONE);
            }
        });

        //加载评分信息
        mBinding.ratingBarComicDetails.setRating((mComic.getRatingNumber() / 10.0f) * 5.0f);
        mBinding.ratingBarDescriptionComicDetails.setText(mComic.getRatingNumber() + "分(10分制), " +
                "共计" + mComic.getRatingPeopleNum() + "人评分");

        //章节列表加载
        if (mComic.isMark() || mComic.isDownload()) {
            mVolAdapter = new VolRecyclerViewAdapter(ComicDetailsActivity.this,
                    mComic.getChapterName(), mComic.getReadChapter(), mFinishedComicChapters);
        } else {
            mVolAdapter = new VolRecyclerViewAdapter(ComicDetailsActivity.this,
                    mComic.getChapterName(), mFinishedComicChapters);
        }

        //初始化RecyclerView
        mBinding.recyclerViewComicDetails.setLayoutManager(new FullyGridLayoutManager(ComicDetailsActivity.this, 4));
        mBinding.recyclerViewComicDetails.setItemAnimator(new DefaultItemAnimator());
        mBinding.recyclerViewComicDetails.setHasFixedSize(true);
        mBinding.recyclerViewComicDetails.setNestedScrollingEnabled(false);
        mVolAdapter.setOnItemClickListener(new VolRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(ComicDetailsActivity.this, GalleryActivity.class);
                intent.putExtra("comic", mComic);
                intent.putExtra("position", position);
                startActivityForResult(intent, 0);
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        mBinding.recyclerViewComicDetails.setAdapter(mVolAdapter);

        //设定收藏状态
        if (mComic.isMark()) {
            mBinding.btnFavoriteComicDetails.setImageResource(R.mipmap.my_favorite);
            mBinding.buttonTextFavoriteComicDetails.setText("已收藏");
        }

        //单击事件注册
        mBinding.FABComicDetails.setOnClickListener(this);
        mBinding.readButton.setOnClickListener(this);
        //四大按钮
        mBinding.btnFavorite.setOnClickListener(this);
        mBinding.btnShare.setOnClickListener(this);
        mBinding.btnFind.setOnClickListener(this);
        mBinding.btnDownload.setOnClickListener(this);
        //其他控件
        mBinding.comicAuthorComicDetails.setOnClickListener(this);
        mBinding.comicDescriptionComicDetailsLl.setOnClickListener(this);

        //读取完毕显示数据
        mBinding.loadingComicInfo.setVisibility(View.GONE);
        mBinding.linearLayoutComicDetails.setVisibility(View.VISIBLE);
        Animation alpha = AnimationUtils.loadAnimation(this, R.anim.alpha_in);
        //加速度插值器
        alpha.setInterpolator(new AccelerateInterpolator());
        mBinding.linearLayoutComicDetails.startAnimation(alpha);
    }

    private void initDBValues() {
        mComicDBHelper = ComicDBHelper.getInstance(this);
        mComicChapterDBHelper = ComicChapterDBHelper.getInstance(this);
        //查找已经开始下载的章节
        mDownloadedComicChapters = mComicChapterDBHelper.findByComicUrl(mUrl);
        //遍历
        if (mDownloadedComicChapters != null) {
            for (int i = 0; i < mDownloadedComicChapters.size(); i++) {
                if (mDownloadedComicChapters.get(i).getDownloadStatus() == Constants.DOWNLOAD_FINISHED) {
                    if (mFinishedComicChapters == null) {
                        mFinishedComicChapters = new ArrayList<>();
                    }
                    mFinishedComicChapters.add(mDownloadedComicChapters.get(i).getChapterName());
                }
            }
        }
    }

    private void initView() {
        //actionBar设定
        setSupportActionBar(mBinding.toolbarComicDetails);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("");
        }

        BaseUtils.setStatusBarTint(this, getResources().getColor(R.color.colorPrimaryDark));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.comic_info, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    this.finishAfterTransition();
                } else {
                    this.finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //注册receiver
        IntentFilter filter = new IntentFilter(DownloadManagerService.ACTION_RECEIVER);
        registerReceiver(mReceiver, filter);

        //如果有下载内容更新界面
        if (mVolAdapter != null && mComicChapterDBHelper != null) {
            mDownloadedComicChapters = mComicChapterDBHelper.findByComicUrl(mComic.getComicUrl());
            //遍历
            if (mDownloadedComicChapters != null && mDownloadedComicChapters.size() != 0) {
                for (int i = 0; i < mDownloadedComicChapters.size(); i++) {
                    if (mDownloadedComicChapters.get(i).getDownloadStatus()
                            == Constants.DOWNLOAD_FINISHED) {
                        if (mFinishedComicChapters == null) {
                            mFinishedComicChapters = new ArrayList<>();
                        }
                        mFinishedComicChapters.add(mDownloadedComicChapters.get(i).getChapterName());
                    }
                }
                mVolAdapter.setFinishedComicChapterList(mFinishedComicChapters);
                //刷新界面
                mVolAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mComic != null && mComic.getLastReadTime() != 0) {
            mComic.setLastReadTime(System.currentTimeMillis());
            if (mComicDBHelper.findByUrl(mComic.getComicUrl()) != null) {
                mComicDBHelper.update(mComic);
                System.out.println("comic update");
            } else {
                mComicDBHelper.add(mComic);
            }
        }

        //注销receiver
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fixInputMethodManagerLeak(this);
    }

    public static void fixInputMethodManagerLeak(Context destContext) {
        if (destContext == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) destContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }

        String[] arr = new String[]{"mCurRootView", "mServedView", "mNextServedView"};
        Field f = null;
        Object obj_get = null;
        for (int i = 0; i < arr.length; i++) {
            String param = arr[i];
            try {
                f = imm.getClass().getDeclaredField(param);
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                } // author: sodino mail:sodino@qq.com
                obj_get = f.get(imm);
                if (obj_get != null && obj_get instanceof View) {
                    View v_get = (View) obj_get;
                    if (v_get.getContext() == destContext) { // 被InputMethodManager持有引用的context是想要目标销毁的
                        f.set(imm, null); // 置空，破坏掉path to gc节点
                    } else {
                        // 不是想要目标销毁的，即为又进了另一层界面了，不要处理，避免影响原逻辑,也就不用继续for循环了
                        /*if (QLog.isColorLevel()) {
                            QLog.d(ReflecterHelper.class.getSimpleName(), QLog.CLR, "fixInputMethodManagerLeak break, context is not suitable, get_context=" + v_get.getContext()+" dest_context=" + destContext);
                        }*/
                        break;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mComic = (Comic) data.getSerializableExtra("comic");
        mComic.setLastReadTime(System.currentTimeMillis());
        if (mVolAdapter != null) {
            mVolAdapter.setReadChapter(mComic.getReadChapter());
        }
    }

    private void read() {
        //单击toolbar的按钮
        if (mComic == null || mComic.getChapterUrl() == null || mComic.getChapterUrl().size() == 0)
            return;
        Intent intent = new Intent(ComicDetailsActivity.this, GalleryActivity.class);
        intent.putExtra("comic", mComic);
        intent.putExtra("position", mComic.getReadChapter());
        startActivityForResult(intent, 0);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.FAB_comic_details:
                read();
                break;
            case R.id.btn_favorite:
                //收藏
                if (mComic == null) return;
                Comic findComic = mComicDBHelper.findByUrl(mComic.getComicUrl());
                if (findComic != null) {
                    if (findComic.isMark()) {
                        mComic.setMark(false);
                        mComic.setLastReadTime(System.currentTimeMillis());
                        mComicDBHelper.update(mComic);
                        Toast.makeText(ComicDetailsActivity.this, "取消收藏成功！", Toast.LENGTH_SHORT).show();
                        mBinding.btnFavoriteComicDetails.setImageResource(R.mipmap.favorite);
                        mBinding.buttonTextFavoriteComicDetails.setText("收藏");
                    } else {
                        mComic.setMark(true);
                        mComic.setLastReadTime(System.currentTimeMillis());
                        mComicDBHelper.update(mComic);
                        Toast.makeText(ComicDetailsActivity.this, "收藏成功!", Toast.LENGTH_SHORT).show();
                        mBinding.btnFavoriteComicDetails.setImageResource(R.mipmap.my_favorite);
                        mBinding.buttonTextFavoriteComicDetails.setText("已收藏");
                    }
                } else if (mComic != null) {
                    mComic.setMark(true);
                    mComic.setLastReadTime(System.currentTimeMillis());
                    mComicDBHelper.add(mComic);
                    mComic.setId(mComicDBHelper.findByUrl(mComic.getComicUrl()).getId());
                    Toast.makeText(ComicDetailsActivity.this, "收藏成功!", Toast.LENGTH_SHORT).show();
                    mBinding.btnFavoriteComicDetails.setImageResource(R.mipmap.my_favorite);
                    mBinding.buttonTextFavoriteComicDetails.setText("已收藏");
                } else if (mComic == null) {
                    Toast.makeText(ComicDetailsActivity.this, "还没有加载完成，请耐心等待~", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_share:
                //分享
                if (mComic == null) break;

                OnekeyShare oks = new OnekeyShare();
                //关闭sso授权
                oks.disableSSOWhenAuthorize();

                // 分享时Notification的图标和文字  2.5.9以后的版本不调用此方法
                //oks.setNotification(R.drawable.ic_launcher, getString(R.string.app_name));
                // title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间使用
                oks.setTitle(mComic.getTitle());
                // titleUrl是标题的网络链接，仅在Linked-in,QQ和QQ空间使用
                oks.setTitleUrl(mComic.getComicUrl());
                // text是分享文本，所有平台都需要这个字段
                oks.setText("我在汗汗漫画网站上看到《" + mComic.getTitle() + "》这个漫画，非常有趣，你也来看看吧。");
                //分享网络图片，新浪微博分享网络图片需要通过审核后申请高级写入接口，否则请注释掉测试新浪微博
                oks.setImageUrl(mComic.getThumbnailUrl());
                // imagePath是图片的本地路径，Linked-In以外的平台都支持此参数
                //oks.setImagePath("/sdcard/test.jpg");//确保SDcard下面存在此张图片
                // url仅在微信（包括好友和朋友圈）中使用
                oks.setUrl(mComic.getComicUrl());
                // comment是我对这条分享的评论，仅在人人网和QQ空间使用
                // oks.setComment("我是测试评论文本");
                // site是分享此内容的网站名称，仅在QQ空间使用
                oks.setSite(getResources().getString(R.string.app_name_short));
                // siteUrl是分享此内容的网站地址，仅在QQ空间使用
                oks.setSiteUrl(Constants.HHCOMIC_URL);

                // 启动分享GUI
                oks.show(this);
                break;
            case R.id.btn_find:
                //搜索同一作者的作品
                if (mComic == null) return;
                intent = new Intent(this, ComicResultListActivity.class);
                intent.setAction(Intent.ACTION_SEARCH);
                intent.putExtra(SearchManager.QUERY, mComic.getAuthor());
                startActivity(intent);
                break;
            case R.id.btn_download:
                //下载
                if (mComic == null) {
                    Toast.makeText(ComicDetailsActivity.this, "请等待加载完成~", Toast.LENGTH_SHORT).show();
                } else {
                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
                    View view = LayoutInflater.from(this).inflate(R.layout.fragment_download_selector, null);
                    //设置各种事件
                    setupContentViewListener(view, bottomSheetDialog);
                    bottomSheetDialog.contentView(view);
                    bottomSheetDialog.heightParam(ViewGroup.LayoutParams.WRAP_CONTENT);
                    bottomSheetDialog.cancelable(true);
                    bottomSheetDialog.show();
                }
                break;
            case R.id.comic_author_comic_details:
                //搜索同一作者的作品
                if (mComic == null) return;
                intent = new Intent(this, ComicResultListActivity.class);
                intent.setAction(Intent.ACTION_SEARCH);
                intent.putExtra(SearchManager.QUERY, mComic.getAuthor());
                startActivity(intent);
                break;
            case R.id.comic_description_comic_details_ll:
                //打开漫画简介，改变箭头方向，如果简介不能打开，返回
                if (!isDescriptionCanOpen) break;

                if (!isDescriptionOpen) {
                    mBinding.ivArrow.setImageResource(R.mipmap.arrow_up_black_24dp);
                    mBinding.comicDescriptionComicDetails.setMaxLines(1024);
                    isDescriptionOpen = !isDescriptionOpen;
                } else {
                    mBinding.ivArrow.setImageResource(R.mipmap.arrow_down_black_24dp);
                    mBinding.comicDescriptionComicDetails.setMaxLines(4);
                    isDescriptionOpen = !isDescriptionOpen;
                }
                break;
            case R.id.readButton:
                //阅读按钮（FAB收起时显示）
                read();
                break;
        }
    }

    private void setupContentViewListener(View view, final BottomSheetDialog dialog) {
        Button startDownload = (Button) view.findViewById(R.id.button_start_download_ds_fragment);
        Button allSelect = (Button) view.findViewById(R.id.button_select_all_ds_fragment);
        ImageView cancel = (ImageView) view.findViewById(R.id.image_cancel_ds_fragment);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_download_selector);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        final VolDownloadSelectorAdapter adapter = new VolDownloadSelectorAdapter(this,
                mComic.getChapterName(), getDownloadedComicChapters(), mFinishedComicChapters);
        adapter.setOnItemClickListener(new VolDownloadSelectorAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                adapter.chapterClick(position);
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        recyclerView.setAdapter(adapter);

        startDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开始下载
                if (adapter.getSelectedchapterNames().size() == 0) {
                    Toast.makeText(ComicDetailsActivity.this, "没有选择下载章节", Toast.LENGTH_SHORT).show();
                } else {
                    //将下载章节列表传送到Activity中
                    sendSelectedChapters(adapter.getSelectedchapterNames());
                    //关闭
                    dialog.dismiss();
                }
            }
        });
        allSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.allSelect();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    public List<String> getDownloadedComicChapters() {
        if (mDownloadedComicChapters != null) {
            List<String> downloadedComicChapter = new ArrayList<>();
            for (int i = 0; i < mDownloadedComicChapters.size(); i++) {
                downloadedComicChapter.add(mDownloadedComicChapters.get(i).getChapterName());
            }
            return downloadedComicChapter;
        } else {
            return null;
        }
    }

    public void sendSelectedChapters(List<String> selectedChapters) {
        //更新漫画的数据库资料
        Comic findComic = mComicDBHelper.findByUrl(mComic.getComicUrl());
        //设置下载标记为true
        mComic.setDownload(true);
        if (findComic != null) {
            //如果在数据库中，更新
            mComic.setLastReadTime(System.currentTimeMillis());
            mComicDBHelper.update(mComic);
        } else if (mComic != null) {
            //如果该漫画不在数据库中
            mComic.setLastReadTime(System.currentTimeMillis());
            //加入数据库
            mComicDBHelper.add(mComic);
            mComic.setId(mComicDBHelper.findByUrl(mComic.getComicUrl()).getId());
        } else if (mComic == null) {
            Toast.makeText(ComicDetailsActivity.this, "还没有加载完成，请耐心等待~", Toast.LENGTH_SHORT).show();
            return;
        }

        //开始下载
        if (selectedChapters.size() != 0) {
            //检查自身权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "没有获得权限，无法下载！", Toast.LENGTH_SHORT).show();
                    mSelectedChapters = selectedChapters;
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                } else {
                    mSelectedChapters = selectedChapters;
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                }
                return;
            }

            int position = -1;
            List<Integer> positionList = new ArrayList<>();
            ComicChapter Chapter = null;
            for (String volName : selectedChapters) {
                position = mComic.getChapterName().indexOf(volName);
                positionList.add(position);
            }
            //将下载任务排序
            Collections.sort(positionList);
            for (int i = 0; i < positionList.size(); i++) {
                position = positionList.get(i);
                Chapter = new ComicChapter(mComic.getTitle(), mComic.getChapterName().get(position),
                        mComic.getChapterUrl().get(position), mComic.getComicUrl());
                Message msg = new Message();
                msg.what = Constants.MSG_DOWNLOAD;
                msg.obj = Chapter;
                //将任务按照顺序加入队伍，时间间隔1000ms
                mHandler.sendMessageDelayed(msg, 1000 * i);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the contacts-related task you need to do.
                    if (mSelectedChapters != null && mSelectedChapters.size() != 0) {
                        int position = -1;
                        List<Integer> positionList = new ArrayList<>();
                        ComicChapter Chapter = null;
                        for (String volName : mSelectedChapters) {
                            position = mComic.getChapterName().indexOf(volName);
                            positionList.add(position);
                        }
                        //将下载任务排序
                        Collections.sort(positionList);
                        for (int i = 0; i < positionList.size(); i++) {
                            position = positionList.get(i);
                            Chapter = new ComicChapter(mComic.getTitle(), mComic.getChapterName().get(position),
                                    mComic.getChapterUrl().get(position), mComic.getComicUrl());
                            Message msg = new Message();
                            msg.what = Constants.MSG_DOWNLOAD;
                            msg.obj = Chapter;
                            //将任务按照顺序加入队伍，时间间隔1000ms
                            mHandler.sendMessageDelayed(msg, 1000 * i);
                        }
                        mSelectedChapters = null;
                    }
                } else {
                    // permission denied, boo! Disable the functionality that depends on this permission.
                    return;
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    class ComicChapterDownloadUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(DownloadManagerService.ACTION_RECEIVER)) return;
            if (mComic == null || mComicChapterDBHelper == null) return;
            ComicChapter comicChapter = (ComicChapter) intent.getSerializableExtra("comicChapter");
            if (comicChapter == null) return;
            if (!comicChapter.getComicUrl().equals(mComic.getComicUrl())) return;

            if (comicChapter.getDownloadStatus() != Constants.DOWNLOAD_DOWNLOADING ||
                    comicChapter.getDownloadStatus() != Constants.DOWNLOAD_ERROR ||
                    comicChapter.getDownloadStatus() != Constants.DOWNLOAD_PAUSE) {
                mDownloadedComicChapters = mComicChapterDBHelper.findByComicUrl(mComic.getComicUrl());
                if (comicChapter.getDownloadStatus() == Constants.DOWNLOAD_FINISHED) {
                    //更新界面
                    if (mVolAdapter == null) return;
                    //遍历
                    for (int i = 0; i < mDownloadedComicChapters.size(); i++) {
                        if (mDownloadedComicChapters.get(i).getDownloadStatus()
                                == Constants.DOWNLOAD_FINISHED) {
                            if (mFinishedComicChapters == null) {
                                mFinishedComicChapters = new ArrayList<>();
                            }
                            mFinishedComicChapters.add(mDownloadedComicChapters.get(i).getChapterName());
                        }
                    }
                    mVolAdapter.setFinishedComicChapterList(mFinishedComicChapters);
                    //刷新界面
                    mVolAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
