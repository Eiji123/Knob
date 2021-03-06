package it.beppi.knoblibrary;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

import it.beppi.balloonpopuplibrary.BalloonPopup;

import static java.lang.Math.PI;

/**
 * Created by Beppi on 06/12/2016.
 */

public class Knob extends View {

    // constructors
    public Knob(Context context) {
        super(context);
        init(null);
    }

    public Knob(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Knob(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Knob(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    // overrides

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        Resources r = Resources.getSystem();
        if(widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST){
            widthSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, r.getDisplayMetrics());
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
        }

        if(heightMode == MeasureSpec.UNSPECIFIED || heightSize == MeasureSpec.AT_MOST){
            heightSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, r.getDisplayMetrics());
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        final int width = getWidth();
        final int height = getHeight();

        externalRadius = Math.min(width, height) * 0.5f;
        knobRadius = externalRadius * knobRelativeRadius;
        centerX = width/2;
        centerY = height/2;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paintKnob(canvas);
        paintMarkers(canvas);
        paintIndicator(canvas);
        paintCircularIndicator(canvas);
        paintKnobCenter(canvas);
        paintKnobBorder(canvas);
        displayBalloons();
    }

    void paintKnob(Canvas canvas) {
        if (knobDrawableRes != 0 && knobDrawable != null) {
            knobDrawable.setBounds((int)(centerX-knobRadius), (int)(centerY-knobRadius), (int)(centerX+knobRadius), (int)(centerY+knobRadius));
            if (knobDrawableRotates) {
                canvas.save();
                canvas.rotate((float)-Math.toDegrees(PI + currentAngle), centerX, centerY);
                knobDrawable.draw(canvas);
                canvas.restore();
            }
            else
                knobDrawable.draw(canvas);
        } else {
            paint.setColor(knobColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(centerX, centerY, knobRadius, paint);
        }
    }

    void paintKnobBorder(Canvas canvas) {
        if (borderWidth == 0) return;
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        canvas.drawCircle(centerX, centerY, knobRadius, paint);
    }

    void paintKnobCenter(Canvas canvas) {
        if (knobDrawableRes != 0 && knobDrawable != null) return;
        if (knobCenterRelativeRadius == 0f) return;
        paint.setColor(knobCenterColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, knobCenterRelativeRadius * knobRadius, paint);
    }

    double normalizeAngle(double angle) {
        while (angle < 0) angle += PI*2;
        while (angle >= PI*2) angle -= PI*2;
        return angle;
    }

    double calcAngle(int position) {
        double min = Math.toRadians((double)minAngle);
        double max = Math.toRadians((double)maxAngle - 0.0001);
        double range = max - min;

        if (numberOfStates <= 1)
            return 0;

        double singleStepAngle = range / (numberOfStates-1);
        if (PI*2 - range < singleStepAngle)
            singleStepAngle = range / numberOfStates;
        return normalizeAngle(PI - min - position * singleStepAngle);

        // return Math.PI - position * (2 * Math.PI / numberOfStates);
    }

    void setIndicatorAngleWithDirection() {
        double angleCurr = normalizeAngle(spring.getCurrentValue());
        double angleNew = calcAngle(actualState);
        if (freeRotation) {
            if (angleCurr > angleNew && angleCurr - angleNew > PI) angleNew += PI * 2;
            else if (angleCurr < angleNew && angleNew - angleCurr > PI) angleNew -= PI * 2;
        }
        spring.setCurrentValue(angleCurr);
        spring.setEndValue(angleNew);
    }

    void paintIndicator(Canvas canvas) {
        if (indicatorWidth == 0) return;
        if (indicatorRelativeLength == 0.0f) return;
        paint.setColor(indicatorColor);
        paint.setStrokeWidth(indicatorWidth);

        float startX = centerX + (float)(knobRadius * (1-indicatorRelativeLength) * Math.sin(currentAngle));
        float startY = centerY + (float)(knobRadius * (1-indicatorRelativeLength) * Math.cos(currentAngle));
        float endX = centerX + (float)(knobRadius * Math.sin(currentAngle));
        float endY = centerY + (float)(knobRadius * Math.cos(currentAngle));
        canvas.drawLine(startX, startY, endX, endY, paint);
    }

    void paintCircularIndicator(Canvas canvas) {
        if (circularIndicatorRelativeRadius == 0.0f) return;
        paint.setColor(circularIndicatorColor);
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.FILL);

        float posX = centerX + (float)(externalRadius * circularIndicatorRelativePosition * Math.sin(currentAngle));
        float posY = centerY + (float)(externalRadius * circularIndicatorRelativePosition * Math.cos(currentAngle));
        canvas.drawCircle(posX, posY, externalRadius * circularIndicatorRelativeRadius, paint);
    }

    void paintMarkers(Canvas canvas) {
        if ((stateMarkersRelativeLength == 0 || stateMarkersWidth == 0) && (stateMarkersAccentRelativeLength == 0 || stateMarkersAccentWidth == 0)) return;
        for (int w=0; w<numberOfStates; w++) {
            boolean big = false;
            boolean selected = false;
            if (stateMarkersAccentPeriodicity != 0)
                big = (w % stateMarkersAccentPeriodicity == 0);
            selected = (w == actualState || (w <= actualState && selectedStateMarkerContinuous));

            paint.setStrokeWidth(big ? stateMarkersAccentWidth : stateMarkersWidth);
            double angle = calcAngle(w);
            float startX = centerX + (float) (externalRadius * (1 - (big ? stateMarkersAccentRelativeLength : stateMarkersRelativeLength)) * Math.sin(angle));
            float startY = centerY + (float) (externalRadius * (1 - (big ? stateMarkersAccentRelativeLength : stateMarkersRelativeLength)) * Math.cos(angle));
            float endX = centerX + (float) (externalRadius * Math.sin(angle));
            float endY = centerY + (float) (externalRadius * Math.cos(angle));
            paint.setColor(selected ? selectedStateMarkerColor : (big ? stateMarkersAccentColor : stateMarkersColor));
            canvas.drawLine(startX, startY, endX, endY, paint);
        }
    }

    int balloonsX() {
        return (int)(centerX + (float)(externalRadius * balloonValuesRelativePosition * Math.sin(currentAngle)));
    }
    int balloonsY() {
        return (int)(centerY + (float)(externalRadius * balloonValuesRelativePosition * Math.cos(currentAngle)));
    }
    String balloonText() {
        if (balloonValuesArray == null)
            return Integer.toString(actualState);
        else
            return balloonValuesArray[actualState].toString();
    }

    void displayBalloons() {
        if (!showBalloonValues) return;
        if (balloonPopup == null || !balloonPopup.isShowing())
            balloonPopup = BalloonPopup.Builder(ctx, this)
                    .text(balloonText())
                    .gravity(BalloonPopup.BalloonGravity.halftop_halfleft)
                    .offsetX(balloonsX())
                    .offsetY(balloonsY())
                    .textSize((int)balloonValuesTextSize)
                    .shape(BalloonPopup.BalloonShape.rounded_square)
                    .timeToLive(balloonValuesTimeToLive)
                    .animation(getBalloonAnimation())
                    .stayWithinScreenBounds(true)
                    .show();
        else {
            balloonPopup.updateOffset(balloonsX(), balloonsY(), true);
            balloonPopup.updateText(balloonText(), true);
            balloonPopup.updateTextSize((int)balloonValuesTextSize, true);  // solo l'ultimo richiede l'aggiornamento del timer?
        }
    }
    BalloonPopup.BalloonAnimation getBalloonAnimation() {
        if (balloonValuesAnimation == 0 && balloonValuesSlightlyTransparent) return BalloonPopup.BalloonAnimation.fade75_and_pop;
        else if (balloonValuesAnimation == 0) return BalloonPopup.BalloonAnimation.fade_and_pop;
        else if (balloonValuesAnimation == 1 && balloonValuesSlightlyTransparent) return BalloonPopup.BalloonAnimation.fade75_and_scale;
        else if (balloonValuesAnimation == 1) return BalloonPopup.BalloonAnimation.fade_and_scale;
        else if (balloonValuesAnimation == 2 && balloonValuesSlightlyTransparent) return BalloonPopup.BalloonAnimation.fade75;
        else return BalloonPopup.BalloonAnimation.fade;
    }

    // default values
    private int numberOfStates = 6;
    private int defaultState = 0;
    private int borderWidth = 2;
    private int borderColor = Color.BLACK;
    private int indicatorWidth = 6;
    private int indicatorColor = Color.BLACK;
    private float indicatorRelativeLength = 0.35f;
    private float circularIndicatorRelativeRadius = 0.0f;
    private float circularIndicatorRelativePosition = 0.7f;
    private int circularIndicatorColor = Color.BLACK;
    private int knobColor = Color.LTGRAY;
    private float knobRelativeRadius = 0.8f;
    private float knobCenterRelativeRadius = 0.45f;
    private int knobCenterColor = Color.DKGRAY;
    private boolean enabled = true;
    private int currentState = defaultState; // can be negative and override expected limits
    private int actualState = currentState; // currentState, modded to the expected limits
    private boolean animation = true;
    private float animationSpeed = 10;
    private float animationBounciness = 40;
    private int stateMarkersWidth = 2;
    private int stateMarkersColor = Color.BLACK;
    private int selectedStateMarkerColor = Color.YELLOW;
    private boolean selectedStateMarkerContinuous = false;
    private float stateMarkersRelativeLength = 0.06f;
    private int swipeDirection = 4;   // circular  (before it was horizontal)
    private int swipeSensibilityPixels = 100;
    private int swipeX=0, swipeY=0;  // used for swipe management
    boolean swipeing = false;        // used for swipe / click management
    private boolean freeRotation = true;
    private float minAngle = 0f;
    private float maxAngle = 360f;
    private int stateMarkersAccentWidth = 3;
    private int stateMarkersAccentColor = Color.BLACK;
    private float stateMarkersAccentRelativeLength = 0.11f;
    private int stateMarkersAccentPeriodicity = 0;  // 0 = off
    private int knobDrawableRes = 0;
    private boolean knobDrawableRotates = true;
    private boolean showBalloonValues = false;
    private int balloonValuesTimeToLive = 400;
    private float balloonValuesRelativePosition = 1.3f;
    private float balloonValuesTextSize = 9;
    private int balloonValuesAnimation = 0;
    private CharSequence[] balloonValuesArray = null;
    private boolean balloonValuesSlightlyTransparent = true;


    // initialize

    void init(AttributeSet attrs) {
        ctx = getContext();
        loadAttributes(attrs);
        initTools();
        initDrawables();
        initBalloons();
        initListeners();
        initStatus();
    }

    private Paint paint;
    private Context ctx;
    private float externalRadius, knobRadius, centerX, centerY;
    SpringSystem springSystem;
    Spring spring;
    private double currentAngle;
    private int previousState = defaultState;
    private Drawable knobDrawable;
    private BalloonPopup balloonPopup;

    void initTools() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeCap(Paint.Cap.ROUND);

        springSystem = SpringSystem.create();
        spring = springSystem.createSpring();
        spring.setSpringConfig(SpringConfig.fromBouncinessAndSpeed((double)animationSpeed, (double)animationBounciness));
        spring.setOvershootClampingEnabled(false);
    }

