package ni.edu.uam.facturacionapp.model;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {
    private Integer id;
    private String nombre;
    private boolean activa;

    @Override
    public String toString() { return nombre; }
}