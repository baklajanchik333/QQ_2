package com.example.qq_2;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qq_2.Adapters.AdapterComment;
import com.example.qq_2.Models.ModelComments;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import com.shashank.sony.fancytoastlib.FancyToast;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostDetailActivity extends AppCompatActivity {
    //Элементы
    private EditText commentEt;
    private ImageButton sendBtn;
    private CircleImageView cAvatarIv;
    private MaterialToolbar toolbar;

    private ProgressDialog progressDialog;

    private ImageView postImageIv;
    private CircleImageView avatarIv;
    private TextView userNameTv, timePostTv, titlePostTv, descPostTv, pCommentsTv, pLikesTv;
    private ImageButton moreBtn, likeBtn, shareBtn;
    private LinearLayout profileLayout;
    private RecyclerView recyclerView;

    //БД
    private FirebaseAuth firebaseAuth;

    //Чтобы получить детализацию пользователя и поста
    private String hisUid, myUid, myEmail, myName, myDp, postId, pLikes, hisDp, hisName, pImage;

    private boolean mProcessComment = false;
    private boolean mProcessLike = false;

    private List<ModelComments> commentList;
    private AdapterComment adapterComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        toolbar = findViewById(R.id.toolbar);
        getSupportActionBar();
        toolbar.setTitle("Информация о публикации");

        firebaseAuth = FirebaseAuth.getInstance();

        //Получаем идентификатор сообщения с помощью Intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        //<editor-fold desc="Инициализация элементов">
        //Для публикации
        avatarIv = findViewById(R.id.avatarIv);
        postImageIv = findViewById(R.id.postImageIv);

        userNameTv = findViewById(R.id.userNameTv);
        timePostTv = findViewById(R.id.timePostTv);
        titlePostTv = findViewById(R.id.titlePostTv);
        descPostTv = findViewById(R.id.descPostTv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        pLikesTv = findViewById(R.id.pLikesTv);

        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
        shareBtn = findViewById(R.id.shareBtn);

        profileLayout = findViewById(R.id.profileLayout);
        recyclerView = findViewById(R.id.recyclerView);

        //Для добавления комментария
        commentEt = findViewById(R.id.commentEt);
        sendBtn = findViewById(R.id.sendBtn);
        cAvatarIv = findViewById(R.id.cAvatarIv);
        //</editor-fold>

        //Нажатие на sendBtn
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postComment();
            }
        });

        //Нажатие на likeBtn
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likePost();
            }
        });

        //Нажатие на moreBtn
        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptions();
            }
        });

        //Нажатие на hareBtn
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pTitle = titlePostTv.getText().toString().trim();
                String pDescription = descPostTv.getText().toString().trim();

                //Некоторые публикации содержат только текст, а некоторые содержат изображения и текст, поэтому будем обрабатывать оба варианта
                //Получаем изображение из ImageView
                BitmapDrawable bitmapDrawable = (BitmapDrawable) postImageIv.getDrawable();
                if (bitmapDrawable == null) {
                    //Публикация без картинки
                    shareTextOnly(pTitle, pDescription);
                } else {
                    //Публикация с картинкой
                    //Конвертируем изображение в растровое изображение (Bitmap)
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle, pDescription, bitmap);
                }
            }
        });

        loadPostInfo();
        checkUserStatus();
        loadUserInfo();
        setLikes();
        loadComments();
    }

    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {
        //Объединяем заголовок и описание, чтобы поделиться
        String shareBody = pTitle + "\n" + pDescription;

        //Сначала сохраним это изображение в кеше, получаем URL сохраненного изображения
        Uri uri = saveImageToShare(bitmap);

        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        sIntent.setType("image/png");
        startActivity(Intent.createChooser(sIntent, "Share Via"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(getCacheDir(), "images");
        Uri uri = null;

        try {
            imageFolder.mkdirs(); //Создаём, если не существует
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this, "com.example.qq_2.fileprovider", file);
        } catch (Exception e) {
            FancyToast.makeText(this, "" + e.getMessage(), FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
        }

        return uri;
    }

    private void shareTextOnly(String pTitle, String pDescription) {
        //Объединяем заголовок и описание, чтобы поделиться
        String shareBody = pTitle + "\n" + pDescription;

        //
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here"); //Если поделимся через приложение электронной почты
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody); //Текст для обмена
        startActivity(Intent.createChooser(sIntent, "Share Via")); //Сообщение для отображения в диалоговом окне обмена
    }

    private void loadComments() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);

        commentList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelComments modelComments = ds.getValue(ModelComments.class);

                    commentList.add(modelComments);

                    adapterComment = new AdapterComment(getApplicationContext(), commentList, myUid, postId);
                    recyclerView.setAdapter(adapterComment);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showMoreOptions() {
        //Создаём всплывающее меню и добавляем туда опцию "Удалить"
        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);
        //Показывать параметры удаления только в постах, которые зарегистрированы на данный момент.
        if (hisUid.equals(myUid)) {
            //Добавление пунктов (опций)
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Удалить");
            //popupMenu.getMenu().add(Menu.NONE, 1, 0, "Редактировать");
        }

        //Нажатие на пункты меню
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == 0) {
                    //Нажатие на пункт "Удалить"
                    beginDelete();
                } else if (id == 1) {
                    //Нажатие на пункт "Редактировать"
                    //Intent intent = new Intent(context, );
                }

                return false;
            }
        });

        popupMenu.show();
    }

    private void beginDelete() {
        //Публикация с изображением или безз него
        if (pImage.equals("noImage")) {
            //Публикация без изображения
            deletePostWithoutImage();
        } else {
            //Публикация с изображением
            deletePostWithImage();
        }
    }

    //Удаление публикации с изображением
    private void deletePostWithImage() {
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Удалить");
        builder.setMessage("Вы действительно хотите удалить публикацию?");
        builder.setIcon(R.drawable.delete_img);
        builder.setBackground(this.getResources().getDrawable(R.drawable.alert_dialog_bg));

        builder.setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressDialog.setMessage("Удаление...");

                /*Шаги:
                1) Удаляем изображение с помощью URL
                2) Удаляем из базы данных, используя идентификатор сообщения*/
                StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
                picRef.delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //Изображение удалено, теперь удаляем из базы
                                Query fQuery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                                fQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                            ds.getRef().removeValue(); //удаляем значения из Firebase, где соответствует pId
                                        }

                                        //Когда удалено
                                        progressDialog.dismiss();
                                        FancyToast.makeText(PostDetailActivity.this, "Публикация удалена.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //Есди ошибка
                                progressDialog.dismiss();
                                FancyToast.makeText(PostDetailActivity.this, "Ошибка! Не удалось удалить публикацию.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                            }
                        });
            }
        });

        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    //Удаление публикации без изображением
    private void deletePostWithoutImage() {
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Удалить");
        builder.setMessage("Вы действительно хотите удалить публикацию?");
        builder.setIcon(R.drawable.delete_img);
        builder.setBackground(this.getResources().getDrawable(R.drawable.alert_dialog_bg));

        builder.setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressDialog.setMessage("Удаление...");

                /*Шаги:
                1) Удаляем изображение с помощью URL
                2) Удаляем из базы данных, используя идентификатор сообщения*/
                //Изображение удалено, теперь удаляем из базы
                Query fQuery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                fQuery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            ds.getRef().removeValue(); //удаляем значения из Firebase, где соответствует pId
                        }

                        //Когда удалено
                        progressDialog.dismiss();
                        FancyToast.makeText(PostDetailActivity.this, "Публикация удалена.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void setLikes() {
        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postId).hasChild(myUid)) {
                    //Пользователю понравилась публикация
                    /*Чтобы указать, что эта запись нравится этому (зарегистрированному) пользователю
                    Изменяем нарисованный левый значок кнопки «Мне нравится»*/
                    likeBtn.setImageResource(R.drawable.ic_like_red);
                } else {
                    //Пользователю не понравилась публикация
                    likeBtn.setImageResource(R.drawable.ic_like);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void likePost() {
        /*Получаем общее количество лайков публикации,
        чья кнопка лайка нажата, если в данный момент вошедший в систему пользователь не понравился,
        прежде чем увеличить значение на 1, в противном случае уменьшить значение на 1*/ // Фигян написана не русская
        mProcessLike = true;
        //Получить идентификатор публикации, которую лайкнули
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        final DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessLike) {
                    if (dataSnapshot.child(postId).hasChild(myUid)) {
                        //Если лайк уже поставлен, убираем его
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) - 1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;
                    } else {
                        //Если нет лайка, ставим его
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) + 1));
                        likesRef.child(postId).child(myUid).setValue("Liked"); //Устанавливаем любое значение
                        mProcessLike = false;

                        //addToHisNotifications("" + hisUid, "" + postId, "Оценили вашу публикацию");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void postComment() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Комментарий добавляется...");

        //Получаем данные из поля ввода комментария
        String comment = commentEt.getText().toString().trim();
        //Проверка
        if (TextUtils.isEmpty(comment)) {
            FancyToast.makeText(this, "Вы не можете добавить пустой комментарий.", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
            return;
        }

        String timeStamp = String.valueOf(System.currentTimeMillis());

        //Добавляется дочерний узел "Comments" для каждого поста
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("cId", timeStamp);
        hashMap.put("comment", comment);
        hashMap.put("timestamp", timeStamp);
        hashMap.put("uid", myUid);
        hashMap.put("uEmail", myEmail);
        hashMap.put("uDp", myDp);
        hashMap.put("uName", myName);
        //Добавляем данные в БД
        ref.child(timeStamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Если добавлено
                        progressDialog.dismiss();
                        FancyToast.makeText(PostDetailActivity.this, "Комментарий добавлен.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                        commentEt.setText("");

                        updateCommentCount();

                        //addToHisNotifications("" + hisUid, "" + postId, "Прокомментировали вашу публикацию");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Если ошибка
                        progressDialog.dismiss();
                        FancyToast.makeText(PostDetailActivity.this, "Не удалось добавить комментарий.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                    }
                });


    }

    private void updateCommentCount() {
        //Каждый раз, когда пользователь добавляет комментарий, увеличиваеися количество комментариев, так же как и лайки
        mProcessComment = true;

        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessComment) {
                    String comments = "" + dataSnapshot.child("pComments").getValue();
                    int newCommentVal = Integer.parseInt(comments) + 1;
                    ref.child("pComments").setValue("" + newCommentVal);
                    mProcessComment = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadUserInfo() {
        //Получаем информацию о зарегестрированном пользователе
        Query myRef = FirebaseDatabase.getInstance().getReference("Users");
        myRef.orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            myName = "" + ds.child("name").getValue();
                            myDp = "" + ds.child("image").getValue();

                            //Устанавливаем данные
                            try {
                                Picasso.get().load(myDp).placeholder(R.drawable.user_default_img).into(cAvatarIv);
                            } catch (Exception e) {
                                cAvatarIv.setImageResource(R.drawable.user_default_img);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadPostInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //ПРоверяем публикации, пока не найдём нужную
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    //Получаем данные
                    String pTitle = "" + ds.child("pTitle").getValue();
                    String pDesc = "" + ds.child("pDesc").getValue();
                    pLikes = "" + ds.child("pLikes").getValue();
                    String pTimeStamp = "" + ds.child("pTime").getValue();
                    String pImage = "" + ds.child("pImage").getValue();
                    hisDp = "" + ds.child("uDp").getValue();
                    hisUid = "" + ds.child("uid").getValue();
                    String uEmail = "" + ds.child("uEmail").getValue();
                    hisName = "" + ds.child("uName").getValue();
                    String commentCount = "" + ds.child("pComments").getValue();

                    //Конвертируем время
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
                    String pTime = DateFormat.format("dd.MM.yyyy HH:mm", calendar).toString();

                    //Устанавливаем данные
                    userNameTv.setText(hisName);
                    titlePostTv.setText(pTitle);
                    descPostTv.setText(pDesc);
                    pLikesTv.setText(pLikes + "лайк(ов)");
                    timePostTv.setText(pTime);
                    pCommentsTv.setText(commentCount + " комментарий(ев)");


                    //Устанавливаем изображение пользователя, который опубликовал
                    //Если публикация без изображения
                    if (pImage.equals("noImage")) {
                        postImageIv.setVisibility(View.GONE);
                    } else {
                        postImageIv.setVisibility(View.VISIBLE);

                        //Устанавливаем изображение поста
                        try {
                            Picasso.get().load(pImage).placeholder(R.drawable.user_default_img).into(postImageIv);
                        } catch (Exception e) {
                            postImageIv.setImageResource(R.drawable.user_default_img);
                        }
                    }

                    //Устанавливаем изображение пользователя в публикации
                    try {
                        Picasso.get().load(hisDp).placeholder(R.drawable.user_default_img).into(avatarIv);
                    } catch (Exception e) {
                        avatarIv.setImageResource(R.drawable.user_default_img);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkUserStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            myEmail = user.getEmail();
            myUid = user.getUid();
        } else {
            startActivity(new Intent(PostDetailActivity.this, IntroActivity.class));
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.edit).setVisible(false);
        menu.findItem(R.id.search).setVisible(false);
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
            startActivity(new Intent(PostDetailActivity.this, UpdateInfoUserActivity.class));
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
