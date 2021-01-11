package com.example.qq_2;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.qq_2.Adapters.AdapterChat;
import com.example.qq_2.Models.ModelChat;
import com.example.qq_2.Models.ModelUser;
import com.example.qq_2.Notifications.Data;
import com.example.qq_2.Notifications.Sender;
import com.example.qq_2.Notifications.Token;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.shashank.sony.fancytoastlib.FancyToast;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    //Элементы
    private MaterialToolbar toolbar;
    private RecyclerView chatRecyclerView;
    private CircleImageView profileIv;
    private TextView nameTv, userStatusTv;
    private EditText messageEt;
    private ImageButton sendBtn, backBtn, attachFileBtn;
    private ImageView blockIv;

    //БД
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference usersDbRef;

    private String hisUid, myUid, hisImage;
    private boolean isBlocked = false;

    //Для проверки, прочитал ли пользователь сообщение или нет
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;

    private List<ModelChat> chatList;
    private AdapterChat adapterChat;

    //Разрешения для уведомлений
    private RequestQueue requestQueue;
    private boolean notify = false;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setSupportActionBar(toolbar);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDbRef = firebaseDatabase.getReference("Users");

        //<editor-fold desc="Инициализция элементов">
        toolbar = findViewById(R.id.toolbar);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        profileIv = findViewById(R.id.profileIv);
        blockIv = findViewById(R.id.blockIv);

        nameTv = findViewById(R.id.nameTv);
        userStatusTv = findViewById(R.id.userStatusTv);

        messageEt = findViewById(R.id.messageEt);

        sendBtn = findViewById(R.id.sendBtn);
        //backBtn = findViewById(R.id.backBtn);
        attachFileBtn = findViewById(R.id.attachFileBtn);
        //</editor-fold>

        //Инициализация массивой для разрешений
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        //Свойства RecyclerView
        chatRecyclerView.setHasFixedSize(true);
        chatRecyclerView.setLayoutManager(linearLayoutManager);

        //Нажатие на sendBtn
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;

                //Получаем текст из EditText
                String message = messageEt.getText().toString().trim();

                //Проверяем, пустой EditText или нет
                if (TextUtils.isEmpty(message)) {
                    //Если пустой
                    sendBtn.setVisibility(View.GONE);
                    FancyToast.makeText(ChatActivity.this, "Вы не можете отправить пустое сообщение...", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                } else {
                    //Если не пустой
                    sendMessage(message);
                }

                //Очищаем EditText после отправки сообщения
                messageEt.setText("");
            }
        });

        //Нажатие на backBtn
        /*backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                *//*startActivity(new Intent(ChatActivity.this, handleFragment(new ChatListFragment)));
                finish();*//*
            }
        });*/

        //Нажатие на attachFileBtn
        attachFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPickDialog();
            }
        });

        //Нажатие на blockIv
        blockIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBlocked) {
                    unBlockUser();
                } else {
                    blockUser();
                }
            }
        });

        //Проверка EditText для статуса (набирает сообщение или нет)
        messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendBtn.setVisibility(View.VISIBLE);

                if (s.toString().trim().length() == 0) {
                    checkTypingStatus("noOne");
                } else {
                    checkTypingStatus(hisUid);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().length() == 0) {
                    sendBtn.setVisibility(View.GONE);
                } else {
                    sendBtn.setVisibility(View.VISIBLE);
                }
            }
        });

        //При нажатии на пользователя из списка пользователей мы передали UID этого пользователя с намерением
        //Так что получаем этот uid здесь, чтобы получить аватарку, имя и начать чат с этим пользователем
        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        //Ищем пользователя, чтобы получить информацию о нём
        Query userQuery = usersDbRef.orderByChild("uid").equalTo(hisUid);
        //Получаем аватар и имя пользователя
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Проверяем утилиту, необходимая информация получена
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    //Получаем данные
                    String name = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("image").getValue();
                    String typingStatus = "" + ds.child("typingTo").getValue();

                    //Проверяем статус typing
                    if (typingStatus.equals(myUid)) {
                        userStatusTv.setText("Пишет...");
                    } else {
                        //Получаем значение статуса (online или нет)
                        String onlineStatus = "" + ds.child("onlineStatus").getValue();
                        if (onlineStatus.equals("online")) {
                            userStatusTv.setText(onlineStatus);
                        } else {
                            //Конвертируем время в нужное
                            Calendar calendar = Calendar.getInstance(Locale.getDefault());
                            calendar.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime = DateFormat.format("dd.MM.yyyy HH:mm", calendar).toString();

                            userStatusTv.setText("В сети: " + dateTime);
                        }
                    }

                    //Устанавливаем данные
                    nameTv.setText(name);

                    try {
                        //Изображение получено, устанавливаем его в Toolbar
                        Picasso.get().load(hisImage).placeholder(R.drawable.user_default_img).into(profileIv);
                    } catch (Exception e) {
                        //Есть произошла какая-то ошибка при получении изображения, устанавливаем изображение по умолчанию
                        Picasso.get().load(R.drawable.user_default_img).into(profileIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        readMessages();
        seenMessage();
        checkIsBlocked();
    }

    private void checkIsBlocked() {
        //Проверяем, если каждый пользователь, если заблокирован или нет
        //Если uid пользователя существует в «BlockedUsers», то этот пользователь блокируется, в противном случае нет
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            if (ds.exists()) {
                                blockIv.setImageResource(R.drawable.ic_block_red);
                                isBlocked = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void blockUser() {
        //Блокируем пользователя, добавив uid к узлу "BlockedUsers" текущего пользователя
        //Помещаем значения в hashMap, чтобы поместить в БД
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", hisUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUid).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Успешная блокировка
                        FancyToast.makeText(ChatActivity.this, "Вы заблокировали пользователя.", FancyToast.LENGTH_SHORT, FancyToast.INFO, false).show();
                        blockIv.setImageResource(R.drawable.ic_block_red);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Ошибка блокировки
                        FancyToast.makeText(ChatActivity.this, "Не удалось заблокировать пользователя.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                    }
                });
    }

    private void unBlockUser() {
        //Разблокируем пользователя, удалив uid из узла "BlockedUsers" текущего пользователя
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            if (ds.exists()) {
                                //Удаляем заблокированные данные пользователя из списка "BlockedUsers" текущего пользователя
                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                //Успешная разблокировка
                                                FancyToast.makeText(ChatActivity.this, "Вы разблокировали пользователя.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                                                blockIv.setImageResource(R.drawable.ic_unblock);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //Ошибка разблокировки
                                                FancyToast.makeText(ChatActivity.this, "Не удалось разблокировать пользователя.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void showPickDialog() {
        //Пункты в диалоговом окне
        String[] options = {"Фотография"};

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setBackground(getResources().getDrawable(R.drawable.alert_dialog_bg));
        builder.setIcon(R.drawable.attach_file_img);
        builder.setTitle("Выбирете какой файл хотите приерепить:")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Обработка нажатий на пункты
                        if (which == 0) {
                            //Фотография
                            showImagePickDialog();
                        }
                    }
                })
                .show();
    }

    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            ModelChat chat = ds.getValue(ModelChat.class);

                            if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) {
                                HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                                hasSeenHashMap.put("isSeen", true);
                                ds.getRef().updateChildren(hasSeenHashMap);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void readMessages() {
        chatList = new ArrayList<>();

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);

                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) ||
                            chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)) {
                        chatList.add(chat);
                    }

                    //Адаптер
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    adapterChat.notifyDataSetChanged();
                    //Устанавливаем адаптер в RecyclerView
                    chatRecyclerView.setAdapter(adapterChat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(final String message) {
        //Будет создан узел чатов, который будет содержать все чаты
        /*Каждый раз, когда пользователь отправляет сообщение, создаётся новый дочерний элемент в узле «Чаты», и этот дочерний элемент будет содержать следующие ключевые значения:
         1)Отправитель: UID отправителя
         2)Получатель: UID, если получено
         3)Сообщение: само сообщение*/
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);
        hashMap.put("type", "text");
        databaseReference.child("Chats").push().setValue(hashMap);

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelUser users = dataSnapshot.getValue(ModelUser.class);

                if (notify) {
                    senNotification(hisUid, users.getName(), message);
                }

                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Создаём узел/дочерний список чата в базе данных Firebase
        final DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(myUid)
                .child(hisUid);
        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(hisUid)
                .child(myUid);
        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void senNotification(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid, name + ": " + message, "Новое сообщение", hisUid, "ChatNotification", R.mipmap.ic_launcher);

                    Sender sender = new Sender(data, token.getToken());

                    //Запрос объекта FCM Json
                    try {
                        JSONObject senderJsonObj = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", senderJsonObj,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        //Ответ на запрос
                                        Log.d("JSON_RESPONSE", "onResponse: " + response.toString());
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("JSON_RESPONSE", "onResponse: " + error.toString());
                            }
                        }) {
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                //Задаём параметры
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Content-Type", "application/json");
                                headers.put("Authorization", "key=AAAAGfC9a8k:APA91bGW3L8kdnlpyqt1bsP7DY4KSnmJxOh2WXyRf4s75E_bZm1_1mL7W7CJnLZT1HiyMvVzzLW54BkofmFqGIyk9AYDW2g8bAlI316ZB7WlAbDm2G_VffbuucgdsE9AvpBVJ3IPiDeM");

                                return headers;
                            }
                        };

                        //Добавляем этот запрос в очередь
                        requestQueue.add(jsonObjectRequest);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendImageMessage(Uri image_uri) throws IOException {
        notify = true;

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Отправка изображения...");
        progressDialog.show();

        final String timeStamp = "" + System.currentTimeMillis();
        String fileNameAndPath = "Chat_Image/" + "post_" + timeStamp;

        //Будет создан узел чатов, который будет содержать все изображения, отправленные в чате
        //Получаем растровое изображение из URL изображения
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray(); //Преобразовываем изображение в байты
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //Изображение загружено
                        progressDialog.dismiss();
                        //Получаем URL загруженного изображения
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;
                        String downloadUri = uriTask.getResult().toString();

                        if (uriTask.isSuccessful()) {
                            //Добавляем изображение URI и другую информацию в базу данных
                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                            //Настройка необходимых данных
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("sender", myUid);
                            hashMap.put("receiver", hisUid);
                            hashMap.put("message", downloadUri);
                            hashMap.put("timestamp", timeStamp);
                            hashMap.put("type", "image");
                            hashMap.put("isSeen", false);
                            //ПОмещаем эти данные в Firebase
                            databaseReference.child("Chats").push().setValue(hashMap);

                            //Отправляем уведомление
                            DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
                            database.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    ModelUser users = dataSnapshot.getValue(ModelUser.class);

                                    if (notify) {
                                        senNotification(hisUid, users.getName(), "отправил Вам фото");
                                    }

                                    notify = false;
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            //Создаём узел/дочерний список чата в базе данных Firebase
                            final DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("ChatList")
                                    .child(myUid)
                                    .child(hisUid);
                            chatRef1.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.exists()) {
                                        chatRef1.child("id").setValue(hisUid);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            final DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("ChatList")
                                    .child(hisUid)
                                    .child(myUid);
                            chatRef2.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.exists()) {
                                        chatRef2.child("id").setValue(myUid);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Ошибка
                        progressDialog.dismiss();
                    }
                });
    }

    private void checkUserStatus() {
        //Получаем текущего пользователя
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //Пользователь вошёл в аккаунт
            myUid = user.getUid();
        } else {
            //Пользователь не вошел в аккаунт, переходим к StartActivity
            startActivity(new Intent(this, IntroActivity.class));
            finish();
        }
    }

    private void checkOnlineStatus(String status) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        //Обновляем значение onlineStatus текущего пользователя
        dbRef.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);
        //Обновляем значение onlineStatus текущего пользователя
        dbRef.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        //Устанавливаем online или нет
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Получем время
        String timestamp = String.valueOf(System.currentTimeMillis());
        //Устанавливаем когда был пользователь был последний раз в сети
        checkOnlineStatus(timestamp);

        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        //Устанавливаем online или нет
        checkOnlineStatus("online");
        super.onResume();
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
                //Используем это изображение для загрузки в FIrebase
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //Изображение выбрано из камеры, получаем URI изображения
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    //</editor-fold>
}
