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

    // Método auxiliar de alerta tal como en la imagen del profesor
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // Método de validación de campos según las reglas especificadas
    private boolean validarCategoria(Integer idActual) {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";

        // 1 y 2. El nombre no puede estar vacío ni contener únicamente espacios
        if (nombre.isEmpty()) {
            mostrarError("Validación", "El nombre de la categoría es obligatorio.");
            txtNombre.requestFocus();
            return false;
        }

        // 3. No deberán existir categorías con el mismo nombre
        if (categoriaDAO.existeNombre(nombre, idActual)) {
            mostrarError("Validación", "Ya existe una categoría con el nombre '" + nombre + "'.");
            txtNombre.requestFocus();
            return false;
        }

        return true;
    }

    @FXML
    private void guardar() {
        // Aplica validaciones 1, 2 y 3
        if (!validarCategoria(null)) {
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
        // 4. Para actualizar debe existir una categoría seleccionada
        if (categoriaSeleccionada == null) {
            mostrarError("Validación", "Debe seleccionar una categoría de la tabla para actualizar.");
            return;
        }

        // Aplica validaciones 1, 2 y 3 (excluyendo la propia categoría al verificar duplicado)
        if (!validarCategoria(categoriaSeleccionada.getId())) {
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
        // 5. Para eliminar debe existir una categoría seleccionada
        if (categoriaSeleccionada == null) {
            mostrarError("Validación", "Debe seleccionar una categoría de la tabla para eliminar.");
            return;
        }

        // Validación de integridad referencial
        if (categoriaDAO.tieneProductosAsociados(categoriaSeleccionada.getId())) {
            mostrarError("Integridad Referencial", "No se puede eliminar la categoría porque tiene productos asociados.");
            return;
        }

        if (categoriaDAO.eliminar(categoriaSeleccionada.getId())) {
            mensaje(Alert.AlertType.INFORMATION, "Categoría eliminada con éxito.");
            cargarCategorias();
            limpiar();
        } else {
            mensaje(Alert.AlertType.ERROR, "Error al eliminar la categoría.");
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
