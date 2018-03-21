package com.xyz.uninstall;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

public class UninstallActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uninstall_layout);

        ((TextView) findViewById(R.id.tv)).setTypeface(Typeface
                .createFromAsset(getAssets(), "fonts/Antic.ttf"));
    }

}
