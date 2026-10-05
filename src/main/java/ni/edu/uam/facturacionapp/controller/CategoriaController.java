package ni.edu.uam.facturacionapp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ni.edu.uam.facturacionapp.dao.CategoriaDAO;
import ni.edu.uam.facturacionapp.model.Categoria;

public class CategoriaController {

    @FXML private TextField txtNombre;
    @FXML private CheckBox chkActivo;
    @FXML private TableView<Categoria> tblCategorias;
    @FXML private TableColumn<Categoria, Integer> colId;
    @FXML private TableColumn<Categoria, String> colNombre;
    @FXML private TableColumn<Categoria, Boolean> colActivo;

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ObservableList<Categoria> listaCategorias = FXCollections.observableArrayList();
    private Categoria categoriaSeleccionada;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        tblCategorias.setItems(listaCategorias);
        cargarCategorias();

        tblCategorias.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                categoriaSeleccionada = newSelection;
                txtNombre.setText(newSelection.getNombre());
                chkActivo.setSelected(newSelection.isActiva());
            }
        });
    }

    private void cargarCategorias() {
        listaCategorias.clear();
        listaCategorias.addAll(categoriaDAO.listar());
    }

    @FXML
    private void guardar() {
        if (txtNombre.getText().isBlank()) {
            mensaje(Alert.AlertType.WARNING, "Ingrese el nombre de la categoría.");
            return;
        }

        Categoria nueva = new Categoria(0, txtNombre.getText().trim(), chkActivo.isSelected());
        if (categoriaDAO.guardar(nueva)) {
            mensaje(Alert.AlertType.INFORMATION, "Categoría guardada con éxito.");
            cargarCategorias();
            limpiar();
        } else {
            mensaje(Alert.AlertType.ERROR, "Error al guardar la categoría.");
        }
    }

    @FXML
    private void actualizar() {
        if (categoriaSeleccionada == null) {
            mensaje(Alert.AlertType.WARNING, "Seleccione una categoría de la tabla.");
            return;
        }

        // VALIDACIÓN CAMPO OBLIGATORIO (Agregado)
        if (txtNombre.getText().isBlank()) {
            mensaje(Alert.AlertType.WARNING, "El nombre de la categoría no puede estar vacío.");
            return;
        }

        categoriaSeleccionada.setNombre(txtNombre.getText().trim());
        categoriaSeleccionada.setActiva(chkActivo.isSelected());

        if (categoriaDAO.actualizar(categoriaSeleccionada)) {
            mensaje(Alert.AlertType.INFORMATION, "Categoría actualizada con éxito.");
            cargarCategorias();
            limpiar();
        } else {
            mensaje(Alert.AlertType.ERROR, "Error al actualizar la categoría.");
        }
    }

    @FXML
    private void eliminar() {
        if (categoriaSeleccionada == null) {
            mensaje(Alert.AlertType.WARNING, "Seleccione una categoría de la tabla.");
            return;
        }

        // VALIDACIÓN PREVIA DE INTEGRIDAD REFERENCIAL (Agregado)
        if (categoriaDAO.tieneProductosAsociados(categoriaSeleccionada.getId())) {
            mensaje(Alert.AlertType.WARNING, "No se puede eliminar la categoría porque tiene productos asociados.");
            return;
        }

        if (categoriaDAO.eliminar(categoriaSeleccionada.getId())) {
            mensaje(Alert.AlertType.INFORMATION, "Categoría eliminada con éxito.");
            cargarCategorias();
            limpiar();
        } else {
            mensaje(Alert.AlertType.ERROR, "Error en la base de datos al intentar eliminar.");
        }
    }

    @FXML
    private void limpiar() {
        txtNombre.clear();
        chkActivo.setSelected(true);
        categoriaSeleccionada = null;
        tblCategorias.getSelectionModel().clearSelection();
    }

    private void mensaje(Alert.AlertType tipo, String texto) {
        new Alert(tipo, texto, ButtonType.OK).showAndWait();
    }
}
