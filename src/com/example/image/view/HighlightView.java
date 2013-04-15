/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.image.view;

import com.example.image.R;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

// This class is used by CropImage to display a highlighted cropping rectangle
// overlayed with the image. There are two coordinate spaces in use. One is
// image, another is screen. computeLayout() uses mMatrix to map from image
// space to screen space.
public class HighlightView {

    @SuppressWarnings("unused")
    private static final String TAG = "HighlightView";
    View mContext; // The View displaying the image. 显示图片的view

    //状态
    public static final int GROW_NONE = (1 << 0);
    public static final int GROW_LEFT_EDGE = (1 << 1);
    public static final int GROW_RIGHT_EDGE = (1 << 2);
    public static final int GROW_TOP_EDGE = (1 << 3);
    public static final int GROW_BOTTOM_EDGE = (1 << 4);
    public static final int MOVE = (1 << 5);

    public HighlightView(View ctx) {
        Log.i("HighlightView监测","HighlightView");
        mContext = ctx;
    }

    /*
     * 初始化资源文件
     */
    private void init() {
        android.content.res.Resources resources = mContext.getResources();
        mResizeDrawableWidth = resources.getDrawable(R.drawable.camera_crop_width);
        mResizeDrawableHeight = resources.getDrawable(R.drawable.camera_crop_height);
        mResizeDrawableDiagonal = resources.getDrawable(R.drawable.indicator_autocrop);
    }

    public boolean mIsFocused;
    boolean mHidden;
    
    
 
    public boolean hasFocus() {
        Log.i("Highlightview 监测","hasFocus");
        return mIsFocused;
    }
   /*
     * 设置聚焦
     */
    public void setFocus(boolean f) {
        Log.i("Highlightview 监测","setFocus");
        mIsFocused = f;
    }
    /*
     * 设置隐藏
     */
    public void setHidden(boolean hidden) {
        Log.i("Highlightview 监测","setHidden");
        mHidden = hidden;
    }

    /*
     * 绘制画板
     */
    public void draw(Canvas canvas) {
        Log.i("Highlightview 监测","draw");
        if (mHidden) {
            return;
        }
        canvas.save();
        Path path = new Path();
        if (!hasFocus()) {
            mOutlinePaint.setColor(0xFF000000);
            canvas.drawRect(mDrawRect, mOutlinePaint);
        } else {
            Rect viewDrawingRect = new Rect();
            mContext.getDrawingRect(viewDrawingRect);
            if (mCircle) {
                float width = mDrawRect.width();
                float height = mDrawRect.height();
                path.addCircle(mDrawRect.left + (width / 2), mDrawRect.top + (height / 2), width / 2, Path.Direction.CW);
                mOutlinePaint.setColor(0xFFEF04D6);
            } else {
                path.addRect(new RectF(mDrawRect), Path.Direction.CW);
                mOutlinePaint.setColor(0xFFFF8A00);
            }
            canvas.clipPath(path, Region.Op.DIFFERENCE);
            canvas.drawRect(viewDrawingRect, hasFocus() ? mFocusPaint : mNoFocusPaint);

            canvas.restore();
            canvas.drawPath(path, mOutlinePaint);

            if (mMode == ModifyMode.Grow) {
                if (mCircle) {
                    int width = mResizeDrawableDiagonal.getIntrinsicWidth();
                    int height = mResizeDrawableDiagonal.getIntrinsicHeight();

                    int d = (int) Math.round(Math.cos(/* 45deg */Math.PI / 4D) * (mDrawRect.width() / 2D));
                    int x = mDrawRect.left + (mDrawRect.width() / 2) + d - width / 2;
                    int y = mDrawRect.top + (mDrawRect.height() / 2) - d - height / 2;
                    mResizeDrawableDiagonal.setBounds(x, y, x + mResizeDrawableDiagonal.getIntrinsicWidth(), y
                            + mResizeDrawableDiagonal.getIntrinsicHeight());
                    mResizeDrawableDiagonal.draw(canvas);
                } else {
                    int left = mDrawRect.left + 1;
                    int right = mDrawRect.right + 1;
                    int top = mDrawRect.top + 4;
                    int bottom = mDrawRect.bottom + 3;

                    int widthWidth = mResizeDrawableWidth.getIntrinsicWidth() / 2;
                    int widthHeight = mResizeDrawableWidth.getIntrinsicHeight() / 2;
                    int heightHeight = mResizeDrawableHeight.getIntrinsicHeight() / 2;
                    int heightWidth = mResizeDrawableHeight.getIntrinsicWidth() / 2;

                    int xMiddle = mDrawRect.left + ((mDrawRect.right - mDrawRect.left) / 2);
                    int yMiddle = mDrawRect.top + ((mDrawRect.bottom - mDrawRect.top) / 2);

                    mResizeDrawableWidth.setBounds(left - widthWidth, yMiddle - widthHeight, left + widthWidth, yMiddle
                            + widthHeight);
                    mResizeDrawableWidth.draw(canvas);

                    mResizeDrawableWidth.setBounds(right - widthWidth, yMiddle - widthHeight, right + widthWidth, yMiddle
                            + widthHeight);
                    mResizeDrawableWidth.draw(canvas);

                    mResizeDrawableHeight.setBounds(xMiddle - heightWidth, top - heightHeight, xMiddle + heightWidth, top
                            + heightHeight);
                    mResizeDrawableHeight.draw(canvas);

                    mResizeDrawableHeight.setBounds(xMiddle - heightWidth, bottom - heightHeight, xMiddle + heightWidth, bottom
                            + heightHeight);
                    mResizeDrawableHeight.draw(canvas);
                }
            }
        }
    }

