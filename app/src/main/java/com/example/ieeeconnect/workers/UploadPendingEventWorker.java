package com.example.ieeeconnect.workers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.ieeeconnect.database.AppDatabase;
import com.example.ieeeconnect.database.PendingEvent;
import com.example.ieeeconnect.database.PendingEventDao;
import com.example.ieeeconnect.domain.model.Event;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class UploadPendingEventWorker extends Worker {
    private static final String TAG = "UploadPendingEventWkr";

    public UploadPendingEventWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public androidx.work.ListenableWorker.Result doWork() {
        Context ctx = getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(ctx);
        PendingEventDao dao = db.pendingEventDao();
        List<PendingEvent> pending = dao.getAllPending();
        if (pending == null || pending.isEmpty()) return androidx.work.ListenableWorker.Result.success();

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        boolean transientFailure = false;

        for (PendingEvent p : pending) {
            try {
                String bannerUrl = p.bannerUrl;

                // If bannerUrl is present but doesn't look like a remote http(s) URL,
                // treat it as a local Uri and upload to Firebase Storage.
                if (bannerUrl != null && !bannerUrl.isEmpty() && !bannerUrl.startsWith("http")) {
                    try {
                        Uri fileUri = Uri.parse(bannerUrl);
                        String remoteName = "event_banners/" + UUID.randomUUID();
                        StorageReference ref = storage.getReference().child(remoteName);

                        UploadTask uploadTask = ref.putFile(fileUri);
                        // Wait for upload to finish
                        Tasks.await(uploadTask);

                        // Get download URL
                        Uri downloadUri = Tasks.await(ref.getDownloadUrl());
                        if (downloadUri != null) {
                            bannerUrl = downloadUri.toString();
                        }
                    } catch (Exception ex) {
                        // If upload fails for this file, log and mark transient failure (retry later)
                        Log.w(TAG, "Failed uploading banner for pending event " + p.localId, ex);
                        transientFailure = true;
                        continue; // skip trying to upload this event now
                    }
                }

                Event e = new Event();
                e.setTitle(p.title);
                e.setDescription(p.description);
                e.setEventTime(p.eventTime);
                e.setBannerUrl(bannerUrl);
                e.setCreatedByUserId(p.createdByUserId == null ? "" : p.createdByUserId);

                // Synchronously add to Firestore and wait for result
                DocumentReference docRef = Tasks.await(firestore.collection("events").add(e));
                if (docRef != null) {
                    Log.d(TAG, "Uploaded pending event " + p.localId + " -> " + docRef.getId());
                    dao.deleteById(p.localId);
                } else {
                    Log.w(TAG, "Firestore returned null DocumentReference for pending event " + p.localId);
                    transientFailure = true;
                }

            } catch (ExecutionException ee) {
                Log.w(TAG, "ExecutionException processing pending event " + p.localId, ee);
                transientFailure = true;
            } catch (InterruptedException ie) {
                Log.w(TAG, "Interrupted while processing pending event " + p.localId, ie);
                Thread.currentThread().interrupt();
                transientFailure = true;
            } catch (Exception ex) {
                Log.w(TAG, "Error processing pending event " + p.localId, ex);
                // don't fail the whole run for a single bad item; mark transient to retry
                transientFailure = true;
            }
        }

        if (transientFailure) {
            // Ask WorkManager to retry later
            return androidx.work.ListenableWorker.Result.retry();
        }

        return androidx.work.ListenableWorker.Result.success();
    }
}
