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
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activa"));

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

    // Método de alerta con firma idéntica al ejemplo de la guía
    private void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarExito(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // Validaciones de nombre antes de guardar o actualizar
    private boolean validarCamposNombre(Integer idActual) {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";

        if (nombre.isEmpty()) {
            mostrarAdvertencia("Validación", "El nombre es obligatorio.");
            txtNombre.requestFocus();
            return false;
        }

        boolean existe = (idActual == null)
                ? categoriaDAO.existeNombre(nombre)
                : categoriaDAO.existeNombre(nombre, idActual);

        if (existe) {
            mostrarAdvertencia("Validación", "Ya existe una categoría con el nombre '" + nombre + "'.");
            txtNombre.requestFocus();
            return false;
        }

        return true;
    }

    @FXML
    private void guardar() {
        if (!validarCamposNombre(null)) {
            return;
        }

        Categoria nueva = new Categoria(0, txtNombre.getText().trim(), chkActivo.isSelected());
        if (categoriaDAO.guardar(nueva)) {
            mostrarExito("Éxito", "Categoría guardada con éxito.");
            cargarCategorias();
            limpiarFormulario();
        } else {
            mostrarError("Error SQL", "No fue posible completar la operación.");
        }
    }

    // 6. Validación para actualizar Categoria
    @FXML
    private void actualizar() {
        Categoria seleccionada = tblCategorias.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarAdvertencia(
                    "Selección requerida",
                    "Debe seleccionar la categoría que desea actualizar."
            );
            return;
        }

        if (!validarCamposNombre(seleccionada.getId())) {
            return;
        }

        seleccionada.setNombre(txtNombre.getText().trim());
        seleccionada.setActiva(chkActivo.isSelected());

        if (categoriaDAO.actualizar(seleccionada)) {
            mostrarExito("Éxito", "Categoría actualizada con éxito.");
            cargarCategorias();
            limpiarFormulario();
        } else {
            mostrarError("Error SQL", "No fue posible completar la operación.");
        }
    }

    // 7. Validar eliminación de Categoria
    @FXML
    private void eliminar() {
        Categoria seleccionada = tblCategorias.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarAdvertencia("Selección requerida", "Debe seleccionar una categoría.");
            return;
        }

        // Categoría con productos
        if (categoriaDAO.tieneProductos(seleccionada.getId())) {
            mostrarAdvertencia(
                    "Categoría con productos",
                    "No puede eliminar la categoría porque tiene productos asociados."
            );
            return;
        }

        // Confirmación y eliminación
        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Está seguro de eliminar la categoría '" + seleccionada.getNombre() + "'?",
                ButtonType.YES, ButtonType.NO
        );

        if (confirmacion.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            if (categoriaDAO.eliminar(seleccionada.getId())) {
                cargarCategorias();
                limpiarFormulario();
                mostrarExito("Éxito", "Categoría eliminada correctamente.");
            } else {
                // Error SQL
                mostrarError("Error SQL", "No fue posible completar la operación.");
            }
        }
    }

    @FXML
    private void limpiarFormulario() {
        txtNombre.clear();
        chkActivo.setSelected(true);
        categoriaSeleccionada = null;
        tblCategorias.getSelectionModel().clearSelection();
    }

    // Alias para mantener compatibilidad si el FXML lo invoca como "limpiar"
    @FXML
    private void limpiar() {
        limpiarFormulario();
    }
}