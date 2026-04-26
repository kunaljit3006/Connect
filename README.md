# Connect - Modern Video Calling & Social Networking

![Connect Banner](connect_app_banner_1777209750427.png)

## 📸 Screenshots

<p align="center">
  <img src="screenshots/signin.png" width="30%" />
  <img src="screenshots/signup.png" width="30%" />
  <img src="screenshots/forgot_password.png" width="30%" />
</p>

<p align="center">
  <img src="screenshots/drawer.png" width="30%" />
  <img src="screenshots/profile.png" width="30%" />
</p>

Connect is a premium, state-of-the-art Android application designed to bridge distances. Built with Kotlin and powered by WebRTC, it offers seamless real-time video calling, screen sharing, and a robust social networking experience.

---

## ✨ Key Features

- 📹 **High-Quality Video Calls**: Crystal clear real-time video communication powered by WebRTC.
- 🖥️ **Screen Sharing**: Share your screen during calls for collaboration or support.
- 🤝 **Friend Management**: Send, receive, and manage friend requests effortlessly.
- 🛡️ **Privacy Controls**: Block or unfriend users to maintain a safe social environment.
- 🌓 **Dynamic Theme**: Full support for Light and Dark modes with a sleek, modern UI.
- 🔔 **Real-time Notifications**: Stay updated with incoming call alerts and friend requests.
- 👤 **Profile Customization**: Personalize your profile with ease.

---

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: Native Android (XML with Material Components)
- **Real-time Communication**: [WebRTC](https://webrtc.org/)
- **Backend**: [Firebase](https://firebase.google.com/)
  - **Authentication**: Secure email/password login and sign-up.
  - **Firestore**: Real-time signaling for WebRTC and data storage.
- **Dependency Management**: Gradle (Kotlin DSL)

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Flamingo or newer.
- JDK 17 or higher.
- A Firebase project (for signaling and auth).

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/Connect.git
   ```

2. **Firebase Setup**:
   - Create a new project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app to your Firebase project with the package name `com.example.connect`.
   - Download the `google-services.json` file and place it in the `app/` directory of the project.
   - Enable **Email/Password** authentication in the Firebase Auth tab.
   - Enable **Cloud Firestore** and set up your security rules.

3. **Build the Project**:
   - Open the project in Android Studio.
   - Let Gradle sync and download dependencies.
   - Build the project using `Build > Make Project`.

4. **Run the App**:
   - Connect your Android device or start an emulator.
   - Click `Run > Run 'app'`.

---

## 📂 Project Structure

```text
Connect/
├── app/
│   ├── src/main/java/com/example/connect/
│   │   ├── adapter/         # RecyclerView adapters
│   │   ├── model/           # Data models
│   │   ├── webrtc/          # WebRTC implementation & Signaling
│   │   └── ...Activities    # UI Components
│   └── src/main/res/        # Resources (Layouts, Drawables, Values)
├── build.gradle.kts         # Root build configuration
└── settings.gradle.kts      # Project settings
```

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📧 Contact

**Developer**:Kunaljit Kashyap
**Project Link**: [https://github.com/your-username/Connect](https://github.com/your-username/Connect)

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

<p align="center">
  Developed with ❤️ for a more connected world.
</p>
