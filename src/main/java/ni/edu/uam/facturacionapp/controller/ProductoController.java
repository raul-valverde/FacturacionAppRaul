package ni.edu.uam.facturacionapp.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ni.edu.uam.facturacionapp.dao.CategoriaDAO;
import ni.edu.uam.facturacionapp.dao.ProductoDAO;
import ni.edu.uam.facturacionapp.model.Categoria;
import ni.edu.uam.facturacionapp.model.Producto;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoController {

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private ComboBox<Categoria> cmbFiltroCategoria;

    @FXML private TextField txtCodigo, txtNombre, txtPrecio, txtExistencia;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private CheckBox chkActivo;
    @FXML private ImageView imgProducto;
    @FXML private TableView<Producto> tblProductos;

    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, Categoria> colCategoria;
    @FXML private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML private TableColumn<Producto, Integer> colExistencia;
    @FXML private TableColumn<Producto, Boolean> colActivo;

    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private FilteredList<Producto> productosFiltrados;
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    private String rutaImagen;
    private Producto productoSeleccionado = null;

    @FXML
    private void initialize() {
        // Configuración de las columnas del TableView
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colExistencia.setCellValueFactory(new PropertyValueFactory<>("existencia"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        // Cargar Categorías reales desde la Base de Datos
        List<Categoria> listaCategorias = categoriaDAO.listar();
        cmbCategoria.setItems(FXCollections.observableArrayList(listaCategorias));

        // Cargar Filtros de Categoría y Estado
        List<Categoria> categoriasFiltro = new ArrayList<>();
        Categoria catTodas = new Categoria(null, "Todas", true);
        categoriasFiltro.add(catTodas);
        categoriasFiltro.addAll(listaCategorias);
        cmbFiltroCategoria.setItems(FXCollections.observableArrayList(categoriasFiltro));
        cmbFiltroCategoria.setValue(catTodas);

        cmbFiltroEstado.setItems(FXCollections.observableArrayList("Todos", "Activos", "Inactivos"));
        cmbFiltroEstado.setValue("Todos");

        // Cargar Datos en TableView y configurar FilteredList
        productos.addAll(productoDAO.listar());
        productosFiltrados = new FilteredList<>(productos, p -> true);
        tblProductos.setItems(productosFiltrados);

        chkActivo.setSelected(true);

        // Escuchadores de Búsqueda y Filtros
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        cmbFiltroEstado.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        cmbFiltroCategoria.valueProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());

        // Escuchador al seleccionar una fila de la tabla
        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                productoSeleccionado = newSelection;
                cargarDatosEnFormulario(newSelection);
            }
        });
    }

    private void aplicarFiltros() {
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().trim().toLowerCase() : "";
        String estado = cmbFiltroEstado.getValue();
        Categoria categoriaFiltro = cmbFiltroCategoria.getValue();

        productosFiltrados.setPredicate(p -> {
            // 1. Filtro por Búsqueda (Código, Nombre o Categoría)
            boolean coincideTexto = texto.isEmpty()
                    || (p.getCodigo() != null && p.getCodigo().toLowerCase().contains(texto))
                    || (p.getNombre() != null && p.getNombre().toLowerCase().contains(texto))
                    || (p.getCategoria() != null && p.getCategoria().getNombre().toLowerCase().contains(texto));

            // 2. Filtro por Estado (Activos / Inactivos)
            boolean coincideEstado = true;
            if ("Activos".equals(estado)) {
                coincideEstado = p.isActivo();
            } else if ("Inactivos".equals(estado)) {
                coincideEstado = !p.isActivo();
            }

            // 3. Filtro por Categoría
            boolean coincideCategoria = true;
            if (categoriaFiltro != null && categoriaFiltro.getId() != null) {
                coincideCategoria = p.getCategoria() != null && p.getCategoria().getId().equals(categoriaFiltro.getId());
            }

            return coincideTexto && coincideEstado && coincideCategoria;
        });
    }

    private void cargarDatosEnFormulario(Producto p) {
        txtCodigo.setText(p.getCodigo());
        txtNombre.setText(p.getNombre());
        txtPrecio.setText(p.getPrecioVenta() != null ? p.getPrecioVenta().toString() : "");
        txtExistencia.setText(String.valueOf(p.getExistencia()));
        chkActivo.setSelected(p.isActivo());
        cmbCategoria.setValue(p.getCategoria());

        rutaImagen = p.getRutaImagen();
        if (rutaImagen != null && !rutaImagen.isBlank()) {
            try {
                imgProducto.setImage(new Image(rutaImagen));
            } catch (Exception e) {
                imgProducto.setImage(null);
            }
        } else {
            imgProducto.setImage(null);
        }
    }

    @FXML
    private void guardar() {
        if (!validarCampos()) return;

        String codigo = txtCodigo.getText().trim();
        if (existeCodigoDuplicado(codigo, null)) {
            mensaje(Alert.AlertType.WARNING, "El código '" + codigo + "' ya existe. Ingrese otro.");
            return;
        }

        try {
            BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
            int existencia = Integer.parseInt(txtExistencia.getText().trim());

            Producto nuevoProducto = new Producto(
                    null,
                    codigo,
                    txtNombre.getText().trim(),
                    cmbCategoria.getValue(),
                    precio,
                    existencia,
                    rutaImagen,
                    chkActivo.isSelected()
            );

            if (productoDAO.guardar(nuevoProducto)) {
                productos.add(0, nuevoProducto);
                mensaje(Alert.AlertType.INFORMATION, "Producto guardado con éxito.");
                limpiarFormulario();
            } else {
                mensaje(Alert.AlertType.ERROR, "No se pudo guardar el producto en la base de datos.");
            }

        } catch (Exception e) {
            mensaje(Alert.AlertType.ERROR, "Error inesperado: " + e.getMessage());
        }
    }

    @FXML
    private void actualizar() {
        if (productoSeleccionado == null) {
            mensaje(Alert.AlertType.WARNING, "Seleccione un producto de la tabla para actualizar.");
            return;
        }

        if (!validarCampos()) return;

        String codigo = txtCodigo.getText().trim();
        if (existeCodigoDuplicado(codigo, productoSeleccionado.getId())) {
            mensaje(Alert.AlertType.WARNING, "El código '" + codigo + "' ya está registrado en otro producto.");
            return;
        }

        try {
            BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
            int existencia = Integer.parseInt(txtExistencia.getText().trim());

            productoSeleccionado.setCodigo(codigo);
            productoSeleccionado.setNombre(txtNombre.getText().trim());
            productoSeleccionado.setCategoria(cmbCategoria.getValue());
            productoSeleccionado.setPrecioVenta(precio);
            productoSeleccionado.setExistencia(existencia);
            productoSeleccionado.setRutaImagen(rutaImagen);
            productoSeleccionado.setActivo(chkActivo.isSelected());

            if (productoDAO.actualizar(productoSeleccionado)) {
                tblProductos.refresh();
                mensaje(Alert.AlertType.INFORMATION, "Producto actualizado correctamente.");
                limpiarFormulario();
            } else {
                mensaje(Alert.AlertType.ERROR, "No se pudo actualizar el producto en la base de datos.");
            }

        } catch (Exception e) {
            mensaje(Alert.AlertType.ERROR, "Error al actualizar: " + e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        if (productoSeleccionado == null) {
            mensaje(Alert.AlertType.WARNING, "Seleccione un producto de la tabla para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Está seguro de eliminar el producto '" + productoSeleccionado.getNombre() + "'?",
                ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent() && respuesta.get() == ButtonType.YES) {
            if (productoDAO.eliminar(productoSeleccionado.getId())) {
                productos.remove(productoSeleccionado);
                mensaje(Alert.AlertType.INFORMATION, "Producto eliminado correctamente.");
                limpiarFormulario();
            } else {
                mensaje(Alert.AlertType.ERROR, "No se pudo eliminar el producto de la base de datos.");
            }
        }
    }

    private boolean validarCampos() {
        if (txtCodigo.getText().isBlank() || txtNombre.getText().isBlank()
                || txtPrecio.getText().isBlank() || txtExistencia.getText().isBlank()
                || cmbCategoria.getValue() == null) {
            mensaje(Alert.AlertType.WARNING, "Todos los campos obligatorios deben estar llenos.");
            return false;
        }

        try {
            BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                mensaje(Alert.AlertType.WARNING, "El precio debe ser un número mayor que cero.");
                return false;
            }
        } catch (NumberFormatException e) {
            mensaje(Alert.AlertType.WARNING, "El precio debe ser un número válido.");
            return false;
        }

        try {
            int existencia = Integer.parseInt(txtExistencia.getText().trim());
            if (existencia < 0) {
                mensaje(Alert.AlertType.WARNING, "La existencia no puede ser negativa.");
                return false;
            }
        } catch (NumberFormatException e) {
            mensaje(Alert.AlertType.WARNING, "La existencia debe ser un número entero válido.");
            return false;
        }

        return true;
    }

    private boolean existeCodigoDuplicado(String codigo, Integer idActual) {
        // Consulta previa en Base de Datos (SQL)
        return productoDAO.existeCodigo(codigo, idActual);
    }

    @FXML
    private void seleccionarImagen() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File archivo = chooser.showOpenDialog(txtCodigo.getScene().getWindow());
        if (archivo != null) {
            rutaImagen = archivo.toURI().toString();
            imgProducto.setImage(new Image(rutaImagen));
        }
    }

    @FXML
    private void limpiarFormulario() {
        txtCodigo.clear();
        txtNombre.clear();
        txtPrecio.clear();
        txtExistencia.clear();
        cmbCategoria.getSelectionModel().clearSelection();
        chkActivo.setSelected(true);
        imgProducto.setImage(null);
        rutaImagen = null;
        productoSeleccionado = null;
        tblProductos.getSelectionModel().clearSelection();
    }

    @FXML
    private void cerrar() {
        ((Stage) txtCodigo.getScene().getWindow()).close();
    }

    private void mensaje(Alert.AlertType tipo, String texto) {
        new Alert(tipo, texto, ButtonType.OK).showAndWait();
    }
}