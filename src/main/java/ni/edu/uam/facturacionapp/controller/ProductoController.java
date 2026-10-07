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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoController {

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private ComboBox<Categoria> cmbFiltroCategoria;

    @FXML private TextField txtCodigo, txtNombre, txtPrecio, txtExistencia;
    @FXML private ComboBox<Categoria> cbCategoria; //Le elimino la m para seguir la guia
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
        cbCategoria.setItems(FXCollections.observableArrayList(listaCategorias));

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
        cargarProductos();

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

    private void cargarProductos() {
        productos.clear();
        productos.addAll(productoDAO.listar());
        productosFiltrados = new FilteredList<>(productos, p -> true);
        tblProductos.setItems(productosFiltrados);
    }

    private void aplicarFiltros() {
        String texto = txtBuscar.getText() != null ? txtBuscar.getText().trim().toLowerCase() : "";
        String estado = cmbFiltroEstado.getValue();
        Categoria categoriaFiltro = cmbFiltroCategoria.getValue();

        productosFiltrados.setPredicate(p -> {
            boolean coincideTexto = texto.isEmpty()
                    || (p.getCodigo() != null && p.getCodigo().toLowerCase().contains(texto))
                    || (p.getNombre() != null && p.getNombre().toLowerCase().contains(texto))
                    || (p.getCategoria() != null && p.getCategoria().getNombre().toLowerCase().contains(texto));

            boolean coincideEstado = true;
            if ("Activos".equals(estado)) {
                coincideEstado = p.isActivo();
            } else if ("Inactivos".equals(estado)) {
                coincideEstado = !p.isActivo();
            }

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
        cbCategoria.setValue(p.getCategoria());

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

    // Método de alerta para mostrar errores de validación según el diseño de la guía
    private void mostrarError(String titulo, String mensaje) {
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

    // 9. Validar campos obligatorios de Producto y reglas adicionales
    private boolean validarProducto(Integer idActual) {
        String codigo = txtCodigo.getText() != null ? txtCodigo.getText().trim() : "";
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";

        // Validaciones obligatorias exactamente según la guía
        if (codigo.isEmpty()) {
            mostrarError(
                    "Validación",
                    "El código del producto es obligatorio."
            );
            txtCodigo.requestFocus();
            return false;
        }

        // Verificación de código duplicado en BD
        boolean existe = (idActual == null)
                ? productoDAO.existeCodigo(codigo)
                : productoDAO.existeCodigo(codigo, idActual);

        if (existe) {
            mostrarError(
                    "Validación",
                    "El código de producto '" + codigo + "' ya se encuentra registrado."
            );
            txtCodigo.requestFocus();
            return false;
        }

        if (nombre.isEmpty()) {
            mostrarError(
                    "Validación",
                    "El nombre del producto es obligatorio."
            );
            txtNombre.requestFocus();
            return false;
        }

        Categoria categoria = cbCategoria.getSelectionModel().getSelectedItem();

        if (categoria == null) {
            mostrarError(
                    "Validación",
                    "Debe seleccionar una categoria."
            );
            cbCategoria.requestFocus();
            return false;
        }

        // Control de código duplicado
        boolean codigoExiste = (idActual == null)
                ? productoDAO.existeCodigo(codigo)
                : productoDAO.existeCodigo(codigo, idActual);

        if (codigoExiste) {
            mostrarError("Validación", "El código '" + codigo + "' ya está registrado en la base de datos.");
            txtCodigo.requestFocus();
            return false;
        }

        // Selección obligatoria de categoría
        if (cbCategoria.getValue() == null) {
            mostrarError("Validación", "Debe seleccionar una categoría.");
            cbCategoria.requestFocus();
            return false;
        }

        // Validación de precio de venta (numérico y > 0)
        try {
            BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());

            if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                mostrarError(
                        "Precio incorrecto",
                        "El precio de venta debe ser mayor que cero."
                );
                txtPrecio.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            mostrarError(
                    "Precio incorrecto",
                    "El precio debe contener únicamente valores numéricos."
            );
            txtPrecio.requestFocus();
            return false;
        }

        // Validación de existencia (entero no negativo)
        try {

            int existencia = Integer.parseInt(txtExistencia.getText().trim());

            if (existencia < 0) {
                mostrarError(
                        "Existencia incorrecta",
                        "La existencia no puede ser negativa."
                );
                txtExistencia.requestFocus();
                return false;
            }

        } catch (NumberFormatException e) {
            mostrarError(
                    "Existencia incorrecta",
                    "La existencia debe ser un número entero."
            );
            txtExistencia.requestFocus();
            return false;
        }

        return true;
    }

    @FXML
    private void guardar() {
        if (!validarProducto(null)) {
            return;
        }

        try {
            BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
            int existencia = Integer.parseInt(txtExistencia.getText().trim());

            Producto producto = new Producto(
                    null,
                    txtCodigo.getText().trim(),
                    txtNombre.getText().trim(),
                    cbCategoria.getValue(),
                    precio,
                    existencia,
                    rutaImagen,
                    chkActivo.isSelected()
            );

            // Operación DAO que lanza SQLException
            productoDAO.guardar(producto);

            mostrarExito(
                    "Producto registrado",
                    "El producto se guardó correctamente."
            );

            cargarProductos();
            limpiarFormulario();

        } catch (SQLException e) {
            mostrarError(
                    "Error de base de datos",
                    "No fue posible registrar el producto."
            );
            System.err.println(e.getMessage());
        }
    }

    @FXML
    private void actualizar() {
        Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarError("Seleccione un producto", "Debe seleccionar un producto de la tabla para actualizar.");
            return;
        }

        if (!validarProducto(seleccionado.getId())) {
            return;
        }

        try {
            BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
            int existencia = Integer.parseInt(txtExistencia.getText().trim());

            seleccionado.setCodigo(txtCodigo.getText().trim());
            seleccionado.setNombre(txtNombre.getText().trim());
            seleccionado.setCategoria(cbCategoria.getValue());
            seleccionado.setPrecioVenta(precio);
            seleccionado.setExistencia(existencia);
            seleccionado.setRutaImagen(rutaImagen);
            seleccionado.setActivo(chkActivo.isSelected());

            if (productoDAO.actualizar(seleccionado)) {
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
        Producto seleccionado = tblProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarError("Seleccione un producto", "Debe seleccionar un producto de la tabla para eliminar.");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Está seguro de eliminar el producto '" + seleccionado.getNombre() + "'?",
                ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent() && respuesta.get() == ButtonType.YES) {
            if (productoDAO.eliminar(seleccionado.getId())) {
                cargarProductos();
                mensaje(Alert.AlertType.INFORMATION, "Producto eliminado correctamente.");
                limpiarFormulario();
            } else {
                mensaje(Alert.AlertType.ERROR, "No se pudo eliminar el producto de la base de datos.");
            }
        }
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
        cbCategoria.getSelectionModel().clearSelection();
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