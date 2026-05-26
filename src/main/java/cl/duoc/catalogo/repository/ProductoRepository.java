package cl.duoc.catalogo.repository;

import cl.duoc.catalogo.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    // Uso de @Query personalizado (Exigencia del profesor)
    @Query("SELECT p FROM Producto p WHERE p.stock >= :stockMinimo AND p.disponibilidad = true")
    List<Producto> findProductosDisponibles(@Param("stockMinimo") Integer stockMinimo);
}