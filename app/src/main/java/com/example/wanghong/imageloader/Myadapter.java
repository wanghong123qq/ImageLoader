package com.example.wanghong.imageloader;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.example.wanghong.imageloader.libcore.io.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by wanghong on 2017/3/13.
 */
public class Myadapter extends ArrayAdapter<String> {
    private LruCache<String,Bitmap> mBitmapLruCache;//文件缓存
    private DiskLruCache mDiskLruCache;//内存缓存
    private Set<Netbasic> mNetworkset;//网络任务
    private GridView mphoto;//照片墙
    public void setHeight(int aheight) {
          if(height==aheight)
          {
              return;
          }
           height=aheight;
        notifyDataSetChanged();
    }

    private  int height=0;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
             final  String url=getItem(position);
              View view;
        if(convertView==null)
        {
            view= LayoutInflater.from(getContext()).inflate(R.layout.item,null);
        }
        else {
            view=convertView;
        }
        final  ImageView imageView= (ImageView) view.findViewById(R.id.imageid);
        if(imageView.getLayoutParams().height!=height)
        {
            imageView.getLayoutParams().height=height;
        }
        imageView.setTag(url);
        imageView.setImageResource(R.drawable.empty_photo);
        loadImage(imageView,url);
        return view;
    }

    private void loadImage(ImageView imageView, String url) {//加载图片
               Bitmap bitmap=getbitmapfrommemory(url);//从主存中加载
              if(bitmap!=null&&imageView!=null)
              {
                  imageView.setImageBitmap(bitmap);
              }
              else {
                 //异步加载缓存图片，首先在磁盘查找，如果没有则发起网络请求
               Netbasic work=new Netbasic();
               mNetworkset.add(work);
               work.execute(url);

              }





    }
    public  void addbitmaptomeromy(String key,Bitmap bitmap)//缓存图片到内存
    {
        if(mBitmapLruCache.get(key)==null)
        {
            mBitmapLruCache.put(key,bitmap);
        }
    }
    public Bitmap getbitmapfrommemory(String key)//从内存中得到图片
    {
         return  mBitmapLruCache.get(key);
    }
    public Myadapter(Context context, int resource, String[] mstrings, GridView gridView) {//构造器
        super(context, resource,mstrings);
        mNetworkset=new HashSet<>();
        int memory=(int)Runtime.getRuntime().maxMemory();
        mphoto=gridView;
        int cachesize=memory/8;
        mBitmapLruCache=new LruCache<String,Bitmap>(cachesize)
        {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        File cachedir =getcachedir(context,"picture");
        try {
            mDiskLruCache=DiskLruCache.open(cachedir,getAppversion(context),1,10*1024*1024);//打开硬盘
        } catch (IOException e) {
            e.printStackTrace();
        }

       }
    public  File getcachedir(Context context,String key)//得到文件缓存的位置
    {   String cache;
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)||!Environment.isExternalStorageRemovable())
        {
         cache=context.getExternalCacheDir().getPath();
        }
        else {
            cache=context.getCacheDir().getPath();
        }
        return  new File(cache+File.separator+key);

    }
    public  int getAppversion(Context context)//得到应用的版本号
    {
        try {
            PackageInfo p=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
             return  p.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }
    class  Netbasic extends AsyncTask<String,Void,Bitmap>//开启线程执行异步缓存
    {
        private String imageurl;
        @Override
        protected Bitmap doInBackground(String... params) {
            imageurl=params[0];
            FileDescriptor filedescript=null;
            FileInputStream fileinputstream=null;
            try { final String key=hashkeyforDisk(imageurl);
                  DiskLruCache.Snapshot sna=mDiskLruCache.get(key);

                   if(sna==null)//网络请求
                   {
                    DiskLruCache.Editor editor=mDiskLruCache.edit(key);
                    if(editor!=null)
                    {
                        OutputStream output=editor.newOutputStream(0);
                        if(downurlfornet(imageurl,output))
                        {
                            editor.commit();
                        }
                        else
                        {
                            editor.abort();
                        }


                    }
                   sna=mDiskLruCache.get(key);

                  }
                if(sna!=null)//硬盘缓存不为空
                {
                    fileinputstream= (FileInputStream) sna.getInputStream(0 );
                    filedescript=fileinputstream.getFD();
                }
                Bitmap bitmap= null;
                if(filedescript!=null)
                {
                    bitmap=BitmapFactory.decodeFileDescriptor(filedescript);
                }
                if(bitmap!=null)//吧硬盘缓存放入内存缓存
                {
                    addbitmaptomeromy(params[0],bitmap);
                }
                return  bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(filedescript==null&&fileinputstream!=null)
                {
                    try {
                        fileinputstream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }


            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView image1= (ImageView) mphoto.findViewWithTag(imageurl);//根据设置的标签找到imageview
            if(image1!=null&&bitmap!=null)
            {
                image1.setImageBitmap(bitmap);
            }
            mNetworkset.remove(this);//在任务队列中删除


        }
    }

    private boolean downurlfornet(String imageurl,OutputStream output) {//从网路下载图片
        HttpURLConnection httpurl=null;
        BufferedInputStream in=null;
        BufferedOutputStream out=null;
        try {
            URL url=new URL(imageurl);
            httpurl= (HttpURLConnection) url.openConnection();
            httpurl.setRequestMethod("GET");
            httpurl.setDoOutput(true);
            httpurl.setDoInput(true);
            httpurl.setReadTimeout(8000);
            httpurl.setConnectTimeout(8000);
            in=new BufferedInputStream(httpurl.getInputStream(),8*1024*1024);
            out=new BufferedOutputStream(output,8*1024*1024);
            int b=0;
            while((b=in.read())!=-1)
            {
                out.write(b);
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(httpurl!=null)
            {
                httpurl.disconnect();
            }
            try { if(out!=null)
                     {

                    out.close();}
                   if(in!=null)
                   {
                      in.close();
                   }}
                 catch ( IOException e) {
                    e.printStackTrace();
                }
            }




        return  false;
    }

    private String hashkeyforDisk(String imageurl) {//得到MD5加密的key
        String cache;
        try {
            MessageDigest m=MessageDigest.getInstance("MD5");
            m.update(imageurl.getBytes());
            cache=bytetoString(m.digest());
        } catch (NoSuchAlgorithmException e) {
            cache=String.valueOf(imageurl.hashCode());
        }
        return  cache;
    }

    private String bytetoString(byte[] digest) {//把十六进制字节数组转换成字符串
        StringBuffer stringbuffer=new StringBuffer();
        for(int i=0;i<digest.length;i++)
        {
            String hex;
            hex=Integer.toHexString(0xFF&digest[i]);
            if(hex.length()==1)
            {
                stringbuffer.append("0");
            }
           stringbuffer.append(hex);
        }

   return  stringbuffer.toString();
    }
   public  void flushcache()
   {
       try { if(mDiskLruCache!=null)
                {

               mDiskLruCache.flush();
           } }catch (IOException e) {
               e.printStackTrace();
           }
       }

  public void cancell()
  {if(mNetworkset!=null)
  {
      for(Netbasic n:mNetworkset)
      {
          n.cancel(false);
      }
  }


  }

   }




