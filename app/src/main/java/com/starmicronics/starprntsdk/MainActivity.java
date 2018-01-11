package com.starmicronics.starprntsdk;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.TextView;

@SuppressWarnings("SimplifiableIfStatement")
public class MainActivity extends CommonActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        String textToPrint =
                "        Star Clothing Boutique\n" +
                        "             123 Star Road\n" +
                        "           City, State 12345\n" +
                        "\n" +
                        "Date:MM/DD/YYYY          Time:HH:MM PM\n" +
                        "--------------------------------------\n" +
                        "SALE\n" +
                        "SKU            Description       Total\n" +
                        "300678566      PLAIN T-SHIRT     10.99\n" +
                        "300692003      BLACK DENIM       29.99\n" +
                        "300651148      BLUE DENIM        29.99\n" +
                        "300642980      STRIPED DRESS     49.99\n" +
                        "30063847       BLACK BOOTS       35.99\n" +
                        "\n" +
                        "Subtotal                        156.95\n" +
                        "Tax                               0.00\n" +
                        "--------------------------------------\n" +
                        "Total                          $156.95\n" +
                        "--------------------------------------\n" +
                        "\n" +
                        "Charge\n" +
                        "156.95\n" +
                        "Visa XXXX-XXXX-XXXX-0123\n" +
                        "Refunds and Exchanges\n" +
                        "Within 30 days with receipt\n" +
                        "And tags attached\n";

        Log.d("Joed", textToPrint);



        TextView v = new TextView(this);
        Log.d("Joed", v.getText().toString());

        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_BACK) {                 // Do not close MainActivity.
            return super.onKeyDown(keyCode, event);
        }
        else {
            return false;
        }
    }
}
