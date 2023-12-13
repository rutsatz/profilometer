# profilometer

This project analyzes images of vehicle profiles and counts how many axles are touching the ground.

Contributors:
Rafael Fernando Rutsatz

Article:

### Run project locally from IDE

To run the project locally, you need to include the opencv module,
containing the native library. For that, include the module path
as a JVM argument. Example:

#### Configure module
```
--module-path /home/raffa/IdeaProjects/profilometer/build/distributions/profilometer-1.0-SNAPSHOT/lib --module com.profilometer/com.profilometer.ProfilometerApplication
```

#### Configure build

Configure your IDE to run this commands before program start:

This will copy the latest code to the module path.

`./gradlew clean build -x test`

This will extract the source code inside the module path:

```
unzip /home/raffa/IdeaProjects/profilometer/build/distributions/profilometer-1.0-SNAPSHOT.zip  -d /home/raffa/IdeaProjects/profilometer/build/distributions/ 
```

For unzip, set the workdir as `/bin`.

### Run distribution version

Use JavaFX 21 as the default JVM. Otherwise, update the distribution script to
export JAVA_HOME using the provided JVM.

Windows x64: `set JAVA_HOME=../zulu21jre-win`

### Run

Use the provided scripts to run the application.

### Using

Click on `File -> Open input folder` to select the folder with the `.jpg` images
with the vehicles profiles.

