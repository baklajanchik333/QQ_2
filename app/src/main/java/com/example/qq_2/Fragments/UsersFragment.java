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
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qq_2.Adapters.AdapterUser;
import com.example.qq_2.IntroActivity;
import com.example.qq_2.Models.ModelUser;
import com.example.qq_2.R;
import com.example.qq_2.UpdateInfoUserActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class UsersFragment extends Fragment {
    //Элементы
    private RecyclerView recyclerView;
    private MaterialToolbar toolbar;

    //БД
    private FirebaseAuth firebaseAuth;

    private AdapterUser adapterUser;
    private List<ModelUser> userList;

    public UsersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle("Пользователи");

        firebaseAuth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.users_recyclerView);

        //Устанавливаем свойства recyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //Инициализация списка пользователей
        userList = new ArrayList<>();
        //Получаем всех пользователей
        getAllUsers();

        return view;
    }

    private void getAllUsers() {
        //Получаем текущего пользователя
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        //Получаем путь к базе данных «Users», содержащей информацию о пользователях
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        //Получаем все данные из пути
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelUser modelUser = ds.getValue(ModelUser.class);

                    //Получаем всех пользователей, кроме зарегистрированного
                    if (!modelUser.getUid().equals(fUser.getUid())) {
                        userList.add(modelUser);
                    }

                    /*DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);
                    dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.recyclerview_divider));
                    recyclerView.addItemDecoration(dividerItemDecoration);*/

                    //Адаптер
                    adapterUser = new AdapterUser(getActivity(), userList);
                    //Устанавливаем адаптер в RecyclerView
                    recyclerView.setAdapter(adapterUser);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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
                    searchUsers(query);
                } else {
                    //Если строка поиска пустая, то отображаются все пользователи
                    getAllUsers();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                //Происходит когда пользователь нажимает на любую кнопку на клавиатуре
                //Если строка поиска не пустая, то выполняется поиск
                if (!TextUtils.isEmpty(query)) {
                    searchUsers(query);
                } else {
                    //Если строка поиска пустая, то отображаются все пользователи
                    getAllUsers();
                }

                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void searchUsers(final String query) {
        //Получаем текущего пользователя
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        //Получаем путь к базе данных «Users», содержащей информацию о пользователях
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        //Получаем все данные из пути
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelUser modelUser = ds.getValue(ModelUser.class);

                   /*Условия выполнения поиска:
                    1)Пользователь не текущий пользователь
                    2)Имя пользователя или электронная почта содержат текст, введенный в SearchView (без учета регистра)*/

                    //Получаем всех искомых пользователей, кроме текущего
                    if (!modelUser.getUid().equals(fUser.getUid())) {
                        if (modelUser.getName().toLowerCase().contains(query.toLowerCase()) || modelUser.getEmail().toLowerCase().contains(query.toLowerCase())) {
                            userList.add(modelUser);
                        }
                    }

                    //Адаптер
                    adapterUser = new AdapterUser(getActivity(), userList);
                    //Обновляем адаптре
                    adapterUser.notifyDataSetChanged();
                    //Устанавливаем адаптер в RecyclerView
                    recyclerView.setAdapter(adapterUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
