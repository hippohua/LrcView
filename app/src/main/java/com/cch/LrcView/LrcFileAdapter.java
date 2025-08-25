package com.cch.LrcView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.List;

// LrcFileAdapter.java
public class LrcFileAdapter extends BaseAdapter {
    private final Context context;
    private final List<File> files;

    public LrcFileAdapter(Context context, List<File> files) {
        this.context = context;
        this.files = files;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public Object getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_lrc_file, parent, false);
        }
        TextView tv = convertView.findViewById(R.id.tv_filename);
        tv.setText(files.get(position).getName());
        return convertView;
    }
}

