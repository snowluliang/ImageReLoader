package snow.com.imagereloader;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ImageLoader {

    private static ImageLoader mInstance;

    /**
     * 缓存图片核心
     */
    private LruCache<String, Bitmap> mLruChahe;
    //线程池
    private ExecutorService mThresdPool;
    //默认线程的数量
    private static final int DEAFULT_THREAD_COUNT = 1;
    //任务类型
    private Type mType = Type.LIFO;
    /**
     * 信号量!!!!!!!!!!!!!!
     */
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);

    private Semaphore mSemaphoreThreadPool;
    public enum Type {
        LIFO, FIFO
    }

    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;

    /**
     * 后台轮询线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;

    /**
     * UI线程中的Handler
     */
    private Handler mUIHandler;


    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    private void init(int threadCount, Type type) {
        mPoolThread = new Thread() {
            @Override
            public void run() {
                //准备looper工作
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池取出任务执行
                        mThresdPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //释放信号量
                mSemaphorePoolThreadHandler.release();
                //消息不断循环jiehsou
                Looper.loop();
            }
        };
        //开启线程
        mPoolThread.start();

        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruChahe = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {

                return value.getRowBytes() * value.getHeight();
            }
        };
        mThresdPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<>();
        mType = type;

        //初始化线程池信号量
        mSemaphoreThreadPool = new Semaphore(threadCount);

    }

    private Runnable getTask() {

        if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        } else if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        }
        return null;
    }

    public static ImageLoader getmInstance() {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(DEAFULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return mInstance;
    }
    public static ImageLoader getmInstance(int threadCount, Type type) {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    public void loadImage(final String path, final ImageView view) {

        //为img设置标签
        view.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    ImageHolder holder = (ImageHolder) msg.obj;
                    Bitmap bitmap = holder.bitmap;
                    String path = holder.path;
                    ImageView imageView = holder.imageView;
                    //进行路径比对,防止显示错位
                    if (imageView.getTag().equals(path)) {
                        imageView.setImageBitmap(bitmap);
                    }
                }
            };
        }
        Bitmap  bm = getBitmapFromLruCache(path);
        if (bm != null) {
            refreshBitmap(path, view, bm);
        } else {
            addTask(new Runnable() {
                @Override
                public void run() {
                    //加载图片
                    ImageSize size = getImageViewSize(view);
                    //压缩图片
                    Bitmap bm = decodeSampleBitmapFromPath(path, size.width, size.height);
                    //添加到缓存中
                    addBitmapToLruCachr(path, bm);
                    refreshBitmap(path, view, bm);

                    //释放信号量
                    mSemaphoreThreadPool.release();
                }
            });
        }


    }

    private void refreshBitmap(String path, ImageView view, Bitmap bm) {
        Message message = Message.obtain();
        ImageHolder holder = new ImageHolder();
        holder.bitmap = bm;
        holder.imageView = view;
        holder.path = path;
        message.obj = holder;
        mUIHandler.sendMessage(message);
    }

    private void addBitmapToLruCachr(String path, Bitmap bitmap) {
        if (getBitmapFromLruCache(path) == null) {
            if (bitmap != null) {
                mLruChahe.put(path, bitmap);
            }
        }

    }

    private Bitmap decodeSampleBitmapFromPath(String path, int width, int height) {
        //第一次加载图片 获取图片的宽和高, 并不把图片加载到内存中
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        //第二次加载 压缩后的图片
        options.inSampleSize = calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }



    /**
     * 根据需求的宽和高及图片实际的宽和高计算inSampleSize
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reWidth, int reHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;
        if (width > reWidth || height > reHeight) {
            int widthRadio = Math.round(width*1.0f / reWidth);
            int heightRadio = Math.round(height*1.0f / reHeight);

            inSampleSize = Math.max(widthRadio, heightRadio);//图片的压缩比例越大,占用内存越小
        }
        return inSampleSize;
    }

    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        ViewGroup.LayoutParams params = imageView.getLayoutParams();
        DisplayMetrics metrics = imageView.getContext().getResources().getDisplayMetrics();

        int width = imageView.getWidth();//获取实际宽度
        if (width <= 0) {
            width = params.width;//在Layout中声明的宽度
        }
        if (width <= 0) {
            width = imageView.getMaxWidth();//获取最大宽度
        }
        if (width <= 0) {
            width = metrics.widthPixels;
        }

        int height = imageView.getHeight();
        if (height <= 0) {
            height = params.height;
        }
        if (height <= 0) {
            height = imageView.getMaxHeight();
        }
        if (height <= 0) {
            height = metrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    private class ImageSize {
        int width;
        int height;
    }

    //同步一下
    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mPoolThreadHandler == null) {
                mSemaphorePoolThreadHandler.acquire();//请求一个信号量
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    private class ImageHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

    /**
     * 从缓存中获取Bitmap
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruChahe.get(key);
    }


}
