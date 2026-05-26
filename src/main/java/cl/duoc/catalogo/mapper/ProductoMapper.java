package cl.duoc.catalogo.mapper;

import cl.duoc.catalogo.dto.ProductoRequestDTO;
import cl.duoc.catalogo.dto.ProductoResponseDTO;
import cl.duoc.catalogo.model.Categoria;
import cl.duoc.catalogo.model.Producto;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public Producto toEntity(ProductoRequestDTO dto, Categoria categoria) {
        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setPrecio(dto.getPrecio());
        producto.setStock(dto.getStock());
        producto.setDisponibilidad(dto.getStock() > 0);
        producto.setVendedorId(dto.getVendedorId());
        producto.setCategoria(categoria);
        return producto;
    }

    public ProductoResponseDTO toDto(Producto producto) {
        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setPrecio(producto.getPrecio());
        dto.setStock(producto.getStock());
        dto.setDisponibilidad(producto.getDisponibilidad());
        dto.setVendedorId(producto.getVendedorId());
        if (producto.getCategoria() != null) {
            dto.setNombreCategoria(producto.getCategoria().getNombre());
        }
        return dto;
    }
}