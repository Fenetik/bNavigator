package com.wienerlinienproject.bac.bnavigator.Presentation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

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

    //TODO destinationRelativePointer
    private Point destinationPointer;
    private Drawable destinationIcon;
    private String destinationLocation;
    private Point destinationRelativePointer;


    //TODO die userpositions müssen auf die ganze map umgerechnet werden, hier sind sie ja relativ je location (in locationmap auslagern?)
    private float userPositionX;
    private float userPositionY;

    private DestinationSetCallback destinationSetCallback;



    //Laut dp im drawn_map.xml (Breite und Höhe in Meter)
    //TODO sollte in Locationmap ausgelagert werden
    private double locationWidth = 6.0;
    private double locationHeight = 14.0;
    //TODO für jede location bezugspunkt speichern (links obere ecke?)
    double xLocation = 0.5;
    double yLocation = 7.8;
    private boolean bitmapRedrawNeeded = false;
    private Paint destinationPaint;


    public PositionView (Context context, AttributeSet attr) {
        super(context, attr);
        destinationSetCallback = (DestinationSetCallback) context;
        init();
    }

    private void init(){

        positionPaint = new Paint();
        positionPaint.setColor(Color.BLUE);

        destinationPaint = new Paint();
        destinationPaint.setColor(Color.GRAY);

        //TODO
        userPositionX = 0;
        userPositionY = 0;

        mPointerX = 300.0f;
        mPointerY = 800.0f;

        //movePlayer0Runnable.run(); //this is the initial call to draw player at index 0

        Log.d("PositionView", "init done");
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Account for padding
        Log.d("zoomed","View got zoomed");

        float xpad = (float)(getPaddingLeft() + getPaddingRight());
        float ypad = (float)(getPaddingTop() + getPaddingBottom());


        //TODO: schauen ob wir das auch brauchen (vielleicht für View darunter also das wir "auf der Map" gehen .. wenn möglich - auf dem Bild)
        // Account for the label
        // if (positionPaint) xpad += mTextWidth;

        float ww = (float)w - xpad;
        float hh = (float)h - ypad;

    }

    //TODO vorhandene bitmap nehmen! nicht immer die initiale drawn Map verwenden! weil desitnation icon überschrieben wird
    //TODO locations überschreiben!
    public void updateUserPosition(double xPos, double yPos, double viewHeight, double viewWidth,Drawable drawable,LocationObject currentLocation){

        indoorViewHeight = viewHeight;
        indoorViewWidth = viewWidth;

        xLocation = currentLocation.getStartPointX();
        yLocation = currentLocation.getStartPointY();

        mPointerX = (float) ((xPos+xLocation)/locationWidth);
        //mPointerX = (float) (xPos/locationWidth * viewWidth);

        mPointerY = (float) ((yPos+yLocation)/locationHeight);
        //positionsupdate umdrehen weil in der cloud falsch gespeichert (verkehrt herum)
        // mPointerY = (float) ((5.5-yPos)/locationHeight * viewHeight);


        Log.d("updatePos", currentLocation.getName()+ ": Pos: " + xPos +" "+ yPos + "Pointer:" + mPointerX +" "+ mPointerY);
        drawUserPosition(drawable);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap temp = drawableToBitmap(initialDrawnMap);
        Canvas c = new Canvas(temp);

        if (destinationPointer != null) {


            //returnt breite/höhe in pixel (tatsächlich angezeigt)
            if(isZoomed()){
                c.drawBitmap(drawableToBitmap(destinationIcon),destinationPointer.x*getCurrentZoom(), destinationPointer.y*getCurrentZoom(),destinationPaint);
            }else{
                c.drawBitmap(drawableToBitmap(destinationIcon),destinationPointer.x, destinationPointer.y,destinationPaint);
            }
            if(userPositionX != 0 && userPositionY != 0){
                c.drawCircle(userPositionX, userPositionY, mPointerRadius,positionPaint);
            }
            //c.drawCircle((mPointerX/getWidth())*maxW, (mPointerY/getHeight())*maxH, mPointerRadius,positionPaint);
            setImageBitmap(temp);
            actualBitmap = temp;
            bitmapRedrawNeeded = false;

            Log.d("PositionView", "drawing done");

        }else if(destinationPointer == null){
            //destinationpointer deleted
            //TODO Userposition dazu zeichnen
            //TODO bei delete jumpt die anzeige auf eine andere position wenn gezoomed ist
            if(userPositionX != 0 && userPositionY != 0){
                c.drawCircle(userPositionX, userPositionY, mPointerRadius,positionPaint);
            }
            actualBitmap = temp;
            setImageBitmap(actualBitmap);
            bitmapRedrawNeeded = false;
        }
        super.onDraw(canvas);
    }
    private void drawUserPosiition (){

    }
    private void drawUserPosition(Drawable drawable) {

        //TODO wenn die aktualle bitmap null is dann in die initial einzeichnen, ansonsten dazu zeichnen
        //if (destinationPointer == null && initialDrawnMap != null && userPositionX == 0 && userPositionY == 0 ) {
            this.initialDrawnMap = drawable;
            actualBitmap = drawableToBitmap(initialDrawnMap);
        //}


        //shiiiiit
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

                //Dadurch dass die Bitmap aufs display gescaled wird muss man dies ier beachten
                //https://stackoverflow.com/questions/12496339/android-imageview-get-pixel-color-from-scaled-image
                Matrix inverse = new Matrix();
                ((ImageView) v).getImageMatrix().invert(inverse);
                float[] touchPoint = new float[] {point.x, point.y};
                inverse.mapPoints(touchPoint);
                scaledX = Integer.valueOf((int)touchPoint[0]);
                scaledY = Integer.valueOf((int)touchPoint[1]);

                //getpixel returns the color of a pixel
                //TODO actualbitmap verwenden?
                int pixelColor;
                if(actualBitmap == null){
                    pixelColor = drawableToBitmap(initialDrawnMap).getPixel(scaledX,scaledY);
                }else{
                    pixelColor = actualBitmap.getPixel(scaledX,scaledY);
                }
                Log.d("within location","X:"+point.x +" Y: "+point.y+ "Color:" + pixelColor);

                String temp = isWithinLocation(pixelColor);

                if(temp !=null){
                    //TODO besser wäre auf koordinaten in der location konvertieren

                        //Bitmat is scaled to the View, so the Event position has to be scaled to to get the actual position on the Bitmap
                        destinationPointer = new Point (scaledX-(destinationIcon.getIntrinsicWidth()/2),scaledY-(destinationIcon.getIntrinsicHeight()/2));
                        destinationRelativePointer = null;
                        destinationLocation = temp;
                        destinationSetCallback.onDestinationSet();
                        bitmapRedrawNeeded = true;
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

                return false;
            }
        });

    }

    public void onDeletePostionClicked() {
        if(destinationPointer != null){
            destinationPointer = null;
            bitmapRedrawNeeded = true;
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

    public boolean isWithinZoomedFrame(Point p){
        Log.d("positionview zoomedF","Normal x:"+p.x +" normal y:"+ p.y +"\n"
        +"scaled x:" + p.x*getCurrentZoom()+" scaled y:"+p.y*getCurrentZoom());
        if(isZoomed()){
            RectF temp = getPixelRektFromZoomRekt(getZoomedRect());
            Log.d("positionview zoomedR","Center x:"+temp.centerX()+" Center y:"+ temp.centerY() + "\n"+
                    "Width:" + temp.width() + " Height:" + temp.height()+ "\n" +
                    " left:"+ temp.left + " right:"+ temp.right + " top:" + temp.top + " bottom:" +temp.bottom + "\n"+
                    " Point: x:" +p.x + " y:"+p.y);
            return temp.contains(p.x,p.y);
        }
        return true;
    }

    private RectF getPixelRektFromZoomRekt(RectF temp) {
        RectF temp2 = new RectF((temp.centerX()-temp.width()/2)*getWidth()
                ,(temp.centerY()-temp.height()/2)*getHeight(),
                (temp.centerX()+temp.width()/2)*getWidth(),(temp.centerY()+temp.height()/2)*getHeight());

        Log.d("positionview pxRekt","PxRekt: left:" + temp2.left + " right:" +temp2.right + " top:"+temp2.top+ " bottom:" + temp2.bottom+"\n"+ " width:" + temp2.width() + " height:"+ temp2.height());
        return temp2;
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

