<div align="center">
  <img src="A-Stride icon.png" width="128" height="128" style="border-radius: 25%;">
  <h1>A Stride</h1>
  <p><b>A high-performance, privacy-focused pedometer engine for Android.</b></p>
</div>

A-Stride is a minimalist step-tracking application built to prove that health data belongs on-device. By interfacing directly with hardware sensors, A-Stride provides precise movement metrics without the battery drain of GPS or the privacy risks of cloud-based tracking.

## ✨ Key Features
* **Hardware-Direct Tracking:** Utilizes the `TYPE_STEP_COUNTER` sensor for maximum battery efficiency.
* **Calibrated Precision:** Optimized with a 0.73m stride length, verified through field testing.
* **Privacy by Design:** Zero internet permissions. No telemetry. No cloud sync.
* **Modern Stack:** Built entirely with Kotlin and Jetpack Compose for a responsive, declarative UI.

## 📈 Field Test & Validation
The underlying algorithm was validated on a known course:

| Metric | Result |
| :--- | :--- |
| **Target Distance** | ~ 3.15 km |
| **Calculated Distance** | **3.10 km** |
| **Total Steps** | 4,240 |
| **Est. Energy Burn** | 170 kcal |

## 🛠 Tech Stack
- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Minimum SDK:** API 29 (Android 10)
- **Architecture:** MVVM (In-Progress)

## 🚀 Roadmap
- [ ] **v0.2.0:** Implementation of a **Foreground Service** for persistent background tracking.
- [ ] **v0.3.0:** **Room Database** integration for encrypted local history.
- [ ] **v0.4.0:** ML-based dynamic stride calibration based on walking cadence.

## 📥 Installation
You can find the latest pre-release builds in the [Releases](https://github.com/YOUR_GITHUB_USERNAME/A-Stride/releases) section. 

*Note: Ensure you grant the **Physical Activity** permission upon first launch.*

## 📄 License
Licensed under the [GNU GPLv3](LICENSE).
