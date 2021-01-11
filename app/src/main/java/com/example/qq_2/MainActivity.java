package com.example.qq_2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.qq_2.Fragments.ChatListFragment;
import com.example.qq_2.Fragments.HomeFragment;
import com.example.qq_2.Fragments.MoreFragment;
import com.example.qq_2.Fragments.ProfileFragment;
import com.example.qq_2.Fragments.UsersFragment;
import com.example.qq_2.Notifications.Token;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class MainActivity extends AppCompatActivity {
    //Элементы
    private BottomNavigationView navigationView;

    //БД
    private FirebaseAuth firebaseAuth;

    String title = "";
    String mUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

        navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //Нажатие на меню
                switch (item.getItemId()) {
                    case R.id.home:
                        title = "Дом";
                        HomeFragment fragment1 = new HomeFragment();
                        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                        ft1.replace(R.id.content, fragment1, "");
                        ft1.commit();
                        //getSupportActionBar().setTitle(title);
                        return true;
                    case R.id.profile:
                        title = "Профиль";
                        ProfileFragment fragment2 = new ProfileFragment();
                        FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                        ft2.replace(R.id.content, fragment2, "");
                        ft2.commit();
                        getSupportActionBar().setTitle(title);
                        return true;
                    case R.id.users:
                        title = "Пользователи";
                        UsersFragment fragment3 = new UsersFragment();
                        FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                        ft3.replace(R.id.content, fragment3, "");
                        ft3.commit();
                        getSupportActionBar().setTitle(title);
                        return true;
                    case R.id.message:
                        title = "Сообщения";
                        ChatListFragment fragment4 = new ChatListFragment();
                        FragmentTransaction ft4 = getSupportFragmentManager().beginTransaction();
                        ft4.replace(R.id.content, fragment4, "");
                        ft4.commit();
                        getSupportActionBar().setTitle(title);
                        return true;
                    case R.id.more:
                        title = "Ещё";
                        MoreFragment fragment5 = new MoreFragment();
                        FragmentTransaction ft5 = getSupportFragmentManager().beginTransaction();
                        ft5.replace(R.id.content, fragment5, "");
                        ft5.commit();
                        getSupportActionBar().setTitle(title);
                        return true;
                }

                return false;
            }
        });

        //Чтобы при открытии приложения открывалось всегда Дом
        HomeFragment fragment1 = new HomeFragment();
        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
        ft1.replace(R.id.content, fragment1);
        ft1.commit();

        checkUserStatus();

        //Обновление токена
        updateToken(FirebaseInstanceId.getInstance().getToken());
    }

    public void updateToken(String token) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        ref.child(mUID).setValue(mToken);
    }

    private void checkUserStatus() {
        //Получаем текущего пользователя
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //Пользователь вошёл в аккаунт
            mUID = user.getUid();

            //Сохраняем uid зарегистрированного пользователя в общих настройках(SharedPreferences)
            SharedPreferences sp = getSharedPreferences("SP_USERS", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID", mUID);
            editor.apply();
        } else {
            //Пользователь не вошел в аккаунт, переходим к StartActivity
            startActivity(new Intent(MainActivity.this, IntroActivity.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        //Проверяем при запуске прилдожения
        checkUserStatus();
        super.onStart();
    }

    @Override
    protected void onResume() {
        checkUserStatus();
        super.onResume();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.exit) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        if (id == R.id.edit) {
            startActivity(new Intent(MainActivity.this, UpdateInfoUserActivity.class));
            finish();
        }
        *//*if (id == R.id.darkTheme) {
            if (preferenceManager.getDarkModeState()) {
                darkMode(false);
            } else {
                darkMode(true);
            }
        }*//*

        return super.onOptionsItemSelected(item);
    }

    *//*private void darkMode(boolean b) {
        preferenceManager.setDarkModeState(b);
        //Toast.makeText(this, "Тёмный режим включён.", Toast.LENGTH_SHORT).show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }, 1000);
    }*/
}
