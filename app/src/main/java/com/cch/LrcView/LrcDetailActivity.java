package com.cch.LrcView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;


// LrcDetailActivity.java
public class LrcDetailActivity extends AppCompatActivity {
    private List<String> fileListPaths; // 存储文件路径列表
    private TextView songTitle;
    private TextView lrcText;
    private SeekBar seekBar;
    private ImageButton btnPrev, btnNext, btnPlayPause, btnReturn;

    private List<LrcLine> lines;
    private int currentFileIndex = 0; // 存储当前文件在列表中的索引

    private long currentTime = 0;
    private long totalTime = 0;

    private TextView currentTimeText;
    private TextView totalTimeText;
    private boolean isPlaying = false;
    private final Handler handler = new Handler();
    private static final String TAG = "LrcDetailActivity";
    private String folderPath; // 存储目录路径
    private GestureDetector gestureDetector;
    private boolean isDragging = false;
    private float lastY = 0;
    private long dragStartTime = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lrc_detail);

        initializeViews();
        setupListeners();
        loadLrcData();
        // 初始化手势检测器
        setupGestureDetector();
        // 重写 TextView 的 performClick 方法
        lrcText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 这里可以处理点击事件，如果需要的话
                // 当前我们只需要满足 performClick 的要求
            }
        });

        // 设置歌词区域的手势监听
        lrcText.setOnTouchListener((v, event) -> handleLrcTouch(event));
        // 启动播放
        togglePlayPause();
    }

    private void initializeViews() {
        songTitle = findViewById(R.id.song_title);
//        songArtist = findViewById(R.id.song_artist);
        lrcText = findViewById(R.id.lrc_text);
        seekBar = findViewById(R.id.seek_bar);
        currentTimeText = findViewById(R.id.current_time);  // 添加当前时间显示
        totalTimeText = findViewById(R.id.total_time);      // 添加总时间显示
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnReturn = findViewById(R.id.btn_return);
    }

    private void setupListeners() {
        btnPrev.setOnClickListener(v -> previousLrc());
        btnNext.setOnClickListener(v -> nextLrc());
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnReturn.setOnClickListener(v -> finish());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTime = (long) (progress * totalTime / 1000.0);
                    updateCurrentLine();
                    scrollToCurrentLine();
                    // 更新时间显示
                    currentTimeText.setText(formatTime(currentTime));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 暂停自动更新，避免进度条跳动
                if (isPlaying) {
                    handler.removeCallbacks(playbackRunnable);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 恢复播放（如果之前是播放状态）
                if (isPlaying) {
                    handler.post(playbackRunnable);
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadLrcData() {
        Intent intent = getIntent();
        String lrcContent = intent.getStringExtra("lrc_content");
        String songName = intent.getStringExtra("song_name");
        String artist = intent.getStringExtra("artist");
        fileListPaths = getIntent().getStringArrayListExtra("file_list");
        currentFileIndex = intent.getIntExtra("current_index", 0); // 获取当前文件索引
        folderPath = intent.getStringExtra("folder_path"); // 获取目录路径

        // 设置默认值，把歌曲名和歌手连接成一个字符串
        if (songName != null){
            if (artist != null){
                songTitle.setText(songName + " - " + artist);
            }else {
                songTitle.setText(songName);
            }
        } else {
            songTitle.setText("未知歌曲");
        }

        // 检查 lrcContent 是否为 null
        if (lrcContent == null || lrcContent.trim().isEmpty()) {
            Toast.makeText(this, "无法加载歌词", Toast.LENGTH_SHORT).show();
            return;
        }

        // 解析歌词
        lines = LrcParser.parse(lrcContent);
        totalTime = extractTotalTime(lines);
        seekBar.setMax(1000);
        // 更新时间显示
        updateTimeDisplay();
        // 初始化显示
        updateLrcDisplay();

    }

    // 添加时间格式化方法
    @SuppressLint("DefaultLocale")
    private String formatTime(long timeInMillis) {
        int totalSeconds = (int) (timeInMillis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // 更新时间显示
    private void updateTimeDisplay() {
        currentTimeText.setText(formatTime(currentTime));
        totalTimeText.setText(formatTime(totalTime));
    }


    private void updateLrcDisplay() {
        // 首先清除之前的样式
        lrcText.setText("");
        StringBuilder sb = new StringBuilder();
        // 添加所有歌词行
        for (int i = 0; i < lines.size(); i++) {
            LrcLine line = lines.get(i);
            sb.append(line.getContent());
            sb.append("\n");

        }

        // 设置默认样式
        lrcText.setText(sb);

    }

    private void updateCurrentLine() {
        if (lines == null || lines.isEmpty()) return;

        // 找到当前播放的歌词行
        int currentLineIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            LrcLine line = lines.get(i);
            if (line.getTime() <= currentTime) {
                currentLineIndex = i;
            } else {
                break;
            }
        }

        if (currentLineIndex == -1) return;

        // 获取 TextView 中的文本
        String text = lrcText.getText().toString();
        String[] linesArray = text.split("\n");

        // 计算当前行在文本中的位置
        int start = 0;
        for (int i = 0; i < currentLineIndex; i++) {
            start += linesArray[i].length() + 1; // +1 for newline
        }

        int end = start + linesArray[currentLineIndex].length();

        // 设置样式
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        // 添加字体大小设置（单位：sp）
        spannable.setSpan(new android.text.style.AbsoluteSizeSpan(24, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        lrcText.setText(spannable);


    }

    private void scrollToCurrentLine() {
        if (lrcText == null) return;

        // 获取当前行的位置
        int currentLineIndex = -1;
        LrcLine nextLine = null;

        for (int i = 0; i < lines.size(); i++) {
            LrcLine line = lines.get(i);
            if (line.getTime() <= currentTime) {
                currentLineIndex = i;
            } else {
                if (i > 0 && currentLineIndex == i - 1) {
                    nextLine = line;
                }
                break;
            }
        }

        if (currentLineIndex == -1) return;

        // 获取 TextView 的布局参数
        Layout layout = lrcText.getLayout();
        if (layout == null) return;

        // 计算当前行的顶部和底部位置
        int lineTop = layout.getLineTop(currentLineIndex);
//        int lineBottom = layout.getLineBottom(currentLineIndex);
        // 如果有下一行，进行插值计算实现更平滑的滚动
        if (nextLine != null && currentLineIndex + 1 < lines.size()) {
            LrcLine currentLine = lines.get(currentLineIndex);
            long timeDiff = nextLine.getTime() - currentLine.getTime();
            if (timeDiff > 0) {
                long timePassed = currentTime - currentLine.getTime();
                float progress = (float) timePassed / timeDiff;

                // 计算下一行的顶部位置
                int nextLineTop = layout.getLineTop(currentLineIndex + 1);

                // 插值计算当前应该滚动到的位置
                lineTop = (int) (lineTop + (nextLineTop - lineTop) * progress);
            }
        }

        // 计算屏幕中心位置
        int screenHeight = lrcText.getHeight();
//        int center = screenHeight / 2;
        int targetY = screenHeight / 3;  // 修改这里，从 center 改为 screenHeight /

        // 计算需要滚动的距离
//        int scrollY = lineTop - center + lrcText.getPaddingTop();
        int scrollY = lineTop - targetY + lrcText.getPaddingTop();

        // 滚动到指定位置
        lrcText.scrollTo(0, scrollY);
    }


    private void previousLrc() {
        // 检查文件列表是否有效
        if (fileListPaths == null || fileListPaths.isEmpty()) {
            return;
        }

        // 计算上一个文件的索引（循环播放）
        int previousIndex = (currentFileIndex == 0) ? fileListPaths.size() - 1 : currentFileIndex - 1;

        // 加载新的歌词文件
        File previousFile = new File(fileListPaths.get(previousIndex));
        loadLrcFromFile(previousFile, previousIndex);
    }

    private void nextLrc() {
        // 检查文件列表是否有效
        if (fileListPaths == null || fileListPaths.isEmpty()) {
            return;
        }

        // 计算下一个文件的索引（循环播放）
        int nextIndex = (currentFileIndex == fileListPaths.size() - 1) ? 0 : currentFileIndex + 1;

        // 加载新的歌词文件
        File nextFile = new File(fileListPaths.get(nextIndex));
        loadLrcFromFile(nextFile,nextIndex);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void loadLrcFromFile(File file, int newIndex) {
        try {
            // 读取文件内容
            String lrcContent = readFileContent(file);

            // 更新当前索引
            currentFileIndex = newIndex;

            // 更新歌词数据
            lines = LrcParser.parse(lrcContent);
            totalTime = extractTotalTime(lines);

            // 重置播放状态
            currentTime = 0;
            seekBar.setProgress(0);
            // 更新时间显示
            updateTimeDisplay();

            // 更新界面显示
            updateLrcDisplay();
            // 重写 TextView 的 performClick 方法
            lrcText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 这里可以处理点击事件，如果需要的话
                    // 当前我们只需要满足 performClick 的要求
                }
            });

            // 设置歌词区域的手势监听
            lrcText.setOnTouchListener((v, event) -> handleLrcTouch(event));

            // 更新歌曲标题
            songTitle.setText(file.getName());

            // 重新开始播放
            if (isPlaying) {
                stopPlayback();
                startPlayback(); // 然后重新开始播放
            } else {
                // 如果之前是暂停状态，保持暂停状态
                btnPlayPause.setImageResource(R.drawable.ic_play);
            }

            Toast.makeText(this, "正在播放: " + file.getName(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "无法加载歌词文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    private String readFileContent(File file) {
        StringBuilder content = new StringBuilder();
        try {
            // 使用 DocumentFile API 读取文件
            DocumentFile root = DocumentFile.fromTreeUri(this, Uri.parse(folderPath));
            if (root != null && root.isDirectory()) {
                DocumentFile lrcFile = root.findFile(file.getName());
                if (lrcFile != null && lrcFile.isFile()) {
                    InputStream inputStream = getContentResolver().openInputStream(lrcFile.getUri());
                    if (inputStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                        reader.close();
                        inputStream.close();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading file: " + file.getName(), e);
        }
        return content.toString();
    }


    private void togglePlayPause() {
        isPlaying = !isPlaying;
        if (isPlaying) {
            startPlayback();
        } else {
            stopPlayback();
        }
    }

    private void startPlayback() {
        // 更新播放按钮图标
        btnPlayPause.setImageResource(R.drawable.ic_pause); // 使用暂停图标
        // 开始播放
        handler.post(playbackRunnable);
    }

    private void stopPlayback() {
        // 更新播放按钮图标
        btnPlayPause.setImageResource(R.drawable.ic_play); // 使用播放图标
        // 停止播放
        handler.removeCallbacks(playbackRunnable);
    }

    private final Runnable playbackRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                currentTime += 50;
                if (currentTime >= totalTime) {
                    isPlaying = false;
                    stopPlayback();
                } else {
                    // 更新进度条
                    int progress = (int) (currentTime * 1000 / totalTime);
                    seekBar.setProgress(progress);
                    // 更新时间显示
                    currentTimeText.setText(formatTime(currentTime));
                    // 只更新当前行的样式
                    updateCurrentLine();
                    // 自动滚动到当前行
                    scrollToCurrentLine();
                    handler.postDelayed(this, 50);
                }
            }
        }
    };

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                // 处理滚动手势
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // 处理快速滑动手势
                return true;
            }
        });
    }

    private boolean handleLrcTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 用户开始触摸歌词区域
                lastY = event.getY();
                dragStartTime = System.currentTimeMillis();

                // 暂停自动播放
                if (isPlaying) {
                    handler.removeCallbacks(playbackRunnable);
                }
                isDragging = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    // 计算垂直移动距离
                    float deltaY = event.getY() - lastY;
                    lastY = event.getY();

                    // 根据移动距离调整时间
                    // 向上滑动是负值（时间前进），向下滑动是正值（时间后退）
                    long timeChange = (long) (-deltaY * 15); // 调整灵敏度

                    // 更新当前时间
                    currentTime = Math.max(0, Math.min(totalTime, currentTime + timeChange));

                    // 更新UI
                    updateCurrentLine();
                    scrollToCurrentLine();
                    updateTimeDisplay();

                    // 更新进度条
                    int progress = (int) (currentTime * 1000 / totalTime);
                    seekBar.setProgress(progress);

                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                // 添加 performClick 调用以解决警告
                lrcText.performClick();
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    long dragDuration = System.currentTimeMillis() - dragStartTime;

                    // 如果是快速拖动，跳转到指定时间
                    if (dragDuration < 1000) { // 拖动时间小于1秒认为是快速拖动
                        // 跳转到指定时间
                        jumpToTime(currentTime);
                    }

                    // 恢复播放（如果之前是播放状态）
                    if (isPlaying) {
                        handler.post(playbackRunnable);
                    }
                }
                return true;
        }

        // 让手势检测器处理其他手势
        return gestureDetector.onTouchEvent(event);
    }

    // 跳转到指定时间
    private void jumpToTime(long targetTime) {
        currentTime = targetTime;

        // 更新UI
        updateCurrentLine();
        scrollToCurrentLine();
        updateTimeDisplay();

        // 更新进度条
        int progress = (int) (currentTime * 1000 / totalTime);
        seekBar.setProgress(progress);

        Toast.makeText(this, "跳转到: " + formatTime(currentTime), Toast.LENGTH_SHORT).show();
    }



    private long extractTotalTime(List<LrcLine> lines) {
        long maxTime = 0;
        for (LrcLine line : lines) {
            if (line.getTime() > maxTime) {
                maxTime = line.getTime();
            }
        }
        return maxTime;
    }
}
