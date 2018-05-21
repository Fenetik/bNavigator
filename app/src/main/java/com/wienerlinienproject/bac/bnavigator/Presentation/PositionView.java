package com.wienerlinienproject.bac.bnavigator.Presentation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.wienerlinienproject.bac.bnavigator.Data.Door;
import com.wienerlinienproject.bac.bnavigator.Data.LocationMap;
import com.wienerlinienproject.bac.bnavigator.Data.LocationObject;


public class PositionView  extends TouchImageView{

    private Paint positionPaint;
    private float mPointerRadius = 50.0f;
    private float mPointerX;
    private float mPointerY;
    private double indoorViewHeight;
    private double indoorViewWidth;
    private Bitmap actualBitmap = null;
    private Drawable initialDrawnMap;

    private boolean arrivedAtDestination;

    private Point destinationPointer;
    private Drawable destinationIcon;
    private String destinationLocation;
    private Point destinationRelativePointer;

    private LocationMap locationMap;
    private LocationObject destinationLocationObject;

    private float userPositionX;
    private float userPositionY;

    private float relativeUserPosX;
    private float relativeUserPosY;

    private DestinationSetCallback destinationSetCallback;

    private boolean navigationActive;
    private boolean simplenavigation;
    private boolean advacednavigation;

    private int counter;

    //Laut dp im drawn_map.xml (Breite und Höhe in Meter)
    private double locationWidth = 6.0;
    private double locationHeight = 14.0;
    double xLocation = 0.5;
    double yLocation = 7.8;
    private boolean bitmapRedrawNeeded = false;
    private Paint destinationPaint;


    public PositionView (Context context, AttributeSet attr) {
        super(context, attr);
        destinationSetCallback = (DestinationSetCallback) context;
        init();
    }

    public boolean getArrivedAtDestination(){
        return arrivedAtDestination;
    }
    public void setArrivedAtDestination(boolean arrived){
        this.arrivedAtDestination= arrived;
    }

    private void init(){

        positionPaint = new Paint();
        positionPaint.setColor(Color.BLUE);

        destinationPaint = new Paint();
        destinationPaint.setColor(Color.GRAY);

        destinationLocationObject = null;
        locationMap = null;

        //Testwerte, wenn kein Beacon in der Nähe ist
        userPositionX = 200;
        userPositionY = 200;
        mPointerX = 0.1f;
        mPointerY = 0.1f;


        counter = 0;

        navigationActive = false;
        simplenavigation = false;
        arrivedAtDestination = false;
        advacednavigation = false;
        //movePlayer0Runnable.run(); //this is the initial call to draw player at index 0

        Log.d("PositionView", "init done");
    }

