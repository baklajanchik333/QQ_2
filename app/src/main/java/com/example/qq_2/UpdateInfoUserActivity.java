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
import android.view.View;

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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.shashank.sony.fancytoastlib.FancyToast;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class UpdateInfoUserActivity extends AppCompatActivity {
    //Элементы
    private CircleImageView profileImg;
    private TextInputEditText nameEt, ageEt, countryEt, cityEt, phoneEt;
    private MaterialButton continueBtn, cancelBtn;
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

    private String name, age, country, city, phone, uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_info_user);

        firebaseAuth = FirebaseAuth.getInstance();

        //<editor-fold desc="Инициализация элементов">
        profileImg = findViewById(R.id.profileImg);

        nameEt = findViewById(R.id.nameEt);
        ageEt = findViewById(R.id.ageEt);
        countryEt = findViewById(R.id.countryEt);
        cityEt = findViewById(R.id.cityEt);
        phoneEt = findViewById(R.id.phoneEt);

        continueBtn = findViewById(R.id.continueBtn);
        cancelBtn = findViewById(R.id.cancelBtn);
        //</editor-fold>

        //Инициализация массивой для разрешений
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this, R.style.MyAlertDialogStyle);
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

        //Нажатие на cancelBtn
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UpdateInfoUserActivity.this, MainActivity.class));
                finish();
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

        checkUser();
    }

    private void inputData() {
        //Входные данные
        name = nameEt.getText().toString().trim();
        age = ageEt.getText().toString().trim();
        country = countryEt.getText().toString().trim();
        city = cityEt.getText().toString().trim();
        phone = phoneEt.getText().toString().trim();

        updateProfile();
    }

    private void updateProfile() {
        progressDialog.setMessage("Обновление информации...");
        progressDialog.show();

        if (image_uri == null) {
            //Обновление без изображения
            //Настраиваем данные для обновления
            final HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", "" + name);
            hashMap.put("age", "" + age);
            hashMap.put("country", "" + country);
            hashMap.put("city", "" + city);
            hashMap.put("phone", "" + phone);

            //Сохраняем данные в БД
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).updateChildren(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //Данные обновлены
                            progressDialog.dismiss();
                            FancyToast.makeText(UpdateInfoUserActivity.this, "Информация обновлена", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                            startActivity(new Intent(UpdateInfoUserActivity.this, MainActivity.class));
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Ошибка при обновлении данных в БД
                            progressDialog.dismiss();
                            FancyToast.makeText(UpdateInfoUserActivity.this, "" + e.getMessage(), FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                        }
                    });
        } else {
            //Обновление с изображением
            //Сначала обновляем изображение
            //Имя и путь к изображению
            String fileNameAndPath = "Profile_and_cover_image/" + "" + firebaseAuth.getUid();

            //Сохранение изображения
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(fileNameAndPath);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //Изображение обновлено, получаем URL загруженного изображения
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful()) ;
                            Uri downloadImageUri = uriTask.getResult();

                            if (uriTask.isSuccessful()) {
                                //URL получен, обновляем данные в БД
                                //Настраиваем данные для обновления
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("name", "" + name);
                                hashMap.put("age", "" + age);
                                hashMap.put("country", "" + country);
                                hashMap.put("city", "" + city);
                                hashMap.put("phone", "" + phone);
                                hashMap.put("image", "" + downloadImageUri);

                                //Сохраняем данные в БД
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                                ref.child(firebaseAuth.getUid()).updateChildren(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //Данные обновлены
                                                progressDialog.dismiss();
                                                FancyToast.makeText(UpdateInfoUserActivity.this, "Информация обновлена", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //Ошибка при обновлении данных в БД
                                                progressDialog.dismiss();
                                                FancyToast.makeText(UpdateInfoUserActivity.this, "" + e.getMessage(), FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            FancyToast.makeText(UpdateInfoUserActivity.this, "" + e.getMessage(), FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                        }
                    });
        }
    }

    private void loadMyInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            String name = "" + ds.child("name").getValue();
                            String age = "" + ds.child("age").getValue();
                            String city = "" + ds.child("city").getValue();
                            String country = "" + ds.child("country").getValue();
                            String phone = "" + ds.child("phone").getValue();
                            String image = "" + ds.child("image").getValue();

                            nameEt.setText(name);
                            ageEt.setText(age);
                            phoneEt.setText(phone);
                            countryEt.setText(country);
                            cityEt.setText(city);

                            try {
                                Picasso.get().load(image).placeholder(R.drawable.user_default_img).into(profileImg);
                            } catch (Exception e) {
                                profileImg.setImageResource(R.drawable.user_default_img);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(getApplicationContext(), StartActivity.class));
            finish();
        } else {
            loadMyInfo();
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
                        FancyToast.makeText(this, "Необходимо разрешить приложению доступ к камере.", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
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
                        FancyToast.makeText(this, "Необходимо разрешить приложению доступ к галлереи.", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
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
