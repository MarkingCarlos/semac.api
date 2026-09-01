package com.semac.java_api.repository.projection;

/* Quantas camisetas uma pessoa já pediu e quantas o ingresso dela dá
   direito de graça (null quando ela não tem ingresso vinculado — caso da
   comissão, cujo ingresso é zerado ao ser confirmada). Usado pelo
   relatório de camisetas para separar "dadas" de "avulsas". */
public interface ContagemCamisetaPessoaView {
    Long getTotalCamisetas();
    Integer getCamisetasGratis();
}
