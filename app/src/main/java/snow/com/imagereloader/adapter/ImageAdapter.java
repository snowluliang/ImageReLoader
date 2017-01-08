package snow.com.imagereloader.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import snow.com.imagereloader.ImageLoader;
import snow.com.imagereloader.R;

/**
 * Created by snow on 2016/12/12.
 * GridView的适配器
 */

public class ImageAdapter extends BaseAdapter {

    private List<String> mImgs;
    private String mPath;
    private Context context;
    private LayoutInflater mInflater;

    private static Set<String> mIngSelect = new HashSet<>();


    public ImageAdapter(Context context, List<String> mImgs, String mPath) {
        this.context = context;
        this.mImgs = mImgs;
        this.mPath = mPath;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mImgs.size();
    }

    @Override
    public Object getItem(int i) {
        return mImgs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        final ViewHolder holder;
        if (view == null) {
            view = mInflater.inflate(R.layout.grid_view_cell, viewGroup, false);

            holder = new ViewHolder();
            holder.mImgSelect = (ImageButton) view.findViewById(R.id.ib_img_select);
            holder.mIngBg = (ImageView) view.findViewById(R.id.grid_img_cell);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.mImgSelect.setImageResource(R.drawable.ic_cloud);
        holder.mIngBg.setImageResource(R.drawable.ic_pic_null);

//       if (holder.mIngBg.getColorFilter() != null) {
////        }

         holder.mIngBg.setColorFilter(null);

        ImageLoader.getmInstance(3, ImageLoader.Type.LIFO)
                .loadImage(mPath + "/" + mImgs.get(position), holder.mIngBg);
        final String filePath = mPath + "/" + mImgs.get(position);

        holder.mIngBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIngSelect.contains(filePath)) {
                    mIngSelect.remove(filePath);
                    holder.mIngBg.setColorFilter(null);
                    holder.mImgSelect.setImageResource(R.drawable.ic_cloud);

                } else {
                    mIngSelect.add(filePath);
                    holder.mIngBg.setColorFilter(Color.parseColor("#77000000"));
                    holder.mImgSelect.setImageResource(R.drawable.ic_cloud_choose);
                }
            }

        });
        if (mIngSelect.contains(filePath)) {
            holder.mIngBg.setColorFilter(Color.parseColor("#77000000"));
            holder.mImgSelect.setImageResource(R.drawable.ic_cloud_choose);
        }
        return view;
    }

    private class ViewHolder {
        ImageView mIngBg;
        ImageButton mImgSelect;
    }
}
