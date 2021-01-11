package com.example.qq_2.Adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qq_2.Models.ModelPost;
import com.example.qq_2.PostDetailActivity;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shashank.sony.fancytoastlib.FancyToast;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterPost extends RecyclerView.Adapter<AdapterPost.MyHolder> {
    private Context context;
    private List<ModelPost> postList;

    //БД
    private DatabaseReference likesRef; //Для узла базы данных лайков
    private DatabaseReference postsRef; //Ссылка публикации

    //Переменные для подсчёта
    private boolean mProcessLike = false;

    private String myUid, timestamp;

    //Конструктор
    public AdapterPost(Context context, List<ModelPost> postList) {
        this.context = context;
        this.postList = postList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_posts, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, final int position) {
        //Получаем данные
        final String uid = postList.get(position).getUid();
        String uEmail = postList.get(position).getuEmail();
        String uName = postList.get(position).getuName();
        String uDp = postList.get(position).getuDp();
        final String pId = postList.get(position).getpId();
        final String pTitle = postList.get(position).getpTitle();
        final String pDescription = postList.get(position).getpDesc();
        final String pImage = postList.get(position).getpImage();
        String pTimeStamp = postList.get(position).getpTime();
        String pLikes = postList.get(position).getpLikes(); //Содержит общее количество лайков в публикации
        String pComments = postList.get(position).getpComments(); //Содержит общее количество комментариев к публикации

        //Конвертируем время
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
        String pTime = DateFormat.format("dd.MM.yyyy HH:mm", calendar).toString();

        //Устанавливаем данные
        holder.userNameTv.setText(uName);
        holder.titlePostTv.setText(pTitle);
        holder.descPostTv.setText(pDescription);
        holder.timePostTv.setText(pTime);
        holder.pLikesTv.setText(pLikes + " лайк(ов)");
        holder.pCommentsTv.setText(pComments + " комментрарий(ев)");

        //Устанавливаем лайки для каждого поста
        setLikes(holder, pId);

        //Устанавливаем аватар пользователя
        try {
            Picasso.get().load(uDp).placeholder(R.drawable.user_default_img).into(holder.avatarIv);
        } catch (Exception e) {

        }

        //Если публикация без изображения
        if (pImage.equals("noImage")) {
            holder.postImageIv.setVisibility(View.GONE);
        } else {
            holder.postImageIv.setVisibility(View.VISIBLE);

            //Устанавливаем изображение поста
            try {
                Picasso.get().load(pImage).into(holder.postImageIv);
            } catch (Exception e) {

            }
        }

        //Нажатие на moreBtn
        holder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoreOptionsPost(holder.moreBtn, uid, myUid, pId, pImage);
            }
        });

        //Нажатие на likeBtn
        holder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Получаем общее количество лайков публикации,
                чья кнопка лайка нажата, если в данный момент вошедший в систему пользователь не понравился,
                прежде чем увеличить значение на 1, в противном случае уменьшить значение на 1*/ // Фигян написана не русская
                final int pLikes = Integer.parseInt(postList.get(position).getpLikes());
                mProcessLike = true;
                //Получить идентификатор публикации, которую лайкнули
                final String postIde = postList.get(position).getpId();
                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (mProcessLike) {
                            if (dataSnapshot.child(postIde).hasChild(myUid)) {
                                //Если лайк уже поставлен, убираем его
                                postsRef.child(postIde).child("pLikes").setValue("" + (pLikes - 1));
                                likesRef.child(postIde).child(myUid).removeValue();
                                mProcessLike = false;
                            } else {
                                //Если нет лайка, ставим его
                                postsRef.child(postIde).child("pLikes").setValue("" + (pLikes + 1));
                                likesRef.child(postIde).child(myUid).setValue("Liked"); //Устанавливаем любое значение
                                mProcessLike = false;

                                //addToHisNotifications("" + uid, "" + pId, "Оценили вашу публикацию");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        //Нажатие на commentBtn
        holder.commentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("postId", pId);
                context.startActivity(intent);
            }
        });

        //Нажатие на shareBtn
        holder.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Некоторые публикации содержат только текст, а некоторые содержат изображения и текст, поэтому будем обрабатывать оба варианта
                //Получаем изображение из ImageView
                BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.postImageIv.getDrawable();
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

        //Нажатие на avatarIv
        holder.avatarIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ThereProfileActivity.class);
                intent.putExtra("uid", uid);
                context.startActivity(intent);
            }
        });
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
        context.startActivity(Intent.createChooser(sIntent, "Share Via"));
    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(context.getCacheDir(), "images");
        Uri uri = null;

        try {
            imageFolder.mkdirs(); //Создаём, если не существует
            File file = new File(imageFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context, "com.example.qq_2.fileprovider", file);
        } catch (Exception e) {
            FancyToast.makeText(context, "" + e.getMessage(), FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show();
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
        context.startActivity(Intent.createChooser(sIntent, "Share Via")); //Сообщение для отображения в диалоговом окне обмена
    }

    private void setLikes(final MyHolder holder, final String postKey) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postKey).hasChild(myUid)) {
                    //Пользователю понравилась публикация
                    /*Чтобы указать, что эта запись нравится этому (зарегистрированному) пользователю
                    Изменяем нарисованный левый значок кнопки «Мне нравится»*/
                    holder.likeBtn.setImageResource(R.drawable.ic_like_red);
                } else {
                    //Пользователю не понравилась публикация
                    holder.likeBtn.setImageResource(R.drawable.ic_like);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }


    private void showMoreOptionsPost(ImageButton moreBtn, String uid, String myUid, final String pId, final String pImage) {
        //Создаём всплывающее меню и добавляем туда опцию "Удалить"
        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);
        //Показывать параметры удаления только в постах, которые зарегистрированы на данный момент.
        if (uid.equals(myUid)) {
            //Добавление пунктов (опций)
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Удалить");
            //popupMenu.getMenu().add(Menu.NONE, 1, 0, "Редактировать");
        }
        popupMenu.getMenu().add(Menu.NONE, 2, 0, "Подробнее");

        //Нажатие на пункты меню
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == 0) {
                    //Нажатие на пункт "Удалить"
                    beginDeletePost(pId, pImage);
                } else if (id == 1) {
                    //Нажатие на пункт "Редактировать"
                } else if (id == 2) {
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("postId", pId);
                    context.startActivity(intent);
                }

                return false;
            }
        });

        popupMenu.show();
    }

    private void beginDeletePost(String pId, String pImage) {
        //Публикация с изображением или безз него
        if (pImage.equals("noImage")) {
            //Публикация без изображения
            deletePostWithoutImage(pId);
        } else {
            //Публикация с изображением
            deletePostWithImage(pId, pImage);
        }
    }

    //Удаление публикации с изображением
    private void deletePostWithImage(final String pId, final String pImage) {
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Удаление");
        builder.setMessage("Вы действительно хотите удалить публикацию?");
        builder.setIcon(R.drawable.delete_img);
        builder.setBackground(context.getResources().getDrawable(R.drawable.alert_dialog_bg));

        builder.setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final ProgressDialog pd = new ProgressDialog(context);
                pd.setMessage("Удаление...");

                /*Шаги:
                1) Удаляем изображение с помощью URL
                2) Удаляем из базы данных, используя идентификатор сообщения*/
                StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
                picRef.delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //Изображение удалено, теперь удаляем из базы
                                Query fQuery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
                                fQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                            ds.getRef().removeValue(); //удаляем значения из Firebase, где соответствует pId
                                        }

                                        //Когда удалено
                                        pd.dismiss();
                                        FancyToast.makeText(context, "Публикация удалена.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
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
                                pd.dismiss();
                                FancyToast.makeText(context, "Ошибка! Не удалось удалить публикацию.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
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
    private void deletePostWithoutImage(final String pId) {
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle("Удаление");
        builder.setMessage("Вы действительно хотите удалить публикацию?");
        builder.setIcon(R.drawable.delete_img);
        builder.setBackground(context.getResources().getDrawable(R.drawable.alert_dialog_bg));

        builder.setPositiveButton("Удалить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final ProgressDialog pd = new ProgressDialog(context);
                pd.setMessage("Удаление...");

                /*Шаги:
                1) Удаляем изображение с помощью URL
                2) Удаляем из базы данных, используя идентификатор сообщения*/
                //Изображение удалено, теперь удаляем из базы
                Query fQuery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
                fQuery.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()) {
                            ds.getRef().removeValue(); //удаляем значения из Firebase, где соответствует pId
                        }

                        //Когда удалено
                        pd.dismiss();
                        FancyToast.makeText(context, "Публикация удалена.", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS, false).show();
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

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView postImageIv;
        CircleImageView avatarIv;
        TextView userNameTv, timePostTv, titlePostTv, descPostTv, pCommentsTv, pLikesTv;
        ImageButton likeBtn, commentBtn, shareBtn, moreBtn;

        MyHolder(@NonNull View itemView) {
            super(itemView);

            //<editor-fold desc="Инициализация элементов">
            postImageIv = itemView.findViewById(R.id.postImageIv);
            avatarIv = itemView.findViewById(R.id.avatarIv);

            userNameTv = itemView.findViewById(R.id.userNameTv);
            timePostTv = itemView.findViewById(R.id.timePostTv);
            titlePostTv = itemView.findViewById(R.id.titlePostTv);
            descPostTv = itemView.findViewById(R.id.descPostTv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            pCommentsTv = itemView.findViewById(R.id.pCommentsTv);

            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            //</editor-fold>
        }
    }
}
