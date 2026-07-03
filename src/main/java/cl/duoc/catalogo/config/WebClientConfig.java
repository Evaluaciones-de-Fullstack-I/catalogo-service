package cl.duoc.catalogo.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced; // 👈 Agregado para Eureka
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    @LoadBalanced // 👈 Habilita la traducción automática de nombres de servicios
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}