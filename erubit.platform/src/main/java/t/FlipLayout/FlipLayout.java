package t.FlipLayout;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public class FlipLayout extends FrameLayout
        implements Animation.AnimationListener, View.OnClickListener, FlipLayoutOnSwipeListener {
    public static final int ANIM_DURATION_MILLIS = 250;
    private static final Interpolator fDefaultInterpolator = new DecelerateInterpolator();
    private OnFlipListener listener;
    private FlipAnimator animator;
    private boolean isFlipped;
    private Direction direction;
    private OnSwipeTouchListener touchListener;
    private View frontView, backView;

    public FlipLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public FlipLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FlipLayout(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        animator = new FlipAnimator();
        animator.setAnimationListener(this);
        animator.setInterpolator(fDefaultInterpolator);
        animator.setDuration(ANIM_DURATION_MILLIS);
        direction = Direction.DOWN;
        setSoundEffectsEnabled(true);
        touchListener = new OnSwipeTouchListener(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (getChildCount() > 2) {
            throw new IllegalStateException("FlipLayout can host only two direct children");
        }

        frontView = getChildAt(0);
        frontView.setOnTouchListener(touchListener);
        frontView.setOnClickListener(this);
        backView = getChildAt(1);
        backView.setOnTouchListener(touchListener);
        backView.setOnClickListener(this);
        touchListener.addSwipeListener(this);
        reset();
    }

    private void toggleView() {
        if (frontView == null || backView == null) {
            return;
        }

        if (isFlipped) {
            frontView.setVisibility(View.VISIBLE);
            backView.setVisibility(View.GONE);
        } else {
            frontView.setVisibility(View.GONE);
            backView.setVisibility(View.VISIBLE);
        }

        isFlipped = !isFlipped;
    }

    public void setOnFlipListener(OnFlipListener listener) {
        this.listener = listener;
    }

    public void reset() {
        isFlipped = false;
        direction = Direction.DOWN;
        frontView.setVisibility(View.VISIBLE);
        backView.setVisibility(View.GONE);
    }

    public void toggleUp() {
        direction = Direction.DOWN;
        startAnimation();
    }

    public void toggleDown() {
        direction = Direction.UP;
        startAnimation();
    }

    public void toggleLeft() {
        direction = Direction.LEFT;
        startAnimation();
    }

    public void toggleRight() {
        direction = Direction.RIGHT;
        startAnimation();
    }

    public void startAnimation() {
        animator.setVisibilitySwapped();
        startAnimation(animator);
    }

    public boolean getIsFlipped() {
        return isFlipped;
    }

    @Override
    public void onAnimationStart(Animation animation) {
        if (listener != null) {
            listener.onFlipStart(this);
        }
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (listener != null) {
            listener.onFlipEnd(this);
        }
        if (direction == Direction.UP) direction = Direction.DOWN;
        if (direction == Direction.DOWN) direction = Direction.UP;
        if (direction == Direction.LEFT) direction = Direction.RIGHT;
        if (direction == Direction.RIGHT) direction = Direction.LEFT;
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    public void setAnimationListener(Animation.AnimationListener listener) {
        animator.setAnimationListener(listener);
    }

    @Override
    public void onClick(View view) {
        toggleDown();
    }

    @Override
    public void onSwipeLeft() {
        toggleLeft();
    }

    @Override
    public void onSwipeRight() {
        toggleRight();
    }

    @Override
    public void onSwipeUp() {
        toggleUp();
    }

    @Override
    public void onSwipeDown() {
        toggleDown();
    }

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public interface OnFlipListener {

        void onFlipStart(FlipLayout view);

        void onFlipEnd(FlipLayout view);
    }

    public class FlipAnimator extends Animation {

        private static final float EXPERIMENTAL_VALUE = 400.f;
        private Camera camera;
        private float centerX;
        private float centerY;
        private boolean visibilitySwapped;

        public FlipAnimator() {
            setFillAfter(true);
        }

        public void setVisibilitySwapped() {
            visibilitySwapped = false;
        }

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
            camera = new Camera();
            this.centerX = width / 2;
            this.centerY = height / 2;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            // Angle around the y-axis of the rotation at the given time. It is
            // calculated both in radians and in the equivalent degrees.
            final double radians = Math.PI * interpolatedTime;

            float degrees = (float) (180.0 * radians / Math.PI);

            if (direction == Direction.UP || direction == Direction.LEFT) {
                degrees = -degrees;
            }

            // Once we reach the midpoint in the animation, we need to hide the
            // source view and show the destination view. We also need to change
            // the angle by 180 degrees so that the destination does not come in
            // flipped around. This is the main problem with SDK sample, it does
            // not
            // do this.
            if (interpolatedTime >= 0.5f) {
                switch (direction) {
                    case LEFT:
                    case UP:
                        degrees += 180.f;
                        break;
                    case RIGHT:
                    case DOWN:
                        degrees -= 180.f;
                        break;
                }

                if (!visibilitySwapped) {
                    toggleView();
                    visibilitySwapped = true;
                }
            }

            final Matrix matrix = t.getMatrix();

            camera.save();
            //you can delete this line, it move camera a little far from view and get back
            camera.translate(0.0f, 0.0f, (float) (EXPERIMENTAL_VALUE * Math.sin(radians)));
            switch (direction) {
                case DOWN:
                case UP:
                    camera.rotateX(degrees);
                    camera.rotateY(0);
                    break;
                case LEFT:
                case RIGHT:
                    camera.rotateY(degrees);
                    camera.rotateX(0);
                    break;
            }
            camera.rotateZ(0);
            camera.getMatrix(matrix);
            camera.restore();

            matrix.preTranslate(-centerX, -centerY);
            matrix.postTranslate(centerX, centerY);
        }
    }

    public static class OnSwipeTouchListener implements OnTouchListener {

        private final GestureDetector gestureDetector;
        public List<FlipLayoutOnSwipeListener> swipeListeners = new ArrayList<>();

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context.getApplicationContext(), new GestureListener());
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        public void addSwipeListener(FlipLayoutOnSwipeListener swipeListener) {
            if (swipeListener != null && !swipeListeners.contains(swipeListener)) {
                swipeListeners.add(swipeListener);
            }
        }

        public void removeSwipeListener(FlipLayoutOnSwipeListener swipeListener) {
            if (swipeListener != null && swipeListeners.contains(swipeListener)) {
                swipeListeners.remove(swipeListener);
            }
        }

        public void removeSwipeListeners() {
            swipeListeners.clear();
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                for (FlipLayoutOnSwipeListener swipeListener : swipeListeners) {
                                    swipeListener.onSwipeRight();
                                }
                            } else {
                                for (FlipLayoutOnSwipeListener swipeListener : swipeListeners) {
                                    swipeListener.onSwipeLeft();
                                }
                            }
                        }
                        result = true;
                    } else if (Math.abs(diffY) > SWIPE_THRESHOLD
                            && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            for (FlipLayoutOnSwipeListener swipeListener : swipeListeners) {
                                swipeListener.onSwipeDown();
                            }
                        } else {
                            for (FlipLayoutOnSwipeListener swipeListener : swipeListeners) {
                                swipeListener.onSwipeUp();
                            }
                        }
                    }
                    result = true;
                } catch (Exception exception) {
                    exception.printStackTrace();
                }

                return result;
            }
        }
    }
}
