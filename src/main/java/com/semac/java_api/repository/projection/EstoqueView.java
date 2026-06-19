package com.semac.java_api.repository.projection;

import com.semac.java_api.model.enums.Modelo;
import com.semac.java_api.model.enums.Tamanho;

public interface EstoqueView {
    Tamanho getTamanho();
    Modelo getModelo();
    Long getTotal();
}