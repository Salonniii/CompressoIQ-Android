package com.example.aiphotocompressor;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentHistoryAdapter extends RecyclerView.Adapter<DocumentHistoryAdapter.DocumentViewHolder> {

    private final Context context;
    private final List<DocumentResponse> documentList;
    private final ApiService apiService;

    public DocumentHistoryAdapter(Context context, List<DocumentResponse> documentList) {
        this.context = context;
        this.documentList = documentList;
        this.apiService = RetrofitClient.getApiService();
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_document_history, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        DocumentResponse item = documentList.get(position);

        holder.tvFileName.setText(safeText(item.getOriginalFileName()));
        holder.tvFileType.setText("Type: " + safeText(item.getFileType()));
        holder.tvOriginalSize.setText("Original: " + formatFileSize(item.getOriginalSize()));
        holder.tvCompressedSize.setText("Compressed: " + formatFileSize(item.getCompressedSize()));
        holder.tvSavings.setText("Saved: " + formatPercentage(item.getSavings()));

        // ===============================
        // STATUS BADGE
        // ===============================
        String status = safeText(item.getStatus());

        if (status.equals("-") || status.trim().isEmpty()) {
            status = inferStatus(item.getSavings(), item.getCompressedSize(), item.getTargetSizeKB());
        }

        holder.tvStatus.setText(status);
        applyStatusStyle(holder.tvStatus, status);

        String fileType = safeText(item.getFileType()).toUpperCase();

        switch (fileType) {
            case "PDF":
                holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_save);
                break;
            case "WORD":
                holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_edit);
                break;
            case "PPT":
                holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_slideshow);
                break;
            case "EXCEL":
                holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_agenda);
                break;
            default:
                holder.ivFileIcon.setImageResource(android.R.drawable.ic_menu_upload);
                break;
        }

        holder.btnDownload.setOnClickListener(v -> {
            if (item.getId() != null && item.getCompressedFileName() != null) {
                downloadDocument(item.getId(), item.getCompressedFileName());
            }
        });

        holder.btnShare.setOnClickListener(v -> {
            if (item.getId() != null && item.getCompressedFileName() != null) {
                shareDocument(item.getId(), item.getCompressedFileName());
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (item.getId() != null) {
                deleteDocument(item.getId(), holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return documentList.size();
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFileIcon;
        TextView tvFileName, tvFileType, tvOriginalSize, tvCompressedSize, tvSavings, tvStatus;
        ImageButton btnDownload, btnShare, btnDelete;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);

            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileType = itemView.findViewById(R.id.tvFileType);
            tvOriginalSize = itemView.findViewById(R.id.tvOriginalSize);
            tvCompressedSize = itemView.findViewById(R.id.tvCompressedSize);
            tvSavings = itemView.findViewById(R.id.tvSavings);
            tvStatus = itemView.findViewById(R.id.tvStatus);

            btnDownload = itemView.findViewById(R.id.btnDownloadHistory);
            btnShare = itemView.findViewById(R.id.btnShareHistory);
            btnDelete = itemView.findViewById(R.id.btnDeleteHistory);
        }
    }

    // ===============================
    // DOWNLOAD
    // ===============================
    private void downloadDocument(Long documentId, String fileName) {
        apiService.downloadDocument(documentId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveToDownloads(response.body(), fileName);
                } else {
                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveToDownloads(ResponseBody body, String fileName) {
        try {
            String finalFileName = fileName;
            if (TextUtils.isEmpty(finalFileName)) {
                finalFileName = "compressed_document_" + System.currentTimeMillis();
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, finalFileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "*/*");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            if (uri == null) {
                Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                Toast.makeText(context, "Failed to open stream", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] buffer = new byte[4096];
            int read;

            while ((read = body.byteStream().read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();

            Toast.makeText(context, "Saved to Downloads 📥", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(context, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ===============================
    // SHARE
    // ===============================
    private void shareDocument(Long documentId, String fileName) {
        apiService.downloadDocument(documentId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        File shareFile = new File(context.getCacheDir(), fileName);

                        java.io.InputStream inputStream = response.body().byteStream();
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
                        shareIntent.setType("*/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        context.startActivity(Intent.createChooser(shareIntent, "Share Document"));

                    } catch (Exception e) {
                        Toast.makeText(context, "Share error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, "Unable to share document", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ===============================
    // DELETE
    // ===============================
    private void deleteDocument(Long documentId, int position) {
        apiService.deleteDocument(documentId).enqueue(new Callback<DeleteResponse>() {
            @Override
            public void onResponse(Call<DeleteResponse> call, Response<DeleteResponse> response) {
                if (response.isSuccessful()) {
                    documentList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, documentList.size());

                    Toast.makeText(context, "Deleted successfully 🗑️", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DeleteResponse> call, Throwable t) {
                Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ===============================
    // HELPERS
    // ===============================
    private String safeText(String value) {
        return value != null ? value : "-";
    }

    private String formatPercentage(Double value) {
        if (value == null) return "0.0%";
        return String.format(Locale.US, "%.2f%%", value);
    }

    private String formatFileSize(Double sizeKB) {
        if (sizeKB == null) return "0 KB";

        if (sizeKB >= 1024) {
            return String.format(Locale.US, "%.2f MB", sizeKB / 1024.0);
        } else {
            return String.format(Locale.US, "%.2f KB", sizeKB);
        }
    }

    private String inferStatus(Double savings, Double compressedSizeKB, Long targetSizeKB) {
        double save = savings != null ? savings : 0.0;
        double compressed = compressedSizeKB != null ? compressedSizeKB : 0.0;
        long target = targetSizeKB != null ? targetSizeKB : 0;

        if (save < 1.0) {
            return "Already Optimized";
        }

        if (save >= 1.0 && save < 5.0) {
            return "Light Compression Applied";
        }

        if (target > 0 && compressed > target * 1.20) {
            return "Quality Protected";
        }

        return "Compressed Successfully";
    }

    private void applyStatusStyle(TextView textView, String status) {
        switch (status) {
            case "Already Optimized":
                textView.setText("⭐ Already Optimized");
                textView.setTextColor(Color.parseColor("#60A5FA")); // blue
                break;

            case "Light Compression Applied":
                textView.setText("ℹ️ Light Compression Applied");
                textView.setTextColor(Color.parseColor("#FACC15")); // yellow
                break;

            case "Quality Protected":
                textView.setText("🛡 Quality Protected");
                textView.setTextColor(Color.parseColor("#C084FC")); // purple
                break;

            default:
                textView.setText("✅ Compressed Successfully");
                textView.setTextColor(Color.parseColor("#22C55E")); // green
                break;
        }
    }
}