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

    private FrameLayout navHome, navEvent, navChat, navCommittee, navAdmin, navProfile;
    private LinearLayout navHomeInner, navEventInner, navChatInner, navCommitteeInner, navAdminInner, navProfileInner;
    private ImageView iconHome, iconEvent, iconChat, iconCommittee, iconAdmin, iconProfile;
    private View indicatorHome, indicatorEvent, indicatorChat, indicatorCommittee, indicatorAdmin, indicatorProfile;

    private OnTabSelectedListener listener;
    private int selected = -1;
    private boolean adminVisible = false; // whether admin tab is currently visible

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
        navAdmin = findViewById(R.id.navAdmin);
        navProfile = findViewById(R.id.navProfile);

        navHomeInner = findViewById(R.id.navHomeInner);
        navEventInner = findViewById(R.id.navEventInner);
        navChatInner = findViewById(R.id.navChatInner);
        navCommitteeInner = findViewById(R.id.navCommitteeInner);
        navAdminInner = findViewById(R.id.navAdminInner);
        navProfileInner = findViewById(R.id.navProfileInner);

        iconHome = findViewById(R.id.iconHome);
        iconEvent = findViewById(R.id.iconEvent);
        iconChat = findViewById(R.id.iconChat);
        iconCommittee = findViewById(R.id.iconCommittee);
        iconAdmin = findViewById(R.id.iconAdmin);
        iconProfile = findViewById(R.id.iconProfile);

        indicatorHome = findViewById(R.id.indicatorHome);
        indicatorEvent = findViewById(R.id.indicatorEvent);
        indicatorChat = findViewById(R.id.indicatorChat);
        indicatorCommittee = findViewById(R.id.indicatorCommittee);
        indicatorAdmin = findViewById(R.id.indicatorAdmin);
        indicatorProfile = findViewById(R.id.indicatorProfile);

        // Setup clicks
        navHome.setOnClickListener(v -> onItemClicked(0));
        navEvent.setOnClickListener(v -> onItemClicked(1));
        navChat.setOnClickListener(v -> onItemClicked(2));
        navCommittee.setOnClickListener(v -> onItemClicked(3));
        navAdmin.setOnClickListener(v -> onItemClicked(adminVisible ? 4 : -1));
        navProfile.setOnClickListener(v -> {
            // if admin is visible, profile index becomes 5
            onItemClicked(adminVisible ? 5 : 4);
        });

        // accessibility initializations
        iconHome.setContentDescription(context.getString(R.string.nav_home));
        iconEvent.setContentDescription(context.getString(R.string.nav_events));
        iconChat.setContentDescription(context.getString(R.string.nav_chat));
        iconCommittee.setContentDescription(context.getString(R.string.nav_committee));
        // Use admin_console as fallback for admin tab content description
        if (iconAdmin != null) iconAdmin.setContentDescription(context.getString(R.string.admin_console));
        iconProfile.setContentDescription(context.getString(R.string.nav_profile));

        // ensure indicators start hidden
        indicatorHome.setVisibility(View.GONE);
        indicatorEvent.setVisibility(View.GONE);
        indicatorChat.setVisibility(View.GONE);
        indicatorCommittee.setVisibility(View.GONE);
        if (indicatorAdmin != null) indicatorAdmin.setVisibility(View.GONE);
        indicatorProfile.setVisibility(View.GONE);

        // Admin tab hidden by default (already set in layout), track state
        adminVisible = (navAdmin != null && navAdmin.getVisibility() == View.VISIBLE);

        // Respect window insets (bottom gesture bar) so nav is positioned above it
        setOnApplyWindowInsetsListener((v, insets) -> {
            int bottom = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            } else {
                bottom = insets.getSystemWindowInsetBottom();
            }
            // add extra 12dp of margin above gesture bar
            int extra = (int) (12 * getResources().getDisplayMetrics().density);
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), bottom + extra);
            return insets;
        });
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * Public API: select a tab by logical index.
     * Logical indices (when admin hidden): 0..4 -> home,event,chat,committee,profile
     * When admin visible: 0..5 -> home,event,chat,committee,admin,profile
     */
    public void selectTab(int index) {
        onItemClicked(index);
    }

    private void onItemClicked(int index) {
        // ignore invalid index
        if (index < 0) return;

        // Avoid re-selecting same
        if (index == selected) return;
        selected = index;

        resetAll();

        // Haptic feedback via system (no explicit vibrate permission required)
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        LinearLayout clickedInner = null;
        ImageView clickedIcon = null;
        View clickedIndicator = null;

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
                if (adminVisible) { // admin
                    clickedInner = navAdminInner; clickedIcon = iconAdmin; clickedIndicator = indicatorAdmin; break;
                } else { // profile in non-admin mode
                    clickedInner = navProfileInner; clickedIcon = iconProfile; clickedIndicator = indicatorProfile; break;
                }
            case 5:
                // profile when admin visible
                clickedInner = navProfileInner; clickedIcon = iconProfile; clickedIndicator = indicatorProfile; break;
            default:
                // unknown index
                return;
        }

        // set selected state
        if (clickedInner != null) clickedInner.setSelected(true);

        // color tints
        int active = ContextCompat.getColor(getContext(), R.color.nav_icon_active);
        if (clickedIcon != null) clickedIcon.setImageTintList(android.content.res.ColorStateList.valueOf(active));

        // start animated drawable if available
        if (clickedIcon != null) {
            Drawable d = clickedIcon.getDrawable();
            if (d instanceof Animatable) {
                try { ((Animatable) d).start(); } catch (Exception ignored) {}
            }

            // spring pulse animation for the icon
            startSpringPulse(clickedIcon);
        }

        // show indicator with small animation
        if (clickedIndicator != null) showIndicator(clickedIndicator);

        // announce for accessibility
        if (getContext() != null && clickedIcon != null) {
            String announce = clickedIcon.getContentDescription() != null ? clickedIcon.getContentDescription().toString() : "";
            announceForAccessibility(announce + " selected");
        }

        // call listener (map internal indices back to logical indices for listener)
        if (listener != null) {
            listener.onTabSelected(index);
        }
    }

    private void resetAll() {
        // reset inner states
        if (navHomeInner != null) navHomeInner.setSelected(false);
        if (navEventInner != null) navEventInner.setSelected(false);
        if (navChatInner != null) navChatInner.setSelected(false);
        if (navCommitteeInner != null) navCommitteeInner.setSelected(false);
        if (navAdminInner != null) navAdminInner.setSelected(false);
        if (navProfileInner != null) navProfileInner.setSelected(false);

        // stop animatable drawables and tint to inactive
        int inactive = ContextCompat.getColor(getContext(), R.color.nav_icon_inactive);
        stopAnimAndTint(iconHome, inactive);
        stopAnimAndTint(iconEvent, inactive);
        stopAnimAndTint(iconChat, inactive);
        stopAnimAndTint(iconCommittee, inactive);
        stopAnimAndTint(iconAdmin, inactive);
        stopAnimAndTint(iconProfile, inactive);

        // hide indicators
        hideIndicator(indicatorHome);
        hideIndicator(indicatorEvent);
        hideIndicator(indicatorChat);
        hideIndicator(indicatorCommittee);
        if (indicatorAdmin != null) hideIndicator(indicatorAdmin);
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
        if (v == null) return;
        v.setAlpha(0f);
        v.setScaleX(0.7f);
        v.setVisibility(View.VISIBLE);
        v.animate().alpha(1f).scaleX(1f).setDuration(200).start();
    }

    private void hideIndicator(View v) {
        if (v == null) return;
        v.animate().alpha(0f).scaleX(0.7f).setDuration(140).withEndAction(() -> v.setVisibility(View.GONE)).start();
    }

    // Public helper to show/hide admin tab depending on user role.
    public void setAdminVisible(boolean visible) {
        if (navAdmin == null) return;
        adminVisible = visible;
        navAdmin.setVisibility(visible ? View.VISIBLE : View.GONE);
        // If admin tab is shown and we currently have profile selected (index 4), shift to profile index 5
        if (!visible && selected == 4) {
            // do nothing special; in non-admin mode index 4 maps to profile
        }
        // If admin becomes visible and profile was selected (index 4), remap selection to profile's new index
        if (visible && selected == 4) {
            // profile is now index 5, update selected and visuals
            selected = -1; // force reselect
            selectTab(5);
        }
    }

    public boolean isAdminVisible() { return adminVisible; }
}
