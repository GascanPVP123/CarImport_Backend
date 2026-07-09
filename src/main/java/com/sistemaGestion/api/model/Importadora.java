package com.sistemaGestion.api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "importadoras")
@Data


public class Importadora {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ruc;

    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    private String telefono;

}
