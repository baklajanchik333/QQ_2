package com.example.qq_2.Fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.qq_2.FeedBackActivity;
import com.example.qq_2.IntroActivity;
import com.example.qq_2.R;
import com.example.qq_2.UpdateInfoUserActivity;
import com.example.qq_2.WriteReviewActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * A simple {@link Fragment} subclass.
 */
public class MoreFragment extends Fragment {
    //Элементы
    private MaterialButton settingsBtn, aboutAppBtn, ratingBtn, feedbackBtn, exitBtn;
    private MaterialToolbar toolbar;

    //БД
    private FirebaseAuth firebaseAuth;

    private String title = "";

    public MoreFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle("Ещё");

        firebaseAuth = FirebaseAuth.getInstance();

        //<editor-fold desc="Инициализация элементов">
        settingsBtn = view.findViewById(R.id.settingsBtn);
        aboutAppBtn = view.findViewById(R.id.aboutAppBtn);
        ratingBtn = view.findViewById(R.id.ratingBtn);
        feedbackBtn = view.findViewById(R.id.feedbackBtn);
        exitBtn = view.findViewById(R.id.exitBtn);
        //</editor-fold>

        //Нажатие на settingsBtn
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsFragment fragment5 = new SettingsFragment();
                FragmentTransaction ft5 = getActivity().getSupportFragmentManager().beginTransaction();
                ft5.replace(R.id.content, fragment5, "");
                ft5.commit();
            }
        });

        //Нажатие на aboutAppBtn
        aboutAppBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity()); //Создаём диалоговое окно
                builder.setTitle("О приложении"); //Заголовок
                builder.setMessage("Версия: 1.0\nРазработчик: Сизов Валентин");
                builder.setIcon(R.drawable.about_app_img); //Иконка
                builder.setBackground(getResources().getDrawable(R.drawable.alert_dialog_bg));

                builder.setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();
            }
        });

        //Нажатие на ratingBtn
        ratingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), WriteReviewActivity.class));
                getActivity().finish();
            }
        });

        //Нажатие на feedbackBtn
        feedbackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), FeedBackActivity. class));
                getActivity().finish();
            }
        });

        //Нажатие на exitBtn
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                checkUserStatus();
            }
        });

        return view;
    }

    private void checkUserStatus() {
        //Получаем текущего пользователя
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //Пользователь вошёл в аккаунт
        } else {
            //Пользователь не вошел в аккаунт, переходим к StartActivity
            startActivity(new Intent(getActivity(), IntroActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true); //ДлЯ отображения меню на фрагменте
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        //Скрываем иконку редактирования из меню (3 точки), т.к. она здесь не нужна
        menu.findItem(R.id.edit).setVisible(false);
        menu.findItem(R.id.search).setVisible(false);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.exit) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        if (id == R.id.edit) {
            startActivity(new Intent(getActivity(), UpdateInfoUserActivity.class));
            getActivity().finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
