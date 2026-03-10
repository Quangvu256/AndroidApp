{
  description = "AndroidApp - Android mobile application with quiz functionality";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            android_sdk.accept_license = true;
            allowUnfree = true;
          };
        };

        # Build tools version used across the configuration
        buildToolsVersion = "35.0.0";

        # Android SDK configuration
        androidComposition = pkgs.androidenv.composeAndroidPackages {
          toolsVersion = "26.1.1";
          platformToolsVersion = "35.0.1";
          buildToolsVersions = [ buildToolsVersion "34.0.0" ];
          includeEmulator = false;
          platformVersions = [ "36" "35" "34" ];
          includeSources = false;
          includeSystemImages = false;
          cmakeVersions = [ "3.22.1" ];
          includeNDK = false;
          useGoogleAPIs = false;
          useGoogleTVAddOns = false;
          includeExtras = [
            "extras;google;gcm"
          ];
        };

        androidSdk = androidComposition.androidsdk;

        # JDK 17 as required by the project
        jdk = pkgs.jdk17;

        # Additional development tools
        gradle = pkgs.gradle;
        git = pkgs.git;
        nodejs = pkgs.nodejs_20;

        # Firebase CLI for local development and deployment
        firebaseCli = pkgs.firebase-tools;

      in
      {
        # Development shell for direct use with `nix develop`
        devShells.default = pkgs.mkShell {
          buildInputs = [
            jdk
            androidSdk
            firebaseCli
            gradle
            git
            nodejs
            # Useful development tools
            pkgs.which
            pkgs.gnused
            pkgs.gawk
            pkgs.gnugrep
            pkgs.findutils
          ];

          shellHook = ''
            export JAVA_HOME="${jdk}"
            export ANDROID_HOME="${androidSdk}/libexec/android-sdk"
            export ANDROID_SDK_ROOT="$ANDROID_HOME"
            export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$PATH"
            export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_HOME/build-tools/${buildToolsVersion}/aapt2"

            echo "Android development environment loaded"
            echo "  JAVA_HOME: $JAVA_HOME"
            echo "  ANDROID_HOME: $ANDROID_HOME"
            echo "  Java version: $(java -version 2>&1 | head -n 1)"
            echo "  Android SDK tools: $(ls -1 $ANDROID_HOME/platform-tools | head -n 3 | tr '\n' ' ')"
            echo ""
            echo "Available commands:"
            echo "  ./gradlew assembleDebug  - Build debug APK"
            echo "  ./gradlew test           - Run unit tests"
            echo "  ./gradlew lint           - Run lint checks"
            echo "  firebase emulators:start - Start Firebase emulators"
          '';
        };

        # Package for Home Manager integration
        packages.default = pkgs.writeShellScriptBin "androidapp-dev" ''
          export JAVA_HOME="${jdk}"
          export ANDROID_HOME="${androidSdk}/libexec/android-sdk"
          export ANDROID_SDK_ROOT="$ANDROID_HOME"
          export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$PATH"
          exec "$@"
        '';

        # Apps for easier CLI access
        apps.default = {
          type = "app";
          program = "${self.packages.${system}.default}/bin/androidapp-dev";
        };

        # Home Manager module for integration
        homeManagerModules.default = { config, lib, ... }: {
          options.programs.androidapp = {
            enable = lib.mkEnableOption "AndroidApp development environment";
          };

          config = lib.mkIf config.programs.androidapp.enable {
            home.packages = [
              jdk
              androidSdk
              firebaseCli
              gradle
              git
              nodejs
            ];

            home.sessionVariables = {
              JAVA_HOME = "${jdk}";
              ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
              ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
            };

            home.sessionPath = [
              "${androidSdk}/libexec/android-sdk/tools"
              "${androidSdk}/libexec/android-sdk/tools/bin"
              "${androidSdk}/libexec/android-sdk/platform-tools"
            ];
          };
        };
      }
    );
}
