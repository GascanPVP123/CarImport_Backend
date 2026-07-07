package com.sistemaGestion.api.model;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.Length;

import java.time.LocalDateTime;

@Data
@Table(name = "proveedores")
@Entity
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 11, unique = true )
    private String ruc;

    @Column(name = "nombre_empresa", nullable = false, length = 100)
    private String nombreEmpresa;

    @Column(name = "contacto_nombre", length = 100)
    private String contactoNombre;

    @Column(length = 20)
    private String telefono;

    @Column(name= "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }
}
