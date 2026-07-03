package cl.duoc.catalogo.controller;

import cl.duoc.catalogo.dto.ProductoRequestDTO;
import cl.duoc.catalogo.dto.ProductoResponseDTO;
import cl.duoc.catalogo.dto.VendedorDTO;
import cl.duoc.catalogo.exception.ResourceNotFoundException;
import cl.duoc.catalogo.mapper.ProductoMapper;
import cl.duoc.catalogo.model.Categoria;
import cl.duoc.catalogo.model.Producto;
import cl.duoc.catalogo.repository.CategoriaRepository;
import cl.duoc.catalogo.repository.ProductoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/productos")
@SuppressWarnings("null")
@Tag(name = "Productos", description = "Controlador para la gestión del catálogo de productos y validación con microservicios externos")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoMapper productoMapper;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @GetMapping("/disponibles")
    @Operation(
        summary = "Obtener productos disponibles",
        description = "Retorna una lista de productos que cumplen con un stock mínimo especificado.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Lista de productos obtenida exitosamente"
            )
        }
    )
    public ResponseEntity<List<ProductoResponseDTO>> obtenerDisponibles(
            @RequestParam(defaultValue = "1") Integer stockMinimo) {
        
        List<Producto> productos = productoRepository.findProductosDisponibles(stockMinimo);
        List<ProductoResponseDTO> response = productos.stream()
                .map(productoMapper::toDto)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Buscar producto por ID",
        description = "Busca y retorna los detalles de un producto en base a su identificador único.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Producto encontrado exitosamente"
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "El producto con el ID entregado no existe",
                content = @Content(schema = @Schema(implementation = String.class))
            )
        }
    )
    public ResponseEntity<ProductoResponseDTO> obtenerPorId(@PathVariable Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con el ID: " + id));
        
        return ResponseEntity.ok(productoMapper.toDto(producto));
    }

    @PostMapping
    @Operation(
        summary = "Crear un nuevo producto",
        description = "Registra un producto asociándolo a una categoría interna y validando la existencia del vendedor a través de un microservicio externo por WebClient.",
        responses = {
            @ApiResponse(
                responseCode = "201", 
                description = "Producto registrado de manera exitosa"
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "Datos de entrada inválidos o faltantes"
            ),
            @ApiResponse(
                responseCode = "403", 
                description = "Operación rechazada: El vendedor no tiene estado APROBADO"
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "La categoría indicada o el vendedor no existen"
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "Error de comunicación con el servicio de Vendedores"
            )
        }
    )
    public ResponseEntity<?> crearProducto(
            @Valid @RequestBody(
                description = "Estructura JSON necesaria para registrar un producto en el catálogo",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductoRequestDTO.class),
                    examples = @ExampleObject(
                        name = "Ejemplo de Creación de Producto",
                        value = "{\n  \"nombre\": \"Audífonos Bluetooth\",\n  \"descripcion\": \"Audífonos inalámbricos con cancelación de ruido\",\n  \"precio\": 49990,\n  \"stock\": 25,\n  \"categoriaId\": 2,\n  \"vendedorId\": 105\n}"
                    )
                )
            )
            @org.springframework.web.bind.annotation.RequestBody ProductoRequestDTO requestDTO) { 
        
        // 1. Validar Categoría
        Categoria categoria = categoriaRepository.findById(requestDTO.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + requestDTO.getCategoriaId()));

        // 2. USO DE WEBCLIENT (Comunicación con el MS de Elizabeth)
        try {
            VendedorDTO vendedor = webClientBuilder.build().get()
                    .uri("http://localhost:8083/api/v1/vendedores/" + requestDTO.getVendedorId())
                    .retrieve()
                    .bodyToMono(VendedorDTO.class)
                    .block();
                    
            if (vendedor == null || !"APROBADO".equalsIgnoreCase(vendedor.getEstado())) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("El vendedor no está autorizado o su estado no es APROBADO.");
            }
            
        } catch (WebClientResponseException.NotFound e) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND)
                     .body("Operación rechazada: El vendedor indicado no existe en el sistema.");
        } catch (Exception e) {
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