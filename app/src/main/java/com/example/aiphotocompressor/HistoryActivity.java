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

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerHistory;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ImageButton btnBackHistory;

    private HistoryAdapter adapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerHistory = findViewById(R.id.recyclerHistory);
        progressBar = findViewById(R.id.progressBarHistory);
        tvEmpty = findViewById(R.id.tvEmptyHistory);
        btnBackHistory = findViewById(R.id.btnBackHistory);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));

        apiService = RetrofitClient.getApiService();

        btnBackHistory.setOnClickListener(v -> finish());

        loadHistory();
    }

    private void loadHistory() {
        SessionManager sessionManager = new SessionManager(this);
        Long userId = sessionManager.getUserId();

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.GONE);

        apiService.getUserHistory(userId).enqueue(new Callback<List<ImageResponse>>() {
            @Override
            public void onResponse(Call<List<ImageResponse>> call, Response<List<ImageResponse>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<ImageResponse> historyList = response.body();

                    if (historyList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerHistory.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerHistory.setVisibility(View.VISIBLE);

                        adapter = new HistoryAdapter(HistoryActivity.this, historyList, () -> {
                            if (adapter.getItemCount() == 0) {
                                tvEmpty.setVisibility(View.VISIBLE);
                                recyclerHistory.setVisibility(View.GONE);
                            }
                        });

                        recyclerHistory.setAdapter(adapter);
                    }
                } else {
                    Toast.makeText(HistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerHistory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<ImageResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerHistory.setVisibility(View.GONE);
                Toast.makeText(HistoryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}