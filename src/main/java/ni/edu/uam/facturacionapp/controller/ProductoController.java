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

    // 17. Construir un método de validación para Producto
    private Producto obtenerProductoFormulario() {
        String codigo = txtCodigo.getText() != null ? txtCodigo.getText().trim() : "";
        String nombre