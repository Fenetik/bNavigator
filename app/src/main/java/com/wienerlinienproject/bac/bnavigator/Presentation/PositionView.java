package com.wienerlinienproject.bac.bnavigator.Presentation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toolbar;

import com.wienerlinienproject.bac.bnavigator.R;


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


//TODO Actionbar title ist noch statisch!

//TODO statt sircle für eigene position, das icon für my position einzeichnen


//todo pro location eine zone
//TODO auf zoomed achten bzw ausprobieren
//TODO bei add destination die destination location zurück geben bzw ausrechnen


//TODO how to deep link?!
//TODO userposition speichern
//TODO treffpunkt position speichern (in meter relativ zum refernzpunkt der location)

//TODO listener nach add location auf alten setzen..wann .. wo
//TODO Action bar menü erst nach wirklichem add ändern, nicht beim click auf dem button

//TODO eine art absolute to relative koordinaten umrechnungs methode in locationmap?



public class PositionView  extends TouchImageView{

    private Paint positionPaint;
    private float mPointerRadius = 50.0f;
    private float mPointerX;
    private float mPointerY;
    private double indoorViewHeight;
    private double indoorViewWidth;
    private Bitmap actualBitmap = null;
    private Drawable drawable;

    //TODO destinationRelativePointer
    private Point destinationPointer;
    private Drawable destinationIcon;
    private String destinationLocation;
    private Point destinationRelativePointer;


    //TODO die userpositions müssen auf die ganze map umgerechnet werden, hier sind sie ja relativ je location
    private float userPositionX;
    private float userPositionY;
    private double userPositionXpx;
    private double userPositionYpx;

    private DestinationSetCallback destinationSetCallback;



    //Laut dp im drawn_map.xml (Breite und Höhe in Meter)
    //TODO sollte in Locationmap ausgelagert werden
    private double locationWidth = 6.0;
    private double locationHeight = 14.0;
    //TODO für jede location bezugspunkt speichern (links obere ecke?)
    int xLocation = 0;
    int yLocation = 0;


    public PositionView (Context context, AttributeSet attr) {
        super(context, attr);
        destinationSetCallback = (DestinationSetCallback) context;
        init();
    }

    private void init(){

        positionPaint = new Paint();
        positionPaint.setColor(Color.BLUE);

        //TODO
        userPositionX = 5;
        userPositionY = 5;
        userPositionXpx = 400;
        userPositionYpx = 400;

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

        userPositionXpx = xPos;
        userPositionYpx = yPos;

        mPointerX = (float) ((xPos+xLocation)/locationWidth);
        //mPointerX = (float) (xPos/locationWidth * viewWidth);

        mPointerY = (float) ((yPos+yLocation)/locationHeight);
        //positionsupdate umdrehen weil in der cloud falsch gespeichert (verkehrt herum)
        // mPointerY = (float) ((5.5-yPos)/locationHeight * viewHeight);


        Log.d("updatePos", locationName+ ": Pos: " + xPos +" "+ yPos + "Pointer:" + mPointerX +" "+ mPointerY);
        drawUserPosition(drawable);

    }

    @Override
    protected void onDraw(Canvas canvas){

        super.onDraw(canvas);
        if(destinationPointer != null){
            canvas.drawBitmap(drawableToBitmap(destinationIcon), destinationPointer.x-65, destinationPointer.y-95,null);
        }

        Log.d("PositionView", "drawing done");
    }

    private void drawUserPosition(Drawable drawable) {
        this.drawable = drawable;
        actualBitmap = drawableToBitmap(drawable);
        Canvas c = new Canvas(actualBitmap);

        //returnt breite/höhe in pixel (tatsächlich angezeigt)
        int maxH = actualBitmap.getHeight();
        int maxW = actualBitmap.getWidth();

        userPositionX = (mPointerX)*maxW;
        userPositionY = (mPointerY)*maxH;

        c.drawCircle(userPositionX, userPositionY, mPointerRadius,positionPaint);
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

    //TODO alten Listener danach zurücksetzen
    //TODO is within location an MainActivity returnen um share/delete buttons anzuzeigen?
    public void onSetPositionClicked(){

        super.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int scaledX = 0;
                int scaledY = 0;

                Point point = new Point();
                point.x = (int)event.getX();
                point.y = (int)event.getY();

                //Dadurch dass die Bitmap aufs display gescaled wird muss man dies ier beachten
                //https://stackoverflow.com/questions/12496339/android-imageview-get-pixel-color-from-scaled-image
                Matrix inverse = new Matrix();
                ((ImageView) v).getImageMatrix().invert(inverse);
                float[] touchPoint = new float[] {point.x, point.y};
                inverse.mapPoints(touchPoint);
                scaledX = Integer.valueOf((int)touchPoint[0]);
                scaledY = Integer.valueOf((int)touchPoint[1]);

                //getpixel returns the color of a pixel
                int pixelColor = drawableToBitmap(((AppCompatImageView)v).getDrawable()).getPixel(scaledX,scaledY);
                Log.d("within location","X:"+point.x +" Y: "+point.y+ "Color:" + pixelColor);

                String temp = isWithinLocation(pixelColor);

                if(temp !=null){
                    destinationPointer = point;
                    destinationRelativePointer = null;
                    destinationLocation = temp;
                    destinationSetCallback.onDestinationSet();
                }

                /*if(temp ==null){
                    destinationPointer = null;
                    destinationRelativePointer = null;
                    destinationLocation = null;
                }else{
                    destinationPointer = point;
                    destinationRelativePointer = null;
                    destinationLocation = temp;
                }*/

                invalidate();

                //TODO stack overflow ex
                //PositionView.super.setOnTouchListener(new PrivateOnTouchListener());
                return false;
            }
        });

        //not responding
        //super.setOnTouchListener(new PrivateOnTouchListener());

    }

    public void onDeletePostionClicked() {
        if(destinationPointer != null){
            destinationPointer = null;
            invalidate();
        }
    }

    //checks the backgroundcolor wether the touched point is within our building or not
    //Background color of the inside of our building is white (000000)
    //TODO sollte ws eher ein location object returnen?
    private String isWithinLocation(int pixelColor) {
        int redValue = Color.red(pixelColor);
        int blueValue = Color.blue(pixelColor);
        int greenValue = Color.green(pixelColor);

        if(redValue == 254 && blueValue ==255 && greenValue == 255){
            Log.d("within Location","Yes(Nats Room), "+"red:"+redValue+" blue:"+blueValue+" green:"+greenValue);
            return "Nats Room";
        }else if(redValue == 255 && blueValue == 254 && greenValue == 255){
            Log.d("within Location","Yes (Nats Flur), "+"red:"+redValue+" blue:"+blueValue+" green:"+greenValue);
            return "Nats Flur";
        }else if (redValue == 255 && blueValue ==255 && greenValue == 254){
            Log.d("within Location","Yes (Nats Kitchen), "+"red:"+redValue+" blue:"+blueValue+" green:"+greenValue);
            return "Nats Kitchen";
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
        setScrollPosition((float)userPositionXpx/getWidth(),(float)userPositionYpx/getHeight());
        Log.d("ScrollingToUser","X:"+userPositionXpx +" X/Width:"+ (float)userPositionXpx/getWidth() +
                " Y:"+userPositionYpx + " Y/Height:"+ (float)userPositionYpx/getHeight());
    }

    public Point getDestinationPointer() {
        return destinationPointer;
    }

    public void resetListener (){
        super.setOnTouchListener(new PrivateOnTouchListener());
    }

    interface DestinationSetCallback{
        public void onDestinationSet();
    }
}

