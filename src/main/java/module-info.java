module com.profilometer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;

    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.lang3;


    opens com.profilometer to javafx.fxml;
    exports com.profilometer;
    exports com.profilometer.config;
    exports com.profilometer.controller;
}
