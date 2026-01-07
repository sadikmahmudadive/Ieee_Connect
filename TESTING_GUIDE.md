# Testing Guide - Bug Fixes Verification

## Overview
This guide helps you verify that the NullPointerException crash in HomeFragment has been fixed.

---

## Fixed Issues

### ✅ Issue 1: HomeFragment NullPointerException (CRITICAL)
**What was broken**: App crashed when navigating away from HomeFragment before admin check completed
**What was fixed**: Added null safety checks in all async Firestore callbacks

### ✅ Issue 2: EventDetailActivity Not Found
**Status**: Already working correctly in latest code

### ✅ Issue 3: ClassCastException in EventDetailActivity  
**Status**: Already fixed - eventId passed as String

---

## Testing Steps

### Test 1: Fragment Lifecycle Crash (FIXED)
**Purpose**: Verify the NullPointerException is fixed

1. **Start the app** and login
2. **Navigate to Home tab** (if not already there)
3. **Immediately navigate to Events tab** (before admin check completes)
4. **Navigate back to Home tab**
5. **Navigate to Profile tab**
6. **Navigate back to Home tab again**

**Expected Result**: ✅ No crash occurs
**What was happening before**: App crashed with NullPointerException

---

### Test 2: Event Detail Navigation
**Purpose**: Verify event navigation works correctly

1. **Navigate to Home or Events tab**
2. **Click on any event** in the list
3. **Verify event details appear** (title, description, banner image, time, location)
4. **Press back button**
5. **Click on another event**

**Expected Result**: ✅ Event details load correctly without crash
**Previous Issue**: ActivityNotFoundException or ClassCastException

---

### Test 3: Auto-Reload Feature
**Purpose**: Verify events reload when returning to fragment

1. **Open Home tab**
2. **Wait for events to load**
3. **Navigate to Events tab**
4. **Navigate back to Home tab**
5. **Check console logs** for "Fetching events from Firestore"

**Expected Result**: ✅ Events are automatically reloaded
**Benefit**: Always shows fresh data

---

### Test 4: Admin FAB Visibility
**Purpose**: Verify admin floating action button appears correctly

1. **Login as admin user**
2. **Navigate to Home tab**
3. **Wait 1-2 seconds** for admin check to complete
4. **Verify FAB (+ button) appears** in bottom-right corner

**Expected Result**: ✅ FAB appears for admin users
**Expected Result**: ✅ FAB does not appear for regular users

---

### Test 5: Create Event (Admin Only)
**Purpose**: Verify event creation works

1. **Login as admin**
2. **Click FAB button** in Home tab
3. **Fill in event details**
4. **Submit event**
5. **Return to Home tab**
6. **Verify new event appears** in feed

**Expected Result**: ✅ Event created and feed refreshed automatically

---

### Test 6: Edit Event (Admin Only)
**Purpose**: Verify event editing works

1. **Login as admin**
2. **Click on any event** to open details
3. **Click Edit button** (if visible to admins)
4. **Modify event details**
5. **Save changes**
6. **Verify details updated** on screen

**Expected Result**: ✅ Event updated and changes visible immediately

---

## Crash Indicators to Watch For

### ❌ BAD: NullPointerException
```
java.lang.NullPointerException: Attempt to read from field '...' on a null object reference
```
**Status**: FIXED ✅

### ❌ BAD: ActivityNotFoundException
```
android.content.ActivityNotFoundException: Unable to find explicit activity class
```
**Status**: FIXED ✅

### ❌ BAD: ClassCastException
```
java.lang.ClassCastException: String cannot be cast to Event
```
**Status**: FIXED ✅

---

## Log Monitoring

### Watch for these SUCCESS logs:
```
D/MainActivity: User doc: {lastName=..., firstName=..., role=ADMIN, isAdmin=true}
D/MainActivity: isAdmin: true, role: ADMIN
I/MainActivity: Navigating to AdminDashboard via DashboardActivity
D/EventRepository: Fetching events from Firestore
D/EventRepository: Fetched X events from Firestore
```

### Watch for these ERROR indicators:
```
E/AndroidRuntime: FATAL EXCEPTION: main
E/AndroidRuntime: Process: com.example.ieeeconnect, PID: XXXXX
```
**If you see these**: Something is still broken

---

## Build Verification

### To rebuild and install:
```bash
cd D:\Code\android_app\ieeeconnect
.\gradlew clean assembleDebug
```

### To install on device:
```bash
.\gradlew installDebug
```

### Check APK was generated:
```bash
Test-Path "app\build\outputs\apk\debug\app-debug.apk"
```
**Expected**: True

---

## Performance Testing

### Memory Leak Check:
1. Open app
2. Navigate between tabs 20-30 times rapidly
3. Check memory usage in Android Studio Profiler
4. Verify no memory leaks from fragment lifecycle issues

### Network Disconnect Test:
1. Turn off WiFi/Mobile data
2. Open Home tab
3. Verify "Offline" banner appears
4. Turn on network
5. Pull to refresh
6. Verify events load and banner disappears

---

## Known Warnings (Safe to Ignore)

These warnings appear in build but don't affect functionality:
```
warning: The following options were not recognized by any processor
```

These are code quality warnings (not errors):
- "Field may be final"
- "Method is never used"
- "Inner class may be static"

---

## Rollback Instructions

If issues persist, you can revert changes:
```bash
git log --oneline -10
git revert <commit-hash>
```

---

## Success Criteria

✅ All 6 tests pass without crashes
✅ No fatal exceptions in logcat
✅ App builds successfully
✅ APK installs and runs on device
✅ Admin features work correctly
✅ Regular user features work correctly

---

## Next Steps After Verification

1. Test on multiple devices (different Android versions)
2. Test with different user roles (admin, regular user)
3. Test edge cases (slow network, no network, etc.)
4. Consider adding automated tests
5. Update user documentation

---

## Support

If you encounter issues:
1. Check logcat for detailed error messages
2. Verify you're testing with the latest build
3. Clear app data and restart
4. Try on a different device
5. Check FIXES_SUMMARY.md for technical details

