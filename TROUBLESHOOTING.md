# Troubleshooting Guide

## If App Still Crashes After Update

### 1. Complete Uninstall and Reinstall
The safest way to ensure all changes take effect:

```bash
# Uninstall the old app completely
adb uninstall com.example.ieeeconnect

# Install the new version
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Clear App Data (Alternative)
If you want to keep the app installed:

```bash
# Clear all app data
adb shell pm clear com.example.ieeeconnect
```

### 3. Verify Database Version
Check logcat to confirm the database migration:
```bash
adb logcat | grep "Room"
```

You should see:
- No more "Expected identity hash" errors
- Database name should be `ieee_connect_database_v5`
- Version should be 15

## Common Errors and Solutions

### Error: "ActivityNotFoundException: EventDetailActivity"
**Cause**: APK not properly updated
**Solution**: 
1. Uninstall old app: `adb uninstall com.example.ieeeconnect`
2. Reinstall: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### Error: "ClassCastException: String cannot be cast to Event"
**Cause**: Old cached code still running
**Solution**: 
1. Force stop app: `adb shell am force-stop com.example.ieeeconnect`
2. Clear cache: `adb shell pm clear com.example.ieeeconnect`
3. Restart app

### Error: Room schema mismatch
**Cause**: Database migration not triggered
**Solution**: 
1. Uninstall app completely
2. OR use: `adb shell pm clear com.example.ieeeconnect`
3. Reinstall

## Testing Checklist

- [ ] App launches without crash
- [ ] Can view events list
- [ ] Can click on event to see details (no ClassCastException)
- [ ] Can create new event with location
- [ ] Location appears in event details
- [ ] Add to Calendar includes location
- [ ] Location is optional (can create event without it)
- [ ] Database errors are gone from logcat

## Build Commands

### Clean Build
```bash
cd D:\Code\android_app\ieeeconnect
.\gradlew clean
.\gradlew assembleDebug
```

### Install to Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### View Logs
```bash
# All logs
adb logcat | grep "ieeeconnect"

# Just errors
adb logcat | grep "ERROR"

# Room database logs
adb logcat | grep "Room"
```

## Key Changes Summary

1. **Database Version**: 14 → 15
2. **Database Name**: `ieee_connect_database_v4` → `ieee_connect_database_v5`
3. **Location Feature**: Already implemented, just needed database migration
4. **EventDetailActivity**: Already fixed to receive eventId as String

## Next Steps After Installation

1. Create a test event with location
2. Verify location appears in event details
3. Test "Add to Calendar" functionality
4. Check that all old database errors are gone
5. Verify app doesn't crash when clicking events

