package com.wienerlinienproject.bac.bnavigator.Presentation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


//TODO hier LocationObjekte laden oder in der Main Activity bzw die locationMap initialisieren?
// => in der Main activity und dem view übergeben
//TODO wer ruft LocationObject auf bzw erstellt diese und kommuniziert bzw fragt diese Objekte ab?
//PositionView
//TODO aktuelle/"aktive" Location wo speichern?
//
//TODO Neighbors selber speichern => Referenzen in jedem Pnbjekt zu den Neighbours
//TODO Boundaries Speichern (sozusagen vom Referenzpunkt aus die Meter speichern (von height width) und so zirka boundaries speichern)
//=> aktive location ist die von der die werte gültig in den jewieligen boudnaries liegen

//TODO wie bekommt man Paths einer vectorgrafik aus android raus (Wände) , sodass man sicherstellen kann dass treffpunkt nicht außerhalb der
//Map genommen wird

public class PositionView  extends TouchImageView{

    private Paint positionPaint;
    private float mPointerRadius = 50.0f;
    private float mPointerX;
    private float mPointerY;
    private double indoorViewHeight;
    private double indoorViewWidth;


    public PositionView (Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    private void init(){

        positionPaint = new Paint();
        positionPaint.setColor(Color.BLUE);

        mPointerX = 300.0f;
        mPointerY = 800.0f;

        //movePlayer0Runnable.run(); //this is the initial call to draw player at index 0

        Log.d("PositionView", "init done");
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Account for padding

        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());


        //TODO: schauen ob wir das auch brauchen (vielleicht für View darunter also das wir "auf der Map" gehen .. wenn möglich - auf dem Bild)
        // Account for the label
        // if (positionPaint) xpad += mTextWidth;

        float ww = (float)w - xpad;
        float hh = (float)h - ypad;

    }

    public void updateUserPosition(double xPos, double yPos, double viewHeight, double viewWidth,Drawable drawable,String locationName){

        indoorViewHeight = viewHeight;
        indoorViewWidth = viewWidth;

        //TODO breite und höhe des gesamten plans in metern
        double locationWidth = 5.5;
        double locationHeight = 5.5;

        //TODO
        int xLocation = 0;
        int yLocation = 0;

        mPointerX = (float) ((xPos+xLocation)/locationWidth);
        //mPointerX = (float) (xPos/locationWidth * viewWidth);

        mPointerY = (float) ((yPos+yLocation)/locationHeight);
        //positionsupdate umdrehen weil in der cloud falsch gespeichert (verkehrt herum)
        // mPointerY = (float) ((5.5-yPos)/locationHeight * viewHeight);


        Log.d("updatePos", locationName+ ": Pos:" + xPos +" "+ yPos + "Pointer:" + mPointerX +" "+ mPointerY);
        drawUserPosition(drawable);

    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        Log.d("PositionView", "drawing done");
    }

    private void drawUserPosition(Drawable drawable) {
        Bitmap bm = drawableToBitmap(drawable);
        Canvas c = new Canvas(bm);

        //returnt breite/höhe in pixel (tatsächlich angezeigt)
        int maxH = bm.getHeight();
        int maxW = bm.getWidth();

        c.drawCircle((mPointerX)*maxW, (mPointerY)*maxH, mPointerRadius,positionPaint);
        //c.drawCircle((mPointerX/getWidth())*maxW, (mPointerY/getHeight())*maxH, mPointerRadius,positionPaint);
        Log.d("Bitmap", "Draw " + " max: " + maxW + ", " + maxH);
        setImageBitmap(bm);
    }

    public Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public float getmPointerX() {
        return mPointerX;
    }

    public float getmPointerY() {
        return mPointerY;
    }


}
