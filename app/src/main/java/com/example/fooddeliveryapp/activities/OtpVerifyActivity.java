package com.example.fooddeliveryapp.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fooddeliveryapp.R;
import com.example.fooddeliveryapp.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * OtpVerifyActivity — Professional 4-box OTP entry screen.
 *
 * Features:
 *  • 4 individual single-digit EditText boxes
 *  • Auto-focus advance and backspace-navigate between boxes
 *  • On verify:
 *    – Correct OTP → borders turn green one-by-one (50ms stagger) → RESULT_OK
 *    – Wrong OTP   → all borders turn red simultaneously → shake animation → error message
 *  • Redesign from scratch to be closer to a real production OTP screen
 *    (dark background, orange brand, card-based layout)
 *
 * Usage:
 *   Intent intent = new Intent(this, OtpVerifyActivity.class);
 *   intent.putExtra("orderId", orderId);
 *   startActivityForResult(intent, 101);
 *
 *   onActivityResult → if resultCode == RESULT_OK → OTP was correct
 */
public class OtpVerifyActivity extends AppCompatActivity {

    private EditText[] otpBoxes = new EditText[4];
    private String     orderId;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verify);

        dbRef   = FirebaseDatabase.getInstance(Constants.FIREBASE_URL).getReference();
        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null || orderId.isEmpty()) { finish(); return; }

        otpBoxes[0] = findViewById(R.id.etOtp1);
        otpBoxes[1] = findViewById(R.id.etOtp2);
        otpBoxes[2] = findViewById(R.id.etOtp3);
        otpBoxes[3] = findViewById(R.id.etOtp4);

        setupOtpBoxes();

        MaterialButton btnVerify = findViewById(R.id.btnVerifyOtp);
        if (btnVerify != null)
            btnVerify.setOnClickListener(v -> verifyOtp());

        TextView tvCancel = findViewById(R.id.tvCancelOtp);
        if (tvCancel != null)
            tvCancel.setOnClickListener(v -> finish());
    }

    // ── Wire up the 4 boxes ───────────────────────────────────────────────────

    private void setupOtpBoxes() {
        for (int i = 0; i < 4; i++) {
            if (otpBoxes[i] == null) continue;
            otpBoxes[i].setFilters(new InputFilter[]{ new InputFilter.LengthFilter(1) });
            otpBoxes[i].setInputType(InputType.TYPE_CLASS_NUMBER);
            final int index = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    resetBoxBorders(); // clear any error/success colour on new input
                    if (s.length() == 1 && index < 3)
                        otpBoxes[index + 1].requestFocus();
                }
            });

            // Backspace → go back to previous box
            otpBoxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL
                        && event.getAction() == android.view.KeyEvent.ACTION_DOWN
                        && otpBoxes[index].getText().toString().isEmpty()
                        && index > 0) {
                    otpBoxes[index - 1].requestFocus();
                    otpBoxes[index - 1].setText("");
                }
                return false;
            });
        }
        // Focus first box
        if (otpBoxes[0] != null) otpBoxes[0].requestFocus();
    }

    // ── Verify logic ──────────────────────────────────────────────────────────

    private void verifyOtp() {
        StringBuilder entered = new StringBuilder();
        for (EditText box : otpBoxes) {
            if (box == null || box.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please enter all 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            entered.append(box.getText().toString());
        }

        String enteredOtp = entered.toString();

        dbRef.child(Constants.NODE_ORDERS).child(orderId).child("deliveryOtp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String storedOtp = snapshot.getValue(String.class);

                        if (storedOtp == null || storedOtp.isEmpty()) {
                            Toast.makeText(OtpVerifyActivity.this,
                                    "OTP not found. Try generating again.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (enteredOtp.equals(storedOtp)) {
                            animateSuccess();
                        } else {
                            animateFailure();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(OtpVerifyActivity.this,
                                "Network error. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── Animations ────────────────────────────────────────────────────────────

    /** Green border staggered one-by-one → then return RESULT_OK after short delay */
    private void animateSuccess() {
        for (int i = 0; i < otpBoxes.length; i++) {
            final int idx = i;
            otpBoxes[i].postDelayed(() -> {
                if (otpBoxes[idx] != null)
                    otpBoxes[idx].setBackgroundResource(R.drawable.bg_otp_box_success);
            }, i * 80L);
        }

        // After all boxes are green, wait then finish with OK
        otpBoxes[3].postDelayed(() -> {
            // Mark OTP verified in Firebase
            dbRef.child(Constants.NODE_ORDERS).child(orderId).child("otpVerified").setValue(true);
            Toast.makeText(this, "OTP Verified! ✅", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }, 500);
    }

    /** Red border on all boxes simultaneously → shake → error toast */
    private void animateFailure() {
        for (EditText box : otpBoxes) {
            if (box != null)
                box.setBackgroundResource(R.drawable.bg_otp_box_error);
        }

        // Shake all boxes
        for (EditText box : otpBoxes) {
            if (box != null) {
                android.view.animation.Animation shake =
                        android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
                box.startAnimation(shake);
            }
        }

        Toast.makeText(this, "❌ Wrong OTP — please try again", Toast.LENGTH_SHORT).show();

        // Clear boxes after a short delay so user can re-enter
        otpBoxes[0].postDelayed(() -> {
            for (EditText box : otpBoxes) {
                if (box != null) box.setText("");
            }
            resetBoxBorders();
            if (otpBoxes[0] != null) otpBoxes[0].requestFocus();
        }, 1000);
    }

    /** Reset all boxes to default (idle) style */
    private void resetBoxBorders() {
        for (EditText box : otpBoxes)
            if (box != null)
                box.setBackgroundResource(R.drawable.bg_otp_box_default);
    }
}
