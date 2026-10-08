package com.example.sinalperdido;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sinalperdido.databinding.ActivityDossierBinding;

import java.util.Locale;

/**
 * Dossiê do caso: apresenta as evidências, controla a gravação interceptada
 * e verifica a solução proposta pelo investigador.
 */
public class DossierActivity extends AppCompatActivity {

    /** Respostas aceitas, já em minúsculas. */
    private static final String[] SOLUTIONS = {"caio ventura", "caio", "faroleiro", "o faroleiro"};
    private static final int MIN_JUSTIFICATION_LENGTH = 20;
    private static final int SEEK_STEP_MS = 10_000;
    private static final long TIME_REFRESH_MS = 250;

    private ActivityDossierBinding binding;
    private MediaPlayer mediaPlayer;
    private final Handler timeHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            updateTimeLabel();
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                timeHandler.postDelayed(this, TIME_REFRESH_MS);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDossierBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Única instância do MediaPlayer, reutilizada por todos os controles.
        mediaPlayer = MediaPlayer.create(this, R.raw.transmissao_interceptada);
        mediaPlayer.setOnCompletionListener(mp -> {
            setAudioStatus(R.string.audio_status_completed);
            updateTimeLabel();
        });
        setAudioStatus(R.string.audio_status_waiting);
        updateTimeLabel();

        binding.buttonPlay.setOnClickListener(v -> playAudio());
        binding.buttonPause.setOnClickListener(v -> pauseAudio());
        binding.buttonRestart.setOnClickListener(v -> restartAudio());
        binding.buttonRewind.setOnClickListener(v -> seekBy(-SEEK_STEP_MS));
        binding.buttonForward.setOnClickListener(v -> seekBy(SEEK_STEP_MS));

        binding.buttonVerify.setOnClickListener(v -> verifySolution());
        binding.buttonBack.setOnClickListener(v -> finish());
    }

    // ---------- Áudio ----------

    private void playAudio() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            setAudioStatus(R.string.audio_status_playing);
            timeHandler.post(timeUpdater);
        }
    }

    private void pauseAudio() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            setAudioStatus(R.string.audio_status_paused);
            updateTimeLabel();
        }
    }

    /** Volta ao início sem stop(): pausa e reposiciona em zero. */
    private void restartAudio() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        mediaPlayer.seekTo(0);
        setAudioStatus(R.string.audio_status_stopped);
        updateTimeLabel();
    }

    /** Desloca a posição respeitando os limites de 0 e da duração total. */
    private void seekBy(int deltaMs) {
        int duration = mediaPlayer.getDuration();
        int target = mediaPlayer.getCurrentPosition() + deltaMs;
        target = Math.max(0, Math.min(target, duration));
        mediaPlayer.seekTo(target);
        updateTimeLabel(target);
    }

    private void setAudioStatus(@StringRes int status) {
        binding.textAudioStatus.setText(status);
    }

    private void updateTimeLabel() {
        updateTimeLabel(mediaPlayer.getCurrentPosition());
    }

    private void updateTimeLabel(int positionMs) {
        binding.textAudioTime.setText(getString(R.string.audio_time,
                formatTime(positionMs), formatTime(mediaPlayer.getDuration())));
    }

    private static String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    // ---------- Solução ----------

    private void verifySolution() {
        String answer = textOf(binding.editAnswer);
        String justification = textOf(binding.editJustification);

        boolean valid = true;
        if (answer.isEmpty()) {
            binding.inputLayoutAnswer.setError(getString(R.string.error_answer_required));
            binding.editAnswer.requestFocus();
            valid = false;
        } else {
            binding.inputLayoutAnswer.setError(null);
        }

        if (justification.length() < MIN_JUSTIFICATION_LENGTH) {
            binding.inputLayoutJustification.setError(
                    getString(R.string.error_justification_short, MIN_JUSTIFICATION_LENGTH));
            if (valid) binding.editJustification.requestFocus();
            valid = false;
        } else {
            binding.inputLayoutJustification.setError(null);
        }

        if (!valid) {
            binding.textResult.setVisibility(View.GONE);
            return;
        }

        binding.textResult.setVisibility(View.VISIBLE);
        if (isCorrect(answer)) {
            binding.textResult.setText(getString(R.string.result_correct)
                    + "\n\n" + getString(R.string.result_epilogue));
            binding.textResult.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else {
            // A resposta certa nunca é revelada: o investigador pode tentar de novo.
            binding.textResult.setText(R.string.result_incorrect);
            binding.textResult.setTextColor(ContextCompat.getColor(this, R.color.error_coral));
        }
    }

    private static boolean isCorrect(String answer) {
        String normalized = answer.trim().toLowerCase(new Locale("pt", "BR"));
        for (String solution : SOLUTIONS) {
            if (solution.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String textOf(EditText field) {
        CharSequence text = field.getText();
        return TextUtils.isEmpty(text) ? "" : text.toString().trim();
    }

    // ---------- Ciclo de vida ----------

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            setAudioStatus(R.string.audio_status_paused);
            updateTimeLabel();
        }
        timeHandler.removeCallbacks(timeUpdater);
    }

    @Override
    protected void onDestroy() {
        timeHandler.removeCallbacks(timeUpdater);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
