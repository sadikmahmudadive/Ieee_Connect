package com.example.ieeeconnect.ui.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ieeeconnect.R;

public class CustomBottomNavView extends LinearLayout {

    private static final int ANIMATION_DURATION = 300;
    private ImageView selectedIcon;
    private int currentSelectedPosition = 0;

    private NavItem[] navItems;
    private NavItem adminNavItem;
    private boolean adminVisible = false;
    private OnNavigationItemSelectedListener listener;

    public CustomBottomNavView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CustomBottomNavView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomBottomNavView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setBackgroundResource(R.drawable.custom_bottom_nav_background);

        // Add elevation and shadow
        setElevation(16f);

        // Add padding
        int paddingVertical = (int) (context.getResources().getDisplayMetrics().density * 12);
        int paddingHorizontal = (int) (context.getResources().getDisplayMetrics().density * 8);
        setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        // Inflate layout defined in res/layout/custom_bottom_nav.xml and wire it up
        LayoutInflater.from(context).inflate(R.layout.custom_bottom_nav, this, true);

        // Initialize navigation items
        initNavItems(context);
    }

    private void initNavItems(Context context) {
        // Tabs wired to layout IDs: navHome, navEvent, navChat, navCommittee, navProfile and optional navAdmin
        navItems = new NavItem[5];

        // resource ids for the base icons (match the drawables used in layout)
        int[] iconResIds = {
                R.drawable.ic_home,
                R.drawable.ic_events,
                R.drawable.ic_chat,
                R.drawable.ic_committee,
                R.drawable.ic_profile
        };

        int[] ids = {R.id.navigation_home, R.id.navigation_events, R.id.navigation_chat,
                R.id.navigation_committee, R.id.navigation_profile};

        // find and wrap views from inflated layout
        FrameLayout homeFrame = findViewById(R.id.navHome);
        ImageView homeIcon = findViewById(R.id.iconHome);
        View homeIndicator = findViewById(R.id.indicatorHome);
        navItems[0] = new NavItem(homeFrame, homeIcon, homeIndicator, iconResIds[0], ids[0]);

        FrameLayout eventFrame = findViewById(R.id.navEvent);
        ImageView eventIcon = findViewById(R.id.iconEvent);
        View eventIndicator = findViewById(R.id.indicatorEvent);
        navItems[1] = new NavItem(eventFrame, eventIcon, eventIndicator, iconResIds[1], ids[1]);

        FrameLayout chatFrame = findViewById(R.id.navChat);
        ImageView chatIcon = findViewById(R.id.iconChat);
        View chatIndicator = findViewById(R.id.indicatorChat);
        navItems[2] = new NavItem(chatFrame, chatIcon, chatIndicator, iconResIds[2], ids[2]);

        FrameLayout committeeFrame = findViewById(R.id.navCommittee);
        ImageView committeeIcon = findViewById(R.id.iconCommittee);
        View committeeIndicator = findViewById(R.id.indicatorCommittee);
        navItems[3] = new NavItem(committeeFrame, committeeIcon, committeeIndicator, iconResIds[3], ids[3]);

        FrameLayout profileFrame = findViewById(R.id.navProfile);
        ImageView profileIcon = findViewById(R.id.iconProfile);
        View profileIndicator = findViewById(R.id.indicatorProfile);
        navItems[4] = new NavItem(profileFrame, profileIcon, profileIndicator, iconResIds[4], ids[4]);

        // Admin (may be hidden)
        FrameLayout adminFrame = findViewById(R.id.navAdmin);
        if (adminFrame != null) {
            ImageView adminIcon = findViewById(R.id.iconAdmin);
            View adminIndicator = findViewById(R.id.indicatorAdmin);
            adminNavItem = new NavItem(adminFrame, adminIcon, adminIndicator, R.drawable.ic_admin, R.id.navigation_admin);
            adminNavItem.setVisibility(GONE);
        }

        // Attach listeners for clicks
        for (int i = 0; i < navItems.length; i++) {
            final int position = i;
            NavItem n = navItems[i];
            if (n != null && n.container != null) {
                n.container.setOnClickListener(v -> selectItem(position));
            }
        }
        if (adminNavItem != null && adminNavItem.container != null) {
            adminNavItem.container.setOnClickListener(v -> selectAdmin());
        }

        // Select first item by default
        selectItem(0);
    }

    /**
     * Programmatically select a tab by position (0..navItems-1). If admin is visible, its position is navItems.length.
     */
    public void selectTab(int position) {
        if (adminVisible && position == navItems.length) {
            selectAdmin();
            return;
        }
        if (position < 0 || position >= navItems.length) return;
        selectItem(position);
    }

    /**
     * Show/hide the Admin tab.
     */
    public void setAdminVisible(boolean visible) {
        adminVisible = visible;
        if (adminNavItem != null) {
            adminNavItem.setVisibility(visible ? VISIBLE : GONE);
        }
        // If admin is hidden while it was selected, go back to home.
        if (!visible && currentSelectedPosition < 0) {
            selectItem(0);
        }
    }

    private void selectAdmin() {
        if (!adminVisible || adminNavItem == null) return;

        // Deselect previous fixed item (if any)
        if (currentSelectedPosition >= 0 && currentSelectedPosition < navItems.length) {
            animateDeselect(navItems[currentSelectedPosition]);
        }

        // Mark as "admin selected" using a sentinel index
        currentSelectedPosition = -1;
        animateSelect(adminNavItem);

        if (listener != null) {
            listener.onNavigationItemSelected(adminNavItem.id);
        }
    }

    private void selectItem(int position) {
        if (position == currentSelectedPosition) {
            // If clicking the same item, add a bounce animation
            animateBounce(navItems[position].iconView);
            return;
        }

        // Deselect previous item
        if (currentSelectedPosition >= 0 && currentSelectedPosition < navItems.length) {
            animateDeselect(navItems[currentSelectedPosition]);
        }

        // Select new item
        currentSelectedPosition = position;
        animateSelect(navItems[position]);

        // Notify listener
        if (listener != null) {
            listener.onNavigationItemSelected(navItems[position].id);
        }
    }

    private void animateSelect(NavItem navItem) {
        if (navItem == null) return;
        // Show indicator
        if (navItem.indicator != null) navItem.indicator.setVisibility(View.VISIBLE);

        // Set filled icon if available
        if (navItem.filledIconResId != 0) {
            navItem.iconView.setImageResource(navItem.filledIconResId);
        }
        // Scale up animation
        AnimatorSet scaleSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(navItem.iconView, "scaleX", 1.0f, 1.3f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(navItem.iconView, "scaleY", 1.0f, 1.3f);
        scaleSet.playTogether(scaleX, scaleY);
        scaleSet.setDuration(ANIMATION_DURATION);
        scaleSet.setInterpolator(new OvershootInterpolator());

        // Alpha animation
        ObjectAnimator alpha = ObjectAnimator.ofFloat(navItem.iconView, "alpha", 0.6f, 1.0f);
        alpha.setDuration(ANIMATION_DURATION);

        // Translation animation (slight bounce up)
        ObjectAnimator translationY = ObjectAnimator.ofFloat(navItem.iconView, "translationY", 0f, -8f, 0f);
        translationY.setDuration(ANIMATION_DURATION);
        translationY.setInterpolator(new OvershootInterpolator());

        // Color change animation (when filled we want colored icon)
        int colorTo = ContextCompat.getColor(getContext(), R.color.onPrimary);
        ValueAnimator colorAnimation = ValueAnimator.ofArgb(Color.TRANSPARENT, colorTo);
        colorAnimation.setDuration(ANIMATION_DURATION);
        colorAnimation.addUpdateListener(animator -> {
            try { navItem.iconView.setColorFilter((int) animator.getAnimatedValue()); } catch (Exception ignored){}
        });

        // Background animation (we rely on indicator + filled icon; optionally tint background)
        if (navItem.container != null) navItem.container.setBackgroundResource(R.drawable.nav_item_selected_background);
        ObjectAnimator backgroundAlpha = ObjectAnimator.ofFloat(navItem.container, "alpha", 0.7f, 1.0f);
        backgroundAlpha.setDuration(ANIMATION_DURATION);

        // Start all animations
        AnimatorSet finalSet = new AnimatorSet();
        finalSet.playTogether(scaleSet, alpha, translationY, colorAnimation, backgroundAlpha);
        finalSet.start();
    }

    private void animateDeselect(NavItem navItem) {
        if (navItem == null) return;
        // Hide indicator
        if (navItem.indicator != null) navItem.indicator.setVisibility(View.GONE);

        // Restore original icon resource if filled was used
        if (navItem.iconResId != 0) {
            navItem.iconView.setImageResource(navItem.iconResId);
        }

        // Scale down animation
        AnimatorSet scaleSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(navItem.iconView, "scaleX", 1.3f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(navItem.iconView, "scaleY", 1.3f, 1.0f);
        scaleSet.playTogether(scaleX, scaleY);
        scaleSet.setDuration(200);
        scaleSet.setInterpolator(new AccelerateDecelerateInterpolator());

        // Alpha animation
        ObjectAnimator alpha = ObjectAnimator.ofFloat(navItem.iconView, "alpha", 1.0f, 0.6f);
        alpha.setDuration(200);

        // Color change animation
        int colorFrom = ContextCompat.getColor(getContext(), R.color.nav_item_active);
        int colorTo = ContextCompat.getColor(getContext(), R.color.nav_item_inactive);
        ValueAnimator colorAnimation = ValueAnimator.ofArgb(colorFrom, colorTo);
        colorAnimation.setDuration(200);
        colorAnimation.addUpdateListener(animator -> {
            try { navItem.iconView.setColorFilter((int) animator.getAnimatedValue()); } catch (Exception ignored){}
        });

        // Remove background
        navItem.container.setBackground(null);

        // Start all animations
        AnimatorSet finalSet = new AnimatorSet();
        finalSet.playTogether(scaleSet, alpha, colorAnimation);
        finalSet.start();
    }

    private void animateBounce(ImageView iconView) {
        AnimatorSet bounceSet = new AnimatorSet();

        ObjectAnimator bounce1 = ObjectAnimator.ofFloat(iconView, "translationY", 0f, -12f);
        bounce1.setDuration(150);
        bounce1.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator bounce2 = ObjectAnimator.ofFloat(iconView, "translationY", -12f, 0f);
        bounce2.setDuration(150);
        bounce2.setInterpolator(new OvershootInterpolator());

        bounceSet.playSequentially(bounce1, bounce2);
        bounceSet.start();
    }

    public void setSelectedItem(int menuItemId) {
        // Admin selection support
        if (adminNavItem != null && adminNavItem.id == menuItemId) {
            setAdminVisible(true);
            selectAdmin();
            return;
        }

        for (int i = 0; i < navItems.length; i++) {
            if (navItems[i].id == menuItemId) {
                selectItem(i);
                break;
            }
        }
    }

    public void setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener listener) {
        this.listener = listener;
    }

    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(int itemId);
    }

    // Inner class for navigation items (wraps existing layout views)
    private static class NavItem {
        FrameLayout container;
        ImageView iconView;
        View indicator;
        int id;
        int iconResId; // original
        int filledIconResId; // optional filled variant

        public NavItem(FrameLayout container, ImageView iconView, View indicator, int iconResId, int id) {
            this.container = container;
            this.iconView = iconView;
            this.indicator = indicator;
            this.id = id;
            this.iconResId = iconResId;

            // ensure base icon is set
            try { this.iconView.setImageResource(iconResId); } catch (Exception ignored) {}

            // find filled variant if present (resource name + _filled)
            try {
                Context ctx = iconView.getContext();
                String entryName = ctx.getResources().getResourceEntryName(iconResId);
                int filledId = ctx.getResources().getIdentifier(entryName + "_filled", "drawable", ctx.getPackageName());
                if (filledId != 0) filledIconResId = filledId; else filledIconResId = 0;
            } catch (Exception ignored) { filledIconResId = 0; }

            // set initial tint/alpha
            try {
                iconView.setColorFilter(ContextCompat.getColor(iconView.getContext(), R.color.nav_item_inactive));
                iconView.setAlpha(0.6f);
            } catch (Exception ignored) {}

            // clickable ripple is on container (defined in layout as foreground)
        }

        public void setVisibility(int v) {
            if (container != null) container.setVisibility(v);
        }
    }
}
