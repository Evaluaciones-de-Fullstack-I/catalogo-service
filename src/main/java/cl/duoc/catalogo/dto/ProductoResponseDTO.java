package cl.duoc.catalogo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoResponseDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private Integer stock;
    private Boolean disponibilidad;
    private Long vendedorId;
    private String nombreCategoria;
}