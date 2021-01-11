package com.example.qq_2.Adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qq_2.ChatActivity;
import com.example.qq_2.Models.ModelUser;
import com.example.qq_2.R;
import com.example.qq_2.ThereProfileActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.shashank.sony.fancytoastlib.FancyToast;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterUser extends RecyclerView.Adapter<AdapterUser.MyHolder> {
    private Context context;
    private List<ModelUser> userList;

    private FirebaseAuth firebaseAuth;

    private String myUid;

    //Конструктор
    public AdapterUser(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;

        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Прикрепляем макет (row_users.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        //Получаем данные
        final String hisUID = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        final String userEmail = userList.get(position).getEmail();

        //Устанавливаем данные
        holder.nameTv.setText(userName);
        holder.emailTv.setText(userEmail);

        try {
            Picasso.get().load(userImage).placeholder(R.drawable.user_default_img).into(holder.avatarIv);
        } catch (Exception e) {
        }

        holder.blockIv.setImageResource(R.drawable.ic_unblock);
        //Проверяем, если каждый пользователь заблокирован или нет
        checkIsBlocked(hisUID, holder, position);

        //Нажатие на элемент
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                builder.setBackground(context.getResources().getDrawable(R.drawable.alert_dialog_bg));

                builder.setItems(new String[]{"Написать сообщение", "Посмотреть профиль"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            //Начинаем общение
                            //Нажимаем на нужного нам пользователя из списка, чтобы начать чат
                            //Начинаем работу, указав UID получателя, который будет использоваться для идентификации пользователя, с которым будем общаться
                            imBlockedOrNot(hisUID);
                        } else if (which == 1) {
                            //Смотрим профиль
                            //После нажатия переходим на страницу пользователя, где будут отображаться его данные и публикации
                            Intent intent = new Intent(context, ThereProfileActivity.class);
                            intent.putExtra("uid", hisUID);
                            context.startActivity(intent);
                        }
                    }
                });

                builder.show();
            }
        });

        //Нажатие на блок
        holder.blockIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userList.get(position).isBlocked()) {
                    unBlockUser(hisUID);
                } else {
                    blockUser(hisUID);
                }
            }
        });
    }

    private void imBlockedOrNot(final String hisUID) {
        //Сначала проверяем, заблокирован ли отправитель (текущий пользователь) получателем или нет
        //Логика: если uid отправителя (текущего пользователя) существует в "BlockedUsers" получателя, то отправитель (текущий пользователь) блокируется, иначе нет
        //Если заблокировано, просто отображаем сообщение, например, Вы заблокированы этим пользователем, не можете отправить сообщение
        //Если не заблокирован, просто начните чат
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUID).child("BlockedUsers").orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            if (ds.exists()) {
                                FancyToast.makeText(context, "Вас заблокировал этот пользователь, отправка сообщения невозможна...", FancyToast.LENGTH_SHORT, FancyToast.WARNING, false).show();
                                //Заблокировано, не двигаемся дальше
                                return;
                            }
                        }
                        //Не заблокировано, открываем чат
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("hisUid", hisUID);
                        context.startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void checkIsBlocked(String hisUID, final MyHolder holder, final int position) {
        //Проверяем, если каждый пользователь, если заблокирован или нет
        //Если uid пользователя существует в «BlockedUsers», то этот пользователь блокируется, в противном случае нет
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            if (ds.exists()) {
                                holder.blockIv.setImageResource(R.drawable.ic_block_red);
                                userList.get(position).setBlocked(true);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void blockUser(String hisUID) {
        //Блокируем пользователя, добавив uid к узлу "BlockedUsers" текущего пользователя
        //Помещаем значения в hashMap, чтобы поместить в БД
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", hisUID);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUID).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Успешная блокировка
                        FancyToast.makeText(context, "Вы заблокировали пользователя.", FancyToast.LENGTH_SHORT, FancyToast.INFO, false).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Ошибка блокировки
                        FancyToast.makeText(context, "Не удалось заблокировать пользователя.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
                    }
                });
    }

    private void unBlockUser(String hisUID) {
        //Разблокируем пользователя, удалив uid из узла "BlockedUsers" текущего пользователя
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
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
                                                FancyToast.makeText(context, "Вы разблокировали пользователя.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //Ошибка разблокировки
                                                FancyToast.makeText(context, "Не удалось разблокировать пользователя.", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
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

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView blockIv;
        CircleImageView avatarIv;
        TextView nameTv, emailTv;

        MyHolder(@NonNull View itemView) {
            super(itemView);

            blockIv = itemView.findViewById(R.id.blockIv);
            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            emailTv = itemView.findViewById(R.id.emailTv);
        }
    }
}
