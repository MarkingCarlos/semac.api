-- Brinde da comissao (26/10): cada pessoa da comissao tem um QR proprio,
-- de leitura unica, mostrado no /admin e lido no /checkin por quem
-- entrega. `codigo` e aleatorio e nao e o uuid do cracha, para um QR nao
-- servir no lugar do outro. Uma linha por pessoa, criada na primeira vez
-- que ela abre o proprio QR.
CREATE TABLE public.brinde_comissao (
    pessoa_id       INT          PRIMARY KEY REFERENCES public.pessoa (id) ON DELETE CASCADE,
    codigo          VARCHAR(64)  NOT NULL UNIQUE,
    retirado_em     TIMESTAMP,
    entregue_por_id INT          REFERENCES public.pessoa (id) ON DELETE SET NULL
);
