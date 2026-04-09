package com.example.aiphotocompressor;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentCompressorActivity extends AppCompatActivity {

    private EditText etTargetSizeDocument;
    private Button btnDocumentHistory;
    private Button btnPickDocument, btnCompressDocument;
    private Button btnPreset50, btnPreset100, btnPreset200, btnPreset300;
    private Button btnDownloadDocument, btnShareWhatsapp, btnShareGmail, btnShareInstagram, btnShareMore;
    private TextView tvSelectedDocument, tvDocumentResult;
    private ImageButton btnBackDocument;

    private Uri selectedDocumentUri;
    private ApiService apiService;
    private SessionManager sessionManager;

    private Long compressedDocumentId = null;
    private String compressedDocumentFileName = null;

    private final ActivityResultLauncher<String[]> documentPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    selectedDocumentUri = uri;
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

                    String fileName = getFileNameFromUri(uri);
                    tvSelectedDocument.setText("📄 Selected: " + fileName);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_compressor);

        apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(this);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etTargetSizeDocument = findViewById(R.id.etTargetSizeDocument);

        btnPickDocument = findViewById(R.id.btnPickDocument);
        btnCompressDocument = findViewById(R.id.btnCompressDocument);
        btnDocumentHistory = findViewById(R.id.btnDocumentHistory);

        btnPreset50 = findViewById(R.id.btnPreset50);
        btnPreset100 = findViewById(R.id.btnPreset100);
        btnPreset200 = findViewById(R.id.btnPreset200);
        btnPreset300 = findViewById(R.id.btnPreset300);

        btnDownloadDocument = findViewById(R.id.btnDownloadDocument);
        btnShareWhatsapp = findViewById(R.id.btnShareWhatsapp);
        btnShareGmail = findViewById(R.id.btnShareGmail);
        btnShareInstagram = findViewById(R.id.btnShareInstagram);
        btnShareMore = findViewById(R.id.btnShareMore);

        tvSelectedDocument = findViewById(R.id.tvSelectedDocument);
        tvDocumentResult = findViewById(R.id.tvDocumentResult);

        btnBackDocument = findViewById(R.id.btnBackDocument);

        btnDownloadDocument.setEnabled(false);
        btnShareWhatsapp.setEnabled(false);
        btnShareGmail.setEnabled(false);
        btnShareInstagram.setEnabled(false);
        btnShareMore.setEnabled(false);
    }

    private void setupClickListeners() {
        btnBackDocument.setOnClickListener(v -> finish());

        btnPickDocument.setOnClickListener(v -> openDocumentPicker());

        btnCompressDocument.setOnClickListener(v -> compressDocument());

        btnDocumentHistory.setOnClickListener(v -> {
            Intent intent = new Intent(DocumentCompressorActivity.this, DocumentHistoryActivity.class);
            startActivity(intent);
        });

        btnPreset50.setOnClickListener(v -> etTargetSizeDocument.setText("50"));
        btnPreset100.setOnClickListener(v -> etTargetSizeDocument.setText("100"));
        btnPreset200.setOnClickListener(v -> etTargetSizeDocument.setText("200"));
        btnPreset300.setOnClickListener(v -> etTargetSizeDocument.setText("300"));

        btnDownloadDocument.setOnClickListener(v -> {
            if (compressedDocumentId != null && compressedDocumentFileName != null) {
                downloadCompressedDocument(compressedDocumentId, compressedDocumentFileName);
            }
        });

        btnShareWhatsapp.setOnClickListener(v -> shareCompressedDocument("com.whatsapp"));
        btnShareGmail.setOnClickListener(v -> shareCompressedDocument("com.google.android.gm"));
        btnShareInstagram.setOnClickListener(v -> shareCompressedDocument("com.instagram.android"));
        btnShareMore.setOnClickListener(v -> shareCompressedDocument(null));
    }

    private void openDocumentPicker() {
        documentPickerLauncher.launch(new String[]{
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        });
    }

    private void compressDocument() {
        if (selectedDocumentUri == null) {
            Toast.makeText(this, "Please select a document first", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetSizeText = etTargetSizeDocument.getText().toString().trim();
        if (TextUtils.isEmpty(targetSizeText)) {
            Toast.makeText(this, "Please enter target size", Toast.LENGTH_SHORT).show();
            return;
        }

        Long userId = sessionManager.getUserId();
        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = FileUtil.from(this, selectedDocumentUri);

            RequestBody requestFile = RequestBody.create(MediaType.parse("*/*"), file);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            RequestBody targetSizeKB = RequestBody.create(
                    MediaType.parse("text/plain"),
                    targetSizeText
            );

            RequestBody userIdBody = RequestBody.create(
                    MediaType.parse("text/plain"),
                    String.valueOf(userId)
            );

            btnCompressDocument.setEnabled(false);
            btnCompressDocument.setText("📦 Compressing...");

            apiService.uploadDocument(filePart, targetSizeKB, userIdBody)
                    .enqueue(new Callback<DocumentResponse>() {
                        @Override
                        public void onResponse(Call<DocumentResponse> call, Response<DocumentResponse> response) {
                            btnCompressDocument.setEnabled(true);
                            btnCompressDocument.setText("🚀 Compress Document");

                            if (response.isSuccessful() && response.body() != null) {
                                DocumentResponse documentResponse = response.body();

                                compressedDocumentId = documentResponse.getId();
                                compressedDocumentFileName = documentResponse.getCompressedFileName();

                                tvDocumentResult.setText(
                                        "✅ Compression Successful\n\n" +
                                                "📄 File Type: " + safeText(documentResponse.getFileType()) + "\n" +
                                                "📁 File Name: " + safeText(documentResponse.getOriginalFileName()) + "\n" +
                                                "📉 Original Size: " + safeDouble(documentResponse.getOriginalSize()) + " KB\n" +
                                                "📦 Compressed Size: " + safeDouble(documentResponse.getCompressedSize()) + " KB\n" +
                                                "💾 Space Saved: " + safeDouble(documentResponse.getSavings()) + "%\n" +
                                                "🎯 Target Size: " + safeLong(documentResponse.getTargetSizeKB()) + " KB"
                                );

                                enableActionButtons();

                                Toast.makeText(DocumentCompressorActivity.this,
                                        "Document compressed successfully 🎉",
                                        Toast.LENGTH_LONG).show();

                            } else {
                                tvDocumentResult.setText("❌ Compression failed");
                                Toast.makeText(DocumentCompressorActivity.this,
                                        "Compression failed",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<DocumentResponse> call, Throwable t) {
                            btnCompressDocument.setEnabled(true);
                            btnCompressDocument.setText("🚀 Compress Document");

                            tvDocumentResult.setText("❌ Error: " + t.getMessage());

                            Toast.makeText(DocumentCompressorActivity.this,
                                    "Error: " + t.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (Exception e) {
            btnCompressDocument.setEnabled(true);
            btnCompressDocument.setText("🚀 Compress Document");

            Toast.makeText(this, "File error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void enableActionButtons() {
        btnDownloadDocument.setEnabled(true);
        btnShareWhatsapp.setEnabled(true);
        btnShareGmail.setEnabled(true);
        btnShareInstagram.setEnabled(true);
        btnShareMore.setEnabled(true);
    }

    private void downloadCompressedDocument(Long documentId, String fileName) {
        apiService.downloadDocument(documentId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveDocumentToDownloads(response.body(), fileName);
                } else {
                    Toast.makeText(DocumentCompressorActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(DocumentCompressorActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveDocumentToDownloads(ResponseBody body, String fileName) {
        try {
            String finalFileName = fileName;
            if (TextUtils.isEmpty(finalFileName)) {
                finalFileName = "compressed_document_" + System.currentTimeMillis() + ".pdf";
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, finalFileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "*/*");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            if (uri == null) {
                Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                Toast.makeText(this, "Failed to open output stream", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] buffer = new byte[4096];
            int read;

            while ((read = body.byteStream().read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();

            Toast.makeText(this, "Saved to Downloads 📥", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareCompressedDocument(String packageName) {
        if (compressedDocumentId == null || compressedDocumentFileName == null) {
            Toast.makeText(this, "No compressed document available", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.downloadDocument(compressedDocumentId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        File shareFile = new File(getCacheDir(), compressedDocumentFileName);
                        java.io.InputStream inputStream = response.body().byteStream();
                        java.io.FileOutputStream outputStream = new java.io.FileOutputStream(shareFile);

                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, read);
                        }

                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();

                        Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                                DocumentCompressorActivity.this,
                                getPackageName() + ".provider",
                                shareFile
                        );

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("*/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        if (packageName != null) {
                            shareIntent.setPackage(packageName);
                        }

                        startActivity(Intent.createChooser(shareIntent, "Share Document"));

                    } catch (Exception e) {
                        Toast.makeText(DocumentCompressorActivity.this,
                                "Share error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(DocumentCompressorActivity.this,
                            "Unable to share document",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(DocumentCompressorActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;

        android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    result = cursor.getString(index);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (result == null) {
            result = "document_file";
        }

        return result;
    }

    private String safeText(String value) {
        return value != null ? value : "-";
    }

    private String safeDouble(Double value) {
        return value != null ? String.valueOf(value) : "0";
    }

    private String safeLong(Long value) {
        return value != null ? String.valueOf(value) : "0";
    }
}