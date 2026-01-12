# Runtime API Configuration Guide

## Overview
The Android app now uses the **Runtime API** (`POST /runtime/next-screen`) for all navigation flows as per the architectural requirement. The app acts as a **pure renderer** and relies on the backend for flow evaluation, config resolution, and snapshot management.

## Configuration

### 1. Update API Base URL
Edit `app/src/main/java/com/kaleidofin/originator/util/ApiConfig.kt`:

```kotlin
object ApiConfig {
    // Update this to your actual backend URL
    const val BASE_URL = "http://10.0.2.2:8080/"  // Android Emulator → localhost
    
    const val ENABLE_LOGGING = true  // Set false in production
    const val TIMEOUT_SECONDS = 30L
}
```

### 2. Base URL Options

**For Android Emulator accessing localhost:**
```kotlin
const val BASE_URL = "http://10.0.2.2:8080/"
```

**For Physical Device on same network:**
```kotlin
const val BASE_URL = "http://192.168.1.100:8080/"  // Replace with your machine's IP
```

**For Production:**
```kotlin
const val BASE_URL = "https://api.yourcompany.com/"
```

## How It Works

### Dashboard Card Click → Runtime API
When a user clicks a card on the dashboard:

1. **HomeScreen** → calls `HomeViewModel.onCardClick()`
2. **HomeViewModel** → calls `POST /runtime/next-screen` with:
   ```json
   {
     "applicationId": "placeholder-application-id",
     "currentScreenId": null,
     "formData": null
   }
   ```
3. **Backend** → evaluates flow, resolves config, returns:
   ```json
   {
     "nextScreenId": "applicant_identity_details",
     "screenConfig": { ... full screen config ... }
   }
   ```
4. **Android** → navigates to DynamicFormScreen with resolved screenId

### Form Submit → Runtime API
When user submits a form:

1. **DynamicFormScreen** → validates form
2. **DynamicFormViewModel** → calls `POST /runtime/next-screen` with:
   ```json
   {
     "applicationId": "placeholder-application-id",
     "currentScreenId": "applicant_identity_details",
     "formData": { "name": "John", "age": 30, ... }
   }
   ```
3. **Backend** → evaluates flow, resolves next screen, returns config
4. **Android** → navigates to next screen

### Back Navigation → Local Stack
Back navigation is handled **locally** by Android:
- Navigation stack is maintained in `DynamicFormViewModel`
- Backend manages flow snapshot for editing previous screens
- When user goes back, Android pops from local stack and restores previous screen

## Switching Between Real and Dummy API

### Use Real API (Default)
- `NetworkModule` provides Retrofit-based API
- Calls actual backend at `ApiConfig.BASE_URL`
- Comment out dummy API in `AppModule.kt` (already done)

### Use Dummy API (For Testing)
Edit `app/src/main/java/com/kaleidofin/originator/di/AppModule.kt`:

```kotlin
@Provides
@Singleton
fun provideFormApiService(gson: Gson): FormApiService {
    return FormApiServiceDummy(gson)  // Uncomment this
}
```

And **remove** or comment out the `NetworkModule.kt` provider.

## API Endpoints Used

### Runtime API (Primary)
- `POST /runtime/next-screen` - All navigation flows

### Master Data API
- `GET /master-data/{dataSource}` - For dropdown values

### Config Builder APIs (NOT USED)
Android **DOES NOT** call these:
- ❌ `/configs/screens`
- ❌ `/configs/flows`
- ❌ `/configs/mappings`

These are config builder APIs only. Backend Runtime API handles all config resolution.

## Troubleshooting

### Connection Refused Error
- **Android Emulator**: Use `10.0.2.2` instead of `localhost`
- **Physical Device**: Ensure device and backend are on same network
- **Check Backend**: Ensure backend is running on specified port

### API Logging
Check Logcat for request/response logs:
- Filter by tag: `OkHttp`
- Shows full request/response when `ENABLE_LOGGING = true`

### Test Backend Connectivity
```bash
# From terminal
curl -X POST http://YOUR_BASE_URL/runtime/next-screen \
  -H "Content-Type: application/json" \
  -d '{"applicationId":"test","currentScreenId":null,"formData":null}'
```

## Architecture Summary

```
Dashboard Card Click
       ↓
POST /runtime/next-screen (currentScreenId = null)
       ↓
Backend Flow Engine
  - Evaluates flow
  - Resolves config (BRANCH → PARTNER → PRODUCT → BASE)
  - Manages snapshot
       ↓
Response: { nextScreenId, screenConfig }
       ↓
Android renders screen
       ↓
User submits form
       ↓
POST /runtime/next-screen (currentScreenId, formData)
       ↓
Backend Flow Engine
  - Evaluates conditions
  - Determines next screen
  - Returns nextScreenId + screenConfig
       ↓
Android navigates to next screen
```

## Key Benefits

✅ **Single API Model**: All navigation through one endpoint
✅ **Atomic Config Delivery**: Backend returns both screenId and config
✅ **Stateless Frontend**: Android doesn't cache or resolve configs
✅ **Flow-Driven**: Backend controls all navigation logic
✅ **Pure Renderer**: Android only renders what backend sends
