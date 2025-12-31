# IEEE Connect Android

This project uses a Java UI with view binding and Material components.

## Current status
- Java-only module; Kotlin/Compose removed.
- Home screen uses RecyclerView via `HomeFragment`.

## Build
```powershell
./gradlew.bat assembleDebug
```

## Notes
- If you see stale errors, clean first: `./gradlew.bat clean assembleDebug`.

## Next steps
- Continue iterating on Java UI, or reintroduce Compose later if desired.
