package com.example.qq_2;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddInfoUserActivity extends AppCompatActivity {
    //Элементы
    private CircleImageView profileImg;
    private TextInputEditText nameEt, ageEt, countryEt, cityEt, phoneEt;
    private MaterialButton continueBtn;
    private ProgressDialog progressDialog;

    //БД
    private FirebaseAuth firebaseAuth;

    //Разрешения для контактов
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    //КОнстанты для выбора изображения
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;
    //Массивы для разрешений
    private String[] cameraPermissions;
    private String[] storagePermissions;

    //Для выбора изображения
    private Uri image_uri;

    private String name, age, country, city, phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_info_user);

        firebaseAuth = FirebaseAuth.getInstance();

        //<editor-fold desc="Инициализация элементов">
        profileImg = findViewById(R.id.profileImg);

        nameEt = findViewById(R.id.nameEt);
        ageEt = findViewById(R.id.ageEt);
        countryEt = findViewById(R.id.countryEt);
        cityEt = findViewById(R.id.cityEt);
        phoneEt = findViewById(R.id.phoneEt);

        continueBtn = findViewById(R.id.continueBtn);
        //</editor-fold>

        //Инициализация массивой для разрешений
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Пожалуйста подождите...");
        progressDialog.setCanceledOnTouchOutside(false);

        //<editor-fold desc="Обработка нажатий">
        //Нажатие на continueBtn
        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Добавляем информацию профиля
                inputData();
            }
        });

        //Нажатие на profileImg
        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Выбираем изображение
                showImagePickDialog();
            }
        });
        //</editor-fold>
    }

    private void inputData() {
        //Входные данные
        name = nameEt.getText().toString().trim();
        age = ageEt.getText().toString().trim();
        country = countryEt.getText().toString().trim();
        city = cityEt.getText().toString().trim();
        phone = phoneEt.getText().toString().trim();

        //Проверяем данные
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(age) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Вы обязательно долны указать имя, возраст и телефон.", Toast.LENGTH_SHORT).show();
        } else {
            saveInfoUser();
        }
    }

    private void saveInfoUser() {
        progressDialog.setMessage("Сохранение информации...");

        //final String timestamp = "" + System.currentTimeMillis();

        if (image_uri == null) {
            //Сохраняем информацию без изображения
            //Настраиваем данные для сохранения
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", "" + name);
            hashMap.put("age", "" + age);
            hashMap.put("country", "" + country);
            hashMap.put("city", "" + city);
            hashMap.put("phone", "" + phone);
            //hashMap.put("timestamp", "" + timestamp);
            hashMap.put("image", "");

            //Сохраняем данные в БД
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).updateChildren(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //Данные сохранены
                            progressDialog.dismiss();
                            startActivity(new Intent(AddInfoUserActivity.this, MainActivity.class));
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Ошибка при сохранении данных в БД
                            progressDialog.dismiss();
                            startActivity(new Intent(AddInfoUserActivity.this, MainActivity.class));
                            finish();
                        }
                    });
        } else {
            //Сохраняем информацию с изображением
            //Имя и путь к изображению
            String fileNameAndPath = "Profile_and_cover_image/" + "" + firebaseAuth.getUid();

            //Сохранение изображения
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(fileNameAndPath);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //Получаем URL загруженного изображения
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful()) ;
                            Uri downloadImageUri = uriTask.getResult();

                            if (uriTask.isSuccessful()) {
                                //Настраиваем данные для сохранения
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("name", "" + name);
                                hashMap.put("age", "" + age);
                                hashMap.put("country", "" + country);
                                hashMap.put("city", "" + city);
                                hashMap.put("phone", "" + phone);
                                //hashMap.put("timestamp", "" + timestamp);
                                hashMap.put("image", "" + downloadImageUri); //URL загруженного изображения

                                //Сохраняем данные в БД
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                                ref.child(firebaseAuth.getUid()).updateChildren(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //Данные сохранены
                                                progressDialog.dismiss();
                                                startActivity(new Intent(AddInfoUserActivity.this, MainActivity.class));
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //Ошибка при сохранении данных в БД
                                                progressDialog.dismiss();
                                                startActivity(new Intent(AddInfoUserActivity.this, MainActivity.class));
                                                finish();
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            FancyToast.makeText(AddInfoUserActivity.this, "" + e.getMessage(), FancyToast.LENGTH_LONG, FancyToast.ERROR, false).show();
                        }
                    });
        }

    }

    //<editor-fold desc="ДЛя добавления фото">
    private void showImagePickDialog() {
        //Пункты в диалоговом окне
        String[] options = {"Камера", "Галлерея"};

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setBackground(getResources().getDrawable(R.drawable.alert_dialog_bg));
        builder.setIcon(R.drawable.update_photo_img);
        builder.setTitle("Выбирете изображение из:")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Обработка нажатий на пункты
                        if (which == 0) {
                            //Камера
                            if (checkCameraPermission()) {
                                //Разрешения приняты
                                pickFromCamera();
                            } else {
                                //Разрешения не приняты
                                requestCameraPermission();
                            }
                        } else {
                            //Галлерея
                            if (checkStoragePermission()) {
                                //Разрешения приняты
                                pickFromGallery();
                            } else {
                                //Разрешения не приняты
                                requestStoragePermission();
                            }
                        }
                    }
                })
                .show();
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Заголовок временного изображения");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Описание временного изображения");

        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        //Разрешение принято
                        pickFromCamera();
                    } else {
                        //Разрешение откланено
                        Toast.makeText(this, "Необходимо разрешить приложению доступ к камере...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        //Разрешение принято
                        pickFromGallery();
                    } else {
                        //Разрешение откланено
                        Toast.makeText(this, "Необходимо разрешить приложению доступ к галлереи...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //Получаем выбранное изображение
                image_uri = data.getData();
                //Устанавливаем выбранное изображение
                profileImg.setImageURI(image_uri);
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //Устанавливаем выбранное изображение
                profileImg.setImageURI(image_uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    //</editor-fold>
}
