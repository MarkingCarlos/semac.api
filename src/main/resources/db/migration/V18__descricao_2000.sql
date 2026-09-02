-- Aumenta o limite da descrição do evento (palestra) e do palestrante para 2000 caracteres.
ALTER TABLE public.evento ALTER COLUMN descricao TYPE character varying(2000);
ALTER TABLE public.palestrante ALTER COLUMN descricao TYPE character varying(2000);
