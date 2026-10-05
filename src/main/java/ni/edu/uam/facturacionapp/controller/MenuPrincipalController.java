package ni.edu.uam.facturacionapp.controller;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import ni.edu.uam.facturacionapp.util.SceneManager;
import java.io.IOException;

public class MenuPrincipalController {
    @FXML
    private void abrirProductos() {
        try {
            SceneManager.abrirVentana(
                    "/ni/edu/uam/facturacionapp/fxml/producto-view.fxml",
                    "Gestión de productos");
        } catch (Exception e) {
            e.printStackTrace(); // <--- Muestra la causa real en la consola de IntelliJ
            new Alert(Alert.AlertType.ERROR, "Detalle del error: " + e.getMessage()).showAndWait();
        }
    }
    @FXML
    private void abrirCategorias() {
        try {
            SceneManager.abrirVentana(
                    "/ni/edu/uam/facturacionapp/fxml/categoria-view.fxml",
                    "Gestión de Categorías");
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al abrir la ventana: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void salir() {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Desea cerrar la aplicación?", ButtonType.OK, ButtonType.CANCEL);
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK)
            Platform.exit();
    }

}