    public void updateUserPosition(double xPos, double yPos, double viewHeight, double viewWidth,Drawable drawable){

        indoorViewHeight = viewHeight;
        indoorViewWidth = viewWidth;

        xLocation = locationMap.getActiveLocation().getStartPointX();
        yLocation = locationMap.getActiveLocation().getStartPointY();

        relativeUserPosX = (float) (xPos+xLocation);
        relativeUserPosY = (float)(yPos+yLocation);

        mPointerX = (float) ((relativeUserPosX)/locationWidth);
        mPointerY = (float) ((relativeUserPosY)/locationHeight);



        Log.d("updatePos", locationMap.getActiveLocation().getName()+ ": Pos: " + xPos +" "+ yPos + "Pointer:" + mPointerX +" "+ mPointerY);
        drawUserPosition(drawable);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap temp = drawableToBitmap(initialDrawnMap);
        Canvas c = new Canvas(temp);

        if (destinationPointer != null) {
            //returnt breite/höhe in pixel (tatsächlich angezeigt)
            if (navigationActive) {
                Paint p = new Paint();
                p.setColor(Color.GREEN);
                p.setStrokeWidth(20f);
                float destinationMiddleX = destinationPointer.x + destinationIcon.getMinimumWidth() / 2;
                float destinationMiddleY = destinationPointer.y + destinationIcon.getMinimumHeight() / 2;
                if (!(userPositionY == 0 && userPositionX == 0)) {
                    // destX <= userX && userX <= destX
                    double destinationRangeXPlus = destinationMiddleX + 150;
                    double destinationRangeYPlus = destinationMiddleY + 150;
                    double destinationRangeXMinus = destinationMiddleX - 150;
                    double destinationRangeYMinus = destinationMiddleY - 150;
                    Log.d("navigate", "destinationRange: x: " + destinationRangeXPlus + " to " + destinationRangeXMinus + " y: " + destinationRangeYPlus + " to " + destinationRangeYMinus);
                    if ((destinationRangeXMinus <= userPositionX && destinationRangeXPlus >= userPositionX) && (destinationRangeYMinus <= userPositionY && destinationRangeYPlus >= userPositionY)) {
                        Log.d("navigate", "You have reached the destination");
                        arrivedAtDestination = true;
                        onDeletePositionClicked();
                        destinationSetCallback.onTargetReached();
                    } else {
                        if (simplenavigation) {
                            if (!destinationLocationObject.getName().equals(locationMap.getActiveLocation().getName())) {
                                navigateUser();
                            } else {
                                Log.d("navigate", "drawSimpleNavigationLine from - x: " + destinationPointer.x + " y: " + destinationPointer.y + " to x: " + userPositionX + " y: " + userPositionY);
                                c.drawLine(destinationMiddleX, destinationMiddleY, userPositionX, userPositionY, p);
                            }
                        } else if (advacednavigation) {
                            if (destinationLocationObject.getName().equals(locationMap.getActiveLocation().getName())) {
                                navigateUser();
                            } else {
                                Door userLocationDoor = destinationLocationObject.getNeighboursList().get(locationMap.getActiveLocation());
                                Door destinationDoor = locationMap.getActiveLocation().getNeighboursList().get(destinationLocationObject);
                                // ((door.x + startpointLocation.x) / locationWidth) * bitmapHeight
                                // ((door.y + startpointLocation.Y) / locationHeight) * bitmapWidth
                                double userLocationDoorX = (userLocationDoor.getStartPointX() + locationMap.getActiveLocation().getStartPointX()) / locationWidth * temp.getWidth();

                                double destinationDoorX = ((destinationDoor.getStartPointX() + destinationLocationObject.getStartPointX()) / locationWidth) * temp.getWidth();
                                double userLocationDoorY = ((userLocationDoor.getStartPointY() + locationMap.getActiveLocation().getStartPointY()) / locationHeight) * temp.getHeight();
                                double destinationDoorY = ((destinationDoor.getStartPointY() + destinationLocationObject.getStartPointY()) / locationHeight) * temp.getHeight();


                                if (!locationMap.getActiveLocation().getName().equals("flur")) {
                                    c.drawLine(userPositionX, userPositionY, (float) userLocationDoorX, (float) userLocationDoorY, p);
                                    c.drawLine((float) userLocationDoorX, (float) userLocationDoorY, (float) destinationDoorX, (float) destinationDoorY, p);
                                    c.drawLine((float) destinationDoorX, (float) destinationDoorY, destinationMiddleX, destinationMiddleY, p);
                                } else {
                                    c.drawLine(userPositionX, userPositionY, (float) userLocationDoorX, (float) userLocationDoorY, p);
                                    c.drawLine((float) userLocationDoorX, (float) userLocationDoorY, destinationMiddleX, destinationMiddleY, p);
                                }

                            }
                        }
                    }

                }
            }
            if(destinationPointer !=null){
                c.drawBitmap(drawableToBitmap(destinationIcon), destinationPointer.x, destinationPointer.y, destinationPaint);
            }

            if (userPositionX != 0 && userPositionY != 0) {
                c.drawCircle(userPositionX, userPositionY, mPointerRadius, positionPaint);
            }

            //draw navigationLine

            setImageBitmap(temp);
            actualBitmap = temp;
            bitmapRedrawNeeded = false;

            Log.d("PositionView", "drawing done");


        }else if (destinationPointer == null) {
            //destinationpointer deleted
            if (userPositionX != 0 && userPositionY != 0) {
                c.drawCircle(userPositionX, userPositionY, mPointerRadius, positionPaint);
            }
            actualBitmap = temp;
            setImageBitmap(actualBitmap);
            bitmapRedrawNeeded = false;
        }

        super.onDraw(canvas);
    }

    public void setLocationMap(LocationMap locationMap){
        this.locationMap = locationMap;
        setActiveLocation("kitchen-2s1");
    }

