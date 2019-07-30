package com.app.smartriumclock.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import androidx.annotation.StringDef;
import com.app.smartriumclock.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by TedPark on 2018. 2. 3..
 */

public class NotificationManager {

    private static final String GROUP_TED_PARK = "tedPark";

    public static void createChannel(Context context) {

        NotificationChannelGroup group1 = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            group1 = new NotificationChannelGroup(GROUP_TED_PARK, GROUP_TED_PARK);
            getManager(context).createNotificationChannelGroup(group1);
        }


        NotificationChannel channelMessage = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channelMessage = new NotificationChannel(Channel.MESSAGE,
                    context.getString(R.string.notification_channel_dust_title), android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channelMessage.setDescription(context.getString(R.string.notification_channel_dust_description));
            channelMessage.setGroup(GROUP_TED_PARK);
            channelMessage.setLightColor(Color.GREEN);
            channelMessage.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            getManager(context).createNotificationChannel(channelMessage);

            NotificationChannel channelComment = new NotificationChannel(Channel.COMMENT,
                    context.getString(R.string.notification_channel_ultra_dust_title), android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channelComment.setDescription(context.getString(R.string.notification_channel_ultra_dust_description));
            channelComment.setGroup(GROUP_TED_PARK);
            channelComment.setLightColor(Color.BLUE);
            channelComment.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getManager(context).createNotificationChannel(channelComment);

            /*NotificationChannel channelNotice = new NotificationChannel(Channel.NOTICE,
                    context.getString(R.string.notification_channel_notice_title), android.app.NotificationManager.IMPORTANCE_HIGH);
            channelNotice.setDescription(context.getString(R.string.notification_channel_notice_description));
            channelNotice.setGroup(GROUP_TED_PARK);
            channelNotice.setLightColor(Color.RED);
            channelNotice.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getManager(context).createNotificationChannel(channelNotice);*/
        }


    }

    private static android.app.NotificationManager getManager(Context context) {
        return (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static void deleteChannel(Context context, @Channel String channel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getManager(context).deleteNotificationChannel(channel);
        }

    }

    public static void sendNotification(Context context, int id, @Channel String channel, String title, String body) {
        Notification.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, channel)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSmallIcon(getSmallIcon())
                    .setAutoCancel(true);
        }

        getManager(context).notify(id, builder.build());
    }

    private static int getSmallIcon() {
        return android.R.drawable.stat_notify_chat;
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            Channel.MESSAGE,
            Channel.COMMENT,
            Channel.NOTICE
    })
    public @interface Channel {
        String MESSAGE = "message";
        String COMMENT = "comment";
        String NOTICE = "notice";
    }

}
