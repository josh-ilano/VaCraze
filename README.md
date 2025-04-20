# VaCraze
Application where you smart plan your vacation


## Google Maps API Key Setup (Android View)

In the backend, put the **Google Maps API Key** in the following files (where it says `MAPS_KEY_HERE` or `API_KEY_HERE`):

---

### 1. `Gradle Scripts -> local.properties (SDK Location)`
      MAPS_KEY=MAPS_KEY_HERE

### 2. Gradle scripts -> build.gradle.kts (module script):
      andriod{
         ...
        defaultConfig{
         ...
          buildConfigField("String", "MAPS_API_KEY", "\"API_KEY_HERE\"")
          }
      }

### 3. app -> mainfest -> AndriodManifest.xml:
      <manifest ....>
        ...
        <application ...
           <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="API_KEY_HERE" />
