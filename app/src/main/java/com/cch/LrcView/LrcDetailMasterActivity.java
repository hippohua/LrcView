package com.cch.LrcView;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;

import com.hw.lrcviewlib.ILrcViewSeekListener;
import com.hw.lrcviewlib.LrcDataBuilder;
import com.hw.lrcviewlib.LrcRow;
import com.hw.lrcviewlib.LrcView;
import java.io.File;
import java.util.List;

public class LrcDetailMasterActivity extends AppCompatActivity {
    private static final String TAG = "LrcDetailMasterActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        List<String> fileListPaths; // 存储文件路径列表
        int currentFileIndex = 0; // 存储当前文件在列表中的索引
        String folderPath = ""; // 存储目录路径
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lrc_detail_master);
        Intent intent = getIntent();
        folderPath = intent.getStringExtra("folder_path"); // 获取目录路径
        fileListPaths = getIntent().getStringArrayListExtra("file_list");
        currentFileIndex = intent.getIntExtra("current_index", 0); // 获取当前文件索引

        // 检查文件列表是否有效
        if (fileListPaths == null || fileListPaths.isEmpty()) {
            return;
        }
        // 加载新的歌词文件
        File lrcFile = new File(fileListPaths.get(currentFileIndex));

        // 从assets加载歌词文件
//        List<LrcRow> lrcRows = new LrcDataBuilder().BuiltFromAssets(this, "test2.lrc");

        // 或者从本地文件加载
        List<LrcRow> lrcRows = new LrcDataBuilder().Build(lrcFile);

        LrcView mLrcView = findViewById(R.id.au_lrcView);
        mLrcView.setLrcData(lrcRows); // 设置歌词数据
        mLrcView.setTextSizeAutomaticMode(true);//是否自动适配文字大小

        // 可选：自定义歌词样式
        mLrcView.getLrcSetting()
                .setTimeTextSize(40) // 时间字体大小
                .setSelectLineColor(Color.WHITE) // 选中线颜色
                .setNormalRowTextSize((int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP, 17, getResources().getDisplayMetrics())); // 普通行字体大小
        mLrcView.commitLrcSettings(); // 应用设置

//        mLrcView.setLrcViewSeekListener(new ILrcViewSeekListener() {
//            @Override
//            public void onSeek(LrcRow currentLrcRow, long currentTime) {
//                //在这里执行播放器控制器控制播放器跳转到指定时间
//                mController.seekTo((int) CurrentSelectedRowTime);
//            }
//        });
//        // 每秒更新一次当前播放时间
//        Handler mHandler = new Handler();
//        Runnable mUpdateTimeTask = new Runnable() {
//            public void run() {
//                long currentTime = yourMediaPlayer.getCurrentPosition();
//                mLrcView.smoothScrollToTime(currentTime); // 平滑滚动到当前时间
//                mHandler.postDelayed(this, 1000); // 每秒更新
//            }
//        };
//        mHandler.postDelayed(mUpdateTimeTask, 1000);
    }
}
