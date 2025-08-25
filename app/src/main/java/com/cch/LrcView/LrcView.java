package com.cch.LrcView;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

// LrcView.java
public class LrcView extends FrameLayout {
    private TextView currentLine;
    private List<LrcLine> lines;
    private final int currentLineIndex = -1; // 声明为 final
    private long currentTime;

    public LrcView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setPadding(20, 20, 20, 20);
    }

    public void setLrc(List<LrcLine> lines) {
        this.lines = lines;
        removeAllViews();

        // 创建一个垂直布局来存放所有歌词行
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        for (int i = 0; i < lines.size(); i++) {
            TextView tv = new TextView(getContext());
            tv.setText(lines.get(i).getContent());
            tv.setTextSize(16);
            tv.setPadding(20, 10, 20, 10);
            tv.setGravity(Gravity.CENTER);
            layout.addView(tv);
        }

        addView(layout);
    }

    public void updateCurrentTime(long time) {
        for (int i = 0; i < lines.size(); i++) {
            LrcLine line = lines.get(i);
            if (line.getTime() <= time) {
                // 高亮当前行
                highlightLine(i);
            } else {
                break;
            }
        }
    }

    private void highlightLine(int index) {
        for (int i = 0; i < getChildCount(); i++) {
            TextView tv = (TextView) getChildAt(i);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(16);
            tv.setTypeface(Typeface.DEFAULT);
        }
        TextView tv = (TextView) getChildAt(index);
        tv.setTextColor(Color.RED);
        tv.setTextSize(20);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
    }
}
