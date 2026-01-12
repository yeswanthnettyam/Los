# Backend-Driven Dashboard - Setup Guide

## Overview
The Android app now has a **fully backend-driven dashboard** that renders flows dynamically based on backend configuration. No code changes needed when flows change!

## Architecture

### Flow Start Process
```
Dashboard Tile Click
     ↓
POST /runtime/next-screen
Body: {
  "applicationId": "...",
  "currentScreenId": "__START__",  ← Special marker for flow initiation
  "formData": null
}
     ↓
Backend Flow Engine
  - Evaluates flow conditions
  - Resolves first screen
  - Returns screenId + screenConfig
     ↓
Android renders Dynamic Form Screen
```

### Back Navigation
```
User presses back
     ↓
Android pops from LOCAL navigation stack
     ↓
Re-renders previous screen with saved formData
     ↓
Backend snapshot remains authoritative
```

## Backend API Requirement

### 1. Dashboard Flows API
**Endpoint:** `GET /api/v1/dashboard/flows`

**Response:**
```json
{
  "flows": [
    {
      "flowId": "APPLICANT_FLOW",
      "title": "Applicant Onboarding",
      "description": "Capture applicant details",
      "icon": "APPLICANT_ONBOARDING",
      "ui": {
        "backgroundColor": "#0B2F70",
        "textColor": "#FFFFFF",
        "iconColor": "#00B2FF"
      },
      "startable": true,
      "productCode": "PL",
      "partnerCode": null,
      "branchCode": null
    }
  ]
}
```

### 2. Runtime API - Flow Start
**Endpoint:** `POST /runtime/next-screen`

**Request for Flow Start:**
```json
{
  "applicationId": "APP123",
  "currentScreenId": "__START__",
  "formData": null
}
```

**Response:**
```json
{
  "nextScreenId": "applicant_identity_details",
  "screenConfig": { ... full screen config ... }
}
```

## Icon Setup

### IconRegistry
Located at: `app/src/main/java/com/kaleidofin/originator/util/IconRegistry.kt`

Maps backend icon keys to Android drawable resources:
- `APPLICANT_ONBOARDING` → `R.drawable.ic_applicant`
- `KYC_CAPTURE` → `R.drawable.ic_kyc`
- `CREDIT_CHECK` → `R.drawable.ic_credit`
- etc.

### Adding New Icons

**Step 1:** Backend sends new icon key in dashboard API
```json
{
  "icon": "NEW_FLOW_TYPE"
}
```

**Step 2:** Add drawable resource
- Place icon in `app/src/main/res/drawable/`
- Name: `ic_new_flow_type.xml` or `.png`

**Step 3:** Update IconRegistry
```kotlin
when (iconKey.uppercase()) {
    "NEW_FLOW_TYPE" -> R.drawable.ic_new_flow_type
    // ... existing mappings
}
```

### Missing Icons Handling
- If icon key not found in registry → shows default icon
- App won't crash on missing icons
- Fallback: `R.drawable.ic_default_flow`

## UI Customization

### Colors
Backend controls all colors via `dashboardMeta.ui`:
```json
{
  "ui": {
    "backgroundColor": "#0B2F70",  // Tile background
    "textColor": "#FFFFFF",         // Title & description text
    "iconColor": "#00B2FF"          // Icon tint
  }
}
```

Supports hex formats:
- `#RGB` → `#RRGGBB`
- `#ARGB` → `#AARRGGBB`
- `#RRGGBB`
- `#AARRGGBB`

### Fallback Behavior
If `ui` metadata is missing:
- Uses Material Theme colors
- backgroundColor → `primaryContainer`
- textColor → `onPrimaryContainer`
- iconColor → `primary`

## Testing with Dummy API

### Current Dummy Flows
Located in: `FormApiServiceDummy.getDashboardFlows()`

Returns 4 sample flows:
1. Applicant Onboarding (Blue)
2. KYC Verification (Dark Blue)
3. Credit Check (Green)
4. Payment Collection (Red)

