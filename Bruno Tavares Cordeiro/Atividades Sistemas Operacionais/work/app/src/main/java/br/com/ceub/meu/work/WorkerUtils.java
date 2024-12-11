/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.com.ceub.meu.work;

import static br.com.ceub.meu.work.Constants.CHANNEL_ID;
import static br.com.ceub.meu.work.Constants.DELAY_TIME_MILLIS;


import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;


final class WorkerUtils {
    private static final String TAG = WorkerUtils.class.getSimpleName();
    /**
     * Create a Notification that is shown as a heads-up notification if possible.
     *
     * For this codelab, this is used to show a notification so that you know when different steps
     * of the background work chain are starting
     *
     * @param message Message shown on the notification
     */
    static void makeStatusNotification(String message, Context context) {

        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            CharSequence name = Constants.VERBOSE_NOTIFICATION_CHANNEL_NAME;
            String description = Constants.VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION;
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Add the channel
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(Constants.NOTIFICATION_TITLE)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[0]);


        showNotification(context, builder);
    }

    @SuppressLint("MissingPermission")
    public static void showNotification(Context context, NotificationCompat.Builder builder) {
        // Enviar a notificação se a permissão estiver concedida
        NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_ID, builder.build());
    }


    // Exemplo de criação de um canal de notificação (necessário para Android O e superior)
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "default_channel_id";
            String channelName = "Default Channel";
            String channelDescription = "Canal padrão para notificações";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Method for sleeping for a fixed amount of time to emulate slower work
     */
    static void sleep() {
        try {
            Thread.sleep(DELAY_TIME_MILLIS, 0);
        } catch (InterruptedException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * Blurs the given Bitmap image
     * @param bitmap Image to blur
     * @param applicationContext Application context
     * @return Blurred bitmap image
     */
    @WorkerThread
    static Bitmap blurBitmap(@NonNull Bitmap bitmap,
                             @NonNull Context applicationContext) {

        RenderScript rsContext = null;
        try {

            // Create the output bitmap
            Bitmap output = Bitmap.createBitmap(
                    bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());

            // Blur the image
            rsContext = RenderScript.create(applicationContext, RenderScript.ContextType.DEBUG);
            Allocation inAlloc = Allocation.createFromBitmap(rsContext, bitmap);
            Allocation outAlloc = Allocation.createTyped(rsContext, inAlloc.getType());
            ScriptIntrinsicBlur theIntrinsic =
                    ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext));
            theIntrinsic.setRadius(25.f);
            theIntrinsic.setInput(inAlloc);
            theIntrinsic.forEach(outAlloc);
            outAlloc.copyTo(output);

            return output;
        } finally {
            if (rsContext != null) {
                rsContext.finish();
            }
        }
    }

    /**
     * Writes bitmap to a temporary file and returns the Uri for the file
     * @param applicationContext Application context
     * @param bitmap Bitmap to write to temp file
     * @return Uri for temp file with bitmap
     * @throws FileNotFoundException Throws if bitmap file cannot be found
     */
    static Uri writeBitmapToFile(
            @NonNull Context applicationContext,
            @NonNull Bitmap bitmap) throws FileNotFoundException {

        String name = String.format("blur-filter-output-%s.png", UUID.randomUUID().toString());
        File outputDir = new File(applicationContext.getFilesDir(), Constants.OUTPUT_PATH);
        if (!outputDir.exists()) {
            outputDir.mkdirs(); // should succeed
        }
        File outputFile = new File(outputDir, name);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /* ignored for PNG */, out);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
        return Uri.fromFile(outputFile);
    }

    private WorkerUtils() {
    }
}