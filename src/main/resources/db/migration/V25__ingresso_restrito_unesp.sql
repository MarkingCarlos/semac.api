-- Ingressos exclusivos para alunos da UNESP (ex.: desconto de permanência,
-- comissão). `restrito_unesp` marca o tipo de ingresso; `eh_unesp` passa a
-- guardar, na própria Pessoa, a resposta que ela deu no cadastro (antes
-- descartada depois de validar RA/e-mail — ver InscricaoService).

ALTER TABLE public.tipo_inscricao ADD COLUMN restrito_unesp BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE public.pessoa ADD COLUMN eh_unesp BOOLEAN NOT NULL DEFAULT FALSE;
