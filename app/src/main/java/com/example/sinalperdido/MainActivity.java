package com.example.sinalperdido;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sinalperdido.databinding.ActivityMainBinding;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Central de Investigação: identifica o investigador e libera o acesso ao dossiê.
 */
public class MainActivity extends AppCompatActivity {

    private static final int MIN_CODENAME_LENGTH = 3;
    private static final String ACCESS_CODE = "042";
    private static final int MIN_READINESS = 50;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        updateReadinessLabel(binding.seekReadiness.getProgress());
        binding.seekReadiness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateReadinessLabel(progress);
                if (progress >= MIN_READINESS) {
                    binding.textReadinessError.setVisibility(View.GONE);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        clearErrorWhenEdited(binding.editCodename, binding.inputLayoutCodename);
        clearErrorWhenEdited(binding.editAccessCode, binding.inputLayoutAccessCode);

        binding.buttonClear.setOnClickListener(v -> clearForm());
        binding.buttonAccess.setOnClickListener(v -> accessDossier());
    }

    private void updateReadinessLabel(int progress) {
        binding.textReadinessValue.setText(getString(R.string.readiness_value, progress));
    }

    private void accessDossier() {
        if (validateAccess()) {
            startActivity(new Intent(this, DossierActivity.class));
        }
    }

    /** Mostra os erros de cada campo e posiciona o foco no primeiro inválido. */
    private boolean validateAccess() {
        View firstInvalid = null;

        if (textOf(binding.editCodename).length() < MIN_CODENAME_LENGTH) {
            binding.inputLayoutCodename.setError(
                    getString(R.string.error_codename_short, MIN_CODENAME_LENGTH));
            firstInvalid = binding.editCodename;
        } else {
            binding.inputLayoutCodename.setError(null);
        }

        if (!ACCESS_CODE.equals(textOf(binding.editAccessCode))) {
            binding.inputLayoutAccessCode.setError(getString(R.string.error_access_code));
            if (firstInvalid == null) firstInvalid = binding.editAccessCode;
        } else {
            binding.inputLayoutAccessCode.setError(null);
        }

        if (binding.seekReadiness.getProgress() < MIN_READINESS) {
            binding.textReadinessError.setText(getString(R.string.error_readiness, MIN_READINESS));
            binding.textReadinessError.setVisibility(View.VISIBLE);
            if (firstInvalid == null) firstInvalid = binding.seekReadiness;
        } else {
            binding.textReadinessError.setVisibility(View.GONE);
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocus();
            return false;
        }
        return true;
    }

    private void clearForm() {
        binding.editCodename.setText(null);
        binding.editAccessCode.setText(null);
        binding.seekReadiness.setProgress(0);
        binding.inputLayoutCodename.setError(null);
        binding.inputLayoutAccessCode.setError(null);
        binding.textReadinessError.setVisibility(View.GONE);
        binding.editCodename.requestFocus();
    }

    private void clearErrorWhenEdited(EditText field, TextInputLayout layout) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (layout.getError() != null) {
                    layout.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private static String textOf(EditText field) {
        CharSequence text = field.getText();
        return TextUtils.isEmpty(text) ? "" : text.toString().trim();
    }
}