    /*
     * 设置模式
     */
    public void setMode(ModifyMode mode) {
        Log.i("Highlightview 监测","setMode");
        if (mode != mMode) {
            mMode = mode;
            mContext.invalidate();
        }
    }

    /*
     * 根据坐标选择改变了哪个边Determines which edges are hit by touching at (x, y).
     */
    public int getHit(float x, float y) {
        Log.i("Highlightview 监测","getHit");
        Rect r = computeLayout();
        final float hysteresis = 20F;
        int retval = GROW_NONE;

        if (mCircle) {
            float distX = x - r.centerX();
            float distY = y - r.centerY();
            int distanceFromCenter = (int) Math.sqrt(distX * distX + distY * distY);
            int radius = mDrawRect.width() / 2;
            int delta = distanceFromCenter - radius;
            if (Math.abs(delta) <= hysteresis) {
                if (Math.abs(distY) > Math.abs(distX)) {
                    if (distY < 0) {
                        retval = GROW_TOP_EDGE;
                    } else {
                        retval = GROW_BOTTOM_EDGE;
                    }
                } else {
                    if (distX < 0) {
                        retval = GROW_LEFT_EDGE;
                    } else {
                        retval = GROW_RIGHT_EDGE;
                    }
                }
            } else if (distanceFromCenter < radius) {
                retval = MOVE;
            } else {
                retval = GROW_NONE;
            }
        } else {
            // verticalCheck makes sure the position is between the top and
            // the bottom edge (with some tolerance). Similar for horizCheck.
            boolean verticalCheck = (y >= r.top - hysteresis) && (y < r.bottom + hysteresis);
            boolean horizCheck = (x >= r.left - hysteresis) && (x < r.right + hysteresis);

            // Check whether the position is near some edge(s).
            if ((Math.abs(r.left - x) < hysteresis) && verticalCheck) {
                retval |= GROW_LEFT_EDGE;
            }
            if ((Math.abs(r.right - x) < hysteresis) && verticalCheck) {
                retval |= GROW_RIGHT_EDGE;
            }
            if ((Math.abs(r.top - y) < hysteresis) && horizCheck) {
                retval |= GROW_TOP_EDGE;
            }
            if ((Math.abs(r.bottom - y) < hysteresis) && horizCheck) {
                retval |= GROW_BOTTOM_EDGE;
            }

            // Not near any edge but inside the rectangle: move.
            if (retval == GROW_NONE && r.contains((int) x, (int) y)) {
                retval = MOVE;
            }
        }
        return retval;
    }

