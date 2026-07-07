package com.sistemaGestion.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cotizaciones")
@Data
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clienteNombre;

    private String clienteDocumento;

    private LocalDateTime fecha = LocalDateTime.now();

    @Column(nullable = false)
    private Double total = 0.0;

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("cotizacion") // Corta el bucle con el hijo
    private List<DetalleCotizacion> detalles = new ArrayList<>();
}