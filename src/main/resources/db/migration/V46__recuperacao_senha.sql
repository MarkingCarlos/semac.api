-- Recuperacao de senha por codigo enviado ao e-mail.
--
-- Uma linha por pessoa (pessoa_id UNIQUE): pedir um codigo novo
-- sobrescreve o anterior, entao so existe um codigo valido por vez.
--
-- O contador de tentativas erradas e o bloqueio ficam nesta mesma linha,
-- e NAO sao zerados quando um codigo novo e pedido -- senao bastaria pedir
-- outro codigo a cada 2 erros para contornar o limite de 3 tentativas.
-- Zeram quando a pessoa acerta o codigo ou quando o bloqueio termina.
--
-- Codigo e token de troca guardados so como hash SHA-256: quem ler o
-- banco nao consegue usar nenhum dos dois.

CREATE TABLE public.recuperacao_senha (
    id                      SERIAL PRIMARY KEY,
    pessoa_id               INTEGER NOT NULL UNIQUE
                            REFERENCES public.pessoa (id) ON DELETE CASCADE,

    -- Codigo de 5 digitos enviado por e-mail. Nulo depois de usado.
    codigo_hash             VARCHAR(64),
    codigo_enviado_em       timestamp without time zone,
    codigo_expira_em        timestamp without time zone,

    -- Anti forca bruta: 3 erros bloqueiam por 30 minutos.
    tentativas_erradas      INTEGER NOT NULL DEFAULT 0,
    bloqueado_ate           timestamp without time zone,

    -- Emitido quando o codigo e validado; autoriza UMA troca de senha.
    token_troca_hash        VARCHAR(64),
    token_troca_expira_em   timestamp without time zone
);
