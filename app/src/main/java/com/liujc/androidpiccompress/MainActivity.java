package com.liujc.androidpiccompress;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.bither.util.NativeUtil;

import java.io.File;

import jpegcompress.FileUtils;

public class MainActivity extends AppCompatActivity {

    private TextView fileSize;
    private TextView imageSize;
    private TextView thumbFileSize;
    private TextView thumbImageSize;
    private ImageView image;
    public static final int REQUEST_PICK_IMAGE = 10011;
    public static final int REQUEST_KITKAT_PICK_IMAGE = 10012;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        fileSize = (TextView) findViewById(R.id.file_size);
        imageSize = (TextView) findViewById(R.id.image_size);
        thumbFileSize = (TextView) findViewById(R.id.thumb_file_size);
        thumbImageSize = (TextView) findViewById(R.id.thumb_image_size);
        image = (ImageView) findViewById(R.id.image);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickFromGallery();
            }
        });
    }
    public void pickFromGallery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"),
                    REQUEST_PICK_IMAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_KITKAT_PICK_IMAGE);
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {

                case REQUEST_PICK_IMAGE:
                    if (data != null) {
                        Uri uri = data.getData();
                        String imgUrl = FileUtils.getPathByUri(MainActivity.this,uri);
                        File imgFile = new File(imgUrl);
                        fileSize.setText(imgFile.length() / 1024 + "k");
                        imageSize.setText(FileUtils.getImageSize(imgUrl)[0] + " * " + FileUtils.getImageSize(imgUrl)[1]);
                        compressImage(imgUrl);
                    } else {
                        Log.e("androidpiccompress", "=====图片为空=====");
                    }
                    break;
                case REQUEST_KITKAT_PICK_IMAGE:
                    if (data != null) {
                        Uri uri = ensureUriPermission(this, data);
                        String imgUrl = FileUtils.getPathByUri(MainActivity.this,uri);
                        File imgFile = new File(imgUrl);
                        fileSize.setText(imgFile.length() / 1024 + "k");
                        imageSize.setText(FileUtils.getImageSize(imgUrl)[0] + " * " + FileUtils.getImageSize(imgUrl)[1]);
                        compressImage(imgUrl);
                    } else {
                        Log.e("androidpiccompress", "=====图片为空=====");
                    }
                    break;
            }
        }
    }
    @SuppressWarnings("ResourceType")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Uri ensureUriPermission(Context context, Intent intent) {
        Uri uri = intent.getData();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final int takeFlags = intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
        }
        return uri;
    }

    public String getSDPath() {
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            return Environment.getExternalStorageDirectory().toString();
        } else {
            return "";
        }
    }

    public String getPicPath() {
        String sdCardPath = getSDPath();
        String picUrl = "";
        if (TextUtils.isEmpty(sdCardPath)) {
//            return "";
        } else {
            picUrl = sdCardPath + File.separator + "PicCompress"
                    + File.separator + "pic";
        }
        File file = new File(picUrl);
        if (!file.exists()){
            file.mkdirs();
        }
        return picUrl;
    }

    //    public void compressImage(Uri uri) {
    public void compressImage(String imageUrl) {
        Log.e("androidpiccompress", "====开始====imageUrl==" + imageUrl);
        File saveFile = new File(getPicPath(), "compress_" + System.currentTimeMillis() + ".jpg");

        Log.e("androidpiccompress", "====开始==压缩==saveFile==" + saveFile.getAbsolutePath());
        NativeUtil.compressBitmap(imageUrl, saveFile.getAbsolutePath());
        Log.e("androidpiccompress", "====完成==压缩==saveFile==" + saveFile.getAbsolutePath());
        File imgFile = new File(saveFile.getAbsolutePath());
        thumbFileSize.setText(imgFile.length() / 1024 + "k");
        thumbImageSize.setText(FileUtils.getImageSize(saveFile.getAbsolutePath())[0] + " * "
                + FileUtils.getImageSize(saveFile.getAbsolutePath())[1]);
        Glide.with(MainActivity.this).load(imgFile).into(image);
    }
}