    private void drawUserPosition(Drawable drawable) {

        this.initialDrawnMap = drawable;
        actualBitmap = drawableToBitmap(initialDrawnMap);

        Canvas c = new Canvas(actualBitmap);

        //returnt breite/höhe in pixel (tatsächlich angezeigt)
        int maxH = actualBitmap.getHeight();
        int maxW = actualBitmap.getWidth();

        userPositionX = (mPointerX)*maxW;
        userPositionY = (mPointerY)*maxH;

        invalidate();
        //c.drawCircle((mPointerX/getWidth())*maxW, (mPointerY/getHeight())*maxH, mPointerRadius,positionPaint);
        Log.d("Bitmap", "Draw:" + (mPointerX)*maxW + "px, " + (mPointerY)*maxH+"px");
        setImageBitmap(actualBitmap);

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

    @SuppressLint("ClickableViewAccessibility")
    public void onSetPositionClicked(){

        super.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int scaledX = 0;
                int scaledY = 0;

                Point point = new Point();
                point.x = (int)event.getX();
                point.y = (int)event.getY();

                //Dadurch dass die Bitmap aufs display gescaled wird muss man dies hier beachten
                //https://stackoverflow.com/questions/12496339/android-imageview-get-pixel-color-from-scaled-image
                Matrix inverse = new Matrix();
                ((ImageView) v).getImageMatrix().invert(inverse);
                float[] touchPoint = new float[] {point.x, point.y};
                inverse.mapPoints(touchPoint);
                scaledX = Integer.valueOf((int)touchPoint[0]);
                scaledY = Integer.valueOf((int)touchPoint[1]);

                //getpixel returns the color of a pixel
                int pixelColor;
                if(actualBitmap == null){
                    pixelColor = drawableToBitmap(initialDrawnMap).getPixel(scaledX,scaledY);
                }else{
                    pixelColor = actualBitmap.getPixel(scaledX,scaledY);
                }
                Log.d("within location","X:"+point.x +" Y: "+point.y+ "Color:" + pixelColor);

                destinationLocationObject = isWithinLocation(pixelColor);

                if(destinationLocationObject !=null){

                    //Bitmat is scaled to the View, so the Event position has to be scaled to to get the actual position on the Bitmap
                    destinationPointer = new Point (scaledX-(destinationIcon.getIntrinsicWidth()/2),scaledY-(destinationIcon.getIntrinsicHeight()/2));
                    destinationRelativePointer = null;
                    destinationLocation = destinationLocationObject.getName();
                    destinationSetCallback.onDestinationSet();
                    bitmapRedrawNeeded = true;
                }

                invalidate();

                return false;
            }
        });

    }

    public boolean navigateUser(){
        navigationActive = true;
        if (destinationLocationObject.getName().equals(locationMap.getActiveLocation().getName())){
            advacednavigation = false;
            simplenavigation = true;
        } else {
            simplenavigation = false;
            advacednavigation = true;
        }
        Canvas c = new Canvas(actualBitmap);
        draw(c);
        return arrivedAtDestination;
    }

    public void onDeletePositionClicked() {
        if(destinationPointer != null){
            destinationPointer = null;
            bitmapRedrawNeeded = true;
            navigationActive = false;
            simplenavigation = false;
            advacednavigation = false;
            invalidate();
        }
    }

    //checks the backgroundcolor wether the touched point is within our building or not
    //Background color of the inside of our building is white (000000)
    private LocationObject isWithinLocation(int pixelColor) {
        int redValue = Color.red(pixelColor);
        int blueValue = Color.blue(pixelColor);
        int greenValue = Color.green(pixelColor);
        navigationActive = false;

        if(redValue == 254 && blueValue ==255 && greenValue == 255){
            Log.d("within Location","Yes(Nats Room), "+"red:"+redValue+" blue:"+blueValue+" green:"+greenValue);
            Log.d("within Location",locationMap.getLocationByName("room-84l").getName());
            return locationMap.getLocationByName("room-84l");
        }else if(redValue == 255 && blueValue == 254 && greenValue == 255){
            Log.d("within Location","Yes (Nats Flur), "+"red:"+redValue+" blue:"+blueValue+" green:"+greenValue);
            //return null;
            return locationMap.getLocationByName("flur");
        }else if (redValue == 255 && blueValue ==255 && greenValue == 254){
            Log.d("within Location","Yes (Nats Kitchen), "+"red:"+redValue+" blue:"+blueValue+" green:"+greenValue);
            Log.d("within Location",locationMap.getLocationByName("kitchen-2s1").getName());
            return locationMap.getLocationByName("kitchen-2s1");
        }
        Log.d("Within Location","No "+"red:"+redValue+" blue:"+blueValue+" green:"+greenValue);
        return null;
    }

    public float getmPointerX() {
        return mPointerX;
    }

    public float getmPointerY() {
        return mPointerY;
    }

    public void setDestinationIcon(Drawable destinationIcon) {
        this.destinationIcon = destinationIcon;
    }

    public void scrollToUser() {
        setScrollPosition(mPointerX,mPointerY);
        Log.d("ScrollingToUser","X:"+mPointerX +" Y: "+mPointerY);
    }

    public void resetListener (){
        setOnTouchListener(null);
    }

    public void setBackgroundMap(Drawable backgroundMap) {
        this.initialDrawnMap = backgroundMap;
    }


    //called to get share information
    public Point getDestinationPointer(){
        return destinationPointer;
    }

    //called to get share information
    public LocationObject getDestinationLocationObject(){
        return locationMap.getLocationByName(destinationLocation);
    }

    //called if clicked on share mark
    public void setDestination(Point p, String s){
        destinationPointer = p;
        destinationLocation = s;
        destinationLocationObject = locationMap.getLocationByName(s);
        destinationSetCallback.onDestinationSet();
        bitmapRedrawNeeded = true;
        invalidate();
    }

    public void setActiveLocation(String s){
        locationMap.setActiveLocation(locationMap.getLocationByName(s));
    }

    public float getRelativeUserPosX() {
        return relativeUserPosX;
    }

    public float getRelativeUserPosY() {
        return relativeUserPosY;
    }

    interface DestinationSetCallback{
        public void onDestinationSet();
        public void onTargetReached();
    }
}
