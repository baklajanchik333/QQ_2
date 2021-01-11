package com.example.qq_2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.util.HashMap;

public class WriteReviewActivity extends AppCompatActivity {
    //Элементы
    private RatingBar ratingBar;
    private TextInputEditText ratingEt;
    private MaterialButton ratingBtn;

    //БД
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        firebaseAuth = FirebaseAuth.getInstance();

        ratingBar = findViewById(R.id.ratingBar);
        ratingEt = findViewById(R.id.ratingEt);
        ratingBtn = findViewById(R.id.ratingBtn);

        ratingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputData();
            }
        });
    }

    private void inputData() {
        String ratings = "" + ratingBar.getRating();
        String review = ratingEt.getText().toString().trim();
        String timestamp = "" + System.currentTimeMillis();

        //Настраиваем данные
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", "" + firebaseAuth.getUid());
        hashMap.put("ratings", "" + ratings);
        hashMap.put("review", "" + review);
        hashMap.put("timestamp", "" + timestamp);
        //Добавляем и сохраняем в БД
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Ratings").updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Отзыв добавлен
                        FancyToast.makeText(WriteReviewActivity.this, "Ваш отзыв отправлен.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();

                        ratingBar.setRating(0);
                        ratingEt.setText("");

                        startActivity(new Intent(WriteReviewActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Ошибка
                        FancyToast.makeText(WriteReviewActivity.this, "Ошибка! Ваш отзыв не был отправлен.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                    }
                });
    }
}