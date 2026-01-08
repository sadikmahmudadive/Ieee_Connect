package com.example.ieeeconnect.ui.views;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ieeeconnect.R;

/**
 * Simple custom bottom navigation controller that inflates `custom_bottom_nav.xml` and
 * exposes a small API used by `DashboardActivity` to show/hide admin tab and listen for
 * selections. When a tab is selected it will:
 *  - show glow view
 *  - swap icon to a filled drawable if available (ic_<name>_filled)
 *  - increase icon size to selected size
 *  - tint active/inactive icons
 */
public class CustomBottomNavView extends FrameLayout {

    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(@IdRes int itemId);
    }

    private OnNavigationItemSelectedListener listener;

    // container slots
    private View navHome, navEvent, navChat, navCommittee, navAdmin, navProfile;
    // inner image views
    private ImageView iconHome, iconEvent, iconChat, iconCommittee, iconAdmin, iconProfile;
    private ImageView glowHome, glowEvent, glowChat, glowCommittee, glowAdmin, glowProfile;

    // ids mapping to R.id.* navigation ids used by DashboardActivity
    public static final int IDX_HOME = 0;
    public static final int IDX_EVENTS = 1;
    public static final int IDX_CHAT = 2;
    public static final int IDX_COMMITTEE = 3;
    public static final int IDX_ADMIN = 4;
    public static final int IDX_PROFILE = 5;

    private int selectedIndex = -1;

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

    private void init(Context ctx) {
        LayoutInflater.from(ctx).inflate(R.layout.custom_bottom_nav, this, true);

        navHome = findViewById(R.id.navHome);
        navEvent = findViewById(R.id.navEvent);
        navChat = findViewById(R.id.navChat);
        navCommittee = findViewById(R.id.navCommittee);
        navAdmin = findViewById(R.id.navAdmin);
        navProfile = findViewById(R.id.navProfile);

        iconHome = findViewById(R.id.iconHome);
        iconEvent = findViewById(R.id.iconEvent);
        iconChat = findViewById(R.id.iconChat);
        iconCommittee = findViewById(R.id.iconCommittee);
        iconAdmin = findViewById(R.id.iconAdmin);
        iconProfile = findViewById(R.id.iconProfile);

        glowHome = findViewById(R.id.glowHome);
        glowEvent = findViewById(R.id.glowEvent);
        glowChat = findViewById(R.id.glowChat);
        glowCommittee = findViewById(R.id.glowCommittee);
        glowAdmin = findViewById(R.id.glowAdmin);
        glowProfile = findViewById(R.id.glowProfile);

        // attach listeners
        navHome.setOnClickListener(v -> onTabClicked(IDX_HOME));
        navEvent.setOnClickListener(v -> onTabClicked(IDX_EVENTS));
        navChat.setOnClickListener(v -> onTabClicked(IDX_CHAT));
        navCommittee.setOnClickListener(v -> onTabClicked(IDX_COMMITTEE));
        navAdmin.setOnClickListener(v -> onTabClicked(IDX_ADMIN));
        navProfile.setOnClickListener(v -> onTabClicked(IDX_PROFILE));

        // default visuals
        updateVisuals(-1);
    }

    private void onTabClicked(int idx) {
        selectTab(idx);
        if (listener != null) {
            int id = mapIdxToId(idx);
            listener.onNavigationItemSelected(id);
        }
    }

    public void setOnNavigationItemSelectedListener(OnNavigationItemSelectedListener l) {
        this.listener = l;
    }

    public void setAdminVisible(boolean visible) {
        if (navAdmin != null) navAdmin.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void selectTab(int idx) {
        if (idx == selectedIndex) return;
        selectedIndex = idx;
        updateVisuals(idx);
    }

    private void updateVisuals(int idx) {
        // Reset all
        resetTab(iconHome, glowHome);
        resetTab(iconEvent, glowEvent);
        resetTab(iconChat, glowChat);
        resetTab(iconCommittee, glowCommittee);
        resetTab(iconAdmin, glowAdmin);
        resetTab(iconProfile, glowProfile);

        // Apply selected
        switch (idx) {
            case IDX_HOME:
                applySelected(iconHome, glowHome, "ic_home");
                break;
            case IDX_EVENTS:
                applySelected(iconEvent, glowEvent, "ic_events");
                break;
            case IDX_CHAT:
                applySelected(iconChat, glowChat, "ic_chat");
                break;
            case IDX_COMMITTEE:
                applySelected(iconCommittee, glowCommittee, "ic_committee");
                break;
            case IDX_ADMIN:
                applySelected(iconAdmin, glowAdmin, "ic_admin");
                break;
            case IDX_PROFILE:
                applySelected(iconProfile, glowProfile, "ic_profile");
                break;
            default:
                // none selected
                break;
        }
    }

    private void resetTab(ImageView icon, ImageView glow) {
        if (icon == null) return;
        // shrink icon to 20dp and set inactive tint and unfilled drawable if available
        int size = dpToPx(20);
        icon.getLayoutParams().width = size;
        icon.getLayoutParams().height = size;
        icon.requestLayout();
        icon.setImageResource(getDrawableIdByName(getBaseNameForIcon(icon), false));
        icon.setColorFilter(ContextCompat.getColor(getContext(), R.color.nav_icon_inactive));
        if (glow != null) glow.setVisibility(View.GONE);
    }

    private void applySelected(ImageView icon, ImageView glow, String baseName) {
        if (icon == null) return;
        // enlarge icon to 26dp
        int size = dpToPx(26);
        icon.getLayoutParams().width = size;
        icon.getLayoutParams().height = size;
        icon.requestLayout();

        // try to set filled drawable (ic_<base>_filled) if exists else fallback to base
        int filledRes = getDrawableIdByName(baseName + "_filled", true);
        if (filledRes != 0) {
            icon.setImageResource(filledRes);
        } else {
            icon.setImageResource(getDrawableIdByName(baseName, true));
        }

        // active tint
        icon.setColorFilter(ContextCompat.getColor(getContext(), R.color.nav_icon_active));

        // show glow if available
        if (glow != null) glow.setVisibility(View.VISIBLE);
    }

    private String getBaseNameForIcon(ImageView icon) {
        int id = icon.getId();
        if (id == R.id.iconHome) return "ic_home";
        if (id == R.id.iconEvent) return "ic_events";
        if (id == R.id.iconChat) return "ic_chat";
        if (id == R.id.iconCommittee) return "ic_committee";
        if (id == R.id.iconAdmin) return "ic_admin";
        if (id == R.id.iconProfile) return "ic_profile";
        return "ic_placeholder";
    }

    private int getDrawableIdByName(String name, boolean allowFallback) {
        if (name == null) return 0;
        Resources res = getResources();
        int id = res.getIdentifier(name, "drawable", getContext().getPackageName());
        if (id == 0 && allowFallback) {
            // try alternative common names without pluralization
            if (name.endsWith("s")) {
                String alt = name.substring(0, name.length()-1);
                id = res.getIdentifier(alt, "drawable", getContext().getPackageName());
            }
        }
        return id;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int mapIdxToId(int idx) {
        switch (idx) {
            case IDX_HOME: return R.id.navigation_home;
            case IDX_EVENTS: return R.id.navigation_events;
            case IDX_CHAT: return R.id.navigation_chat;
            case IDX_COMMITTEE: return R.id.navigation_committee;
            case IDX_ADMIN: return R.id.navigation_admin;
            case IDX_PROFILE: return R.id.navigation_profile;
            default: return R.id.navigation_home;
        }
    }

    // Convenience: select by menu id used in DashboardActivity
    public void selectByMenuId(@IdRes int menuId) {
        if (menuId == R.id.navigation_home) selectTab(IDX_HOME);
        else if (menuId == R.id.navigation_events) selectTab(IDX_EVENTS);
        else if (menuId == R.id.navigation_chat) selectTab(IDX_CHAT);
        else if (menuId == R.id.navigation_committee) selectTab(IDX_COMMITTEE);
        else if (menuId == R.id.navigation_admin) selectTab(IDX_ADMIN);
        else if (menuId == R.id.navigation_profile) selectTab(IDX_PROFILE);
    }
}
