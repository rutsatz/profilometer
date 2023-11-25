module com.profilometer.profilometer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;

    opens com.profilometer.profilometer to javafx.fxml;
    exports com.profilometer.profilometer;
}