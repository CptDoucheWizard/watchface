/*
 * Copyright (C) 2017 The Android Open Source Project
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

package tickingtimeladdies.astronomy.multiuse.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;



/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */





public class MyWatchFaceService extends CanvasWatchFaceService {

    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);



    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (R.id.message_update == message.what) {
                    invalidate();
                    if (shouldTimerBeRunning()) {
                        long timeMs = System.currentTimeMillis();
                        long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                        mUpdateTimeHandler.sendEmptyMessageDelayed(R.id.message_update, delayMs);
                    }
                }
            }
        };



        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());

                invalidate();
            }
        };
/*
        private final BroadcastReceiver shutdown = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                    Log.d("MyWatchFaceService","Saving");
                    String filename = "lastPressure";
                    String string = String.valueOf(pressureBuffer.peekFirst());
                    FileOutputStream outputStream;

                    try {
                        File file = new File(context.getFilesDir(), filename);
                        outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                        outputStream.write(string.getBytes());
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };*/

        private boolean mRegisteredTimeZoneReceiver = false;

        // Feel free to change these values and see what happens to the watch face.
        private static final float STROKE_WIDTH = 2f;
        private static final int SHADOW_RADIUS = 2;
        private static final int BATT_RING_OFFSET = 5;
        private static final float HAND_END_CAP_RADIUS = 4f+BATT_RING_OFFSET;


        private Calendar mCalendar;
        private Calendar UTCCalendar;

        private Paint mBackgroundPaint;
        private Paint mHandPaint;
        private Paint altiPaint;

        private boolean mAmbient;
        private boolean minuteFlag;

        private float pressure;
        private long lastTime;

        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;

        private Bitmap mercuryHourHandAmb;
        private Bitmap venusHourHandAmb;
        private Bitmap marsHourHandAmb;
        private Bitmap earthHourHandAmb;
        private Bitmap jupiterHourHandAmb;
        private Bitmap saturnHourHandAmb;
        private Bitmap uranusHourHandAmb;
        private Bitmap neptuneHourHandAmb;
        private Bitmap plutoHourHandAmb;
        private Bitmap moonHandAmb;

        private Bitmap minuteHandAmb;
        private Bitmap secondsHandAmb;
        private Bitmap centerImAmb;

        private Bitmap mercuryHourHandColour;
        private Bitmap venusHourHandColour;
        private Bitmap marsHourHandColour;
        private Bitmap earthHourHandColour;
        private Bitmap jupiterHourHandColour;
        private Bitmap saturnHourHandColour;
        private Bitmap uranusHourHandColour;
        private Bitmap neptuneHourHandColour;
        private Bitmap plutoHourHandColour;
        private Bitmap moonHandColour;

        private Bitmap minuteHandColour;
        private Bitmap secondsHandColour;
        private Bitmap centerImColour;




        private int handState;

        /**
         * Whether the display supports fewer bits for each color in ambient mode.
         * When true, we disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;
        /**
         * Whether the display supports burn in protection in ambient mode.
         * When true, remove the background in ambient mode.
         */
        private boolean mBurnInProtection;

        private int mWidth;
        private int mHeight;
        private float mCenterX;
        private float mCenterY;

        private float marsH;
        private float marsW;

        private float centerImH;
        private float centerImW;

        private BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);

        private Matrix transformation= new Matrix();
        private Matrix centerTransform= new Matrix();
        private Matrix secondsTransform =new Matrix();

        private float mScale = 1;

        public SensorManager mSensorManager;
        public Sensor mPressure;
        public pressureReader pReader= new pressureReader();
        private boolean pSensor;

        private LinkedList<Float> pressureBuffer=new LinkedList<Float>();

        private Paint goodWeatherPaint;
        private Paint badWeatherPaint;



        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
             handState=1;

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null){
                mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
                pSensor=true;
            }
            else {
                pSensor=false;
            }






            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFaceService.this).build());

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);

            final int backgroundResId = R.drawable.custom_background_black;

            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), backgroundResId);



            centerImAmb = BitmapFactory.decodeResource(getResources(),R.drawable.center_image_white);

            mercuryHourHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.mercury_hand_dark);
            venusHourHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.venus_hand_dark);
            earthHourHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.earth_hand_white);
            marsHourHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.mars_hand_dark);
            jupiterHourHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.jupiter_hand_dark);
            saturnHourHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.saturn_hand_dark);
            uranusHourHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.uranus_hand_dark);
            neptuneHourHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.neptune_hand_dark);
            plutoHourHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.pluto_hand_white);

            moonHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.moon_hand_dark);
            minuteHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.minute_hand_white);
            secondsHandAmb = BitmapFactory.decodeResource(getResources(),R.drawable.seconds_hand_white);


            centerImColour= BitmapFactory.decodeResource(getResources(),R.drawable.center_image_colour);

            mercuryHourHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.mercury_hand_colour);
            venusHourHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.venus_hand_colour);
            earthHourHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.earth_hand_colour);
            marsHourHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.mars_hand_colour);
            jupiterHourHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.jupiter_hand_colour);
            saturnHourHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.saturn_hand_colour);
            uranusHourHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.uranus_hand_colour);
            neptuneHourHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.neptune_hand_colour);
            plutoHourHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.pluto_hand_colour);

            moonHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.moon_hand_colour);
            minuteHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.minute_hand_colour);
            secondsHandColour = BitmapFactory.decodeResource(getResources(),R.drawable.seconds_hand_colour);


            mHandPaint = new Paint();
            mHandPaint.setColor(Color.WHITE);
            mHandPaint.setStrokeWidth(STROKE_WIDTH);
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);
            mHandPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, Color.WHITE);
            mHandPaint.setStyle(Paint.Style.STROKE);
            mHandPaint.setFilterBitmap(true);

            goodWeatherPaint = new Paint();
            goodWeatherPaint.setColor(Color.GREEN);
            goodWeatherPaint.setStrokeWidth(STROKE_WIDTH*3);
            goodWeatherPaint.setAntiAlias(true);
            goodWeatherPaint.setStrokeCap(Paint.Cap.ROUND);
            goodWeatherPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, Color.WHITE);
            goodWeatherPaint.setStyle(Paint.Style.STROKE);
            goodWeatherPaint.setFilterBitmap(true);

            badWeatherPaint = new Paint();
            badWeatherPaint.setColor(Color.BLUE);
            badWeatherPaint.setStrokeWidth(STROKE_WIDTH*3);
            badWeatherPaint.setAntiAlias(true);
            badWeatherPaint.setStrokeCap(Paint.Cap.ROUND);
            badWeatherPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, Color.WHITE);
            badWeatherPaint.setStyle(Paint.Style.STROKE);
            badWeatherPaint.setFilterBitmap(true);

            altiPaint = new Paint();
            altiPaint.setColor(Color.WHITE);
            altiPaint.setAntiAlias(true);
            altiPaint.setShadowLayer(SHADOW_RADIUS/2, 0, 0, Color.WHITE);

            mCalendar = Calendar.getInstance();
            UTCCalendar=Calendar.getInstance(TimeZone.getTimeZone("GMT"));




        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(R.id.message_update);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            //Log.d("MyWatchFaceServce","Triggered");

            if (inAmbientMode) {
                mHandPaint.setAntiAlias(false);
                handState=2;
                if(pSensor==true) {
                }

            } else {
                mHandPaint.setAntiAlias(true);
                handState=1;

            }
           // handState=1;

            /*
             * Whether the timer should be running depends on whether we're visible (as well as
             * whether we're in ambient mode), so we may need to start or stop the timer.
             */
            updateTimer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mWidth = width;
            mHeight = height;
            /*
             * Find the coordinates of the center point on the screen.
             * Ignore the window insets so that, on round watches
             * with a "chin", the watch face is centered on the entire screen,
             * not just the usable portion.
             */
            mCenterX = mWidth / 2f;
            mCenterY = mHeight / 2f;
            mScale = ((float) width) / (float) mBackgroundBitmap.getWidth();
            /*
             * Calculate the lengths of the watch hands and store them in member variables.
             */

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * mScale),
                    (int) (mBackgroundBitmap.getHeight() * mScale), true);

            if (!mBurnInProtection || !mLowBitAmbient) {
                initGrayBackgroundBitmap();
            }

            marsH=marsHourHandColour.getHeight();
            marsW=marsHourHandColour.getWidth();
            float handScale=(mCenterY-(BATT_RING_OFFSET)-2)/(marsH+centerImColour.getHeight()/2);
            marsH=handScale*marsH;
            marsW=handScale*marsW;

            centerImH=centerImColour.getHeight()*handScale;
            centerImW=centerImColour.getWidth()*handScale;

            transformation.setScale(handScale,handScale);
            transformation.postTranslate(mCenterX-marsW/2,mCenterY-marsH-centerImH/2+2);

            centerTransform.setScale(handScale,handScale);
            centerTransform.postTranslate(mCenterX-centerImH/2,mCenterY-centerImW/2);

            secondsTransform.setScale(handScale,handScale);
            secondsTransform.postTranslate(mCenterX-centerImW/2,HAND_END_CAP_RADIUS);

            altiPaint.setTextSize(mCenterX/10);


        }

        private void initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(mBackgroundBitmap.getWidth(),
                    mBackgroundBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
            Paint grayPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            grayPaint.setColorFilter(filter);
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            UTCCalendar.setTimeInMillis(now);


            //Log.d("MyWatchFaceService", "Pressure: " + pReader.millibar);

            //pressure = pReader.millibar;

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);

            float secondsRotation = seconds * 6f;

            //final float marsMinute=mCalendar.get(Calendar.DAY_OF_YEAR);
            //final float marsHandOffset= now/1000/60

            final float UTC = UTCCalendar.getTimeInMillis()/1000;

            final float mercuryHourRot = (float)(24f*(UTC%(Math.pow(10,6)*5.06701))/(Math.pow(10,6)*5.06701))%12f*30f;
            final float venusHourRot = (float)(24f*(UTC%(Math.pow(10,6)*10.0872))/(Math.pow(10,6)*10.0872))%12f*30f;
            final float marsHourRot = (float)((24f*(UTC%88775.22)/88775.22)%12f*30f-150.3356f)%360;
            final float jupiterHourRot = (float)(24f*(UTC%35733.312)/35733.312)%12f*30f;
            //final float jupiterHourRot = (float)((UTC/86400f-2451545f)/86400f);
            final float saturnHourRot = (float)(24f*(UTC%38517.12)/38517.12)%12f*30f;
            final float uranusHourRot = (float)(24f*(UTC%62035.2)/62035.2)%12f*30f;
            final float neptuneHourRot = (float)(24f*(UTC%57974.4)/57974.4)%12f*30f;
            final float plutoHourRot = (float)(24f*(UTC%551880f)/551880f)%12f*30f;




            final float moonHourRot = (float)((360*(UTC%2551442.87)/2551442.87)-84.425f)%360;


            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            if(pSensor==true) {
                Log.d("MyWatchFaceService","minutes"+(int)minutesRotation);

                if((minutesRotation%36==0)&&(minuteFlag==true)) {
                    Log.d("MyWatchFaceService","minute");
                    mSensorManager.registerListener(pReader, mPressure, SensorManager.SENSOR_DELAY_FASTEST);

                    minuteFlag = false;

                   // mSensorManager.unregisterListener(pReader);

                }
                if(minutesRotation%36!=0){
                    minuteFlag=true;
                }
                if(pReader.time!=lastTime){
                    pressure=pReader.millibar;
                    pressureBuffer.addFirst(pressure);
                    Log.d("MyWatchFaceService","pressure"+pressure);

                    if(pressureBuffer.size()>60){
                        pressureBuffer.pollLast();
                    }
                    Log.d("MyWatchFaceService","pressure"+pressure);


                    mSensorManager.unregisterListener(pReader);
                    lastTime=pReader.time;
                    Log.d("MyWatchFaceService","unreg");
                }
            }

            handState=2;
           // Log.d("MyWatchFaceService","ambientMode" +mAmbient);

            switch(handState) {

               case 1:
                    // save the canvas state before we begin to rotate it
                    canvas.save();
                    if((pSensor==true)&&(pressureBuffer.size()>0)) {
                        int altitude = (int) ((1 - Math.pow(pressureBuffer.peekFirst() / 1013.25, 0.190284)) * 4430);
                        //Log.d("MyWatchFaceService", "millibar: " + pReader.millibar);
                        canvas.drawText((10 * altitude + "m"), 1.6f * mCenterX, mCenterY+mCenterX/40, altiPaint);

                        float pressureChange=4*(pressureBuffer.peekFirst()-pressureBuffer.peekLast());
                        Log.d("MyWatchFaceService", "Pressure: " + pressureChange);

                        if(pressureChange>180){
                            pressureChange=180;
                        }
                        if(pressureChange<-180){
                            pressureChange=-180;
                        }
                        if(pressureChange>0){
                            canvas.drawArc(BATT_RING_OFFSET,BATT_RING_OFFSET,mHeight-BATT_RING_OFFSET,mWidth-BATT_RING_OFFSET,pressureChange+270,-pressureChange,false,goodWeatherPaint);
                        }else{
                            canvas.drawArc(BATT_RING_OFFSET,BATT_RING_OFFSET,mHeight-BATT_RING_OFFSET,mWidth-BATT_RING_OFFSET,270f,pressureChange,false,badWeatherPaint);
                        }
                    }

                   canvas.rotate(marsHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(marsHourHandColour, transformation, mHandPaint);

                    canvas.rotate(mercuryHourRot - marsHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(mercuryHourHandColour, transformation, mHandPaint);

                    canvas.rotate(venusHourRot - mercuryHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(venusHourHandColour, transformation, mHandPaint);

                    canvas.rotate(jupiterHourRot - venusHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(jupiterHourHandColour, transformation, mHandPaint);

                    canvas.rotate(saturnHourRot - jupiterHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(saturnHourHandColour, transformation, mHandPaint);

                    canvas.rotate(uranusHourRot - saturnHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(uranusHourHandColour, transformation, mHandPaint);

                    canvas.rotate(neptuneHourRot - uranusHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(neptuneHourHandColour, transformation, mHandPaint);

                   // canvas.rotate(plutoHourRot - neptuneHourRot, mCenterX, mCenterY);
                    //canvas.drawBitmap(plutoHourHandColour, transformation, mHandPaint);

                    canvas.rotate(moonHourRot - neptuneHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(moonHandColour, transformation, mHandPaint);

                    canvas.rotate(minutesRotation - moonHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(minuteHandColour, transformation, mHandPaint);

                    canvas.rotate(hoursRotation - minutesRotation, mCenterX, mCenterY);
                    canvas.drawBitmap(earthHourHandColour, transformation, mHandPaint);

                    canvas.drawBitmap(centerImColour, centerTransform, mHandPaint);


                   canvas.rotate(secondsRotation - hoursRotation, mCenterX, mCenterY);
                    canvas.drawBitmap(secondsHandColour, secondsTransform, mHandPaint);

                    canvas.restore();

                   float batLevel = (float)3.6*bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                   canvas.drawArc(BATT_RING_OFFSET, BATT_RING_OFFSET, mHeight - BATT_RING_OFFSET, mWidth - BATT_RING_OFFSET, 270f, batLevel, false, mHandPaint);

                    break;


                case 2:



                    canvas.save();

                    canvas.rotate(marsHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(marsHourHandAmb, transformation, mHandPaint);

                    canvas.rotate(mercuryHourRot - marsHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(mercuryHourHandAmb, transformation, mHandPaint);

                    canvas.rotate(venusHourRot - mercuryHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(venusHourHandAmb, transformation, mHandPaint);

                    canvas.rotate(jupiterHourRot - venusHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(jupiterHourHandAmb, transformation, mHandPaint);

                    canvas.rotate(saturnHourRot - jupiterHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(saturnHourHandAmb, transformation, mHandPaint);

                    canvas.rotate(uranusHourRot - saturnHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(uranusHourHandAmb, transformation, mHandPaint);

                    canvas.rotate(neptuneHourRot - uranusHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(neptuneHourHandAmb, transformation, mHandPaint);

                   // canvas.rotate(plutoHourRot - neptuneHourRot, mCenterX, mCenterY);
                    //canvas.drawBitmap(plutoHourHandAmb, transformation, mHandPaint);

                    canvas.rotate(moonHourRot - neptuneHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(moonHandAmb, transformation, mHandPaint);

                    canvas.rotate(minutesRotation - moonHourRot, mCenterX, mCenterY);
                    canvas.drawBitmap(minuteHandAmb, transformation, mHandPaint);

                    canvas.rotate(hoursRotation - minutesRotation, mCenterX, mCenterY);
                    canvas.drawBitmap(earthHourHandAmb, transformation, mHandPaint);

                    canvas.drawBitmap(centerImAmb, centerTransform, mHandPaint);

                    canvas.restore();
                    break;

                default:
                    break;

            }
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /*
             * Whether the timer should be running depends on whether we're visible
             * (as well as whether we're in ambient mode),
             * so we may need to start or stop the timer.
             */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(R.id.message_update);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(R.id.message_update);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}


