package com.example.qq_2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.qq_2.Adapters.MyPagerAdapter;
import com.google.android.material.button.MaterialButton;

public class IntroActivity extends AppCompatActivity {
    //Элементы
    private ViewPager viewPager;
    private LinearLayout dotLayout;
    private TextView[] dotsTv;
    private int[] layouts;
    private MaterialButton nextBtn, skipBtn;

    private MyPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        setStatusBarTransparent();

        SharedPreferences preferences = getSharedPreferences("Preferences", MODE_PRIVATE);
        String FirstTime = preferences.getString("FirstTimeInstall", "");

        if (FirstTime.equals("Yes")) {
            startActivity(new Intent(IntroActivity.this, StartActivity.class));
            finish();
        } else {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("FirstTimeInstall", "Yes");
            editor.apply();
        }

        //<editor-fold desc="Инициализация элементов">
        viewPager = findViewById(R.id.viewPager);
        dotLayout = findViewById(R.id.dotLayout);
        nextBtn = findViewById(R.id.nextBtn);
        skipBtn = findViewById(R.id.skipBtn);
        //</editor-fold>

        //<editor-fold desc="Обработка нажатий">
        //Нажатие на nextBtn
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPage = viewPager.getCurrentItem() + 1;
                if (currentPage < layouts.length) {
                    //Переходим на следующую страницу
                    viewPager.setCurrentItem(currentPage);
                } else {
                    startActivity();
                }
            }
        });

        //Нажатие на skipBtn
        skipBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //После нажатия сразу переходим к MainActivity
                startActivity();
                ;
            }
        });
        //</editor-fold>

        layouts = new int[]{R.layout.slide_1, R.layout.slide_2, R.layout.slide_3};
        pagerAdapter = new MyPagerAdapter(layouts, getApplicationContext());
        viewPager.setAdapter(pagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == layouts.length - 1) {
                    //Последняя страница
                    nextBtn.setText("Начать");
                    skipBtn.setVisibility(View.GONE);
                } else {
                    nextBtn.setText("Дальше");
                    skipBtn.setVisibility(View.VISIBLE);
                }

                setDotStatus(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        setDotStatus(0);
    }

    private void startActivity() {
        startActivity(new Intent(IntroActivity.this, StartActivity.class));
        finish();
    }

    private void setStatusBarTransparent() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void setDotStatus(int page) {
        dotLayout.removeAllViews();
        dotsTv = new TextView[layouts.length];
        for (int i = 0; i < dotsTv.length; i++) {
            dotsTv[i] = new TextView(this);
            dotsTv[i].setText(Html.fromHtml("&#8226;"));
            dotsTv[i].setTextSize(30);
            dotsTv[i].setTextColor(Color.parseColor("#a9b4bb"));
            dotLayout.addView(dotsTv[i]);
        }

        //Устанавливаем текущую точку активной
        if (dotsTv.length > 0) {
            dotsTv[page].setTextColor(Color.parseColor("#FFFFFF"));
        }
    }

}
