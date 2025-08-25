package com.cch.LrcView;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// MainActivity.java
public class MainActivity extends AppCompatActivity {
    ListView lrcList;
    SwipeRefreshLayout swipeRefreshLayout; // 添加下拉刷新布局
    private TextView tvCurrentFolder; // 添加当前文件夹显示文本
    private LrcFileAdapter adapter;
    List<File> lrcFiles = new ArrayList<>();
    private String selectedFolderPath;
    private ActivityResultLauncher<Intent> folderSelectorLauncher;
    private static final String PREFS_NAME = "LrcPrefs";
    private static final String KEY_LRC_PATH = "lrc_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lrcList = findViewById(R.id.lrc_list);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout); // 初始化下拉刷新布局
        Button btnSelectFolder = findViewById(R.id.btn_select_folder);
        tvCurrentFolder = findViewById(R.id.tv_current_folder); // 初始化当前文件夹
        // 初始化 SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // 获取之前保存的路径
        selectedFolderPath = prefs.getString(KEY_LRC_PATH, null);
        //打印路径
        Log.d("MainActivity", "获取之前保存的路径: " + selectedFolderPath);
        // 显示当前选择的文件夹路径
        updateCurrentFolderDisplay();


        // 初始化适配器
        adapter = new LrcFileAdapter(this, lrcFiles);
        lrcList.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= 30){
            if (!Environment.isExternalStorageManager()){
                Intent getPermission = new Intent();
                getPermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(getPermission);
            }
        }

        // 设置选择文件夹按钮点击事件
        btnSelectFolder.setOnClickListener(v -> selectLrcFolder());

        // 设置下拉刷新监听器
        setupSwipeRefresh();

        // 检查是否有保存的路径
        if (selectedFolderPath != null && !selectedFolderPath.isEmpty()) {
            Log.d("MainActivity", "已经有路径，尝试加载文件 ");
            // 直接尝试加载文件，让 loadLrcFiles 处理权限问题
            loadLrcFiles();
        } else {
            // 没有保存的路径，提示用户选择
            Toast.makeText(this, "请选择文件夹", Toast.LENGTH_SHORT).show();
//            selectLrcFolder();
        }

        // 设置列表项点击事件
        lrcList.setOnItemClickListener((parent, view, position, id) -> {
            File file = lrcFiles.get(position);
            openLrcDetail(file);
        });



        folderSelectorLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    Uri treeUri = data.getData();
                    if (treeUri != null) {
                        getContentResolver().takePersistableUriPermission(treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        // 重要：保存 treeUri 而不是 documentFile.getUri()
                        String folderPath = treeUri.toString(); // 直接保存 treeUri
                        // 保存路径到 SharedPreferences
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(KEY_LRC_PATH, folderPath);
                        editor.apply();
                        Log.d("MainActivity", "保存的路径: " + folderPath);
                        // 更新当前选择的文件夹显示
                        selectedFolderPath = folderPath;
                        updateCurrentFolderDisplay();
                        loadLrcFiles();
                    } else{
                        String folderPath = treeUri.getPath();
                        if (folderPath != null && !folderPath.isEmpty()) {
                            getContentResolver().takePersistableUriPermission(treeUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                            // 保存路径到 SharedPreferences
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(KEY_LRC_PATH, folderPath);
                            editor.apply();
                            Log.d("MainActivity", "treeUri.getPath()保存的路径: " + folderPath);
                            // 更新当前选择的文件夹显示
                            selectedFolderPath = folderPath;
                            updateCurrentFolderDisplay();
                            loadLrcFiles();
                        }

                    }


                }
            }
        });
    }

    // 更新当前文件夹显示
    private void updateCurrentFolderDisplay() {
        if (selectedFolderPath != null && !selectedFolderPath.isEmpty()) {
            // 尝试从 URI 中提取文件夹名称
            try {
                Uri folderUri = Uri.parse(selectedFolderPath);
                String folderName = getFolderNameFromUri(folderUri);
                tvCurrentFolder.setText(folderName);
            } catch (Exception e) {
                // 如果解析失败，显示完整路径
                tvCurrentFolder.setText(selectedFolderPath);
            }
        } else {
            tvCurrentFolder.setText("未选择文件夹");
        }
    }

    // 从 URI 中提取文件夹名称
    private String getFolderNameFromUri(Uri folderUri) {
        String path = folderUri.toString();

        // 处理 DocumentFile URI 格式
        if (path.contains("/tree/")) {
            // 提取文件夹部分
            String folderPart = path.substring(path.indexOf("/tree/") + 6);
            if (folderPart.contains(":")) {
                folderPart = folderPart.substring(folderPart.indexOf(":") + 1);
            }

            // 将 % 编码转换为正常字符
            try {
                folderPart = java.net.URLDecoder.decode(folderPart, "UTF-8");
            } catch (Exception e) {
                // 忽略解码错误
            }

            // 获取最后一级文件夹名称
            if (folderPart.contains("/")) {
                folderPart = folderPart.substring(folderPart.lastIndexOf("/") + 1);
            }

            return folderPart;
        }

        return path;
    }

    private void setupSwipeRefresh() {
        // 设置下拉刷新监听器
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 执行刷新操作
                refreshFileList();
            }
        });

        // 设置进度圈颜色
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    private void refreshFileList() {
        // 如果已有文件夹路径，则重新加载文件列表
        if (selectedFolderPath != null && !selectedFolderPath.isEmpty()) {
            loadLrcFiles();
        } else {
            // 如果没有选择文件夹，提示用户选择
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "请先选择文件夹", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectLrcFolder() {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE);
        try {
            folderSelectorLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "请检查是否已安装文件管理器", Toast.LENGTH_SHORT).show();
        }


    }

    private void loadLrcFiles() {
        // 显示刷新动画
        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }
        try {
            // 检查路径是否有效
            if (selectedFolderPath == null || selectedFolderPath.isEmpty()) {
                Toast.makeText(this, "请先选择歌词文件夹", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                return;
            }
            // 兼容旧版本保存的错误路径
            Uri folderUri;
            if (selectedFolderPath.contains("/document/")) {
                // 这是文件URI，需要转换为树URI
                folderUri = Uri.parse(selectedFolderPath);
                // 提取树URI部分
                String uriString = folderUri.toString();
                if (uriString.contains("/tree/")) {
                    folderUri = Uri.parse(uriString.substring(0, uriString.indexOf("/document/")));
                } else {
                    Toast.makeText(this, "路径格式不正确，请重新选择文件夹", Toast.LENGTH_SHORT).show();
                    requestFolderAccessPermission();
                    swipeRefreshLayout.setRefreshing(false);
                    return;
                }
            } else {
                folderUri = Uri.parse(selectedFolderPath);
            }
            // 使用 DocumentFile API 统一处理所有类型的路径
//            DocumentFile root = DocumentFile.fromTreeUri(this, Uri.parse(selectedFolderPath));
            DocumentFile root = DocumentFile.fromTreeUri(this, folderUri);
            if (root != null && root.isDirectory()) {
                // 检查是否有访问权限
                if (checkFolderAccessPermission(root)) {
                    // 有权限，加载文件列表
                    loadLrcFilesInternal(root);
                } else {
                    // 没有权限，重新请求权限
                    requestFolderAccessPermission();
                }
            } else {
                Toast.makeText(this, "无效的文件夹路径", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "加载文件失败", e);
            Toast.makeText(this, "加载文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();

        } finally {
            // 确保刷新动画停止
            swipeRefreshLayout.setRefreshing(false);
        }
    }
    private boolean checkFolderAccessPermission(DocumentFile folder) {
        try {
            // 尝试访问文件列表来验证权限
            DocumentFile[] files = folder.listFiles();
            return files.length > 0;
        } catch (SecurityException e) {
            return false; // 没有权限
        }
    }

    private void requestFolderAccessPermission() {
        Toast.makeText(this, "需要文件夹访问权限，请重新选择文件夹", Toast.LENGTH_SHORT).show();
        selectLrcFolder();
    }

    private void loadLrcFilesInternal(DocumentFile root) {
        DocumentFile[] files = root.listFiles();
        List<File> lrcFilesTemp = new ArrayList<>();
        for (DocumentFile file : files) {
            if (file.isFile() && file.getName() != null && file.getName().toLowerCase().endsWith(".lrc")) {
                lrcFilesTemp.add(new File(file.getName()));
            }
        }
        lrcFiles.clear();
        lrcFiles.addAll(lrcFilesTemp);
        adapter.notifyDataSetChanged();

        // 如果没有找到歌词文件，给出提示
        if (lrcFiles.isEmpty()) {
            Toast.makeText(this, "该文件夹中没有找到.lrc文件", Toast.LENGTH_SHORT).show();
        }
        // 停止刷新动画
        swipeRefreshLayout.setRefreshing(false);
    }


    private void openLrcDetail(File file) {
        try {
            // 获取文件在列表中的位置
            int position = lrcFiles.indexOf(file);

            // 如果找不到文件，position 会是 -1
            if (position == -1) {
                Toast.makeText(this, "文件不存在于列表中", Toast.LENGTH_SHORT).show();
                return;
            }
            // 获取文件名
            String fileName = file.getName();

            // 使用 DocumentFile API 统一处理所有类型的路径
            DocumentFile root = DocumentFile.fromTreeUri(this, Uri.parse(selectedFolderPath));
            //打印路径
//            Log.d("MainActivity", "selectedFolderPath: " + selectedFolderPath);
            if (root != null && root.isDirectory()) {
                DocumentFile lrcFile = root.findFile(fileName);
                //打印lrcFile
//                Log.d("MainActivity", "lrcFile: " + lrcFile);
                if (lrcFile != null && lrcFile.isFile()) {
                    // 使用 ContentResolver 读取文件内容
                    InputStream inputStream = getContentResolver().openInputStream(lrcFile.getUri());
                    if (inputStream != null) {
                        StringBuilder content = new StringBuilder();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                        reader.close();
                        inputStream.close();

                        // 提取歌曲信息
                        String songName = extractSongName(content.toString());
                        //打印歌曲信息
                        Log.d("MainActivity", "songName: " + songName);
                        String artist = extractArtist(content.toString());

                        // 启动详情页面
                        Intent intent = new Intent(MainActivity.this, LrcDetailActivity.class);
                        intent.putExtra("folder_path", selectedFolderPath);
                        intent.putExtra("lrc_content", content.toString());
                        intent.putExtra("song_name", songName);
                        intent.putExtra("artist", artist);
                        // 在启动 LrcDetailActivity 的 Intent 中添加
                        intent.putExtra("current_index", position); // 当前文件在列表中的位置
                        // 需要将 lrcFiles 列表转换为路径字符串列表进行传递
                        ArrayList<String> filePaths = new ArrayList<>();
                        for (File file2 : lrcFiles) {
                            filePaths.add(file2.getAbsolutePath());
                        }
                        intent.putStringArrayListExtra("file_list", filePaths);
                        startActivity(intent);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "读取失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String extractSongName(String content) {
        Pattern pattern = Pattern.compile("\\[ti:(.*?)]");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "未知歌曲";
    }

    private String extractArtist(String content) {
        Pattern pattern = Pattern.compile("\\[ar:(.*?)]");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "未知歌手";
    }
}
