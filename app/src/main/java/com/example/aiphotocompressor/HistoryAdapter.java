package com.example.aiphotocompressor;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<ImageResponse> historyList;
    private final OnHistoryChangedListener listener;

    public interface OnHistoryChangedListener {
        void onHistoryChanged();
    }

    public HistoryAdapter(Context context, List<ImageResponse> historyList, OnHistoryChangedListener listener) {
        this.context = context;
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageResponse item = historyList.get(position);

        holder.tvFileName.setText("🖼 Image #" + (item.getId() != null ? item.getId() : "N/A"));
        holder.tvOriginalSize.setText("Original: " + (item.getOriginalSize() != null ? item.getOriginalSize() : 0) + " KB");
        holder.tvCompressedSize.setText("Compressed: " + (item.getCompressedSize() != null ? item.getCompressedSize() : 0) + " KB");

        String imageUrl = RetrofitClient.getBaseUrl() + "api/images/download/" + item.getId();

        Glide.with(context)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.ivHistoryImage);

        holder.btnDownload.setOnClickListener(v -> {
            if (item.getId() != null) {
                downloadImageToGallery(item.getId(), item.getOutputFormat());
            } else {
                Toast.makeText(context, "Invalid image ID", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnShare.setOnClickListener(v -> {
            if (item.getId() != null) {
                shareImage(item.getId(), item.getOutputFormat());
            } else {
                Toast.makeText(context, "Invalid image ID", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && item.getId() != null) {
                showDeleteDialog(item.getId(), currentPosition);
            }
        });
    }

    private void showDeleteDialog(Long imageId, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Image");
        builder.setMessage("Are you sure you want to delete this image from history?");
        builder.setPositiveButton("Delete", (dialog, which) -> deleteItem(imageId, position));
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void deleteItem(Long imageId, int position) {
        ApiService apiService = RetrofitClient.getApiService();

        apiService.deleteImage(imageId).enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                if (response.isSuccessful()) {
                    historyList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, historyList.size());

                    if (listener != null) {
                        listener.onHistoryChanged();
                    }

                    String msg = "🗑 Deleted successfully";
                    if (response.body() != null && response.body().getMessage() != null) {
                        msg = response.body().getMessage();
                    }

                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeleteResponse> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadImageToGallery(Long imageId, String outputFormat) {
        ApiService apiService = RetrofitClient.getApiService();

        apiService.downloadCompressedImage(imageId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        InputStream inputStream = response.body().byteStream();

                        String extension = getSafeExtension(outputFormat);
                        String mimeType = getMimeType(extension);
                        String fileName = "compressed_" + imageId + "." + extension;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CompressoIQ");
                            values.put(MediaStore.Images.Media.IS_PENDING, 1);

                            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                            if (uri == null) {
                                Toast.makeText(context, "Failed to create file in gallery", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);

                            if (outputStream == null) {
                                Toast.makeText(context, "Failed to open output stream", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            byte[] buffer = new byte[4096];
                            int read;

                            while ((read = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, read);
                            }

                            outputStream.flush();
                            outputStream.close();
                            inputStream.close();

                            values.clear();
                            values.put(MediaStore.Images.Media.IS_PENDING, 0);
                            context.getContentResolver().update(uri, values, null, null);

                            Toast.makeText(context, "⬇ Saved to Gallery / Pictures", Toast.LENGTH_LONG).show();

                        } else {
                            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                            File appDir = new File(picturesDir, "CompressoIQ");

                            if (!appDir.exists()) {
                                appDir.mkdirs();
                            }

                            File file = new File(appDir, fileName);
                            FileOutputStream outputStream = new FileOutputStream(file);

                            byte[] buffer = new byte[4096];
                            int read;

                            while ((read = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, read);
                            }

                            outputStream.flush();
                            outputStream.close();
                            inputStream.close();

                            MediaScannerConnection.scanFile(
                                    context,
                                    new String[]{file.getAbsolutePath()},
                                    new String[]{mimeType},
                                    null
                            );

                            Toast.makeText(context, "⬇ Saved to Gallery / Pictures", Toast.LENGTH_LONG).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(context, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareImage(Long imageId, String outputFormat) {
        ApiService apiService = RetrofitClient.getApiService();

        apiService.downloadCompressedImage(imageId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String extension = getSafeExtension(outputFormat);
                        String mimeType = getMimeType(extension);

                        File shareFile = new File(context.getCacheDir(), "shared_image_" + imageId + "." + extension);

                        InputStream inputStream = response.body().byteStream();
                        FileOutputStream outputStream = new FileOutputStream(shareFile);

                        byte[] buffer = new byte[4096];
                        int read;

                        while ((read = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, read);
                        }

                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();

                        Uri fileUri = FileProvider.getUriForFile(
                                context,
                                context.getPackageName() + ".provider",
                                shareFile
                        );

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType(mimeType);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        context.startActivity(Intent.createChooser(shareIntent, "Share Image"));

                    } catch (Exception e) {
                        Toast.makeText(context, "Share failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSafeExtension(String outputFormat) {
        if (outputFormat == null) return "jpg";

        String format = outputFormat.toLowerCase().trim();

        switch (format) {
            case "jpeg":
            case "jpg":
                return "jpg";
            case "png":
                return "png";
            case "webp":
                return "webp";
            default:
                return "jpg";
        }
    }

    private String getMimeType(String extension) {
        switch (extension.toLowerCase()) {
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            case "jpg":
            case "jpeg":
            default:
                return "image/jpeg";
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvFileName, tvOriginalSize, tvCompressedSize;
        ImageView ivHistoryImage;
        Button btnDownload, btnShare, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvOriginalSize = itemView.findViewById(R.id.tvOriginalSize);
            tvCompressedSize = itemView.findViewById(R.id.tvCompressedSize);
            ivHistoryImage = itemView.findViewById(R.id.ivHistoryImage);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}