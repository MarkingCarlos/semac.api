package com.semac.java_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/* Marca (entregue=true) ou desfaz (entregue=false) a entrega do kit.
   Quem entregou sai do token, nunca do corpo.
   `uuid` é o QR code lido do crachá no celular: opcional (o desktop e a
   comissão marcam sem ler), mas quando vem precisa ser da própria pessoa
   — a conferência é no backend, porque a lista de kits não expõe o uuid.
   `justificativaSemQr` é o motivo digitado quando o celular não conseguiu
   ler o QR e entregou mesmo assim; ignorada se vier junto com o uuid. */
public record KitEntregueRequestDTO(@NotNull Boolean entregue,
                                    String uuid,
                                    @Size(max = 255) String justificativaSemQr) {}
