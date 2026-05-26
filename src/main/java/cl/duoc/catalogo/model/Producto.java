package cl.duoc.catalogo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Data
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private BigDecimal precio;
    private Integer stock;
    private Boolean disponibilidad;
    private Long vendedorId;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}
