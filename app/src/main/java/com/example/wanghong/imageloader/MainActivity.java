package com.example.wanghong.imageloader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewTreeObserver;
import android.widget.GridView;

public class MainActivity extends AppCompatActivity {
    private GridView mGridView;
    private  Myadapter mMyadapter;
    private  int imagsize,spacsize;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGridView= (GridView) findViewById(R.id.gridview);
        imagsize=getResources().getDimensionPixelSize(R.dimen.colu);
        spacsize=getResources().getDimensionPixelSize(R.dimen.spca);
        mMyadapter=new Myadapter(this,R.layout.item,Images.imageThumbUrls,mGridView);
        mGridView.setAdapter(mMyadapter);
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                       final int numcol=(int) Math.floor(mGridView.getWidth()/(imagsize+spacsize));
                       if(numcol>0)
                       {int colwidth=mGridView.getWidth()/numcol-spacsize;
                         mMyadapter.setHeight(colwidth);
                           mGridView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                       }
                    }
                }

        );

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMyadapter.flushcache();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMyadapter.cancell();
    }
}
