package com.cch.LrcView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// LrcParser.java
public class LrcParser {
    public static List<LrcLine> parse(String lrcText) {
        List<LrcLine> lines = new ArrayList<>();
        String[] parts = lrcText.split("\n");

        for (String part : parts) {
            if (part.trim().isEmpty()) continue;

            // 匹配时间戳格式 [mm:ss.ms]
            Matcher timeMatcher = Pattern.compile("\\[([0-9]{2}):([0-9]{2})\\.([0-9]{2,3})](.*)").matcher(part);
            if (timeMatcher.matches()) {
                try {
                    int min = Integer.parseInt(Objects.requireNonNull(timeMatcher.group(1)));
                    int sec = Integer.parseInt(Objects.requireNonNull(timeMatcher.group(2)));
                    int ms = Integer.parseInt(Objects.requireNonNull(timeMatcher.group(3)));
                    long time = (min * 60L + sec) * 1000 + ms;
                    String text = Objects.requireNonNull(timeMatcher.group(4)).trim();

                    // 过滤空文本
                    if (!text.isEmpty()) {
                        lines.add(new LrcLine(time, text));
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误的行
                    continue;
                }
            }
        }

        // 按时间排序
        lines.sort((a, b) -> Long.compare(a.getTime(), b.getTime()));

        return lines;
    }

    // 提取元数据的方法
    public static String extractSongName(String lrcText) {
        Pattern pattern = Pattern.compile("\\[ti:(.*?)]");
        Matcher matcher = pattern.matcher(lrcText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "未知歌曲";
    }

    public static String extractArtist(String lrcText) {
        Pattern pattern = Pattern.compile("\\[ar:(.*?)]");
        Matcher matcher = pattern.matcher(lrcText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "未知歌手";
    }
}
