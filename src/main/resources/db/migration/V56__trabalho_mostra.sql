-- Mostra Tecnica: os trabalhos expostos nos stands e as visitas dos
-- participantes. Cada stand tem um QR code impresso com o link
-- /mostra/<codigo_qr>; o participante le com a camera do celular e a visita
-- fica registrada. Quem visita todos os trabalhos do ano ganha a conquista
-- MOSTRA_COMPLETA (ver ConquistaService.avaliarMostraCompleta).
--
-- A presenca na Mostra NAO passa por aqui: continua sendo o check-in normal
-- da comissao na entrada (evento MOSTRA_TECNICA).

CREATE TABLE public.trabalho_mostra (
    id             SERIAL PRIMARY KEY,
    ano            INT          NOT NULL,
    titulo         VARCHAR(200) NOT NULL,
    -- Texto livre ("Ana Silva e Joao Souza"): quem apresenta nao precisa
    -- ter conta no sistema.
    apresentadores VARCHAR(300) NOT NULL,
    stand          VARCHAR(50),
    -- Aleatorio, e nao o id: com o id daria para chutar /mostra/1, /mostra/2...
    codigo_qr      VARCHAR(32)  NOT NULL UNIQUE,
    criado_em      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX trabalho_mostra_ano_idx ON public.trabalho_mostra (ano);

-- Uma visita por pessoa por trabalho -- e o UNIQUE que garante isso, entao
-- ler o mesmo QR de novo nao conta duas vezes. Remover o trabalho leva as
-- visitas junto.
CREATE TABLE public.visita_trabalho_mostra (
    id           SERIAL PRIMARY KEY,
    pessoa_id    INT       NOT NULL REFERENCES public.pessoa(id) ON DELETE CASCADE,
    trabalho_id  INT       NOT NULL REFERENCES public.trabalho_mostra(id) ON DELETE CASCADE,
    visitada_em  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT visita_trabalho_mostra_pessoa_trabalho_key UNIQUE (pessoa_id, trabalho_id)
);

CREATE INDEX visita_trabalho_mostra_trabalho_idx ON public.visita_trabalho_mostra (trabalho_id);
