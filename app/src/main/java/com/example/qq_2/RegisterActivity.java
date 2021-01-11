package com.example.qq_2;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {
    //Элементы
    private TextInputEditText emailEt, passwordEt, password2Et;
    private MaterialButton registerBtnReg, backToLoginBtn;
    private ProgressDialog progressDialog;

    //БД
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();

        //<editor-fold desc="Инициализация элементов">
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);
        password2Et = findViewById(R.id.password2Et);

        registerBtnReg = findViewById(R.id.registerBtnReg);
        backToLoginBtn = findViewById(R.id.backToLoginBtn);
        //</editor-fold>

        //<editor-fold desc="Обработка нажатий">
        //Нажатие на registerBtn
        registerBtnReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Вводим email и пароль
                String email = emailEt.getText().toString().trim();
                String password = passwordEt.getText().toString().trim();
                String password2 = password2Et.getText().toString().trim();

                //Проверяем правильность введённых данных
                if (email.isEmpty() || password.isEmpty() || password2.isEmpty()) {
                    //Если все пустые поля
                    FancyToast.makeText(RegisterActivity.this, "Заполните все поля.", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    //Если неправильно введена почта
                    FancyToast.makeText(RegisterActivity.this, "Неверный адрес электронной почты.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                } else if (password.length() < 6) {
                    //Если неправильно введен пароль
                    FancyToast.makeText(RegisterActivity.this, "Пароль должен содержать не менее 6 символов.", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                } else if (!password.equals(password2)) {
                    //Если неправильно введен пароль
                    FancyToast.makeText(RegisterActivity.this, "Пароли не совпадают.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                } else {
                    registerUsers(email, password);
                }
            }
        });

        //Нажатие на backToLoginBtn
        backToLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
        //</editor-fold>
    }

    private void registerUsers(String email, String password) {
        //Если пройдена проверка, выполняется регистрация аккаунта
        //Отображение progressDialog при нажатии на кнопку Зарегистрироваться
        createProgressDialog();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //Если всё успешно, убирается полоса загрузки, показывается сообщение и выполняется переход на MainActivity
                            progressDialog.dismiss();

                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            //Получаем электронную почту и идентификатор пользователя от auth
                            String email = user.getEmail();
                            String uid = user.getUid();
                            //Когда пользователь зарегистрирован, сохраняем информацию о нём в базе данных реального времени Firebase, используя HashMap.
                            HashMap<Object, String> hashMap = new HashMap<>();
                            //Заносим информацию в hashMap
                            hashMap.put("email", email);
                            hashMap.put("uid", uid);
                            hashMap.put("name", "");
                            hashMap.put("onlineStatus", "online");
                            hashMap.put("typingTo", "noOne");
                            hashMap.put("age", "");
                            hashMap.put("country", "");
                            hashMap.put("city", "");
                            hashMap.put("phone", "");
                            hashMap.put("image", "");
                            hashMap.put("cover", "");
                            //Экземпляр базы данных Firebase
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            //Задаём путь для хранения пользовательских данных с именем «Users»
                            DatabaseReference reference = database.getReference("Users");
                            //Помещаем данные hashMap в базу данных
                            reference.child(uid).setValue(hashMap);

                            FancyToast.makeText(RegisterActivity.this, "Аккаунт " + user.getEmail() + " зарегестрирован", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false);

                            startActivity(new Intent(RegisterActivity.this, AddInfoUserActivity.class));
                            finish();
                        } else {
                            //Если не удаётся зарегистрироваться
                            progressDialog.dismiss();
                            FancyToast.makeText(RegisterActivity.this, "Ошибка аутентификации.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Если ошибка при регистрации, то полоса загрузки скрывается и выдаётся сообщение об ошибке
                        progressDialog.dismiss();
                        FancyToast.makeText(RegisterActivity.this, "Произошла ошибка сети (например, время ожидания истекло, прерванное соединение или недоступный хост).", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                    }
                });
    }

    public void createProgressDialog() {
        progressDialog = new ProgressDialog(this, R.style.MyAlertDialogStyle);
        progressDialog.setMessage("Создание аккаунта...");
        progressDialog.show();
    }
}
