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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qq_2.Adapters.AdapterPost;
import com.example.qq_2.IntroActivity;
import com.example.qq_2.Models.ModelPost;
import com.example.qq_2.R;
import com.example.qq_2.UpdateInfoUserActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {
    //Элементы
    private CircleImageView avatarIv;
    private TextView nameTv, emailTv, phoneTv;
    private ImageView coverIv;
    private RecyclerView recyclerViewPost;
    private MaterialToolbar toolbar;
    private MaterialButton infoProfileBtn;

    //БД
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    private List<ModelPost> postList;
    private AdapterPost adapterPost;
    private String uid;

    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle("Профиль");

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");

        //<editor-fold desc="Инициализация элементов">
        avatarIv = view.findViewById(R.id.avatarIv);
        coverIv = view.findViewById(R.id.coverIv);

        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);

        infoProfileBtn = view.findViewById(R.id.infoProfileBtn);

        recyclerViewPost = view.findViewById(R.id.recyclerViewPost);
        //</editor-fold>

         /*Мы должны получить информацию о зарегистрированном в данный момент пользователе. Можем получить его, используя электронную почту пользователя или UID
          Получим информацию пользователя, используя электронную почту*/
        /*Используя запрос orderByChild, мы получим детали от узла, чей ключ с именем email имеет значение, равное значению, подписанному в настоящее время в письме.
          Он будет искать все узлы, где ключ соответствует, он получит свои детали*/
        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
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
                        Picasso.get().load(image).into(avatarIv);
                    } catch (Exception e) {
                        //Если аватар не установлен или есть ошибки, то отображаем стандартное изображение
                        Picasso.get().load(R.drawable.user_default_img).into(avatarIv);
                    }

                    /*try {
                        //Если обложка установлена, то отображаем её
                        Picasso.get().load(cover).into(coverIv);
                    } catch (Exception e) {
                        //Если обложка не установлена или есть ошибки, то отображаем стандартное изображение
                        Picasso.get().load(R.drawable.user_default_img).into(coverIv);
                    }*/
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        infoProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
                builder.setTitle("Информация о пользователе");
                builder.setIcon(R.drawable.info_profile_img);
                builder.setBackground(getResources().getDrawable(R.drawable.alert_dialog_bg));

                LayoutInflater inflater = LayoutInflater.from(getContext());
                View account = inflater.inflate(R.layout.info_profile, null);
                final CircleImageView profileIv = account.findViewById(R.id.profileIv);
                final TextView nameTv = account.findViewById(R.id.nameTv);
                final TextView emailTv = account.findViewById(R.id.emailTv);
                final TextView ageTv = account.findViewById(R.id.ageTv);
                final TextView countryTv = account.findViewById(R.id.countryTv);
                final TextView cityTv = account.findViewById(R.id.cityTv);
                final TextView phoneTv = account.findViewById(R.id.phoneTv);

                builder.setView(account);

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                ref.orderByChild("uid").equalTo(firebaseAuth.getUid())
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                user = firebaseAuth.getCurrentUser();

                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    String name = "" + ds.child("name").getValue();
                                    String email = user.getEmail();
                                    String age = "" + ds.child("age").getValue();
                                    String city = "" + ds.child("city").getValue();
                                    String country = "" + ds.child("country").getValue();
                                    String phone = "" + ds.child("phone").getValue();
                                    String image = "" + ds.child("image").getValue();

                                    nameTv.setText(name);
                                    emailTv.setText(email);
                                    ageTv.setText(age);
                                    phoneTv.setText(phone);
                                    countryTv.setText(country);
                                    cityTv.setText(city);
                                    try {
                                        Picasso.get().load(image).placeholder(R.drawable.user_default_img).into(profileIv);
                                    } catch (Exception e) {
                                        profileIv.setImageResource(R.drawable.user_default_img);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                builder.show();
            }
        });

        postList = new ArrayList<>();

        checkUserStatus();
        loadMyPosts();

        return view;
    }

    private void loadMyPosts() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
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
                    adapterPost = new AdapterPost(getActivity(), postList);
                    //Устанавливаем адаптер в RecyclerView
                    recyclerViewPost.setAdapter(adapterPost);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //Toast.makeText(getActivity(), "Не удалось загрузить публикации", Toast.LENGTH_LONG).show();
                //FancyToast.makeText(getActivity(), "" + databaseError.getMessage(), FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
            }
        });
    }

    private void searchMyPosts(final String searchQuery) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
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
                    adapterPost = new AdapterPost(getActivity(), postList);
                    //Устанавливаем адаптер в RecyclerView
                    recyclerViewPost.setAdapter(adapterPost);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Не удалось загрузить публикации", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkUserStatus() {
        //Получаем текущего пользователя
        user = firebaseAuth.getCurrentUser();
        if (user != null) {
            //Пользователь вошёл в аккаунт
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
                    searchMyPosts(query);
                } else {
                    //Если строка поиска пустая, то отображаются все пользователи
                    loadMyPosts();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                //Происходит когда пользователь нажимает на любую кнопку на клавиатуре
                //Если строка поиска не пустая, то выполняется поиск
                if (!TextUtils.isEmpty(query)) {
                    searchMyPosts(query);
                } else {
                    //Если строка поиска пустая, то отображаются все пользователи
                    loadMyPosts();
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
}
