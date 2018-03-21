package tickingtimeladdies.astronomy.multiuse.watchface;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

import static android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH;

/**
 * Created by Alex on 16/01/2018.
 */

public class pressureReader implements SensorEventListener {
    public float millibar;
    public long time;
    private boolean highAcc=true;
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.

        if(accuracy==SENSOR_STATUS_ACCURACY_HIGH){
            highAcc=true;
        }
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        //if(highAcc==true) {
            millibar = event.values[0];
            time=event.timestamp;


          //  highAcc = false;
        //}
        //Log.d("MyWatchFaceService", "variable_you_want_to_log: " + millibar);
        // Do something with this sensor data.


    }

}
