package com.semac.java_api.model.enums;

/* Como uma conquista é obtida.

   AUTOMATICA — o próprio sistema avalia a regra (ver ConquistaService) e
   concede sozinho. A pessoa não faz nada além de cumprir o critério.

   MANUAL — não há regra avaliável: alguém da diretoria confere no mundo
   físico (o cartaz carimbado, por exemplo) e concede lendo o QR code do
   crachá no /checkin. */
public enum TipoValidacaoConquista {
    AUTOMATICA,
    MANUAL
}
