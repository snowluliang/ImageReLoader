package snow.com.imagereloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

import snow.com.imagereloader.bean.FolderBean;

public class MyPopUpWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;
    private List<FolderBean> mDatas;

    /**
     * 接口回调方法
     */
    public interface OnDirSelectListener {
        void onSelect(FolderBean folderBean);

    }

    public OnDirSelectListener mListener;

    public void setOnDirSelectListener(OnDirSelectListener listener) {
        this.mListener = listener;
    }


    public MyPopUpWindow(Context context, List<FolderBean> list) {
        calWidthAndHeight(context);
        this.mDatas = list;

        mConvertView = LayoutInflater.from(context)
                .inflate(R.layout.popup_item, null);

        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);//点击边界外消失
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        initView(context);
        initEvent();

    }

    private void initView(Context context) {
        mListView = (ListView) mConvertView.findViewById(R.id.pup_list_view);
        mListView.setAdapter(new ListDirAdapter(context, mDatas));

    }

    private void initEvent() {
        //ListView的点击事件
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mListener != null) {
                    mListener.onSelect(mDatas.get(i));
                }
            }
        });


    }

    //计算高度和宽度
    private void calWidthAndHeight(Context context) {

        WindowManager wm =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        mWidth = metrics.widthPixels;
        mHeight = (int) (metrics.heightPixels * 0.7);
    }

    private class ListDirAdapter extends ArrayAdapter<FolderBean> {

        private LayoutInflater mInflater;
        private List<FolderBean> mDatas;

        public ListDirAdapter(Context context, List<FolderBean> objects) {
            super(context, 0, objects);
            this.mDatas = objects;
            mInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.popup_cell, parent, false);

                holder.mImg = (ImageView) convertView.findViewById(R.id.iv_pup_img);
                holder.mNameText = (TextView) convertView.findViewById(R.id.tv_pup_name);
                holder.mCountText = (TextView) convertView.findViewById(R.id.tv_pup_count);
               // holder.mBox = (CheckBox) convertView.findViewById(R.id.cb_pup_box);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
//            FolderBean bean = mDatas.get(position);
            FolderBean bean = getItem(position);
            //重置图片加载
            holder.mImg.setImageResource(R.drawable.ic_pic_null);

            ImageLoader.getmInstance().loadImage(bean.getFirstImgPath(), holder.mImg);
            holder.mCountText.setText(String.valueOf(bean.getCount()));
            String name = bean.getName().substring(1);
            holder.mNameText.setText(name);

            return convertView;
        }

        private class ViewHolder {
            private ImageView mImg;
            private TextView mNameText;
            private TextView mCountText;
            private CheckBox mBox;

        }
    }

}