    /*
     * 处理边框移动Handles motion (dx, dy) in screen space.
     */
    // The "edge" parameter specifies which edges the user is dragging.
    public void handleMotion(int edge, float dx, float dy) {
        Log.i("Highlightview 监测","handleMotion");
        Rect r = computeLayout();
        if (edge == GROW_NONE) {
            return;
        } else if (edge == MOVE) {
            // Convert to image space before sending to moveBy().
            moveBy(dx * (mCropRect.width() / r.width()), dy * (mCropRect.height() / r.height()));
        } else {
            if (((GROW_LEFT_EDGE | GROW_RIGHT_EDGE) & edge) == 0) {//不是左右移动
                dx = 0;
            }

            if (((GROW_TOP_EDGE | GROW_BOTTOM_EDGE) & edge) == 0) {//不是上下移动
                dy = 0;
            }

            // Convert to image space before sending to growBy().
            float xDelta = dx * (mCropRect.width() / r.width());
            float yDelta = dy * (mCropRect.height() / r.height());
            growBy((((edge & GROW_LEFT_EDGE) != 0) ? -1 : 1) * xDelta, (((edge & GROW_TOP_EDGE) != 0) ? -1 : 1) * yDelta);//移动参数设置 向左和向上都设置为负
        }
    }

    /*
     *  在图像区域根据(dx,dy)改变rec大小Grows the cropping rectange by (dx, dy) in image space.
     */
    void moveBy(float dx, float dy) {
        Log.i("Highlightview 监测","moveBy");
        Rect invalRect = new Rect(mDrawRect);

        mCropRect.offset(dx, dy);

        // Put the cropping rectangle inside image rectangle.
        mCropRect.offset(Math.max(0, mImageRect.left - mCropRect.left), Math.max(0, mImageRect.top - mCropRect.top));

        mCropRect.offset(Math.min(0, mImageRect.right - mCropRect.right), Math.min(0, mImageRect.bottom - mCropRect.bottom));

        mDrawRect = computeLayout();
        invalRect.union(mDrawRect);
        invalRect.inset(-10, -10);
        mContext.invalidate(invalRect);
    }

    /*
     * 修改矩形框大小 Grows the cropping rectange by (dx, dy) in image space.
     */
    void growBy(float dx, float dy) {
        Log.i("Highlightview 监测","growBy");
        if (mMaintainAspectRatio) {
            if (dx != 0) {
                dy= dx / mInitialAspectRatio;
            } else if (dy != 0) {
                dx = dy * mInitialAspectRatio;
            }
        }

        // Don't let the cropping rectangle grow too fast.
        // Grow at most half of the difference between the image rectangle and
        // the cropping rectangle.
        RectF r = new RectF(mCropRect);
        if (dx > 0F && r.width() + 2 * dx > mImageRect.width()) {//裁剪矩形框超出图像横向边框
            float adjustment = (mImageRect.width() - r.width()) / 2F;//最大变到图像大小
            dx = adjustment;
            if (mMaintainAspectRatio) {//如果需要保持纵横比
                dy = dx / mInitialAspectRatio;
            }
        }
        if (dy > 0F && r.height() + 2 * dy > mImageRect.height()) {//裁剪矩形框超出图像纵向边框
            float adjustment = (mImageRect.height() - r.height()) / 2F;
            dy = adjustment;
            if (mMaintainAspectRatio) {
                dx = dy * mInitialAspectRatio;
            }
        }

        r.inset(-dx, -dy);

        // Don't let the cropping rectangle shrink too fast.
        final float widthCap = 5F;
        if (r.width() < widthCap) {
            r.inset(-(widthCap - r.width()) / 2F, 0F);
        }
        float heightCap = mMaintainAspectRatio ? (widthCap / mInitialAspectRatio) : widthCap;
        if (r.height() < heightCap) {
            r.inset(0F, -(heightCap - r.height()) / 2F);
        }

        // Put the cropping rectangle inside the image rectangle.
        if (r.left < mImageRect.left) {//裁剪矩形左边超过了图像矩形的左边界
            r.offset(mImageRect.left - r.left, 0F);
        } else if (r.right > mImageRect.right) {//裁剪矩形右边超过了图像矩形的右边界
            r.offset(-(r.right - mImageRect.right), 0);
        }
        if (r.top < mImageRect.top) {//裁剪矩形上边超过了图像矩形的上边界
            r.offset(0F, mImageRect.top - r.top);
        } else if (r.bottom > mImageRect.bottom) {//裁剪矩形下边超过了图像矩形的下边界
            r.offset(0F, -(r.bottom - mImageRect.bottom));
        }

        mCropRect.set(r);
        mDrawRect = computeLayout();
        mContext.invalidate();
    }

