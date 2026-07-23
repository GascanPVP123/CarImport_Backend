package com.sistemaGestion.api.dto;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductoDTO {

    private Long id;

    private String codigoSku;

    private String nombre;

    private String descripcion;


    private Integer stock;

    private BigDecimal precioCompra;

    private BigDecimal precioMenor;

    private BigDecimal precioMayor;

    private BigDecimal precioVenta;

    private String unidadMedida;

    private Long importadoraId;
}