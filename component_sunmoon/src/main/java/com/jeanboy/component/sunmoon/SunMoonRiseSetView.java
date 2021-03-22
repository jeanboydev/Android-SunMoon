package com.jeanboy.component.sunmoon;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by jeanboy on 2020/6/20 15:08.
 */
public class SunMoonRiseSetView extends View {

    private Paint borderPaint = new Paint(); // 线条画笔
    private Paint arcPaint = new Paint(); // 扇形画笔
    private Paint textPaint = new Paint(); // 文字画笔

    private Path borderPath = new Path(); // 边框路径
    private Path arcPath = new Path(); // 扇形路径

    private Point startPoint = new Point(); // 升起位置
    private Point endPoint = new Point(); // 下落位置
    private Point arcCenterPoint = new Point(); // 扇形圆形位置

    private int lineWidth; // 底部横线宽度
    private int lineStartX; // 底部横线开始位置 x
    private int lineStartY; // 底部横线开始位置 y
    private int lineCenterX; // 底部横线中心位置 x

    private int arcHeight; // 扇形高度
    private int arcRadius; // 扇形半径
    private float startAngle; // 扇形开始角度
    private float sweepAngle; // 扇形划过角度
    private float progressAngle; // 当前划过角度

    private WeakReference<Bitmap> sunBitmap;
    private Matrix matrix = new Matrix();

    private int textHeight; // 文本高度
    private long riseTime; // 上升时间
    private long downTime; // 下降时间
    private String riseTimeText = "";
    private String downTimeText = "";

    private float progress;
    private static final float PROGRESS_MAX = 100f;
    private ValueAnimator valueAnimator;
    private int animProgressTime;


    /*  ------- 可配置参数 ------- */
    private int bottomLineSize; // 底部横线宽度
    private int bottomLineBallSize; // 底部横线上点的大小
    private int bottomLineColor; // 底部横线颜色
    private int arcLineSize; // 扇形线宽度
    private int arcIconSize; // 图标宽度
    private float arcRatio; // 扇形半径与底部直线的比例

    private int textSize; // 文本大小
    private int textColor; // 文本颜色
    private int textMargin; // 文本距离直线的间距
    private String leftText; // 左边文本
    private String rightText; // 右边文本
    private int animTotalTime; // 动画总时长


    public SunMoonRiseSetView(Context context) {
        this(context, null);
    }

