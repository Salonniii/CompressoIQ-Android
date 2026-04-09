package com.example.aiphotocompressor;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Button btnSelectImage, btnUpload, btnHistory, btnLogout;
    private Button btnPdfCompressor, btnWordCompressor, btnPptCompressor, btnExcelCompressor;
    private Button btnForm, btnEmail, btnWebsite, btnInstagram, btnWhatsapp, btnId, btnJob, btnAssignment;
    private Button btn50KB, btn100KB, btn200KB, btn500KB, btn1MB;

    private EditText etTargetSize;
    private Spinner spinnerMode, spinnerFormat;

    private ImageView imagePreview, imageCompressedPreview;

    private TextView tvSelectedFile, tvResult, tvOriginalSize, tvCompressedSize, tvSavings, tvScore, tvFormatBadge;

    private CardView resultCard;

    private Button btnDownload, btnShareWhatsapp, btnShareEmail, btnShareInstagram, btnShareGeneric;

    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private Uri compressedImageUri;
    private Uri cameraImageUri;

    private ApiService apiService;
    private SessionManager sessionManager;

    private Long latestCompressedImageId = null;
    private String latestOutputFormat = "jpg";

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imagePreview.setImageURI(uri);
                    tvSelectedFile.setText("📁 Selected: " + getFileName(uri));
                    Toast.makeText(this, "Image Selected ✅", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    imagePreview.setImageURI(cameraImageUri);
                    tvSelectedFile.setText("📷 Captured Photo");
                    Toast.makeText(this, "Photo Captured ✅", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        initViews();
        setupModeSpinner();
        setupFormatSpinner();
        setupClickListeners();

        apiService = RetrofitClient.getApiService();
    }

    private void initViews() {
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnUpload = findViewById(R.id.btnUpload);
        btnHistory = findViewById(R.id.btnHistory);
        btnLogout = findViewById(R.id.btnLogout);

        btnPdfCompressor = findViewById(R.id.btnPdfCompressor);
        btnWordCompressor = findViewById(R.id.btnWordCompressor);
        btnPptCompressor = findViewById(R.id.btnPptCompressor);
        btnExcelCompressor = findViewById(R.id.btnExcelCompressor);

        btnForm = findViewById(R.id.btnForm);
        btnEmail = findViewById(R.id.btnEmail);
        btnWebsite = findViewById(R.id.btnWebsite);
        btnInstagram = findViewById(R.id.btnInstagram);
        btnWhatsapp = findViewById(R.id.btnWhatsapp);
        btnId = findViewById(R.id.btnId);
        btnJob = findViewById(R.id.btnJob);
        btnAssignment = findViewById(R.id.btnAssignment);

        btn50KB = findViewById(R.id.btn50KB);
        btn100KB = findViewById(R.id.btn100KB);
        btn200KB = findViewById(R.id.btn200KB);
        btn500KB = findViewById(R.id.btn500KB);
        btn1MB = findViewById(R.id.btn1MB);

        etTargetSize = findViewById(R.id.etTargetSize);
        spinnerMode = findViewById(R.id.spinnerMode);
        spinnerFormat = findViewById(R.id.spinnerFormat);

        imagePreview = findViewById(R.id.imagePreview);
        imageCompressedPreview = findViewById(R.id.imageCompressedPreview);

        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        tvResult = findViewById(R.id.tvResult);
        tvOriginalSize = findViewById(R.id.tvOriginalSize);
        tvCompressedSize = findViewById(R.id.tvCompressedSize);
        tvSavings = findViewById(R.id.tvSavings);
        tvScore = findViewById(R.id.tvScore);
        tvFormatBadge = findViewById(R.id.tvFormatBadge);

        resultCard = findViewById(R.id.resultCard);

        btnDownload = findViewById(R.id.btnDownload);
        btnShareWhatsapp = findViewById(R.id.btnShareWhatsapp);
        btnShareEmail = findViewById(R.id.btnShareEmail);
        btnShareInstagram = findViewById(R.id.btnShareInstagram);
        btnShareGeneric = findViewById(R.id.btnShareGeneric);

        progressBar = findViewById(R.id.progressBar);
    }

    private void setupModeSpinner() {
        String[] modes = {"FORM_UPLOAD", "EMAIL", "WEBSITE", "INSTAGRAM", "WHATSAPP", "ID_CARD", "JOB_PORTAL", "ASSIGNMENT"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_white,
                modes
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_white);
        spinnerMode.setAdapter(adapter);
    }

    private void setupFormatSpinner() {
        String[] formats = {"JPG", "PNG"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_white,
                formats
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_white);
        spinnerFormat.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> showImagePickerDialog());
        btnUpload.setOnClickListener(v -> uploadImage());

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, HistoryActivity.class)));

        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        btnPdfCompressor.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DocumentCompressorActivity.class)));

        btnWordCompressor.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DocumentCompressorActivity.class)));

        btnPptCompressor.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DocumentCompressorActivity.class)));

        btnExcelCompressor.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DocumentCompressorActivity.class)));

        btnForm.setOnClickListener(v -> setPreset("FORM_UPLOAD", "JPG", "50"));
        btnEmail.setOnClickListener(v -> setPreset("EMAIL", "JPG", "100"));
        btnWebsite.setOnClickListener(v -> setPreset("WEBSITE", "JPG", "200"));
        btnInstagram.setOnClickListener(v -> setPreset("INSTAGRAM", "JPG", "200"));
        btnWhatsapp.setOnClickListener(v -> setPreset("WHATSAPP", "JPG", "100"));
        btnId.setOnClickListener(v -> setPreset("ID_CARD", "JPG", "50"));
        btnJob.setOnClickListener(v -> setPreset("JOB_PORTAL", "JPG", "100"));
        btnAssignment.setOnClickListener(v -> setPreset("ASSIGNMENT", "JPG", "200"));

        btn50KB.setOnClickListener(v -> etTargetSize.setText("50"));
        btn100KB.setOnClickListener(v -> etTargetSize.setText("100"));
        btn200KB.setOnClickListener(v -> etTargetSize.setText("200"));
        btn500KB.setOnClickListener(v -> etTargetSize.setText("500"));
        btn1MB.setOnClickListener(v -> etTargetSize.setText("1024"));

        btnDownload.setOnClickListener(v -> {
            if (latestCompressedImageId != null) {
                downloadCompressedImageAndSave(latestCompressedImageId, latestOutputFormat);
            } else {
                Toast.makeText(this, "No compressed image available", Toast.LENGTH_SHORT).show();
            }
        });

        btnShareWhatsapp.setOnClickListener(v -> shareImageToSpecificApp("com.whatsapp"));
        btnShareInstagram.setOnClickListener(v -> shareImageToSpecificApp("com.instagram.android"));
        btnShareEmail.setOnClickListener(v -> shareViaEmail());
        btnShareGeneric.setOnClickListener(v -> shareImageGeneric());
    }

    private void showImagePickerDialog() {
        String[] options = {"📷 Take Photo", "🖼️ Choose from Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Select Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndOpen();
                    } else {
                        imagePickerLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Captured_Image");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Photo taken from camera");

        cameraImageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

        if (cameraImageUri != null) {
            cameraLauncher.launch(cameraImageUri);
        } else {
            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void setPreset(String mode, String format, String size) {
        setSpinnerSelection(spinnerMode, mode);
        setSpinnerSelection(spinnerFormat, format);
        etTargetSize.setText(size);
        Toast.makeText(this, "Preset Applied ⚡", Toast.LENGTH_SHORT).show();
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void uploadImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetSizeText = etTargetSize.getText().toString().trim();
        if (targetSizeText.isEmpty()) {
            Toast.makeText(this, "Enter target size", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = FileUtil.from(this, selectedImageUri);

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            RequestBody targetSize = RequestBody.create(MediaType.parse("text/plain"), targetSizeText);
            RequestBody compressionMode = RequestBody.create(MediaType.parse("text/plain"), spinnerMode.getSelectedItem().toString());
            RequestBody outputFormat = RequestBody.create(MediaType.parse("text/plain"), spinnerFormat.getSelectedItem().toString());

            long userId = sessionManager.getUserId();
            RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(userId));

            progressBar.setVisibility(View.VISIBLE);
            resultCard.setVisibility(View.GONE);

            latestCompressedImageId = null;
            latestOutputFormat = "jpg";
            compressedImageUri = null;

            Toast.makeText(this, "⏳ Photo is compressing... please wait", Toast.LENGTH_SHORT).show();

            apiService.uploadImage(imagePart, targetSize, compressionMode, outputFormat, userIdBody)
                    .enqueue(new Callback<ImageResponse>() {
                        @Override
                        public void onResponse(Call<ImageResponse> call, Response<ImageResponse> response) {
                            progressBar.setVisibility(View.GONE);

                            if (response.isSuccessful() && response.body() != null) {
                                ImageResponse res = response.body();

                                resultCard.setVisibility(View.VISIBLE);

                                double original = res.getOriginalSize() != null ? res.getOriginalSize() : 0.0;
                                double compressed = res.getCompressedSize() != null ? res.getCompressedSize() : 0.0;
                                double savings = res.getSavings() != null ? res.getSavings() : 0.0;
                                String score = res.getScore() != null ? res.getScore() : "N/A";
                                String output = res.getOutputFormat() != null ? res.getOutputFormat() : "JPG";

                                tvResult.setText("🎉 Compression Completed!");
                                tvOriginalSize.setText("Original Size: " + original + " KB");
                                tvCompressedSize.setText("Compressed Size: " + compressed + " KB");
                                tvSavings.setText("Space Saved: " + savings + "%");
                                tvScore.setText("🤖 AI Score: " + score);
                                tvFormatBadge.setText("📦 Output: " + output);

                                Glide.with(MainActivity.this)
                                        .load(selectedImageUri)
                                        .into(imageCompressedPreview);

                                latestCompressedImageId = res.getId();
                                latestOutputFormat = output.toLowerCase();

                                Toast.makeText(MainActivity.this, "Compression successful ✅", Toast.LENGTH_SHORT).show();

                            } else {
                                Toast.makeText(MainActivity.this, "Compression failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ImageResponse> call, Throwable t) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void downloadCompressedImageAndSave(Long imageId, String outputFormat) {
        apiService.downloadCompressedImage(imageId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveImageToGallery(response.body(), outputFormat);
                } else {
                    Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveImageToGallery(ResponseBody body, String outputFormat) {
        try {
            String extension = getSafeExtension(outputFormat);
            String mimeType = getMimeType(extension);

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "compressed_" + System.currentTimeMillis() + "." + extension);
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AIPhotoCompressor");
            }

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(body.bytes());
                    outputStream.close();

                    compressedImageUri = uri;

                    Glide.with(MainActivity.this)
                            .load(compressedImageUri)
                            .into(imageCompressedPreview);

                    Toast.makeText(MainActivity.this, "Saved to Gallery! 🖼️", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Save Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareImageGeneric() {
        if (latestCompressedImageId == null) {
            Toast.makeText(this, "No compressed image to share", Toast.LENGTH_SHORT).show();
            return;
        }

        downloadImageToCacheAndShare(latestCompressedImageId, latestOutputFormat, null);
    }

    private void shareImageToSpecificApp(String packageName) {
        if (latestCompressedImageId == null) {
            Toast.makeText(this, "No compressed image to share", Toast.LENGTH_SHORT).show();
            return;
        }

        downloadImageToCacheAndShare(latestCompressedImageId, latestOutputFormat, packageName);
    }

    private void shareViaEmail() {
        if (latestCompressedImageId == null) {
            Toast.makeText(this, "No compressed image to share", Toast.LENGTH_SHORT).show();
            return;
        }

        downloadImageToCacheAndShare(latestCompressedImageId, latestOutputFormat, "email");
    }

    private void downloadImageToCacheAndShare(Long imageId, String outputFormat, String packageName) {
        apiService.downloadCompressedImage(imageId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String extension = getSafeExtension(outputFormat);
                        String mimeType = getMimeType(extension);

                        File shareFile = new File(getCacheDir(), "shared_image_" + imageId + "." + extension);

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
                                MainActivity.this,
                                getPackageName() + ".provider",
                                shareFile
                        );

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType(mimeType);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        if ("email".equals(packageName)) {
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Compressed Image from CompressoIQ");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is my compressed image ✨");
                            startActivity(Intent.createChooser(shareIntent, "Send Email"));
                        } else if (packageName != null) {
                            shareIntent.setPackage(packageName);
                            try {
                                startActivity(shareIntent);
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this, "App not installed", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            startActivity(Intent.createChooser(shareIntent, "Share compressed image via"));
                        }

                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Share failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Share failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSafeExtension(String outputFormat) {
        if (outputFormat == null) return "jpg";

        String format = outputFormat.toLowerCase().trim();

        switch (format) {
            case "png":
                return "png";
            case "jpg":
            case "jpeg":
            default:
                return "jpg";
        }
    }

    private String getMimeType(String extension) {
        switch (extension.toLowerCase()) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
            default:
                return "image/jpeg";
        }
    }

    private String getFileName(Uri uri) {
        String result = "selected_image";
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        return result;
    }
}