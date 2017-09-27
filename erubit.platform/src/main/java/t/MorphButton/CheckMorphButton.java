package t.MorphButton;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

import a.erubit.platform.R;

public class CheckMorphButton extends MorphingButton {
    private int mWidth = 0;

    public static final int DURATION = 200;

    public State state = State.AVAILABLE;
    private int mDpMargin = 8;
    private int mPixelMargin;

    public enum State {
        AVAILABLE,
        CHECKED,
        LOCKED
    }

    public CheckMorphButton(Context context) {
        super(context);
    }

    public CheckMorphButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckMorphButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int dimen(@DimenRes int resId) {
        return (int) getResources().getDimension(resId);
    }

    public int color(@ColorRes int resId) {
        return ContextCompat.getColor(getContext(), resId);
    }

    public void initialize(boolean active) {
        final float scale = getResources().getDisplayMetrics().density;
        mPixelMargin =  (int)(mDpMargin * scale + 0.5f);

        mWidth = ((View)getParent()).getWidth();
        if (active) {
            morphToActivated(0);
        } else {
            morphToAvailable(0);
        }
    }

    public void morphToAvailable(int duration) {
        int height = getHeight();
        @SuppressWarnings("SuspiciousNameCombination")
        MorphingButton.Params square = MorphingButton.Params.create()
                .duration(duration)
                .cornerRadius(height)
                .width(height - 2 * mPixelMargin)
                .height(height)
                .margin(mPixelMargin)
                .color(color(R.color.color_chkbtn_inactive))
                .colorPressed(color(R.color.color_chkbtn_pressed));
        morph(square);
        state = State.AVAILABLE;
    }

    public void morphToActivated(int duration) {
        MorphingButton.Params circle = MorphingButton.Params.create()
                .duration(duration)
                .cornerRadius(0)
                .width(mWidth)
                .height(getHeight() + 2 * mPixelMargin)
                .margin(0)
                .color(color(R.color.color_chkbtn_active))
                .colorPressed(color(R.color.color_chkbtn_pressed));
        morph(circle);
        state = State.CHECKED;
    }

    public void morphToLocked(int duration) {
        int height = getHeight();
        @SuppressWarnings("SuspiciousNameCombination")
        MorphingButton.Params circle = MorphingButton.Params.create()
                .duration(duration)
                .cornerRadius(height)
                .width(height)
                .height(height)
                .margin(mPixelMargin)
                .color(color(R.color.color_red))
                .colorPressed(color(R.color.color_red_dark));
        morph(circle);
        state = State.LOCKED;
    }

}
