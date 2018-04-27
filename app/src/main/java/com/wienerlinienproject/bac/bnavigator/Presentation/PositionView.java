package com.wienerlinienproject.bac.bnavigator.Presentation;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.wienerlinienproject.bac.bnavigator.Data.Door;
import com.wienerlinienproject.bac.bnavigator.Data.LocationMap;
import com.wienerlinienproject.bac.bnavigator.Data.LocationObject;


//TODO hier LocationObjekte laden oder in der Main Activity bzw die locationMap initialisieren?
// => in der Main activity und dem view übergeben
//TODO wer ruft LocationObject auf bzw erstellt diese und kommuniziert bzw fragt diese Objekte ab?
//PositionView
//TODO aktuelle/"aktive" Location wo speichern?
//
//TODO Neighbors selber speichern => Referenzen in jedem Pnbjekt zu den Neighbours
//TODO Boundaries Speichern (sozusagen vom Referenzpunkt aus die Meter speichern (von height width) und so zirka boundaries speichern)
//=> aktive location ist die von der die werte gültig in den jewieligen boudnaries liegen


//TODO Actionbar title ist noch statisch!

//TODO statt circle für eigene position, das icon für my position einzeichnen


//todo pro location eine zone
//TODO bei add destination die destination location zurück geben bzw ausrechnen


//TODO how to deep link?!
//TODO userposition speichern
//TODO destination position speichern (in meter relativ zum refernzpunkt der location)

//TODO eine art absolute to relative koordinaten umrechnungs methode in locationmap?

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

    //TODO destinationRelativePointer
    private Point destinationPointer;
    private Drawable destinationIcon;
    private String destinationLocation;
    private Point destinationRelativePointer;

    private LocationMap locationMap;
    private LocationObject destinationLocationObject;

    //TODO die userpositions müssen auf die ganze map umgerechnet werden, hier sind sie ja relativ je location
    private float userPositionX;
    private float userPositionY;

    private DestinationSetCallback destinationSetCallback;

    private boolean navigationActive;
    private boolean simplenavigation;
    private boolean advacednavigation;

    private int counter;

    //Laut dp im drawn_map.xml (Breite und Höhe in Meter)
    //TODO sollte in Locationmap ausgelagert werden
    private double locationWidth = 6.0;
    private double locationHeight = 14.0;
    //TODO für jede location bezugspunkt speichern (links obere ecke)
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

    private void init(){

        positionPaint = new Paint();
        positionPaint.setColor(Color.BLUE);

        destinationPaint = new Paint();
        destinationPaint.setColor(Color.GRAY);

        destinationLocationObject = null;
        locationMap = null;

        userPositionX = 0;
        userPositionY = 0;

        mPointerX = 300.0f;
        mPointerY = 800.0f;

        counter = 0;

        navigationActive = false;
        simplenavigation = false;
        arrivedAtDestination = false;
        advacednavigation = false;
        //movePlayer0Runnable.run(); //this is the initial call to draw player at index 0

        Log.d("PositionView", "init done");
    }

    //TODO vorhandene bitmap nehmen! nicht immer die initiale drawn Map verwenden! weil desitnation icon überschrieben wird
    //TODO locations überschreiben!
    public void updateUserPosition(double xPos, double yPos, double viewHeight, double viewWidth,Drawable drawable){

        indoorViewHeight = viewHeight;
        indoorViewWidth = viewWidth;

        xLocation = locationMap.getActiveLocation().getStartPointX();
        yLocation = locationMap.getActiveLocation().getStartPointY();

        mPointerX = (float) ((xPos+xLocation)/locationWidth);

        mPointerY = (float) ((yPos+yLocation)/locationHeight);



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
                        // TODO Notification that user arrived at destination
                        Log.d("navigate", "You have reached the destination");
                        arrivedAtDestination = true;
                        onDeletePostionClicked();
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
            c.drawBitmap(drawableToBitmap(destinationIcon), destinationPointer.x, destinationPointer.y, destinationPaint);

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
            //TODO Userposition dazu zeichnen
            //TODO bei delete jumpt die anzeige auf eine andere position wenn gezoomed ist
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

        //TODO hier invalidate not so good i guess, aber userposition wird sonst nicht gezeichnet wenn drawcircle nicht in ondraw
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
                    //TODO besser wäre auf koordinaten in der location konvertieren

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

    public void onDeletePostionClicked() {
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
            return null;
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

    public Point getDestinationPointer() {
        return destinationPointer;
    }

    public void resetListener (){
        setOnTouchListener(null);
    }

    public void setBackgroundMap(Drawable backgroundMap) {
        this.initialDrawnMap = backgroundMap;
    }

    interface DestinationSetCallback{
        public void onDestinationSet();
    }
}
