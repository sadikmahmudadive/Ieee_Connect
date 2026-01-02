package com.example.ieeeconnect.ui.views;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.example.ieeeconnect.R;

public class CustomBottomNavView extends FrameLayout {

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    private FrameLayout navHome, navEvent, navChat, navCommittee, navProfile;
    private LinearLayout navHomeInner, navEventInner, navChatInner, navCommitteeInner, navProfileInner;
    private ImageView iconHome, iconEvent, iconChat, iconCommittee, iconProfile;
    private View indicatorHome, indicatorEvent, indicatorChat, indicatorCommittee, indicatorProfile;

    private OnTabSelectedListener listener;
    private int selected = -1;

    public CustomBottomNavView(@NonNull Context context) {
        this(context, null);
    }

    public CustomBottomNavView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomBottomNavView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.custom_bottom_nav, this, true);

        navHome = findViewById(R.id.navHome);
        navEvent = findViewById(R.id.navEvent);
        navChat = findViewById(R.id.navChat);
        navCommittee = findViewById(R.id.navCommittee);
        navProfile = findViewById(R.id.navProfile);

        navHomeInner = findViewById(R.id.navHomeInner);
        navEventInner = findViewById(R.id.navEventInner);
        navChatInner = findViewById(R.id.navChatInner);
        navCommitteeInner = findViewById(R.id.navCommitteeInner);
        navProfileInner = findViewById(R.id.navProfileInner);

        iconHome = findViewById(R.id.iconHome);
        iconEvent = findViewById(R.id.iconEvent);
        iconChat = findViewById(R.id.iconChat);
        iconCommittee = findViewById(R.id.iconCommittee);
        iconProfile = findViewById(R.id.iconProfile);

        indicatorHome = findViewById(R.id.indicatorHome);
        indicatorEvent = findViewById(R.id.indicatorEvent);
        indicatorChat = findViewById(R.id.indicatorChat);
        indicatorCommittee = findViewById(R.id.indicatorCommittee);
        indicatorProfile = findViewById(R.id.indicatorProfile);

        // Setup clicks
        navHome.setOnClickListener(v -> onItemClicked(0));
        navEvent.setOnClickListener(v -> onItemClicked(1));
        navChat.setOnClickListener(v -> onItemClicked(2));
        navCommittee.setOnClickListener(v -> onItemClicked(3));
        navProfile.setOnClickListener(v -> onItemClicked(4));

        // accessibility initializations
        iconHome.setContentDescription(context.getString(R.string.nav_home));
        iconEvent.setContentDescription(context.getString(R.string.nav_events));
        iconChat.setContentDescription(context.getString(R.string.nav_chat));
        iconCommittee.setContentDescription(context.getString(R.string.nav_committee));
        iconProfile.setContentDescription(context.getString(R.string.nav_profile));

        // ensure indicators start hidden
        indicatorHome.setVisibility(View.GONE);
        indicatorEvent.setVisibility(View.GONE);
        indicatorChat.setVisibility(View.GONE);
        indicatorCommittee.setVisibility(View.GONE);
        indicatorProfile.setVisibility(View.GONE);

        // Respect window insets (bottom gesture bar) so nav is positioned above it
        setOnApplyWindowInsetsListener((v, insets) -> {
            int bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            // add extra 12dp of margin above gesture bar
            int extra = (int) (12 * getResources().getDisplayMetrics().density);
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), bottom + extra);
            return insets;
        });
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    public void selectTab(int index) {
        onItemClicked(index);
    }

    private void onItemClicked(int index) {
        // Avoid re-selecting same
        if (index == selected) return;
        selected = index;

        resetAll();

        // Haptic feedback via system (no explicit vibrate permission required)
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        LinearLayout clickedInner;
        ImageView clickedIcon;
        View clickedIndicator;

        switch (index) {
            case 0:
                clickedInner = navHomeInner; clickedIcon = iconHome; clickedIndicator = indicatorHome; break;
            case 1:
                clickedInner = navEventInner; clickedIcon = iconEvent; clickedIndicator = indicatorEvent; break;
            case 2:
                clickedInner = navChatInner; clickedIcon = iconChat; clickedIndicator = indicatorChat; break;
            case 3:
                clickedInner = navCommitteeInner; clickedIcon = iconCommittee; clickedIndicator = indicatorCommittee; break;
            case 4:
            default:
                clickedInner = navProfileInner; clickedIcon = iconProfile; clickedIndicator = indicatorProfile; break;
        }

        // set selected state
        if (clickedInner != null) clickedInner.setSelected(true);

        // color tints
        int active = ContextCompat.getColor(getContext(), R.color.nav_icon_active);
        clickedIcon.setImageTintList(android.content.res.ColorStateList.valueOf(active));

        // start animated drawable if available
        Drawable d = clickedIcon.getDrawable();
        if (d instanceof Animatable) {
            try { ((Animatable) d).start(); } catch (Exception ignored) {}
        }

        // spring pulse animation for the icon
        startSpringPulse(clickedIcon);

        // show indicator with small animation
        showIndicator(clickedIndicator);

        // announce for accessibility
        if (getContext() != null) {
            String announce = clickedIcon.getContentDescription() != null ? clickedIcon.getContentDescription().toString() : "";
            announceForAccessibility(announce + " selected");
        }

        // call listener
        if (listener != null) listener.onTabSelected(index);
    }

    private void resetAll() {
        // reset inner states
        navHomeInner.setSelected(false);
        navEventInner.setSelected(false);
        navChatInner.setSelected(false);
        navCommitteeInner.setSelected(false);
        navProfileInner.setSelected(false);

        // stop animatable drawables and tint to inactive
        int inactive = ContextCompat.getColor(getContext(), R.color.nav_icon_inactive);
        stopAnimAndTint(iconHome, inactive);
        stopAnimAndTint(iconEvent, inactive);
        stopAnimAndTint(iconChat, inactive);
        stopAnimAndTint(iconCommittee, inactive);
        stopAnimAndTint(iconProfile, inactive);

        // hide indicators
        hideIndicator(indicatorHome);
        hideIndicator(indicatorEvent);
        hideIndicator(indicatorChat);
        hideIndicator(indicatorCommittee);
        hideIndicator(indicatorProfile);
    }

    private void stopAnimAndTint(ImageView v, int tint) {
        if (v == null) return;
        Drawable d = v.getDrawable();
        if (d instanceof Animatable) {
            try { ((Animatable) d).stop(); } catch (Exception ignored) {}
        }
        try { v.setImageTintList(android.content.res.ColorStateList.valueOf(tint)); } catch (Exception ignored) {}
    }

    private void startSpringPulse(View v) {
        if (v == null) return;
        SpringAnimation sx = new SpringAnimation(v, SpringAnimation.SCALE_X, 1.06f);
        SpringAnimation sy = new SpringAnimation(v, SpringAnimation.SCALE_Y, 1.06f);
        SpringForce sf = new SpringForce(1f);
        sf.setStiffness(SpringForce.STIFFNESS_LOW);
        sf.setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        sx.setSpring(sf);
        sy.setSpring(sf);
        // set initial velocity to create the 'pop'
        sx.setStartValue(1.0f);
        sy.setStartValue(1.0f);
        sx.animateToFinalPosition(1.06f);
        sy.animateToFinalPosition(1.06f);
        // return to 1 using spring
        sx.addEndListener((animation, canceled, value, velocity) -> {
            SpringAnimation backX = new SpringAnimation(v, SpringAnimation.SCALE_X, 1f);
            SpringAnimation backY = new SpringAnimation(v, SpringAnimation.SCALE_Y, 1f);
            SpringForce backForce = new SpringForce(1f);
            backForce.setStiffness(SpringForce.STIFFNESS_MEDIUM);
            backForce.setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY);
            backX.setSpring(backForce); backY.setSpring(backForce);
            backX.animateToFinalPosition(1f); backY.animateToFinalPosition(1f);
        });
    }

    private void showIndicator(View v) {
        v.setAlpha(0f);
        v.setScaleX(0.7f);
        v.setVisibility(View.VISIBLE);
        v.animate().alpha(1f).scaleX(1f).setDuration(200).start();
    }

    private void hideIndicator(View v) {
        v.animate().alpha(0f).scaleX(0.7f).setDuration(140).withEndAction(() -> v.setVisibility(View.GONE)).start();
    }
}
