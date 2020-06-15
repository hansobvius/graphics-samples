package com.example.android.pdfrendererbasic;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class PdfPagerAdapter extends PagerAdapter {

    private List<Bitmap> mBitmapList = new ArrayList<>();
    private ImageView mImageView;

    public PdfPagerAdapter(List<Bitmap> bitmap){
        this.mBitmapList = bitmap;
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(container.getContext());
        View view  = inflater.inflate(R.layout.adapter_pager_pdf, container, false);
        mImageView = view.findViewById(R.id.image_pager_pdf);
        mImageView.setImageBitmap(mBitmapList.get(position));
        container.addView(view);
        return view;
    }

    @Override
    public int getCount() {
        return mBitmapList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object){
        container.removeView((View) object);
    }
}
