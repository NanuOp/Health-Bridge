package com.nanu.healthbridge.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.nanu.healthbridge.MainActivity;
import com.nanu.healthbridge.R;
import com.nanu.healthbridge.db.HealthDatabase;
import com.nanu.healthbridge.db.UserProfile;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private UserProfile profile = new UserProfile();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new OnboardingAdapter(this));
        viewPager.setUserInputEnabled(false); // Disable swiping, use buttons
    }

    private class OnboardingAdapter extends FragmentStateAdapter {
        public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return OnboardingSlideFragment.newInstance(position);
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }

    public static class OnboardingSlideFragment extends Fragment {
        private static final String ARG_POS = "pos";
        private int position;

        public static OnboardingSlideFragment newInstance(int pos) {
            OnboardingSlideFragment fragment = new OnboardingSlideFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_POS, pos);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) position = getArguments().getInt(ARG_POS);
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            int layoutId = R.layout.onboarding_welcome;
            switch (position) {
                case 1: layoutId = R.layout.onboarding_info; break;
                case 2: layoutId = R.layout.onboarding_fitness; break;
                case 3: layoutId = R.layout.onboarding_sport; break;
                case 4: layoutId = R.layout.onboarding_done; break;
            }
            return inflater.inflate(layoutId, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            OnboardingActivity activity = (OnboardingActivity) getActivity();
            if (activity == null) return;

            MaterialButton btnNext = view.findViewById(R.id.btn_next);
            if (btnNext != null) {
                btnNext.setOnClickListener(v -> {
                    if (position == 1) {
                        TextInputEditText etName = view.findViewById(R.id.et_name);
                        NumberPicker npAge = view.findViewById(R.id.np_age);
                        if (etName.getText() == null || etName.getText().toString().isEmpty()) {
                            Toast.makeText(getContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        activity.profile.name = etName.getText().toString();
                        activity.profile.age = npAge.getValue();
                    }
                    activity.viewPager.setCurrentItem(position + 1);
                });
            }

            if (position == 1) {
                NumberPicker npAge = view.findViewById(R.id.np_age);
                npAge.setMinValue(15);
                npAge.setMaxValue(60);
                npAge.setValue(22);
            }

            if (position == 2) {
                setupFitnessSelection(view, activity);
            }

            if (position == 4) {
                TextView tvTitle = view.findViewById(R.id.tv_done_title);
                TextView tvSummary = view.findViewById(R.id.tv_done_summary);
                tvTitle.setText("You're all set, " + activity.profile.name + "! 🎉");
                tvSummary.setText("Your recovery scores are now calibrated\nfor a " + activity.profile.fitnessLevel + " level.");
                
                view.findViewById(R.id.btn_finish).setOnClickListener(v -> {
                    saveAndFinish(activity);
                });
            }
        }

        private void setupFitnessSelection(View view, OnboardingActivity activity) {
            int[] cardIds = {R.id.card_sedentary, R.id.card_active, R.id.card_fit, R.id.card_athlete, R.id.card_elite};
            int[] checkIds = {R.id.check_sedentary, R.id.check_active, R.id.check_fit, R.id.check_athlete, R.id.check_elite};
            String[] levels = {"SEDENTARY", "ACTIVE", "FIT", "ATHLETE", "ELITE"};

            for (int i = 0; i < cardIds.length; i++) {
                int index = i;
                MaterialCardView card = view.findViewById(cardIds[i]);
                card.setOnClickListener(v -> {
                    activity.profile.fitnessLevel = levels[index];
                    for (int j = 0; j < cardIds.length; j++) {
                        MaterialCardView c = view.findViewById(cardIds[j]);
                        ImageView check = view.findViewById(checkIds[j]);
                        if (j == index) {
                            c.setStrokeWidth(4);
                            c.setStrokeColor(getResources().getColor(R.color.accent_green));
                            check.setVisibility(View.VISIBLE);
                        } else {
                            c.setStrokeWidth(0);
                            check.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }

        private void saveAndFinish(OnboardingActivity activity) {
            new Thread(() -> {
                activity.profile.createdAt = System.currentTimeMillis();
                activity.profile.updatedAt = System.currentTimeMillis();
                HealthDatabase.getInstance(activity).healthDao().insertUserProfile(activity.profile);
                
                // Mark onboarding as done
                activity.getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("onboarding_done", true).apply();
                
                activity.startActivity(new Intent(activity, MainActivity.class));
                activity.finish();
            }).start();
        }
    }
}
