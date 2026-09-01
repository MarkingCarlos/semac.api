package com.semac.java_api.repository.projection;

import com.semac.java_api.model.enums.Role;

/* Quantas camisetas existem por combinação de avulsa (true/false) e role da
   pessoa. Base do relatório de camisetas — soma-se essas linhas para chegar
   em dadas/avulsas e comissão/participantes. */
public interface ContagemCamisetaGrupoView {
    Boolean getAvulsa();
    Role getRole();
    Long getTotal();
}