### Switching to Real API
Already configured! Just ensure backend is running at `ApiConfig.BASE_URL`

## Navigation Rules

### ✅ Android DOES:
- Maintain local navigation stack for back button
- Call Runtime API with `__START__` to initiate flows
- Render UI from backend config
- Handle back navigation locally (pop stack)

### ❌ Android DOES NOT:
- Decide next screen
- Cache flow configurations
- Infer flow logic
- Call backend for back navigation
- Hardcode flow relationships

## Flow Stack

### Implementation
Located in: `DynamicFormViewModel._navigationStack`

**Structure:**
```kotlin
data class NavigationStackEntry(
    val screenId: String,
    val screenConfig: FormScreenDto,
    val formData: Map<String, Any>?
)
```

**Rules:**
- Push on forward navigation
- Pop on back navigation
- Cleared on flow start
- Stack size > 1 → back enabled
- Stack size <= 1 → exit flow

## Error Handling

### Dashboard Load Failure
- Shows error message
- Provides retry button
- Falls back to empty state if no flows

### Flow Start Failure
- Shows error in snackbar
- Stays on dashboard
- User can retry by clicking again

### Network Errors
- Full request/response logging (if `ENABLE_LOGGING = true`)
- Check Logcat tag: `OkHttp`

## Key Files

```
Data Layer:
- data/dto/DashboardDto.kt                    // API response models
- data/mapper/DashboardMapper.kt              // DTO → Domain mapping
- data/api/FormApiService.kt                  // API endpoints
- data/api/FormApiServiceDummy.kt             // Dummy implementation
- data/datasource/FormDataSource.kt           // Data source interface
- data/datasource/impl/FormDataSourceImpl.kt  // Data source implementation

Domain Layer:
- domain/model/DashboardFlow.kt               // Domain models

Presentation Layer:
- presentation/viewmodel/HomeViewModel.kt     // Dashboard logic
- presentation/screen/HomeScreen.kt           // Dashboard UI
- presentation/component/DashboardFlowCard.kt // Flow tile component
- presentation/ui/state/HomeUiState.kt        // UI state

Utilities:
- util/IconRegistry.kt                        // Icon key mapping
- util/ApiConfig.kt                           // API configuration
```

## Benefits

✅ **Zero Code Changes:** Add/remove/modify flows without touching Android code
✅ **Backend Control:** All flow logic, sequencing, and conditions in backend
✅ **Dynamic UI:** Colors, icons, text all configurable per flow
✅ **Pure Renderer:** Android focuses only on rendering and UX
✅ **Safe Navigation:** Local back stack prevents flow corruption
✅ **Atomic Config:** Backend returns screenId + config in single response
✅ **Flow Snapshots:** Backend manages versioning and consistency

## Next Steps

1. **Add Icons:** Place icon files in `res/drawable/` and update `IconRegistry`
2. **Backend Implementation:** Implement `GET /api/v1/dashboard/flows` endpoint
3. **Runtime API:** Ensure `POST /runtime/next-screen` handles `"__START__"` marker
4. **Test Flows:** Click dashboard tiles and verify Runtime API calls
5. **Customize UI:** Configure colors in FlowConfig.dashboardMeta

## Troubleshooting

**Problem:** Dashboard shows "Loading..." forever
- **Solution:** Check backend is running at configured BASE_URL
- **Check:** Logcat for network errors

**Problem:** Tiles show wrong colors
- **Solution:** Verify hex color format in dashboard API response
- **Check:** Colors parse correctly in `DashboardMapper.parseColor()`

**Problem:** Icons not showing
- **Solution:** Add drawable resources for icon keys in IconRegistry
- **Check:** Default icon shows if mapping missing (app won't crash)

**Problem:** Flow doesn't start
- **Solution:** Ensure Runtime API handles `currentScreenId = "__START__"`
- **Check:** Logcat for API request/response

**Problem:** Back button doesn't work
- **Solution:** Flow stack should have > 1 entry for back to work
- **Check:** `DynamicFormViewModel._navigationStack` size

