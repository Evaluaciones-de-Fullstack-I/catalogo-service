package cl.duoc.catalogo.controller;

import cl.duoc.catalogo.dto.ProductoRequestDTO;
import cl.duoc.catalogo.dto.ProductoResponseDTO;
import cl.duoc.catalogo.dto.VendedorDTO; // <-- Agrega este import para tu nuevo DTO
import cl.duoc.catalogo.exception.ResourceNotFoundException;
import cl.duoc.catalogo.mapper.ProductoMapper;
import cl.duoc.catalogo.model.Categoria;
import cl.duoc.catalogo.model.Producto;
import cl.duoc.catalogo.repository.CategoriaRepository;
import cl.duoc.catalogo.repository.ProductoRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient; // <-- Descomenta este import
import org.springframework.web.reactive.function.client.WebClientResponseException; // <-- Agrega este import para manejar errores 404

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/productos")
@SuppressWarnings("null")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoMapper productoMapper;

    @Autowired // <-- Descomenta esto
    private WebClient.Builder webClientBuilder; // <-- Descomenta esto

    // Uso de @RequestParam
    @GetMapping("/disponibles")
    public ResponseEntity<List<ProductoResponseDTO>> obtenerDisponibles(
            @RequestParam(defaultValue = "1") Integer stockMinimo) {
        
        List<Producto> productos = productoRepository.findProductosDisponibles(stockMinimo);
        List<ProductoResponseDTO> response = productos.stream()
                .map(productoMapper::toDto)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }

    // Uso de @PathVariable
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerPorId(@PathVariable Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con el ID: " + id));
        
        return ResponseEntity.ok(productoMapper.toDto(producto));
    }

    // Uso de @Valid, ResponseEntity y WebClient
    @PostMapping
    public ResponseEntity<?> crearProducto(@Valid @RequestBody ProductoRequestDTO requestDTO) { // <-- Cambiamos ProductoResponseDTO a ? para poder devolver mensajes de error
        
        // 1. Validar Categoría
        Categoria categoria = categoriaRepository.findById(requestDTO.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + requestDTO.getCategoriaId()));

        // 2. USO DE WEBCLIENT (Comunicación con el MS de Elizabeth)
        try {
            // Hacemos la llamada a la ruta real de Elizabeth en el puerto 8083
            VendedorDTO vendedor = webClientBuilder.build().get()
                    .uri("http://localhost:8083/api/v1/vendedores/" + requestDTO.getVendedorId())
                    .retrieve()
                    .bodyToMono(VendedorDTO.class)
                    .block();
                    
            // Validamos que el vendedor exista y su estado (ajusta "APROBADO" si ella usa otra palabra)
            if (vendedor == null || !"APROBADO".equalsIgnoreCase(vendedor.getEstado())) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("El vendedor no está autorizado o su estado no es APROBADO.");
            }
            
        } catch (WebClientResponseException.NotFound e) {
             // Si el API de Elizabeth devuelve un 404 (no encontró al vendedor)
             return ResponseEntity.status(HttpStatus.NOT_FOUND)
                     .body("Operación rechazada: El vendedor indicado no existe en el sistema.");
        } catch (Exception e) {
             // Por si el microservicio de Elizabeth está apagado
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body("Error de comunicación con el servicio de Vendedores. Intente más tarde.");
        }

        // 3. Convertir DTO a Entidad usando el Mapper y Guardar
        Producto producto = productoMapper.toEntity(requestDTO, categoria);
        Producto productoGuardado = productoRepository.save(producto);

        // 4. Retornar DTO con código 201 CREATED
        return new ResponseEntity<>(productoMapper.toDto(productoGuardado), HttpStatus.CREATED);
    }
}