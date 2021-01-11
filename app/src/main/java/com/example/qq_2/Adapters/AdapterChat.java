package com.example.qq_2.Adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qq_2.Models.ModelChat;
import com.example.qq_2.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.shashank.sony.fancytoastlib.FancyToast;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder> {
    private Context context;
    private List<ModelChat> chatList;
    private String imageUrl;

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    //БД
    private FirebaseUser fUser;

    //Конструктор
    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Прикрепляем макеты (row_chat_left.xml и  row_chat_right)
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false);
            return new MyHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false);
            return new MyHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, final int position) {
        //Получаем данные
        String message = chatList.get(position).getMessage();
        String timeStamp = chatList.get(position).getTimestamp();
        String type = chatList.get(position).getType();

        //КОнвертируем время
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(timeStamp));
        String dateTime = DateFormat.format("HH:mm", calendar).toString();

        if (type.equals("text")) {
            //Текстовое сообщение
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);

            holder.messageTv.setText(message);
        } else {
            //Изображение сообщение
            holder.messageTv.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.VISIBLE);

            Picasso.get().load(message).placeholder(R.drawable.update_photo_img).into(holder.messageIv);
        }

        //Устанавливаем данные
        holder.messageTv.setText(message);
        holder.timeTv.setText(dateTime);

        /*try {
            Picasso.get().load(imageUrl).into(holder.avatarCv);
        } catch (Exception e) {

        }*/

        //Нажатие на своё сообщение (для его удвления)
        holder.messageLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context); //Создаём диалоговое окно
                builder.setTitle("Удалить сообщение"); //Заголовок

                builder.setIcon(R.drawable.delete_img); //Иконка
                builder.setBackground(context.getResources().getDrawable(R.drawable.alert_dialog_bg));
                builder.setMessage("Вы действительно хотите удалить сообщение?");

                builder.setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMessage(position);
                    }
                });

                builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.show();

                return false;
            }
        });

        //Устанавливаем прочтано/отправлено состояние сообщения
        /*if (position == chatList.size() - 1) {
            if (chatList.get(position).isSeen()) {
                //holder.isSeenTv.setVisibility(View.VISIBLE);
                holder.isSeenTv.setText("Прочитано");
                //Toast.makeText(context, "Прочитано", Toast.LENGTH_SHORT);
            } else {
                //holder.isSeenTv.setVisibility(View.VISIBLE);
                holder.isSeenTv.setText("Отправлено");
                //Toast.makeText(context, "Отправлено", Toast.LENGTH_SHORT);
            }
        } else {
            holder.isSeenTv.setVisibility(View.GONE);
        }*/
    }

    private void deleteMessage(int position) {
        final String myUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //Как работает:
        /*Получаем метку времени (когда нажали на сообщение)
          Сравните эту метку со всеми сообщениями в чатах.
          Если оба значения совпадают, удалите это сообщение*/
        String msgTimeStamp = chatList.get(position).getTimestamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");

        Query query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    /*Если мы хотите разрешить отправителю удалять только его сообщение,
                     сравниваем значение отправителя с текущим идентификатором пользователя,
                     если они совпадают, это означает, что сообщение отправителя пытается удалить*/
                    if (ds.child("sender").getValue().equals(myUID)) {
                        /*Мы можем сделать два способа удаления сообщения:
                          1) Удалить сообщение из чата
                          2) Установить вместо самого сообщения надпись «Это сообщение было удалено ...»*/

                        //1-ый способ
                        ds.getRef().removeValue();

                        //2-ой способ
                        /*HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("message", "Это сообщение было удалено...");
                        ds.getRef().updateChildren(hashMap);*/

                        FancyToast.makeText(context, "Сообщение удалено.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
                    } else {
                        FancyToast.makeText(context, "Вы можете удалить только своё сообщение.", FancyToast.LENGTH_LONG, FancyToast.INFO, false).show();
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
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        //Получаем текущего пользователя
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (chatList.get(position).getSender().equals(fUser.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    class MyHolder extends RecyclerView.ViewHolder {
        //CircleImageView avatarCv;
        ImageView messageIv;
        TextView messageTv, timeTv, isSeenTv;
        LinearLayout messageLayout;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //avatarCv = itemView.findViewById(R.id.avatarCv);
            messageIv = itemView.findViewById(R.id.messageIv);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            isSeenTv = itemView.findViewById(R.id.isSeenTv);
            messageLayout = itemView.findViewById(R.id.messageLayout);
        }
    }
}
