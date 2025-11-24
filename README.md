# Galeria de Defensores

Galeria de Defensores is an Android application designed for managing character sheets for the 3D&T Alpha system. This application allows users to create, view, and manage their characters in an intuitive interface.

## Features

- **Character Management**: Create and manage character sheets with various attributes.
- **User-Friendly Interface**: Easy navigation through character lists and details.
- **Data Persistence**: Save character data for future access.

## Project Structure

The project is organized as follows:

```
galeria-de-defensores
├── app
│   ├── src
│   │   └── main
│   │       ├── AndroidManifest.xml
│   │       ├── java
│   │       │   └── com
│   │       │       └── galeria
│   │       │           └── defensores
│   │       │               ├── MainActivity.kt
│   │       │               ├── models
│   │       │               │   └── Character.kt
│   │       │               └── ui
│   │       │                   ├── CharacterListFragment.kt
│   │       │                   └── CharacterDetailFragment.kt
│   │       └── res
│   │           ├── layout
│   │           │   ├── activity_main.xml
│   │           │   ├── fragment_character_list.xml
│   │           │   └── fragment_character_detail.xml
│   │           └── values
│   │               ├── strings.xml
│   │               ├── colors.xml
│   │               └── themes.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle
│   └── wrapper
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── build.gradle
├── settings.gradle
├── gradle.properties
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── .gitignore
└── README.md
```

## Getting Started

To get started with the project, clone the repository and open it in your preferred IDE. Make sure to have the necessary Android SDK and tools installed.

## Installation

1. Clone the repository:
   ```
   git clone <repository-url>
   ```
2. Open the project in Android Studio.
3. Sync the Gradle files.
4. Run the application on an emulator or physical device.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any enhancements or bug fixes.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.