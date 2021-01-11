package com.example.qq_2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qq_2.Adapters.AdapterPost;
import com.example.qq_2.Models.ModelPost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ThereProfileActivity extends AppCompatActivity {
    //Элементы
    private RecyclerView recyclerViewPost;
    CircleImageView avatar;
    ImageView coverIv;
    TextView nameTv, emailTv, phoneTv;

    //БД
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    StorageReference storageReference;

    private List<ModelPost> postList;
    private AdapterPost adapterPost;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        //storageReference = getInstance().getReference();

        //<editor-fold desc="Инициализация элементов">
        avatar = findViewById(R.id.avatarIv);
        coverIv = findViewById(R.id.coverIv);

        nameTv = findViewById(R.id.nameTv);
        emailTv = findViewById(R.id.emailTv);
        phoneTv = findViewById(R.id.phoneTv);

        //moreInformBtn = findViewById(R.id.more_inform_btn);

        recyclerViewPost = findViewById(R.id.recyclerViewPost);
        //</editor-fold>

        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");

        /*Мы должны получить информацию о зарегистрированном в данный момент пользователе. Можем получить его, используя электронную почту пользователя или UID
          Получим информацию пользователя, используя электронную почту*/
        /*Используя запрос orderByChild, мы получим детали от узла, чей ключ с именем email имеет значение, равное значению, подписанному в настоящее время в письме.
          Он будет искать все узлы, где ключ соответствует, он получит свои детали*/
        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Проверяем, пока не получим необходимые данные
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    //Получаем данные
                    String name = "" + ds.child("name").getValue();
                    String email = "" + ds.child("email").getValue();
                    String phone = "" + ds.child("phone").getValue();
                    String image = "" + ds.child("image").getValue();
                    String cover = "" + ds.child("cover").getValue();

                    //Устанавливаем данные
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    try {
                        //Если аватар установлен, то отображаем его
                        Picasso.get().load(image).into(avatar);
                    } catch (Exception e) {
                        //Если аватар не установлен или есть ошибки, то отображаем стандартное изображение
                        Picasso.get().load(R.drawable.user_default_img).into(avatar);
                    }

                    try {
                        //Если обложка установлена, то отображаем её
                        Picasso.get().load(cover).into(coverIv);
                    } catch (Exception e) {
                        //Если обложка не установлена или есть ошибки, то отображаем стандартное изображение
                        //Picasso.get().load(R.drawable.user_default).into(coverIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        postList = new ArrayList<>();

        checkUserStatus();
        loadHisPosts();
    }

    private void loadHisPosts() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(ThereProfileActivity.this);
        //Отображение нового поста в начале списка
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerViewPost.setLayoutManager(layoutManager);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelPost modelPost = ds.getValue(ModelPost.class);

                    postList.add(modelPost);

                    //Адаптер
                    adapterPost = new AdapterPost(ThereProfileActivity.this, postList);
                    //Устанавливаем адаптер в RecyclerView
                    recyclerViewPost.setAdapter(adapterPost);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Toast.makeText(ThereProfileActivity.this, "Не удалось загрузить публикации", Toast.LENGTH_LONG).show();
                //Toast.makeText(ThereProfileActivity.this, "" + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void searchHisPosts(final String searchQuery) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(ThereProfileActivity.this);
        //Отображение нового поста в начале списка
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        recyclerViewPost.setLayoutManager(layoutManager);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
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
                    adapterPost = new AdapterPost(ThereProfileActivity.this, postList);
                    //Устанавливаем адаптер в RecyclerView
                    recyclerViewPost.setAdapter(adapterPost);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ThereProfileActivity.this, "Не удалось загрузить публикации", Toast.LENGTH_LONG).show();
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
            startActivity(new Intent(ThereProfileActivity.this, IntroActivity.class));
            finish();
        }
    }

    //<editor-fold desc="Для меню (3 точки)">
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        //Поиск
        MenuItem item = menu.findItem(R.id.search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        //Поиск пользователя
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Происходит когда пользователь нажимает кнопку поиска на клавиатуре
                //Если строка поиска не пустая, то выполняется поиск
                if (!TextUtils.isEmpty(query)) {
                    searchHisPosts(query);
                } else {
                    //Если строка поиска пустая, то отображаются все пользователи
                    loadHisPosts();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                //Происходит когда пользователь нажимает на любую кнопку на клавиатуре
                //Если строка поиска не пустая, то выполняется поиск
                if (!TextUtils.isEmpty(query)) {
                    searchHisPosts(query);
                } else {
                    //Если строка поиска пустая, то отображаются все пользователи
                    loadHisPosts();
                }

                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.exit) {
            firebaseAuth.signOut();
            checkUserStatus();
        }

        return super.onOptionsItemSelected(item);
    }
    //</editor-fold>
}