    /*
     * 返回切割矩形框 Returns the cropping rectangle in image space.
     */
    public Rect getCropRect() {
        Log.i("Highlightview 监测","getCropRect");
        return new Rect((int) mCropRect.left, (int) mCropRect.top, (int) mCropRect.right, (int) mCropRect.bottom);
    }

    /*
     * 将矩形框从图像区域映射到屏幕区域 Maps the cropping rectangle from image space to screen space.
     */
    private Rect computeLayout() {
        Log.i("Highlightview 监测","computeLayout");
		RectF r = new RectF(mCropRect.left, mCropRect.top, mCropRect.right, mCropRect.bottom);
        mMatrix.mapRect(r);//矩形r用mMatrix变形
        return new Rect(Math.round(r.left), Math.round(r.top), Math.round(r.right), Math.round(r.bottom));
    }

    /*
     * 刷新view
     */
    public void invalidate() {
        mDrawRect = computeLayout();
    }

    /*
     * 设置view参数格式等信息
     */
    public void setup(Matrix m, Rect imageRect, RectF cropRect, boolean circle, boolean maintainAspectRatio) {
        Log.i("Highlightview 监测","setup");
        if (circle) {//按住的是矩形拐角
            maintainAspectRatio = true;
        }
        mMatrix = new Matrix(m);

        mCropRect = cropRect;
        mImageRect = new RectF(imageRect);
        mMaintainAspectRatio = maintainAspectRatio;
        mCircle = circle;

        mInitialAspectRatio = mCropRect.width() / mCropRect.height();
        mDrawRect = computeLayout();

        mFocusPaint.setARGB(125, 50, 50, 50);
        mNoFocusPaint.setARGB(125, 50, 50, 50);
        mOutlinePaint.setStrokeWidth(3F);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setAntiAlias(true);

        mMode = ModifyMode.None;
        init();
    }
/*
 * ModifyMode 修改模式
 */
    public enum ModifyMode {
        None, Move, Grow
    }
/*
 * 成员变量
 */
    private ModifyMode mMode = ModifyMode.None;

    public Rect mDrawRect; // in screen space
    private RectF mImageRect; // in image space 图像矩形
    public RectF mCropRect; // in image space 切割矩形
    public Matrix mMatrix;

    private boolean mMaintainAspectRatio = false;
    private float mInitialAspectRatio;
    private boolean mCircle = false;

    private Drawable mResizeDrawableWidth;
    private Drawable mResizeDrawableHeight;
    private Drawable mResizeDrawableDiagonal;

    private final Paint mFocusPaint = new Paint();
    private final Paint mNoFocusPaint = new Paint();
    private final Paint mOutlinePaint = new Paint();
}
