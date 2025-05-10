# VaCraze
Application where you smart plan your vacation

## Required Google APIs:

In your Google Console please enable these apis:
### 1. Maps SDK for Android
![Screenshot 2025-05-10 000412](https://github.com/user-attachments/assets/49455889-ecec-4151-8606-d38f9c8faa83)

### 2. Places API
![Screenshot 2025-05-10 000509](https://github.com/user-attachments/assets/77674ad7-5cdf-4f1d-a90a-f1d455d83b66)

### 3. Weather API
![Screenshot 2025-05-10 000236](https://github.com/user-attachments/assets/88a7c6b1-f271-4439-80bd-f09b164b557c)

---

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
      
      ### IMPORTANT: please kee the "\"....\"" so an example is "\"AXHDSOESsf323432|""
 ### 3. app -> mainfest -> AndriodManifest.xml:
      <manifest ....>
        ...
        <application ...
           <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="API_KEY_HERE" />



## Firebase Setup 
IMPORTANT: I have shared the firebase storage with you. You will have to accept it, it should be in your emails.
### 1. In the Main Menue go to Tools -> FireBase
![Screenshot 2025-05-10 000946](https://github.com/user-attachments/assets/3e1bf8d6-11ab-4a3a-943f-4b6fc0c415a8)

### 2. In the FireBase dash board: Go to Authentication -> Authenticate using a custom authentication system 
IMPORTANT: Don't use the Authenticate using a custom authentication system (Java) version
![Screenshot 2025-05-10 001414](https://github.com/user-attachments/assets/de88b4db-d1b5-4645-9679-0451ed8e9d16)

### 3. Click on Connect, and connect it to the shared firebase storage. 
### 4. After Connected, click on Add the FirebaseAuthentication SDK to your app

## FOR TABLETS or if you see a "Couldn't get current location" for NearbyPlaces page
### We learned while testing that Tablets on Android Studio don't come auto connected with a GPS location, so the app will run and so will the sepcific search, but you will be unable to use the area search or near by places features.
In extended controls: location -> Set Location ->  Enable GPS Location (You might have to completely restart the emulator in one device it worked by just restarting the app, on another the whole emulator had to be restarted)
![Screenshot 2025-05-10 002654](https://github.com/user-attachments/assets/39396080-a4bc-4fd8-9649-833f1e0652b2)


