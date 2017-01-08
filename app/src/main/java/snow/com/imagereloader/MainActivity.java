package snow.com.imagereloader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import snow.com.imagereloader.adapter.ImageAdapter;
import snow.com.imagereloader.bean.FolderBean;

public class MainActivity extends Activity implements View.OnClickListener {


    private GridView mGridView;
    private List<String> mImgs;
    private ImageAdapter adapter;

    private RelativeLayout mRelayout;
    private TextView mDirName;
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;
    private List<FolderBean> mBeans = new ArrayList<>();

    private ProgressDialog mProgressDialog;

    private MyPopUpWindow mPopUpWindow;


    private static final int DATA_LOADING = 0x110;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DATA_LOADING:
                    mProgressDialog.dismiss();
                    data2View();
                    initPopUpWindow();

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();

    }


    private void initEvent() {
        //TODO NullPointerException
        mPopUpWindow.setOnDirSelectListener(new MyPopUpWindow.OnDirSelectListener() {
            @Override
            public void onSelect(FolderBean folderBean) {
                mCurrentDir = new File(folderBean.getDir());
                mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String filename) {

                        return filename.endsWith(".jpg") || filename.endsWith(".png")
                                || filename.endsWith(".jpeg");
                    }
                }));

                adapter = new ImageAdapter(MainActivity.this, mImgs, mCurrentDir.getAbsolutePath());
                mGridView.setAdapter(adapter);

                String name = folderBean.getName().substring(1);
                mDirName.setText(name);
                mDirCount.setText(String.valueOf(mImgs.size()));

                mPopUpWindow.dismiss();
            }
        });

    }

    /**
     * 初始化数据
     * 利用ContentProvider扫描图片
     */
    private void initData() {
        //判断是否有SDCard,没有就给出提示
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this, "没有SDCard!!", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this, null, "正在加载...");

        new Thread() {
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver resolver = MainActivity.this.getContentResolver();
                Cursor cursor = resolver.query(mImgUri, null,
                        MediaStore.Images.Media.MIME_TYPE + "= ? or " +
                                MediaStore.Images.Media.MIME_TYPE + "= ? ",
                        new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED);
                Set<String> mDirPaths = new HashSet<String>();
                while (cursor.moveToNext()) {
                    //图片路径
                    String path = cursor.getString(
                            cursor.getColumnIndex(MediaStore.Images.Media.DATA));

                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null) {
                        continue;
                    }
                    //文件绝对路径
                    String dirPath = parentFile.getAbsolutePath();

                    FolderBean folderBean = null;
                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgPath(path);

                    }
                    if (parentFile.list() == null) {
                        continue;
                    }
                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String filename) {

                            return filename.endsWith(".jpg") || filename.endsWith(".png")
                                    || filename.endsWith(".jpeg");
                        }
                    }).length;
                    folderBean.setCount(picSize);
                    mBeans.add(folderBean);

                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }
                }
                cursor.close();
                mHandler.sendEmptyMessage(DATA_LOADING);
            }
        }.start();
    }

    //绑定数据
    private void data2View() {
        if (mCurrentDir == null) {
            Toast.makeText(this, "没有图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());
        adapter = new ImageAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(adapter);

        mDirName.setText(mCurrentDir.getName());
        mDirCount.setText(String.valueOf(mMaxCount));

    }

    //初始化popUpWindow
    private void initPopUpWindow() {
        mPopUpWindow = new MyPopUpWindow(this, mBeans);

        mPopUpWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        initEvent();
    }

    //初始化控件
    private void initView() {
        mGridView = (GridView) findViewById(R.id.gv_gridView);
        mRelayout = (RelativeLayout) findViewById(R.id.bottom_ly);
        mDirCount = (TextView) findViewById(R.id.tv_dir_count);
        mDirName = (TextView) findViewById(R.id.tv_dir_name);

        mDirName.setOnClickListener(this);
        mDirCount.setOnClickListener(this);
        mRelayout.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_dir_name:

                break;
            case R.id.tv_dir_count:

                break;
            case R.id.bottom_ly:

                mPopUpWindow.showAsDropDown(mRelayout, 0, 0);
                lightOff();
                break;

        }

    }

//    private void popUpAnima() {
//        ObjectAnimator animator = ObjectAnimator.ofFloat(mPopUpWindow, "translationY", 110);
//        animator.setDuration(1200);
//        animator.start();
//    }

    //显示popUpWindow时候,内容背景变暗
    private void lightOff() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 0.3f;
        getWindow().setAttributes(params);


    }

    //显示popUpWindow时候,内容背景变亮
    private void lightOn() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;

        getWindow().setAttributes(params);

    }
}
