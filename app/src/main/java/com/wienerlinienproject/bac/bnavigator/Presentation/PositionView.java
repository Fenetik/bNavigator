package com.wienerlinienproject.bac.bnavigator.Presentation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class PositionView  extends View{

    private Paint positionPaint;
    private float mPointerRadius = 30.0f;
    private float mPointerX;
    private float mPointerY;

    public PositionView (Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    private void init(){

        positionPaint = new Paint();
        positionPaint.setColor(Color.BLUE);

        mPointerX = 100.0f;
        mPointerY = 800.0f;

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

        // Figure out how big we can make the pie.
        float diameter = Math.min(ww, hh);
    }

    public void updatePosition(double xPos, double yPos){

        mPointerX = (float) xPos;
        mPointerY = (float) yPos;

        Canvas canvas = new Canvas();
        draw(canvas);

    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

        canvas.drawCircle(mPointerX, mPointerY, mPointerRadius, positionPaint);

        Log.d("PositionView", "drawing done");
    }

}