    void initDrawables() {
        if (knobDrawableRes != 0) {
            knobDrawable = getResources().getDrawable(knobDrawableRes);
        }
    }

    void loadAttributes(AttributeSet attrs) {
        if (attrs == null) return;

        TypedArray typedArray = ctx.obtainStyledAttributes(attrs, R.styleable.Knob);

        numberOfStates = typedArray.getInt(R.styleable.Knob_kNumberOfStates, numberOfStates);
        defaultState = typedArray.getInt(R.styleable.Knob_kDefaultState, defaultState);

        borderWidth = typedArray.getDimensionPixelSize(R.styleable.Knob_kBorderWidth, borderWidth);
        borderColor = typedArray.getColor(R.styleable.Knob_kBorderColor, borderColor);

        indicatorWidth = typedArray.getDimensionPixelSize(R.styleable.Knob_kIndicatorWidth, indicatorWidth);
        indicatorColor = typedArray.getColor(R.styleable.Knob_kIndicatorColor, indicatorColor);
        indicatorRelativeLength = typedArray.getFloat(R.styleable.Knob_kIndicatorRelativeLength, indicatorRelativeLength);

        circularIndicatorRelativeRadius = typedArray.getFloat(R.styleable.Knob_kCircularIndicatorRelativeRadius, circularIndicatorRelativeRadius);
        circularIndicatorRelativePosition = typedArray.getFloat(R.styleable.Knob_kCircularIndicatorRelativePosition, circularIndicatorRelativePosition);
        circularIndicatorColor = typedArray.getColor(R.styleable.Knob_kCircularIndicatorColor, circularIndicatorColor);

        knobColor = typedArray.getColor(R.styleable.Knob_kKnobColor, knobColor);
        knobRelativeRadius = typedArray.getFloat(R.styleable.Knob_kKnobRelativeRadius, knobRelativeRadius);

        knobCenterRelativeRadius = typedArray.getFloat(R.styleable.Knob_kKnobCenterRelativeRadius, knobCenterRelativeRadius);
        knobCenterColor = typedArray.getColor(R.styleable.Knob_kKnobCenterColor, knobCenterColor);
        
        knobDrawableRes = typedArray.getResourceId(R.styleable.Knob_kKnobDrawable, knobDrawableRes);
        knobDrawableRotates = typedArray.getBoolean(R.styleable.Knob_kKnobDrawableRotates, knobDrawableRotates);

        stateMarkersWidth = typedArray.getDimensionPixelSize(R.styleable.Knob_kStateMarkersWidth, stateMarkersWidth);
        stateMarkersColor = typedArray.getColor(R.styleable.Knob_kStateMarkersColor, stateMarkersColor);
        selectedStateMarkerColor = typedArray.getColor(R.styleable.Knob_kSelectedStateMarkerColor, selectedStateMarkerColor);
        stateMarkersRelativeLength = typedArray.getFloat(R.styleable.Knob_kStateMarkersRelativeLength, stateMarkersRelativeLength);
        selectedStateMarkerContinuous = typedArray.getBoolean(R.styleable.Knob_kSelectedStateMarkerContinuous, selectedStateMarkerContinuous);

        animation = typedArray.getBoolean(R.styleable.Knob_kAnimation, animation);
        animationSpeed = typedArray.getFloat(R.styleable.Knob_kAnimationSpeed, animationSpeed);
        animationBounciness = typedArray.getFloat(R.styleable.Knob_kAnimationBounciness, animationBounciness);

        swipeDirection = swipeAttrToInt(typedArray.getString(R.styleable.Knob_kSwipe));
        swipeSensibilityPixels = typedArray.getInt(R.styleable.Knob_kSwipeSensitivityPixels, swipeSensibilityPixels);

        freeRotation = typedArray.getBoolean(R.styleable.Knob_kFreeRotation, freeRotation);
        minAngle = typedArray.getFloat(R.styleable.Knob_kMinAngle, minAngle);
        maxAngle = typedArray.getFloat(R.styleable.Knob_kMaxAngle, maxAngle);

        stateMarkersAccentWidth = typedArray.getDimensionPixelSize(R.styleable.Knob_kStateMarkersAccentWidth, stateMarkersAccentWidth);
        stateMarkersAccentColor = typedArray.getColor(R.styleable.Knob_kStateMarkersAccentColor, stateMarkersAccentColor);
        stateMarkersAccentRelativeLength = typedArray.getFloat(R.styleable.Knob_kStateMarkersAccentRelativeLength, stateMarkersAccentRelativeLength);
        stateMarkersAccentPeriodicity = typedArray.getInt(R.styleable.Knob_kStateMarkersAccentPeriodicity, stateMarkersAccentPeriodicity);

        showBalloonValues = typedArray.getBoolean(R.styleable.Knob_kShowBalloonValues, showBalloonValues);
        balloonValuesTimeToLive = typedArray.getInt(R.styleable.Knob_kBalloonValuesTimeToLive, balloonValuesTimeToLive);
        balloonValuesRelativePosition = typedArray.getFloat(R.styleable.Knob_kBalloonValuesRelativePosition, balloonValuesRelativePosition);
        balloonValuesTextSize = typedArray.getDimension(R.styleable.Knob_kBalloonValuesTextSize, balloonValuesTextSize);
        balloonValuesAnimation = balloonAnimationAttrToInt(typedArray.getString(R.styleable.Knob_kBalloonValuesAnimation));
        balloonValuesArray = typedArray.getTextArray(R.styleable.Knob_kBalloonValuesArray);
        balloonValuesSlightlyTransparent = typedArray.getBoolean(R.styleable.Knob_kBalloonValuesSlightlyTransparent, balloonValuesSlightlyTransparent);

        enabled = typedArray.getBoolean(R.styleable.Knob_kEnabled, enabled);

        typedArray.recycle();
    }
    int swipeAttrToInt(String s) {
        if (s == null) return 2;
        if (s.equals("0")) return 0;
        else if (s.equals("1")) return 1;  // vertical
        else if (s.equals("2")) return 2;  // default  - horizontal
        else if (s.equals("3")) return 3;  // both
        else if (s.equals("4")) return 4;  // circular
        else return 2;
    }
    int balloonAnimationAttrToInt(String s) {
        if (s == null) return 0;
        if (s.equals("0")) return 0;       // pop
        else if (s.equals("1")) return 1;  // scale
        else if (s.equals("2")) return 2;  // fade
        else return 0;
    }

