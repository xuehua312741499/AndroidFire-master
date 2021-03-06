/*
 * Copyright (c) 2016 咖枯 <kaku201313@163.com | 3772304@qq.com>
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
 *
 */
package com.jaydenxiao.androidfire.widget;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.widget.TextView;

import com.jaydenxiao.androidfire.app.AppApplication;
import com.jaydenxiao.androidfire.R;
import com.jaydenxiao.androidfire.api.Api;
import com.jaydenxiao.androidfire.api.HostType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.ResponseBody;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * 自定义加载网络图片
 * @author 咖枯
 * @version 1.0 2016/6/19
 * Html类用于处理的HTML字符串并将其转换成可显示的样式文本。但并不是所有的HTML标记的支持。
 */
public class URLImageGetter implements Html.ImageGetter {
    private TextView mTextView;
    private int mPicWidth;
    private String mNewsBody;
    private int mPicCount;
    private int mPicTotal;
    private static final String mFilePath = AppApplication.getAppContext().getCacheDir().getAbsolutePath();
    public Subscription mSubscription;

    public URLImageGetter(TextView textView, String newsBody, int picTotal) {
        mTextView = textView;
        mPicWidth = mTextView.getWidth();
        mNewsBody = newsBody;
        mPicTotal = picTotal;
    }

    @Override
    public Drawable getDrawable(final String source) {
        Drawable drawable;
        // 封装路径
        File file = new File(mFilePath, source.hashCode() + "");
        // 判断路径是否存在
        if (file.exists()) {
            // 存在即获取drawable
            mPicCount++;
            drawable = getDrawableFromDisk(file);
        } else {
            // 不存在即开启异步任务加载网络图片
            drawable = getDrawableFromNet(source);
        }
        return drawable;
    }

    @Nullable
    private Drawable getDrawableFromDisk(File file) {
        Drawable drawable = Drawable.createFromPath(file.getAbsolutePath());
        if (drawable != null) {
            int picHeight = calculatePicHeight(drawable);
            drawable.setBounds(0, 0, mPicWidth, picHeight);
        }
        return drawable;
    }

    private int calculatePicHeight(Drawable drawable) {
        float imgWidth = drawable.getIntrinsicWidth();
        float imgHeight = drawable.getIntrinsicHeight();
        float rate = imgHeight / imgWidth;
        return (int) (mPicWidth * rate);
    }

    /**
     * 加载网络图片异步类
     * @param source
     * @return
     */
    @NonNull
    private Drawable getDrawableFromNet(final String source) {
        mSubscription = Api.getDefault(HostType.NEWS_DETAIL_HTML_PHOTO)
                .getNewsBodyHtmlPhoto(Api.getCacheControl(),source)
                .unsubscribeOn(Schedulers.io())//取消订阅
                .subscribeOn(Schedulers.io())//订阅
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<ResponseBody, Boolean>() {
                    @Override
                    public Boolean call(ResponseBody response) {
                        return WritePicToDisk(response, source);
                    }
                }).subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Boolean isLoadSuccess) {
                        mPicCount++;
                        if (isLoadSuccess && (mPicCount == mPicTotal - 1)) {
                            mTextView.setText(Html.fromHtml(mNewsBody, URLImageGetter.this, null));
                        }
                    }
                });

        return createPicPlaceholder();
    }

    /**加载网络图片*/
    @NonNull
    private Boolean WritePicToDisk(ResponseBody response, String source) {
        File file = new File(mFilePath, source.hashCode() + "");
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = response.byteStream();
            out = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
            }
        }
    }

    @SuppressWarnings("deprecation")
    @NonNull
    private Drawable createPicPlaceholder() {
        Drawable drawable;
        int color = R.color.white;
        drawable = new ColorDrawable(AppApplication.getAppContext().getResources().getColor(color));
        drawable.setBounds(0, 0, mPicWidth, mPicWidth / 3);
        return drawable;
    }

//    /**
//     * 本地图片
//     * @author Susie
//     */
//    private final class LocalImageGetter implements Html.ImageGetter{
//
//        @Override
//        public Drawable getDrawable(String source) {
//            // 获取本地图片
//            Drawable drawable = Drawable.createFromPath(source);
//            // 必须设为图片的边际,不然TextView显示不出图片
//            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
//            // 将其返回
//            return drawable;
//        }
//    }
//    /**
//     * 项目资源图片
//     * @author Susie
//     */
//    private final class ProImageGetter implements Html.ImageGetter{
//
//        @Override
//        public Drawable getDrawable(String source) {
//            // 获取到资源id
//            int id = Integer.parseInt(source);
//            Drawable drawable = getResources().getDrawable(id);
//            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
//            return drawable;
//        }
//    }

}