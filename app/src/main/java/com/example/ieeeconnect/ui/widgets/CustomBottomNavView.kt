package com.example.ieeeconnect.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.ieeeconnect.R

/**
 * Small helper view that controls the custom bottom nav icons behaviour:
 * - swaps to filled icon when selected
 * - scales selected icon to 26dp and others to 20dp
 * - toggles glow drawable
 * - exposes a listener for selection changes
 */
class CustomBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val navHome: View
    private val navEvent: View
    private val navChat: View
    private val navCommittee: View
    private val navAdmin: View
    private val navProfile: View

    private val iconHome: ImageView
    private val iconEvent: ImageView
    private val iconChat: ImageView
    private val iconCommittee: ImageView
    private val iconAdmin: ImageView
    private val iconProfile: ImageView

    private val glowHome: ImageView
    private val glowEvent: ImageView
    private val glowChat: ImageView
    private val glowCommittee: ImageView
    private val glowAdmin: ImageView
    private val glowProfile: ImageView

    var onItemSelected: ((id: Int) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.custom_bottom_nav, this, true)

        navHome = findViewById(R.id.navHome)
        navEvent = findViewById(R.id.navEvent)
        navChat = findViewById(R.id.navChat)
        navCommittee = findViewById(R.id.navCommittee)
        navAdmin = findViewById(R.id.navAdmin)
        navProfile = findViewById(R.id.navProfile)

        iconHome = findViewById(R.id.iconHome)
        iconEvent = findViewById(R.id.iconEvent)
        iconChat = findViewById(R.id.iconChat)
        iconCommittee = findViewById(R.id.iconCommittee)
        iconAdmin = findViewById(R.id.iconAdmin)
        iconProfile = findViewById(R.id.iconProfile)

        glowHome = findViewById(R.id.glowHome)
        glowEvent = findViewById(R.id.glowEvent)
        glowChat = findViewById(R.id.glowChat)
        glowCommittee = findViewById(R.id.glowCommittee)
        glowAdmin = findViewById(R.id.glowAdmin)
        glowProfile = findViewById(R.id.glowProfile)

        navHome.setOnClickListener { select(R.id.navHome) }
        navEvent.setOnClickListener { select(R.id.navEvent) }
        navChat.setOnClickListener { select(R.id.navChat) }
        navCommittee.setOnClickListener { select(R.id.navCommittee) }
        navAdmin.setOnClickListener { select(R.id.navAdmin) }
        navProfile.setOnClickListener { select(R.id.navProfile) }

        // default selection
        post { select(R.id.navHome, animate = false) }
    }

    fun showAdmin(show: Boolean) {
        navAdmin.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun select(id: Int, animate: Boolean = true) {
        updateState(id, animate)
        onItemSelected?.invoke(id)
    }

    private fun updateState(selectedId: Int, animate: Boolean) {
        // helper to set per icon state
        setIconState(iconHome, glowHome, selectedId == R.id.navHome, animate,
            R.drawable.ic_home_filled, R.drawable.ic_home)
        setIconState(iconEvent, glowEvent, selectedId == R.id.navEvent, animate,
            R.drawable.ic_events_filled, R.drawable.ic_events)
        setIconState(iconChat, glowChat, selectedId == R.id.navChat, animate,
            R.drawable.ic_chat_filled, R.drawable.ic_chat)
        setIconState(iconCommittee, glowCommittee, selectedId == R.id.navCommittee, animate,
            R.drawable.ic_committee_filled, R.drawable.ic_committee)
        setIconState(iconAdmin, glowAdmin, selectedId == R.id.navAdmin, animate,
            R.drawable.ic_admin_filled, R.drawable.ic_admin)
        setIconState(iconProfile, glowProfile, selectedId == R.id.navProfile, animate,
            R.drawable.ic_profile_filled, R.drawable.ic_profile)
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun setIconState(icon: ImageView, glow: ImageView, selected: Boolean, animate: Boolean,
                              filledRes: Int, normalRes: Int) {
        // swap drawable
        icon.setImageDrawable(ContextCompat.getDrawable(context, if (selected) filledRes else normalRes))
        // tint handled in drawables/colors: filled icons are colored; unfilled use nav_icon_inactive
        val targetSize = if (selected) dpToPx(26) else dpToPx(20)
        if (animate) {
            ValueAnimator.ofInt(icon.width.takeIf { it>0 } ?: dpToPx(20), targetSize).apply {
                duration = 180
                addUpdateListener { v ->
                    val value = v.animatedValue as Int
                    val lp = icon.layoutParams
                    lp.width = value
                    lp.height = value
                    icon.layoutParams = lp
                }
                start()
            }
        } else {
            val lp = icon.layoutParams
            lp.width = targetSize
            lp.height = targetSize
            icon.layoutParams = lp
        }

        // glow
        glow.visibility = if (selected) View.VISIBLE else View.GONE
    }
}

