package com.example.ieeeconnect.ui.onboarding;

public class OnboardingPage {
    private final int animationRes;
    private final String title;
    private final String subtitle;

    public OnboardingPage(int animationRes, String title, String subtitle) {
        this.animationRes = animationRes;
        this.title = title;
        this.subtitle = subtitle;
    }

    public int getAnimationRes() { return animationRes; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
}

