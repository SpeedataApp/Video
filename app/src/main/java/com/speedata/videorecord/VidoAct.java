package com.speedata.videorecord;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

/**
 * Created by suntianwei on 2017/3/10.
 */

public class VidoAct extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F2
                ||keyCode==KeyEvent.KEYCODE_VOLUME_UP) {
            Intent intent=new Intent();
            intent.setClass(this,ShowVidoAct.class);
            startActivity(intent);
        }
        return super.onKeyDown(keyCode, event);

    }
}
