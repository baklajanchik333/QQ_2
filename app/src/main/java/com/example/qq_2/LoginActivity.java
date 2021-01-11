package com.example.qq_2;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {
    //Элементы
    private TextInputEditText emailEt, passwordEt;
    private MaterialButton loginBtnLog, forgotPassBtn, backToRegisterBtn;
    private ProgressDialog progressDialog;
    SignInButton logGoogle;

    //БД
    FirebaseAuth firebaseAuth;

    //Для Google
    private static final int RC_SIGN_IN = 100;
    GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Обязательно надо перед firebaseAuth
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        firebaseAuth = FirebaseAuth.getInstance();

        //<editor-fold desc="Инициализация элементов">
        emailEt = findViewById(R.id.emailEt);
        passwordEt = findViewById(R.id.passwordEt);

        loginBtnLog = findViewById(R.id.loginBtnLog);
        forgotPassBtn = findViewById(R.id.forgotPassBtn);
        backToRegisterBtn = findViewById(R.id.backToRegisterBtn);
        logGoogle = findViewById(R.id.logGoogle);
        //</editor-fold>

        //Нажатие на loginBtn
        loginBtnLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Вводим данные
                String email = emailEt.getText().toString().trim();
                String password = passwordEt.getText().toString().trim();

                //Проверяем
                if (email.isEmpty() || password.isEmpty()) {
                    //Если пустые поля
                    FancyToast.makeText(LoginActivity.this, "Заполните все поля.", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    //Если неправильно введена почта
                    FancyToast.makeText(LoginActivity.this, "Неверный адрес электронной почты.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                } else if (password.length() < 6) {
                    //Если неправильно введен пароль
                    FancyToast.makeText(LoginActivity.this, "Неверный пароль.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                } else {
                    loginUsers(email, password);
                }
            }
        });

        //Нажатие на backToRegisterBtn
        backToRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                finish();
            }
        });

        //Нажатие на forgotPass
        forgotPassBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecoverPasswordDialog();
            }
        });

        //Нажатие googleLogBtn
        logGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });
    }

    private void showRecoverPasswordDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(LoginActivity.this); //Создаём диалоговое окно
        builder.setTitle("Восстановление пароля"); //Заголовок

        LinearLayout linearLayout = new LinearLayout(this);
        final EditText editText = new EditText(this);
        editText.setHint("Email");
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        editText.setMinEms(16);

        linearLayout.addView(editText);
        linearLayout.setPadding(16, 16, 16, 16);

        builder.setView(linearLayout);

        builder.setIcon(R.drawable.forgot_password_img); //Иконка
        builder.setBackground(getResources().getDrawable(R.drawable.alert_dialog_bg));
        builder.setMessage("Введите Ваш Email, чтобы мы отправили на него письмо с ссылкой для восстановление пароля");

        builder.setPositiveButton("Отправить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Вводим данные
                String email = editText.getText().toString().trim();

                beginRecovery(email);
            }
        });

        builder.show();
    }

    private void beginRecovery(String email) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            FancyToast.makeText(LoginActivity.this, "Письмо отправлено.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                        } else {
                            FancyToast.makeText(LoginActivity.this, "Письмо не отправлено. Адрес электронной почты не верного формата.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                        }
                    }
                });
    }

    private void loginUsers(String email, String password) {
        createProgressDialog();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //Если всё успешно, убирается полоса загрузки, показывается сообщение и выполняется переход на MainActivity
                            progressDialog.dismiss();
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            //Если не удаётся войти
                            progressDialog.dismiss();
                            FancyToast.makeText(LoginActivity.this, "Ошибка входа.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Если ошибка при входе, то полоса загрузки скрывается и выдаётся сообщение об ошибке
                        progressDialog.dismiss();
                        FancyToast.makeText(LoginActivity.this, "Произошла ошибка сети (например, время ожидания истекло, прерванное соединение или недоступный хост).", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                    }
                });
    }

    public void createProgressDialog() {
        progressDialog = new ProgressDialog(this, R.style.MyAlertDialogStyle);
        progressDialog.setMessage("Вход в аккаунт...");
        progressDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                FancyToast.makeText(LoginActivity.this, "Произошла ошибка сети (например, время ожидания истекло, прерванное соединение или недоступный хост).", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //Если всё успешно, убирается полоса загрузки, показывается сообщение и выполняется переход на MainActivity
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            //Если пользователь вошел в систему в первый раз, то получаем и показываем информацию о пользователе из аккаунта Google
                            if (task.getResult().getAdditionalUserInfo().isNewUser()) {
                                //Получаем электронную почту и идентификатор пользователя от auth
                                String email = user.getEmail();
                                String uid = user.getUid();
                                //String name = user.getDisplayName();
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
                            }

                            startActivity(new Intent(LoginActivity.this, AddInfoUserActivity.class));
                            finish();
                        } else {
                            //Если не удаётся войти
                            FancyToast.makeText(LoginActivity.this, "Ошибка входа.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Если ошибка при входе, то полоса загрузки скрывается и выдаётся сообщение об ошибке
                        FancyToast.makeText(LoginActivity.this, "Произошла ошибка сети (например, время ожидания истекло, прерванное соединение или недоступный хост).", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                    }
                });
    }
}
