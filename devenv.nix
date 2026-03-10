{ pkgs, lib, config, inputs, ... }:

{
  # https://devenv.sh/basics/
  env = {
    JAVA_HOME = "${pkgs.jdk17}";
  };

  # https://devenv.sh/packages/
  packages = with pkgs; [
    git
    gradle
    firebase-tools
    nodejs_20
  ];

  # https://devenv.sh/languages/
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk17;
  };

  languages.kotlin = {
    enable = true;
  };

  # https://devenv.sh/scripts/
  scripts.build-debug.exec = ''
    ./gradlew assembleDebug
  '';

  scripts.build-release.exec = ''
    ./gradlew assembleRelease
  '';

  scripts.test.exec = ''
    ./gradlew test
  '';

  scripts.lint.exec = ''
    ./gradlew lint
  '';

  scripts.clean.exec = ''
    ./gradlew clean
  '';

  scripts.firebase-emulators.exec = ''
    firebase emulators:start
  '';

  # https://devenv.sh/processes/
  # Uncomment to run Firebase emulators automatically
  # processes.firebase-emulators.exec = "firebase emulators:start";

  # https://devenv.sh/services/
  # Android development doesn't typically need services in devenv

  # https://devenv.sh/pre-commit-hooks/
  pre-commit.hooks = {
    # Kotlin formatting and linting
    ktlint = {
      enable = false; # Set to true if ktlint is available
    };

    # Prevent committing secrets
    detect-secrets = {
      enable = true;
    };

    # General checks
    trailing-whitespace = {
      enable = true;
    };

    end-of-file-fixer = {
      enable = true;
    };
  };

  # https://devenv.sh/integrations/dotenv/
  dotenv.enable = true;

  # Android SDK configuration
  android = {
    enable = true;

    # Platforms
    platforms.version = [ "36" "35" "34" ];

    # Build tools
    buildTools.version = [ "35.0.0" "34.0.0" ];

    # System images for emulator
    systemImages = [];

    # Platform tools
    platformTools.enable = true;

    # Emulator
    emulator.enable = true;

    # Accept licenses
    acceptLicense = true;
  };

  # Environment variables
  env.ANDROID_HOME = config.env.DEVENV_STATE + "/android-sdk";
  env.ANDROID_SDK_ROOT = config.env.ANDROID_HOME;

  enterShell = ''
    echo ""
    echo "🚀 AndroidApp Development Environment"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "📱 Android SDK: $ANDROID_HOME"
    echo "☕ Java: $(java -version 2>&1 | head -n 1)"
    echo "🔥 Firebase CLI: $(firebase --version)"
    echo ""
    echo "Available scripts:"
    echo "  build-debug        - Build debug APK"
    echo "  build-release      - Build release APK"
    echo "  test               - Run unit tests"
    echo "  lint               - Run lint checks"
    echo "  clean              - Clean build artifacts"
    echo "  firebase-emulators - Start Firebase emulators"
    echo ""
    echo "Quick start:"
    echo "  ./gradlew assembleDebug"
    echo ""
  '';
}
