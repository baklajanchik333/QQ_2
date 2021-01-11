package com.example.qq_2.Adapters;

import android.content.Context;
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
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterChatList extends RecyclerView.Adapter<AdapterChatList.MyHolder>{
    private Context context;
    private List<ModelUser> usersList;
    private HashMap<String, String> lastMessageMap;

    //КОнструктор
    public AdapterChatList(Context context, List<ModelUser> usersList) {
        this.context = context;
        this.usersList = usersList;
        lastMessageMap = new HashMap<>();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_chatlist, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {//Получаем данные
        final String hisUid = usersList.get(position).getUid();
        String userImage = usersList.get(position).getImage();
        String userName = usersList.get(position).getName();
        String lastMessage = lastMessageMap.get(hisUid);

        //Устанавливаем данные
        holder.nameTv.setText(userName);
        if (lastMessage == null || lastMessage.equals("default")) {
            holder.lastMessageTv.setVisibility(View.GONE);
        } else {
            holder.lastMessageTv.setVisibility(View.VISIBLE);
            holder.lastMessageTv.setText(lastMessage);
        }

        try {
            Picasso.get().load(userImage).placeholder(R.drawable.user_default_img).into(holder.profileIv);
        } catch (Exception e) {
            Picasso.get().load(R.drawable.user_default_img).into(holder.profileIv);
        }

        //Устанавливаем онлайн статус других пользователей в списке чата
        if (usersList.get(position).getOnlineStatus().equals("online")) {
            //Online
            holder.onlineStatusIv.setImageResource(R.drawable.circle_online);
        } else {
            //Offline
            holder.onlineStatusIv.setImageResource(R.drawable.circle_offline);
        }

        //Нажатие на пользователя в списке
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Открываем переписку с пользователем
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("hisUid", hisUid);
                context.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public void setLastMessageMap(String userId, String lastMessage) {
        lastMessageMap.put(userId, lastMessage);
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        CircleImageView profileIv;
        ImageView onlineStatusIv;
        TextView nameTv, lastMessageTv;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            profileIv = itemView.findViewById(R.id.profileIv);
            onlineStatusIv = itemView.findViewById(R.id.onlineStatusIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            lastMessageTv = itemView.findViewById(R.id.lastMessageTv);
        }
    }
}