    private void disallowParentToHandleTouchEvents() {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    void initListeners() {

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!enabled) return;
                toggle(animation);
            }
        });

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (!enabled) return false;
                if (swipeDirection == 0) { toggle(animation); return false; }
                int action = motionEvent.getAction();
                if (swipeDirection == 1) {  // vertical
                    int y = (int) motionEvent.getY();
                    if (action == MotionEvent.ACTION_DOWN) {
                        swipeY = y;
                        swipeing = false;
                        disallowParentToHandleTouchEvents(); // needed when Knob's parent is a ScrollView
                    }
                    else if (action == MotionEvent.ACTION_MOVE) {
                        if (y - swipeY > swipeSensibilityPixels) {
                            swipeY = y;
                            swipeing = true;
                            decreaseValue();
                            return true;
                        }
                        else if (swipeY - y > swipeSensibilityPixels) {
                            swipeY = y;
                            swipeing = true;
                            increaseValue();
                            return true;
                        }
                    }
                    else if (action == MotionEvent.ACTION_UP) {
                        if (!swipeing) toggle(animation);    // click
                        return true;
                    }
                    return false;
                }
                else if (swipeDirection == 2) {  // horizontal
                    int x = (int) motionEvent.getX();
                    if (action == MotionEvent.ACTION_DOWN) {
                        swipeX = x;
                        swipeing = false;
                        disallowParentToHandleTouchEvents(); // needed when Knob's parent is a ScrollView
                    }
                    else if (action == MotionEvent.ACTION_MOVE) {
                        if (x - swipeX > swipeSensibilityPixels) {
                            swipeX = x;
                            swipeing = true;
                            increaseValue();
                            return true;
                        }
                        else if (swipeX - x > swipeSensibilityPixels) {
                            swipeX = x;
                            swipeing = true;
                            decreaseValue();
                            return true;
                        }
                    }
                    else if (action == MotionEvent.ACTION_UP) {
                        if (!swipeing) toggle(animation);    // click
                        return true;
                    }
                    return false;
                }
                else if (swipeDirection == 3) {  // both
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();
                    if (action == MotionEvent.ACTION_DOWN) {
                        swipeX = x;
                        swipeY = y;
                        swipeing = false;
                        disallowParentToHandleTouchEvents(); // needed when Knob's parent is a ScrollView
                    }
                    else if (action == MotionEvent.ACTION_MOVE) {
                        if (x - swipeX > swipeSensibilityPixels || swipeY - y > swipeSensibilityPixels ) {
                            swipeX = x;
                            swipeY = y;
                            swipeing = true;
                            increaseValue();
                            return true;
                        }
                        else if (swipeX - x > swipeSensibilityPixels || y - swipeY > swipeSensibilityPixels) {
                            swipeX = x;
                            swipeY = y;
                            swipeing = true;
                            decreaseValue();
                            return true;
                        }
                    }
                    else if (action == MotionEvent.ACTION_UP) {
                        if (!swipeing) toggle(animation);    // click
                        return true;
                    }
                    return false;
                }
                else if (swipeDirection == 4) { // circular
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();
                    if (action == MotionEvent.ACTION_DOWN) {
                        swipeing = false;
                        disallowParentToHandleTouchEvents(); // needed when Knob's parent is a ScrollView
                    }
                    else if (action == MotionEvent.ACTION_MOVE) {
                        double angle = Math.atan2((double)(y-centerY), (double)(x-centerX));
                        swipeing = true;
                        setValueByAngle(angle, animation);
                        return true;
                    }
                    else if (action == MotionEvent.ACTION_UP) {
                        if (!swipeing) toggle(animation);    // click
                        return true;
                    }
                    return false;

                }

                return false;
            }
        });
        spring.addListener(new SimpleSpringListener(){
                               @Override
                               public void onSpringUpdate(Spring spring) {
                                   currentAngle = spring.getCurrentValue();
                                   postInvalidate();
                               }});
    }

    void initStatus() {
        currentState = defaultState;
        previousState = defaultState;
        calcActualState();
        currentAngle = calcAngle(currentState);
        spring.setCurrentValue(currentAngle);
    }

    void initBalloons() {

    }

    // behaviour

    public void toggle(boolean animate) {
        increaseValue(animate);
    }
    public void toggle() {
        toggle(animation);
    }

    private void calcActualState() {
        actualState = currentState % numberOfStates;
        if (actualState < 0) actualState += numberOfStates;
    }

    public void increaseValue(boolean animate) {
        previousState = currentState;
        currentState = (currentState+1); // % numberOfStates;
        if (!freeRotation && currentState >= numberOfStates) currentState = numberOfStates-1;
        calcActualState();
        if(listener != null) listener.onState(actualState);
        takeEffect(animate);
    }
    public void increaseValue() { increaseValue(animation);}

    public void decreaseValue(boolean animate) {
        previousState = currentState;
        currentState = (currentState-1); // % numberOfStates;
        if (!freeRotation && currentState<0) currentState = 0;
        calcActualState();
        if(listener != null) listener.onState(actualState);
        takeEffect(animate);
    }
    public void decreaseValue() { decreaseValue(animation);}

    public void setValueByAngle(double angle, boolean animate) {  // sets the value of the knob given an angle instead of a state
        if (numberOfStates <= 1)
            return;
        previousState = currentState;
        double min = Math.toRadians((double)minAngle);
        double max = Math.toRadians((double)maxAngle - 0.0001);
        double range = max - min;

        double singleStepAngle = range / (numberOfStates);
        if (PI*2 - range < singleStepAngle)
            singleStepAngle = range / numberOfStates;

        min = (float)normalizeAngle(min);
        while (min > max) max += 2*PI;      // both min and max are positive and in the correct order.

        angle = normalizeAngle(angle + PI/2);
        while (angle < min) angle += 2*PI;             // set angle after minangle
        if (angle > max) { // if angle is out of range because the range is limited set to the closer limit
            if (angle - max > min - angle + PI*2)
                angle = min;
            else
                angle = max;
        }

        currentState = (int)((angle - min) / singleStepAngle);   // calculate value
        if (!freeRotation && Math.abs(currentState - previousState) == numberOfStates-1)    // manage free rotation
            currentState = previousState;

        calcActualState();
        if(listener != null) listener.onState(actualState);
        takeEffect(animate);
    }

    private void takeEffect(boolean animate) {
        if (animate) {
            setIndicatorAngleWithDirection();
        } else {
            spring.setCurrentValue(calcAngle(actualState));
        }
        postInvalidate();
    }

    // public listener interface

    private OnStateChanged listener;
    public interface OnStateChanged{
        public void onState(int state);
    }

    public void setOnStateChanged(OnStateChanged onStateChanged) {
        listener = onStateChanged;
    }

    // methods

    public void setState(int newState) {
        setState(newState, animation);
    }
    public void setState(int newState, boolean animate) {
        forceState(newState, animate);
        if(listener != null) listener.onState(currentState);
    }
    public void forceState(int newState) {
        forceState(newState, animation);
    }
    public void forceState(int newState, boolean animate) {
        previousState = currentState;
        currentState = newState;
        calcActualState();
        takeEffect(animate);
    }
    public int getState() {
        return actualState;
    }

    // getters and setters

    public int getNumberOfStates() {
        return numberOfStates;
    }

    public void setNumberOfStates(int numberOfStates) {
        setNumberOfStates(numberOfStates, animation);
    }
    public void setNumberOfStates(int numberOfStates, boolean animate) {
        this.numberOfStates = numberOfStates;
        takeEffect(animate);
    }

    public int getDefaultState() {
        return defaultState;
    }

    public void setDefaultState(int defaultState) {
        this.defaultState = defaultState;
    }

    public int getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        takeEffect(animation);
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        takeEffect(animation);
    }

    public int getIndicatorWidth() {
        return indicatorWidth;
    }

    public void setIndicatorWidth(int indicatorWidth) {
        this.indicatorWidth = indicatorWidth;
        takeEffect(animation);
    }

    public int getIndicatorColor() {
        return indicatorColor;
    }

    public void setIndicatorColor(int indicatorColor) {
        this.indicatorColor = indicatorColor;
        takeEffect(animation);
    }

    public float getIndicatorRelativeLength() {
        return indicatorRelativeLength;
    }

    public void setIndicatorRelativeLength(float indicatorRelativeLength) {
        this.indicatorRelativeLength = indicatorRelativeLength;
        takeEffect(animation);
    }

    public int getKnobColor() {
        return knobColor;
    }

    public void setKnobColor(int knobColor) {
        this.knobColor = knobColor;
        takeEffect(animation);
    }

    public float getKnobRelativeRadius() {
        return knobRelativeRadius;
    }

    public void setKnobRelativeRadius(float knobRelativeRadius) {
        this.knobRelativeRadius = knobRelativeRadius;
        takeEffect(animation);
    }

    public float getKnobCenterRelativeRadius() {
        return knobCenterRelativeRadius;
    }

    public void setKnobCenterRelativeRadius(float knobCenterRelativeRadius) {
        this.knobCenterRelativeRadius = knobCenterRelativeRadius;
        takeEffect(animation);
    }

    public int getKnobCenterColor() {
        return knobCenterColor;
    }

    public void setKnobCenterColor(int knobCenterColor) {
        this.knobCenterColor = knobCenterColor;
        takeEffect(animation);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        takeEffect(animation);
    }

    public boolean isAnimation() {
        return animation;
    }

    public void setAnimation(boolean animation) {
        this.animation = animation;
    }

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    public float getAnimationBounciness() {
        return animationBounciness;
    }

    public void setAnimationBounciness(float animationBounciness) {
        this.animationBounciness = animationBounciness;
    }

    public int getStateMarkersWidth() {
        return stateMarkersWidth;
    }

    public void setStateMarkersWidth(int stateMarkersWidth) {
        this.stateMarkersWidth = stateMarkersWidth;
        takeEffect(animation);
    }

    public int getStateMarkersColor() {
        return stateMarkersColor;
    }

    public void setStateMarkersColor(int stateMarkersColor) {
        this.stateMarkersColor = stateMarkersColor;
        takeEffect(animation);
    }

    public int getSelectedStateMarkerColor() {
        return selectedStateMarkerColor;
    }

    public void setSelectedStateMarkerColor(int selectedStateMarkerColor) {
        this.selectedStateMarkerColor = selectedStateMarkerColor;
        takeEffect(animation);
    }

    public float getStateMarkersRelativeLength() {
        return stateMarkersRelativeLength;
    }

    public void setStateMarkersRelativeLength(float stateMarkersRelativeLength) {
        this.stateMarkersRelativeLength = stateMarkersRelativeLength;
        takeEffect(animation);
    }

    public float getKnobRadius() {
        return knobRadius;
    }

    public void setKnobRadius(float knobRadius) {
        this.knobRadius = knobRadius;
        takeEffect(animation);
    }

    public boolean isFreeRotation() {
        return freeRotation;
    }

    public void setFreeRotation(boolean freeRotation) {
        this.freeRotation = freeRotation;
    }

    public int getSwipeDirection() {
        return swipeDirection;
    }

    public void setSwipeDirection(int swipeDirection) {
        this.swipeDirection = swipeDirection;
    }

    public int getSwipeSensibilityPixels() {
        return swipeSensibilityPixels;
    }

    public void setSwipeSensibilityPixels(int swipeSensibilityPixels) {
        this.swipeSensibilityPixels = swipeSensibilityPixels;
    }

    public int getStateMarkersAccentWidth() {
        return stateMarkersAccentWidth;
    }

    public void setStateMarkersAccentWidth(int stateMarkersAccentWidth) {
        this.stateMarkersAccentWidth = stateMarkersAccentWidth;
        takeEffect(animation);
    }

    public int getStateMarkersAccentColor() {
        return stateMarkersAccentColor;
    }

    public void setStateMarkersAccentColor(int stateMarkersAccentColor) {
        this.stateMarkersAccentColor = stateMarkersAccentColor;
        takeEffect(animation);
    }

    public float getStateMarkersAccentRelativeLength() {
        return stateMarkersAccentRelativeLength;
    }

    public void setStateMarkersAccentRelativeLength(float stateMarkersAccentRelativeLength) {
        this.stateMarkersAccentRelativeLength = stateMarkersAccentRelativeLength;
        takeEffect(animation);
    }

    public int getStateMarkersAccentPeriodicity() {
        return stateMarkersAccentPeriodicity;
    }

    public void setStateMarkersAccentPeriodicity(int stateMarkersAccentPeriodicity) {
        this.stateMarkersAccentPeriodicity = stateMarkersAccentPeriodicity;
        takeEffect(animation);
    }

    public int getKnobDrawableRes() {
        return knobDrawableRes;
    }

    public void setKnobDrawableRes(int knobDrawableRes) {
        this.knobDrawableRes = knobDrawableRes;
        takeEffect(animation);
    }

    public boolean isKnobDrawableRotates() {
        return knobDrawableRotates;
    }

    public void setKnobDrawableRotates(boolean knobDrawableRotates) {
        this.knobDrawableRotates = knobDrawableRotates;
        takeEffect(animation);
    }

    public float getCircularIndicatorRelativeRadius() {
        return circularIndicatorRelativeRadius;
    }

    public void setCircularIndicatorRelativeRadius(float circularIndicatorRelativeRadius) {
        this.circularIndicatorRelativeRadius = circularIndicatorRelativeRadius;
        takeEffect(animation);
    }

    public float getCircularIndicatorRelativePosition() {
        return circularIndicatorRelativePosition;
    }

    public void setCircularIndicatorRelativePosition(float circularIndicatorRelativePosition) {
        this.circularIndicatorRelativePosition = circularIndicatorRelativePosition;
        takeEffect(animation);
    }

    public int getCircularIndicatorColor() {
        return circularIndicatorColor;
    }

    public void setCircularIndicatorColor(int circularIndicatorColor) {
        this.circularIndicatorColor = circularIndicatorColor;
        takeEffect(animation);
    }

    public boolean isSelectedStateMarkerContinuous() {
        return selectedStateMarkerContinuous;
    }

    public void setSelectedStateMarkerContinuous(boolean selectedStateMarkerContinuous) {
        this.selectedStateMarkerContinuous = selectedStateMarkerContinuous;
        takeEffect(animation);
    }

    public float getMinAngle() {
        return minAngle;
    }

    public void setMinAngle(float minAngle) {
        this.minAngle = minAngle;
        takeEffect(animation);
    }

    public float getMaxAngle() {
        return maxAngle;
    }

    public void setMaxAngle(float maxAngle) {
        this.maxAngle = maxAngle;
        takeEffect(animation);
    }

    public float getExternalRadius() {
        return externalRadius;
    }

    public void setExternalRadius(float externalRadius) {
        this.externalRadius = externalRadius;
        takeEffect(animation);
    }

    public Drawable getKnobDrawable() {
        return knobDrawable;
    }

    public void setKnobDrawable(Drawable knobDrawable) {
        this.knobDrawable = knobDrawable;
        takeEffect(animation);
    }

    public boolean isShowBalloonValues() {
        return showBalloonValues;
    }

    public void setShowBalloonValues(boolean showBalloonValues) {
        this.showBalloonValues = showBalloonValues;
    }

    public int getBalloonValuesTimeToLive() {
        return balloonValuesTimeToLive;
    }

    public void setBalloonValuesTimeToLive(int balloonValuesTimeToLive) {
        this.balloonValuesTimeToLive = balloonValuesTimeToLive;
    }

    public float getBalloonValuesRelativePosition() {
        return balloonValuesRelativePosition;
    }

    public void setBalloonValuesRelativePosition(float balloonValuesRelativePosition) {
        this.balloonValuesRelativePosition = balloonValuesRelativePosition;
    }

    public float getBalloonValuesTextSize() {
        return balloonValuesTextSize;
    }

    public void setBalloonValuesTextSize(float balloonValuesTextSize) {
        this.balloonValuesTextSize = balloonValuesTextSize;
    }

    public boolean isBalloonValuesSlightlyTransparent() {
        return balloonValuesSlightlyTransparent;
    }

    public void setBalloonValuesSlightlyTransparent(boolean balloonValuesSlightlyTransparent) {
        this.balloonValuesSlightlyTransparent = balloonValuesSlightlyTransparent;
    }
}
