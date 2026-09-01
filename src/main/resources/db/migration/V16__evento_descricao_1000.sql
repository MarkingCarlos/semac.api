-- Aumenta o limite da descrição do evento de 255 para 1000 caracteres.
ALTER TABLE public.evento ALTER COLUMN descricao TYPE character varying(1000);
