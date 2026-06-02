package cl.duoc.catalogo.dto; // Ajusta esto según tu nombre de paquete

import lombok.Data;

@Data
public class VendedorDTO {
    private Integer id; // Usamos Integer tal como lo definió ella
    private String nombre;
    private String estado;
}