package com.example.qq_2.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qq_2.Adapters.AdapterChatList;
import com.example.qq_2.IntroActivity;
import com.example.qq_2.Models.ModelChat;
import com.example.qq_2.Models.ModelChatList;
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
public class ChatListFragment extends Fragment {
    //Элементы
    private RecyclerView recyclerView;
    private MaterialToolbar toolbar;

    //БД
    private FirebaseAuth firebaseAuth;
    private DatabaseReference reference;
    private FirebaseUser currentUser;

    private List<ModelChatList> chatListList;
    private List<ModelUser> usersList;
    private AdapterChatList adapterChatList;

    public ChatListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        toolbar.setTitle("Сообщения");

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("ChatList").child(currentUser.getUid());

        recyclerView = view.findViewById(R.id.recyclerView);

        chatListList = new ArrayList<>();

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatListList.clear();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChatList chatList = ds.getValue(ModelChatList.class);

                    chatListList.add(chatList);
                }

                loadChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return view;
    }

    private void loadChats() {
        usersList = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelUser users = ds.getValue(ModelUser.class);

                    for (ModelChatList chatList : chatListList) {
                        if (users.getUid() != null && users.getUid().equals(chatList.getId())) {
                            usersList.add(users);
                            break;
                        }
                    }

                    adapterChatList = new AdapterChatList(getContext(), usersList);
                    recyclerView.setAdapter(adapterChatList);

                    //Отображение последнего сообщения
                    for (int i = 0; i < usersList.size(); i++) {
                        lastMessage(usersList.get(i).getUid());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void lastMessage(final String userId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String theLastMessage = "default";

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat == null) {
                        continue;
                    }

                    String sender = chat.getSender();
                    String receiver = chat.getReceiver();
                    if (sender == null || receiver == null) {
                        continue;
                    }
                    if (chat.getReceiver().equals(currentUser.getUid()) && chat.getSender().equals(userId) ||
                            chat.getReceiver().equals(userId) && chat.getSender().equals(currentUser.getUid())) {
                        if (chat.getType().equals("image")) {
                            theLastMessage = "Изображение";
                        } else {
                            theLastMessage = chat.getMessage();
                        }
                    }
                }

                adapterChatList.setLastMessageMap(userId, theLastMessage);
                adapterChatList.notifyDataSetChanged();
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
        menu.findItem(R.id.search).setVisible(false);

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
