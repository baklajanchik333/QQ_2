package com.example.qq_2.Fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qq_2.Adapters.AdapterPost;
import com.example.qq_2.IntroActivity;
import com.example.qq_2.Models.ModelPost;
import com.example.qq_2.R;
import com.example.qq_2.UpdateInfoUserActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.shashank.sony.fancytoastlib.FancyToast;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {
    //Элкменты
    private FloatingActionButton addPostBtn;
    private ProgressDialog progressDialog;
    private RecyclerView postRecyclerView;
    private MaterialToolbar toolbar;

    private Dialog popupAddPost;
    private ImageView imageAddPostIv;
    private CircleImageView avatarIv, popAddPostBtn;
    private EditText titleAddPostEt, descAddPostEt;
    private TextView nameTv;

    //БД
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userDbRef;

    private List<ModelPost> postList;
    private AdapterPost adapterPost;

    //Разрешения для контактов
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    //КОнстанты для выбора изображения
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;
    //Массивы для разрешений
    private String[] cameraPermissions;
    private String[] storagePermissions;

    //Uri выбранного изображения
    private Uri image_uri = null;

    //Для информации о пользователе в публикации, которую он выложил
    private String uid, name, email, dp;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        firebaseAuth = FirebaseAuth.getInstance();
        userDbRef = FirebaseDatabase.getInstance().getReference("Users");

        addPostBtn = view.findViewById(R.id.addPostBtn);
        postRecyclerView = view.findViewById(R.id.postRecyclerView);
        nameTv = view.findViewById(R.id.nameTv);

        Intent intent = new Intent();
        //Получаем данные и их тип из намерения
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image")) {
                handleSendImage(intent);
            }
        }

        //Инициализация массивой для разрешений
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(getActivity(), R.style.MyAlertDialogStyle);
        progressDialog.setTitle("Пожалуйста подождите");
        progressDialog.setCanceledOnTouchOutside(false);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //Отображение нового поста в начале списка
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        postRecyclerView.setLayoutManager(layoutManager);

        //Получаем некоторую информацию о текущем пользователе, чтобы включить её в публикацию
        Query query = userDbRef.orderByChild("uid").equalTo(firebaseAuth.getUid());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    name = "" + ds.child("name").getValue();
                    email = "" + ds.child("email").getValue();
                    dp = "" + ds.child("image").getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        addPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPost();
            }
        });

        checkUserStatus();
        postList = new ArrayList<>();

        loadPosts();

        return view;
    }

    private void handleSendImage(Intent intent) {
        //Обработка полученного изображения (URI)
        Uri imageURI = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageURI != null) {
            image_uri = imageURI;
            //Устанаыливаем в imageAddPostIv
            imageAddPostIv.setImageURI(image_uri);
        }
    }

    private void loadPosts() {
        //Путь ко всем постам
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //Получаем все данные из этой ссылки
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelPost modelPost = ds.getValue(ModelPost.class);

                    postList.add(modelPost);

                    //Адаптер
                    adapterPost = new AdapterPost(getActivity(), postList);
                    //Устанавливаем адаптер в RecyclerView
                    postRecyclerView.setAdapter(adapterPost);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Если ошибка
                //Toast.makeText(getActivity(), "Не удалось загрузить публикации", Toast.LENGTH_LONG).show();
                //FancyToast.makeText(getActivity(), "Не удалось загрузить публикации.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
            }
        });
    }

    //Для поиска публикаций по заголовку и/или описанию
    private void searchPosts(final String searchQuery) {
        //Путь ко всем постам
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //Получаем все данные из этой ссылки
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelPost modelPost = ds.getValue(ModelPost.class);

                    if (modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            modelPost.getpDesc().toLowerCase().contains(searchQuery.toLowerCase())) {
                        postList.add(modelPost);
                    }

                    //Адаптер
                    adapterPost = new AdapterPost(getActivity(), postList);
                    //Устанавливаем адаптер в RecyclerView
                    postRecyclerView.setAdapter(adapterPost);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Если ошибка
                //Toast.makeText(getActivity(), "Не удалось загрузить публикации", Toast.LENGTH_LONG).show();
                FancyToast.makeText(getActivity(), "Не удалось загрузить публикации.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
            }
        });
    }

    private void createPost() {
        popupAddPost = new Dialog(getActivity());
        popupAddPost.setContentView(R.layout.popup_add_post);
        popupAddPost.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupAddPost.getWindow().setLayout(Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.WRAP_CONTENT);
        popupAddPost.getWindow().getAttributes().gravity = Gravity.TOP;

        imageAddPostIv = popupAddPost.findViewById(R.id.imageAddPostIv);
        avatarIv = popupAddPost.findViewById(R.id.avatarIv);
        popAddPostBtn = popupAddPost.findViewById(R.id.popAddPostBtn);
        titleAddPostEt = popupAddPost.findViewById(R.id.titleAddPostEt);
        descAddPostEt = popupAddPost.findViewById(R.id.descAddPostEt);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            String image = "" + ds.child("image").getValue();

                            try {
                                Picasso.get().load(image).placeholder(R.drawable.user_default_img).into(avatarIv);
                            } catch (Exception e) {
                                avatarIv.setImageResource(R.drawable.user_default_img);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        popAddPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //ПОлучаем данные (заголовок и описание) из EditText
                String title = titleAddPostEt.getText().toString().trim();
                String description = descAddPostEt.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    //popupProgressBar.setVisibility(View.INVISIBLE);
                    FancyToast.makeText(getActivity(), "Вы должны написать заголовок", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                } else if (TextUtils.isEmpty(description)) {
                    //popupProgressBar.setVisibility(View.INVISIBLE);
                    FancyToast.makeText(getActivity(), "Вы должны написать описание", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                } else {
                    if (image_uri == null) {
                        //Публикация без картинки
                        uploadPost(title, description, "noImage");
                    } else {
                        //Публикация с картинкой
                        uploadPost(title, description, String.valueOf(image_uri));
                    }
                }
            }
        });

        imageAddPostIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickDialog();
            }
        });

        popupAddPost.show();
    }

    private void uploadPost(final String title, final String description, String uri) {
        progressDialog.setMessage("Добавление публикации...");
        progressDialog.show();

        final String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/" + "post_" + timeStamp;

        if (!uri.equals("noImage")) {
            //Публикация с картинкой
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putFile(Uri.parse(uri))
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //Изображение загружено в хранилище Firebase, теперь полуячаем его URL
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful()) ;

                            String downloadUri = uriTask.getResult().toString();

                            if (uriTask.isSuccessful()) {
                                //URL получен, загружаем пост в базу данных Firebase
                                HashMap<Object, String> hashMap = new HashMap<>();
                                //ПОмезаем информацию
                                hashMap.put("uid", "" + uid);
                                hashMap.put("uName", "" + name);
                                hashMap.put("uEmail", "" + email);
                                hashMap.put("uDp", "" + dp);
                                hashMap.put("pId", "" + timeStamp);
                                hashMap.put("pTitle", "" + title);
                                hashMap.put("pDesc", "" + description);
                                hashMap.put("pImage", "" + downloadUri);
                                hashMap.put("pTime", "" + timeStamp);
                                hashMap.put("pLikes", "0");
                                hashMap.put("pComments", "0");

                                //Путь для хранения данных публикаций
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                //Помещаем данные в ссылку
                                ref.child(timeStamp).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //Добавлено в базу данных
                                                progressDialog.dismiss();
                                                FancyToast.makeText(getActivity(), "Публикация прошла успешно.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();

                                                titleAddPostEt.setText("");
                                                descAddPostEt.setText("");
                                                imageAddPostIv.setImageURI(null);
                                                image_uri = null;
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //Не удалось добавить запись в базу данных
                                                progressDialog.dismiss();
                                                FancyToast.makeText(getActivity(), "Не удалось опубликовать.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Не удалось загрузить изображение
                            progressDialog.dismiss();
                            FancyToast.makeText(getActivity(), "Не удалось опубликовать.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                        }
                    });
        } else {
            //Публикация без картинки
            //URL получен, загружаем пост в базу данных Firebase
            HashMap<Object, String> hashMap = new HashMap<>();
            //ПОмезаем информацию
            hashMap.put("uid", "" + uid);
            hashMap.put("uName", "" + name);
            hashMap.put("uEmail", "" + email);
            hashMap.put("uDp", "" + dp);
            hashMap.put("pId", "" + timeStamp);
            hashMap.put("pTitle", "" + title);
            hashMap.put("pDesc", "" + description);
            hashMap.put("pImage", "noImage");
            hashMap.put("pTime", "" + timeStamp);
            hashMap.put("pLikes", "0");
            hashMap.put("pComments", "0");

            //Путь для хранения данных публикаций
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
            //Помещаем данные в ссылку
            ref.child(timeStamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //Добавлено в базу данных
                            progressDialog.dismiss();
                            FancyToast.makeText(getActivity(), "Публикация прошла успешно.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();

                            titleAddPostEt.setText("");
                            descAddPostEt.setText("");
                            imageAddPostIv.setImageURI(null);
                            image_uri = null;
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Не удалось добавить запись в базу данных
                            progressDialog.dismiss();
                            FancyToast.makeText(getActivity(), "Не удалось опубликовать.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                        }
                    });
        }
    }

    private void checkUserStatus() {
        //Получаем текущего пользователя
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //Пользователь вошёл в аккаунт
            email = user.getEmail();
            uid = user.getUid();
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

        //ПОиск поста по заголовку или описанию
        MenuItem item = menu.findItem(R.id.search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        //ПОиск
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Происходит когда пользователь нажимает кнопку поиска на клавиатуре
                //Если строка поиска не пустая, то выполняется поиск
                if (!TextUtils.isEmpty(query)) {
                    searchPosts(query);
                } else {
                    //Если строка поиска пустая, то отображаются все пользователи
                    loadPosts();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                //Происходит когда пользователь нажимает на любую кнопку на клавиатуре
                //Если строка поиска не пустая, то выполняется поиск
                if (!TextUtils.isEmpty(query)) {
                    searchPosts(query);
                } else {
                    //Если строка поиска пустая, то отображаются все пользователи
                    loadPosts();
                }

                return false;
            }
        });

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

    //<editor-fold desc="ДЛя добавления фото">
    private void showImagePickDialog() {
        //Пункты в диалоговом окне
        String[] options = {"Камера", "Галлерея"};

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
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

        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(getActivity(), storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(getActivity(), cameraPermissions, CAMERA_REQUEST_CODE);
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
                        FancyToast.makeText(getActivity(), "Необходимо разрешить приложению доступ к камере.", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
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
                        FancyToast.makeText(getActivity(), "Необходимо разрешить приложению доступ к галлереи.", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                    }
                }
            }
            break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //Получаем выбранное изображение
                image_uri = data.getData();
                //Устанавливаем выбранное изображение
                imageAddPostIv.setImageURI(image_uri);
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //Устанавливаем выбранное изображение
                imageAddPostIv.setImageURI(image_uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    //</editor-fold>


    @Override
    public void onStart() {
        super.onStart();
        checkUserStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkUserStatus();
    }
}