    public SunMoonRiseSetView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SunMoonRiseSetView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupView(context, attrs);
    }

    private void setupView(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SunMoonRiseSetView);
        try {
            bottomLineSize = typedArray.getDimensionPixelSize(R.styleable.SunMoonRiseSetView_bottomLineSize, dp2px(2));
            bottomLineBallSize = typedArray.getDimensionPixelSize(R.styleable.SunMoonRiseSetView_bottomLineBallSize, dp2px(8));
            bottomLineColor = typedArray.getColor(R.styleable.SunMoonRiseSetView_bottomLineColor, Color.parseColor("#8059576B"));
            arcLineSize = typedArray.getDimensionPixelSize(R.styleable.SunMoonRiseSetView_arcLineSize, dp2px(1f));
            arcIconSize = typedArray.getDimensionPixelSize(R.styleable.SunMoonRiseSetView_arcIconSize, dp2px(50f));
            arcRatio = typedArray.getFloat(R.styleable.SunMoonRiseSetView_arcRatio, 0.86f);

            textSize = (int) typedArray.getDimension(R.styleable.SunMoonRiseSetView_textSize, sp2px(10));
            textColor = typedArray.getColor(R.styleable.SunMoonRiseSetView_textColor, Color.parseColor("#A3A1B0"));
            textMargin = typedArray.getDimensionPixelSize(R.styleable.SunMoonRiseSetView_arcLineSize, dp2px(10));
            leftText = typedArray.getString(R.styleable.SunMoonRiseSetView_leftText);
            rightText = typedArray.getString(R.styleable.SunMoonRiseSetView_rightText);
            animTotalTime = typedArray.getInt(R.styleable.SunMoonRiseSetView_animTotalTime, 5000);
        } finally {
            typedArray.recycle();
        }

        borderPaint = new Paint();
        borderPaint.setAntiAlias(true); // 打开抗锯齿
        borderPaint.setStyle(Paint.Style.STROKE);

        arcPaint = new Paint();
        arcPaint.setAntiAlias(true);
        arcPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(textColor);

        borderPath = new Path();
        arcPath = new Path();

        valueAnimator = ValueAnimator.ofFloat(0, PROGRESS_MAX);
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                progress = PROGRESS_MAX;
                postInvalidate();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                progress = PROGRESS_MAX;
                postInvalidate();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                progress = 0;
                postInvalidate();
            }
        });
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int mWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int mHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

        lineWidth = mWidth;
        lineStartX = getPaddingLeft();
        textHeight = getTextHeight(textPaint, textSize);
        lineStartY = getMeasuredHeight() - getPaddingBottom() - textHeight * 2 - textMargin;
        lineCenterX = lineStartX + lineWidth / 2;

        // 扇形高度
        arcHeight = lineStartY - getPaddingTop() - arcLineSize;
        // 扇形半径
        arcRadius = (int) (lineWidth * arcRatio / 2);
        // 扇形圆点
        arcCenterPoint.set(lineWidth / 2 + lineStartX, arcRadius + getPaddingTop() + arcLineSize);

        // 扇形底部到圆心的距离
        int arcDistance = arcRadius - arcHeight;
        // 根据勾股定理求出长边长度
        int arcWidth = (int) Math.sqrt(arcRadius * arcRadius - arcDistance * arcDistance); // a
        // 扇形与直线交叉的偏移量
        int lineOffset = (lineWidth - arcWidth * 2) / 2;
        startPoint.set(lineStartX + lineOffset, lineStartY);
        endPoint.set(lineStartX + lineWidth - lineOffset, lineStartY);

        // 根据反三角函数计算扇形划过角度
        sweepAngle = (float) Math.toDegrees(Math.acos(arcDistance * 1f / arcRadius)) * 2;
        // 扇形开始的角度
        startAngle = (180 - sweepAngle) / 2 - 180;
        // 计算当前需要划过的角度
        progressAngle = getProgressAngle(riseTime, downTime, System.currentTimeMillis());
        Log.e("---", "---" + progressAngle * 1.0f / sweepAngle);
        animProgressTime = (int) ((progressAngle * 1.0f / sweepAngle) * animTotalTime);
        Log.e("---", "---" + animProgressTime);
        if (valueAnimator != null) {
            valueAnimator.setDuration(animProgressTime);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        drawDetail(canvas);
    }

    private void drawDetail(Canvas canvas) {
        // 画底部横线
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(bottomLineSize);
        borderPaint.setColor(bottomLineColor);
        borderPaint.setShader(null);
        borderPath.moveTo(lineStartX, lineStartY);
        borderPath.lineTo(lineStartX + lineWidth, lineStartY);
        canvas.drawPath(borderPath, borderPaint);

        // 画弧线
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(arcLineSize);
        borderPaint.setColor(Color.WHITE);
        RectF arcRect = getArcRect(arcCenterPoint.x, arcCenterPoint.y, arcRadius);
        LinearGradient lineGradient = new LinearGradient(arcRect.left, arcRect.bottom, arcRect.right, arcRect.bottom,
                Color.parseColor("#FFDB48"), Color.parseColor("#2C3981"), Shader.TileMode.CLAMP);
        borderPaint.setShader(lineGradient);
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, borderPaint);

        // 画圆点
        borderPaint.setStyle(Paint.Style.FILL);
        borderPaint.setColor(Color.WHITE);
        int bottomLineBallSizeHalf = bottomLineBallSize / 2;
        LinearGradient startGradient = new LinearGradient(startPoint.x, startPoint.y - bottomLineBallSizeHalf,
                startPoint.x, startPoint.y + bottomLineBallSizeHalf,
                Color.parseColor("#FF8712"), Color.parseColor("#FFD228"), Shader.TileMode.CLAMP);
        borderPaint.setShader(startGradient);
        canvas.drawCircle(startPoint.x, startPoint.y, bottomLineBallSize * 0.5f, borderPaint);

        LinearGradient endGradient = new LinearGradient(endPoint.x, endPoint.y - bottomLineBallSizeHalf,
                endPoint.x, endPoint.y + bottomLineBallSizeHalf,
                Color.parseColor("#FF8712"), Color.parseColor("#51598F"), Shader.TileMode.CLAMP);
        borderPaint.setShader(endGradient);
        canvas.drawCircle(endPoint.x, endPoint.y, bottomLineBallSize * 0.5f, borderPaint);

        // 画扇形
        float offsetAngle = progressAngle * (progress * 1.0f / PROGRESS_MAX);
        Point circlePoint = getCirclePoint(arcCenterPoint, arcRadius, offsetAngle + startAngle);
        LinearGradient arcGradient = new LinearGradient(arcRect.left, arcRect.bottom, arcRect.right, arcRect.bottom,
                Color.parseColor("#80FFDB48"), Color.parseColor("#802C3981"), Shader.TileMode.CLAMP);
        arcPaint.setShader(arcGradient);
        arcPath.moveTo(startPoint.x, startPoint.y);
        arcPath.addArc(arcRect, startAngle, offsetAngle);
        arcPath.lineTo(circlePoint.x, lineStartY);
        arcPath.close();
        canvas.drawPath(arcPath, arcPaint);

        // 画太阳
        borderPaint.setStyle(Paint.Style.FILL);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setShader(null);
        Bitmap bitmap = getSunBitmap();
        matrix.reset();
        matrix.postScale(arcIconSize * 1.0f / bitmap.getWidth(), arcIconSize * 1.0f / bitmap.getHeight());
        matrix.postTranslate(circlePoint.x - arcIconSize * 1.0f / 2, circlePoint.y - arcIconSize * 1.0f / 2);
        canvas.drawBitmap(bitmap, matrix, borderPaint);

        // 文字
        drawText(canvas, riseTimeText, startPoint.x, startPoint.y + textMargin + arcLineSize, textSize, textColor);
        if (!TextUtils.isEmpty(leftText)) {
            drawText(canvas, leftText, startPoint.x, startPoint.y + textMargin + textHeight + arcLineSize, textSize, textColor);
        }
        drawText(canvas, downTimeText, endPoint.x, endPoint.y + textMargin + arcLineSize, textSize, textColor);
        if (!TextUtils.isEmpty(rightText)) {
            drawText(canvas, rightText, endPoint.x, endPoint.y + textMargin + textHeight + arcLineSize, textSize, textColor);
        }
    }

    private int getTextHeight(Paint paint, int size) {
        paint.setTextSize(size);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        return (int) (fontMetrics.bottom - fontMetrics.top);
    }

    private RectF getArcRect(float x, float y, float radius) {
        return new RectF(x - radius, y - radius, x + radius, y + radius);
    }

    private Point getCirclePoint(Point center, int radius, float angle) {
        int x = (int) (center.x + radius * Math.cos(angle * Math.PI / 180));
        int y = (int) (center.y + radius * Math.sin(angle * Math.PI / 180));
        return new Point(x, y);
    }

    private void drawText(Canvas canvas, String text, float x, float y, int textSize, int textColor) {
        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float fontTotalHeight = fontMetrics.bottom - fontMetrics.top;
        float offsetY = fontTotalHeight / 2 - fontMetrics.bottom;

        int textPadding = 0;
        float newX = x - textPadding;
        float newY = y + offsetY;
        canvas.drawText(text, newX, newY, textPaint);
    }

    private int dp2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private int sp2px(float spValue) {
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    private Bitmap getSunBitmap() {
        if (sunBitmap == null) {
            sunBitmap = new WeakReference<>(BitmapFactory.decodeResource(getResources(), R.drawable.ic_sun));
        }
        Bitmap bitmap = sunBitmap.get();
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_sun);
            sunBitmap = new WeakReference<>(bitmap);
        }
        return bitmap;
    }

    private float getProgressAngle(long riseTime, long downTime, long currentTime) {
        if (currentTime <= riseTime) {
            return 0;
        } else if (currentTime >= downTime) {
            return sweepAngle;
        } else {
            return (currentTime - riseTime) * 1.0f / (downTime - riseTime) * sweepAngle;
        }
    }

    public void setData(long riseTime, long downTime) {
        this.riseTime = riseTime;
        this.downTime = downTime;
        String format = DateFormat.is24HourFormat(getContext()) ? "HH:mm" : "h:mm a";
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.getDefault());
        riseTimeText = dateFormat.format(riseTime);
        downTimeText = dateFormat.format(downTime);
        requestLayout();
    }

    public void startAnim() {
        if (valueAnimator != null) {
            if (valueAnimator.isStarted() || valueAnimator.isRunning()) return;
            valueAnimator.start();
        }
    }

    public void stopAnim() {
        if (valueAnimator != null) {
            if (valueAnimator.isStarted() || valueAnimator.isRunning()) {
                valueAnimator.cancel();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnim();
        super.onDetachedFromWindow();
    }
}
