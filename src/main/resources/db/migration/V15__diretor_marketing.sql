-- Adiciona o papel DIRETOR_MARKETING ao enum Role (pessoa.role).
ALTER TABLE public.pessoa DROP CONSTRAINT pessoa_role_check;

ALTER TABLE public.pessoa ADD CONSTRAINT pessoa_role_check CHECK (((role)::text = ANY ((ARRAY[
    'PARTICIPANTE'::character varying,
    'MEMBRO'::character varying,
    'DIRETOR_SITE'::character varying,
    'DIRETOR_CONTEUDO'::character varying,
    'DIRETOR_PATROCINIO'::character varying,
    'DIRETOR_APOIO'::character varying,
    'DIRETOR_MARKETING'::character varying,
    'PRESIDENTE'::character varying
])::text[])));
