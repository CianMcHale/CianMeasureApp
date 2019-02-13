package com.example.cian.cianmeasureapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    EditText widthInput;

    Button SubmitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        widthInput = (EditText) findViewById(R.id.widthInput);

        SubmitButton = (Button) findViewById(R.id.SubmitButton);
        SubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity2();

            }
        });
    }
    public void openActivity2(){
        Intent intent = new Intent(this, Main2Activity.class);
        //float width = Float.valueOf(widthInput.getText().toString());
        //intent.putExtra("userWidthInput", width);
        startActivity(intent);
    }
}
