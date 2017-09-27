package t;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class PercentageLayout extends FrameLayout {
    public PercentageLayout(Context context) {
        super(context);
    }

    public PercentageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PercentageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = (int)(.7 * getMeasuredWidth());

        super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), heightMeasureSpec);
        setMeasuredDimension(w, getMeasuredHeight());
    }
}
