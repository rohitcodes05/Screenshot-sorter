# ScreenSort — Strict UI & Design Guidelines

> **Mandatory Rule for All Future Development**:
> Whenever creating, refactoring, or modifying UI components, app icons, color themes, or dialogs in ScreenSort, these guidelines **MUST** be strictly adhered to.
> Any flashiness, neon effects, decorative gradients, or heavy shadows will be rejected.

---

## 1. Design Philosophy: Utilitarian & Minimal

- **Reference Standard**: Google's official utility applications — **Google Files**, **Google Keep**, and **Google Tasks**.
- **The "Boring App" Principle**: Professional, high-utility tools are quiet, minimal, and dependable. Avoid gimmicks, futuristic "AI aesthetic" clichés (no floating sparkles, glowing halos, or glossy glassmorphic cards).
- **Function Over Form**: Every UI element must serve an immediate user action. Content (the user's screenshots and categories) is the primary focus; the chrome and scaffolding should stay subtle and unobtrusive.

---

## 2. Color Palette Rules

### Permitted Colors:
- **Surfaces & Backgrounds**: Soft neutral grays, off-whites, and tonal containers.
  - Light mode: Clean off-white (`#FEF7FF`, `#F3EDF7`, `#F8F9FA`).
  - Dark mode: Deep neutral charcoal/black (`#121212`, `#1D1B20`).
- **Text & Borders**: High-contrast, legible neutral tones (`#1D1B20`, `#49454F`, `#CAC4D0`).
- **Primary Accent**: Exactly **ONE** restrained, flat accent color (e.g. flat Indigo `#3949AB`, flat Deep Teal `#00695C`, or M3 Primary `#6750A4`).

### Strictly Prohibited:
- ❌ **No Gradients**: Zero linear, radial, or sweep gradients on buttons, cards, app icons, or headers.
- ❌ **No Neon or Electric Colors**: No neon purple, electric cyan, vibrant magenta, or bright neon green.
- ❌ **No Glossy / Glassmorphic Effects**: No background blurs, frosted glass, or shine overlays.
- ❌ **No Heavy Shadows**: Maximum elevation is 1dp–2dp tonal elevation or flat 1dp subtle border outlines.

---

## 3. App Icon & Asset Rules (SS Monogram with Cream Tactile Outer Rim)

- **Approved Final Direction**: **Option 2 (Refined)** — Creative **SS** lettermark hero monogram with 4 supporting screenshot cards, seamless rich blue (`#356AC3`) felt background, and a soft cream tactile outer rim/frame.
- **Hero Monogram & Supporting Artwork**:
  - **Hero Element**: Prominent interlocking cream felt **SS** monogram (`#F2EAD8`) with stitched inner border and raised cream 3D clay bevel (`#E6DAC4`), providing immediate lettermark recognition at 48dp dock size.
  - **Supporting Cards**: 4 stacked screenshot cards peeking behind the SS (light blue app grid, slate UI wireframe, slate-navy photo card with sun/mountains, and warm peach article card with text lines and folded corner) supporting the app concept without competing with the SS.
  - **Tactile Depth**: Authentic directional drop shadows beneath the SS and cards, cast naturally onto the rich blue felt background. Zero glossy plastic finishes, zero neon, zero glowing halos, and zero glassmorphism.
- **Refined Cream Tactile Outer Rim**:
  - **Thickness**: Refined by ~12% (`~2.5dp` stroke on a 72dp mask / `9.2px` @ 270px) for ideal balance between frame presence and open breathing room.
  - **Dimensional Styling**: Soft cream gradient (`#FCF5E2` top-left highlight to `#E6D7C0` bottom-right warm tone) with an inner contact drop shadow (`#061437`) casting inward onto the blue felt tray.
  - **Adaptive Mask Conformity**: Conforms naturally to the OEM launcher shape (circular bezel on `ic_launcher_round`, squircle bezel on `ic_launcher`), eliminating awkward corner clipping across all devices.
- **Color & Material Specifications**:
  - **Background**: Rich royal blue (`#356AC3` / `rgb(53, 106, 195)`) featuring subtle tactile felt/wool texture and gentle studio ambient lighting.
  - **Palette**: Natural clay and felt tones (cream, light blue, slate, peach, rich blue) with realistic ambient occlusion.
- **Adaptive Layout & Safe Zone Rules**:
  - **Scale**: Centered cluster ($s = 0.82$, centered at 512, 510 on 1024px canvas), fitting ~96% of the central 66dp safe zone ($R_{\max} \le 31.2\text{dp}$ on a 108dp canvas).
  - **Safe Zone Compliance**: 100% safe inside both circular (Pixel/AOSP) and rounded-square (Samsung/Xiaomi) masks with zero unintended cropping.
- **Asset Pipeline & Anti-Blur Standards**:
  - **Density Buckets**: Exported across standard Android densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).
  - **Adaptive vs Legacy Decoupling**: Adaptive icons use a 108dp canvas; legacy mipmaps are rendered independently into standard 48dp canvases with built-in squircle and circular cream frames to eliminate double padding.
  - **Sharpened Downsampling**: Downscaling uses multi-step pyramidal half-scaling followed by subtle unsharp masking at target resolution to maintain crisp borders, stitching, and bevel edges without blur.
- **Android 13+ Themed Icons**: Provide a matching [`ic_launcher_monochrome.xml`](file:///c:/Users/nobod/Desktop/screenshoter/app/src/main/res/drawable/ic_launcher_monochrome.xml) with clean silhouette cutouts for dynamic Material You wallpaper tinting.

---

## 4. Jetpack Compose & Component Guidelines

- **Cards**: Flat `Card` or `OutlinedCard` with `RoundedCornerShape(12.dp)` and zero or minimal tonal elevation.
- **Chips**: Standard `FilterChip` / `AssistChip` with flat tonal containers.
- **TopAppBar**: Flat `TopAppBar` matching the window background; avoid decorative title badges or cluttered row layouts.
- **Dialogs & BottomSheets**: Standard Material 3 containers without custom gradient backgrounds.

---

## 5. Security & Secrets Management Policy

1. **100% Offline Integrity**:
   - ScreenSort is intentionally designed with zero internet permissions.
   - The `android.permission.INTERNET` permission must **never** be added to `AndroidManifest.xml`.

2. **Zero Hardcoded Secrets Policy**:
   - No API key, bearer token, client secret, OAuth credential, or private endpoint may ever be committed to git or hardcoded in source code files.
   - If any external service or sync functionality is ever added in the future:
     - **Local Development**: Read credentials strictly from `local.properties` (which is gitignored).
     - **CI/CD Builds**: Read credentials via environment variables.
     - **Gradle Injection**: Inject credentials into code exclusively via `buildConfigField` in `build.gradle.kts`:
       ```kotlin
       val apiKey: String = gradleLocalProperties(rootDir, providers).getProperty("MY_API_KEY", "")
       buildConfigField("String", "MY_API_KEY", "\"$apiKey\"")
       ```
     - Never commit `.env`, `secrets.properties`, or `google-services.json` to source control.
