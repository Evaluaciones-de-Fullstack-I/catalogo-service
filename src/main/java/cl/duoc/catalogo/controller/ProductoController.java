package cl.duoc.catalogo.controller;

import cl.duoc.catalogo.dto.ProductoRequestDTO;
import cl.duoc.catalogo.dto.ProductoResponseDTO;
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
//import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoMapper productoMapper;

    //@Autowired
    //private WebClient.Builder webClientBuilder;

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
    public ResponseEntity<ProductoResponseDTO> crearProducto(@Valid @RequestBody ProductoRequestDTO requestDTO) {
        
        // 1. Validar Categoría
        Categoria categoria = categoriaRepository.findById(requestDTO.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + requestDTO.getCategoriaId()));

        /* 2. USO DE WEBCLIENT (Ejemplo de comunicación con el MS de Elizabeth)
         Descomenta esto cuando el MS de Elizabeth (ej. puerto 8082) esté listo.
         
         Boolean vendedorExiste = webClientBuilder.build().get()
                .uri("http://localhost:8082/api/vendedores/verificar/" + requestDTO.getVendedorId())
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();
                
         if (Boolean.FALSE.equals(vendedorExiste)) {
             throw new ResourceNotFoundException("El vendedor no está registrado o aprobado");
         }
        */

        // 3. Convertir DTO a Entidad usando el Mapper y Guardar
        Producto producto = productoMapper.toEntity(requestDTO, categoria);
        Producto productoGuardado = productoRepository.save(producto);

        // 4. Retornar DTO con código 201 CREATED
        return new ResponseEntity<>(productoMapper.toDto(productoGuardado), HttpStatus.CREATED);
    }
}