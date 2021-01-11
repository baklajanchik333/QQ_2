package com.example.qq_2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class FeedBackActivity extends AppCompatActivity {
    //Элементы
    private TextView text2;
    private TextInputEditText emailFeedbackEt, feedbackEt;
    private MaterialButton feedbackBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_back);

        text2 = findViewById(R.id.text2);
        emailFeedbackEt = findViewById(R.id.emailFeedbackEt);
        feedbackEt = findViewById(R.id.feedbackEt);
        feedbackBtn = findViewById(R.id.feedbackBtn);

        feedbackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mailTo = emailFeedbackEt.getText().toString().trim();
                String[] emailTo = mailTo.split(",");
                String message = feedbackEt.getText().toString().trim();

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_EMAIL, emailTo);
                intent.putExtra(Intent.EXTRA_TEXT, message);
                intent.setType("message/rfc822");
                startActivity(Intent.createChooser(intent, "Отправить сообщение"));
            }
        });
    }
}