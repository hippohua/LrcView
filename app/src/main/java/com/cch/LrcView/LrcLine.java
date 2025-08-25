package com.cch.LrcView;

public class LrcLine {
    private final long time;
    private final String content;

    public LrcLine(long time, String content) {
        this.time = time;
        this.content = content;
    }

    // Getter 方法
    public long getTime() {
        return time;
    }

    public String getContent() {
        return content;
    }
}
