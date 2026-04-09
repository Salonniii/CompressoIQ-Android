package com.example.aiphotocompressor;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoDocuments;
    private ImageButton btnBack;

    private ApiService apiService;
    private SessionManager sessionManager;
    private DocumentHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_history);

        apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(this);

        initViews();
        loadDocumentHistory();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerDocumentHistory);
        progressBar = findViewById(R.id.progressBarDocumentHistory);
        tvNoDocuments = findViewById(R.id.tvNoDocuments);
        btnBack = findViewById(R.id.btnBackDocumentHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadDocumentHistory() {
        Long userId = sessionManager.getUserId();

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvNoDocuments.setVisibility(View.GONE);

        apiService.getDocumentHistory(userId).enqueue(new Callback<List<DocumentResponse>>() {
            @Override
            public void onResponse(Call<List<DocumentResponse>> call, Response<List<DocumentResponse>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<DocumentResponse> documentList = response.body();

                    if (documentList.isEmpty()) {
                        tvNoDocuments.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        tvNoDocuments.setVisibility(View.GONE);

                        adapter = new DocumentHistoryAdapter(DocumentHistoryActivity.this, documentList);
                        recyclerView.setAdapter(adapter);
                    }
                } else {
                    Toast.makeText(DocumentHistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<DocumentResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DocumentHistoryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}