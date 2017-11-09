package com.wienerlinienproject.bac.bnavigator.Presentation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PositionView  extends View{

    private Paint positionPaint;
    private float mPointerRadius = 30.0f;
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



    public void updatePosition(double xPos, double yPos, double viewHeight, double viewWidth){

        indoorViewHeight = viewHeight;
        indoorViewWidth = viewWidth;

        double locationWidth = 5.5;
        double locationHeight = 5.5;

        mPointerX = (float) (xPos/locationWidth * viewWidth);
        mPointerY = (float) ((5.5-yPos)/locationHeight * viewHeight);


        Log.d("updatePos", "Pos:" + xPos +" "+ yPos + "Pointer:" + mPointerX +" "+ mPointerY);

    }



    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        canvas.drawCircle(mPointerX, mPointerY, mPointerRadius, positionPaint);

        Log.d("PositionView", "drawing done");
    }

    /**Handler handler = new Handler(Looper.getMainLooper());
    Runnable movePlayer0Runnable = new Runnable(){
        public void run(){
            invalidate(); //will trigger the onDraw
            handler.postDelayed(this,5000); //in 5 sec player0 will move again
        }
    };*/

    public float getmPointerX() {
        return mPointerX;
    }

    public float getmPointerY() {
        return mPointerY;
    }

}
