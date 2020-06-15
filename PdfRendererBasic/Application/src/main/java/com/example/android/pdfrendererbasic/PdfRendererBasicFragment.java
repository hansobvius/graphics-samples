/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.pdfrendererbasic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This fragment has a big {@link ImageView} that shows PDF pages, and 2
 * {@link android.widget.Button}s to move between pages.
 */
public class PdfRendererBasicFragment extends Fragment {

    public PdfPagerAdapter mPdfPageAdapter;
    public PdfRenderer mPdfRenderer;
    public ParcelFileDescriptor mParcelFileDescriptor;
    public File mFile;
    public ViewPager mViewPager;

    public static final String FILENAME = "sample.pdf";

    public static PdfRendererBasicFragment newInstance(){
        PdfRendererBasicFragment fragment = new PdfRendererBasicFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i("TEST", "PdfFragment onCreateView called");
        View view = inflater.inflate(
                R.layout.pdf_renderer_basic_fragment, container, false);
        mViewPager = view.findViewById(R.id.pdf_view_pager);
        try {
            loadPdf();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        renderContent();
//        new FileAsyncTask().execute();
    }

    private void initPagerView(){
//        try {
//            loadPdf();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }finally {
//
//        }
    }

    private void renderContent(){
//        try {
//            loadPdf();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }finally {
//            mPdfPageAdapter = new PdfPagerAdapter(renderPdf(mPdfRenderer.getPageCount()));
//            mViewPager.setAdapter(mPdfPageAdapter);
//        }
        mPdfPageAdapter = new PdfPagerAdapter(renderPdf(mPdfRenderer.getPageCount()));
        mViewPager.setAdapter(mPdfPageAdapter);
    }

    private void loadPdf() throws IOException {
        mFile = getFile();
        try {
            mParcelFileDescriptor = ParcelFileDescriptor.open(mFile, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            Log.i("TEST", "PdfFragment FileNotFoundException");
            e.printStackTrace();
        }finally{
            mPdfRenderer = new PdfRenderer(mParcelFileDescriptor);
        }

    }

    private File getFile(){
        File file = new File(Objects.requireNonNull(this.getActivity()).getApplication().getCacheDir(), FILENAME);
        if(!file.exists()){
            try {
                final InputStream asset = this.getActivity().getApplication().getAssets().open(FILENAME);
                final FileOutputStream outputStream = new FileOutputStream(file);
                final byte[] buffer = new byte[1024];
                int size;
                while((size = asset.read(buffer)) != -1){
                    outputStream.write(buffer, 0, size);
                }
                asset.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private List<Bitmap> renderPdf(int countList){
        List<Bitmap> list = new ArrayList<Bitmap>();
        for(int c = 0; c < countList; c++){
            list.add(renderPage(mPdfRenderer.openPage(c)));
        }
        return list;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Bitmap renderPage(PdfRenderer.Page page){
        Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();
        return bitmap;
    }

//    private List<Bitmap> renderAsyncPdf(PdfRenderer pdfRenderer){
//        List<Bitmap> list = new ArrayList<Bitmap>();
//        int pageCounter = pdfRenderer.getPageCount();
//        for(int c = 0; c < pageCounter; c++){
//            list.add(renderPage(pdfRenderer.openPage(c)));
//        }
//        return list;
//    }

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    @SuppressLint("StaticFieldLeak")
//    class FileAsyncTask extends AsyncTask<Void, Void, PdfRenderer> {
//
//        PdfPagerAdapter mPdfPageAdapter;
//        PdfRenderer mPdfRenderer;
//        ParcelFileDescriptor mParcelFileDescriptor;
//        File mFile;
//
//        @Override
//        protected PdfRenderer doInBackground(Void... voids) {
//            this.mFile = getFile();
//            try {
//                this.mParcelFileDescriptor = ParcelFileDescriptor.open(mFile, ParcelFileDescriptor.MODE_READ_ONLY);
//            } catch (FileNotFoundException e) {
//                Log.i("TEST", "PdfFragment FileNotFoundException");
//                e.printStackTrace();
//            }
//            try {
//                this.mPdfRenderer = new PdfRenderer(mParcelFileDescriptor);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return this.mPdfRenderer;
//        }
//
//        @Override
//        protected void onPostExecute(PdfRenderer pdfRenderer) {
//            super.onPostExecute(pdfRenderer);
//            this.mPdfPageAdapter = new PdfPagerAdapter(renderAsyncPdf(pdfRenderer));
//            mViewPager.setAdapter(this.mPdfPageAdapter);
//        }
//    }
}