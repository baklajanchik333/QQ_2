package com.example.qq_2.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.example.qq_2.IntroActivity;
import com.example.qq_2.R;
import com.example.qq_2.UpdateInfoUserActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.shashank.sony.fancytoastlib.FancyToast;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends Fragment {
    //Элементы
    private MaterialToolbar toolbar;
    private MaterialButton updatePassBtn;

    //БД
    private FirebaseAuth firebaseAuth;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle("Настройки");

        firebaseAuth = FirebaseAuth.getInstance();

        updatePassBtn = view.findViewById(R.id.updatePassBtn);

        updatePassBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangePasswordDialog();
            }
        });

        return view;
    }

    private void showChangePasswordDialog() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_update_password, null);
        final TextInputEditText passwordEt = view.findViewById(R.id.passEd);
        final TextInputEditText newPasswordEt = view.findViewById(R.id.passConEd);
        MaterialButton updateBtn = view.findViewById(R.id.updateBtn);

        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity()); //Создаём диалоговое окно
        builder.setBackground(getResources().getDrawable(R.drawable.alert_dialog_bg));
        builder.setView(view);
        builder.show();

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Проверяем данные
                String oldPass = passwordEt.getText().toString().trim();
                String newPass = newPasswordEt.getText().toString().trim();

                if (TextUtils.isEmpty(oldPass)) {
                    FancyToast.makeText(getContext(), "Введите Ваш старый пароль.", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                    return;
                } else if (newPass.length() < 6) {
                    FancyToast.makeText(getContext(), "Пароль должен содержать не меньше 6 символов.", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                    return;
                } else {
                    updatePassword(oldPass, newPass);
                }
            }
        });
    }

    private void updatePassword(String oldPass, final String newPass) {
        final FirebaseUser user = firebaseAuth.getCurrentUser();

        //Перед сменой пароля проверяем аутентификацию пользователя
        AuthCredential authCredential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);
        user.reauthenticate(authCredential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Всё успешно
                        user.updatePassword(newPass)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //Пароль обновлён
                                        FancyToast.makeText(getContext(), "Пароль успешно обновлён.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //Пароль не обновлён
                                        FancyToast.makeText(getContext(), "Ошибка! Невозможно обновить пароль.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        FancyToast.makeText(getContext(), "Ошибка! Невозможно обновить пароль.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                    }
                });
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