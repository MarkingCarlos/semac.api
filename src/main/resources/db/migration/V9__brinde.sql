-- Brindes sorteados durante o evento (nome + quantidade em estoque,
-- gerenciados na aba "Brindes" do /admin). `sorteio.brinde_id` liga cada
-- sorteio realizado ao brinde entregue; a quantidade já entregue de um
-- brinde é calculada contando os sorteios vinculados a ele (não há coluna
-- acumuladora — mesmo padrão de saldo/pontuação usado no resto do banco).

CREATE TABLE public.brinde (
    id         SERIAL PRIMARY KEY,
    nome       VARCHAR(255) NOT NULL,
    quantidade INT NOT NULL
);

ALTER TABLE public.sorteio ADD COLUMN brinde_id INT;
ALTER TABLE public.sorteio ADD CONSTRAINT fk_sorteio_brinde FOREIGN KEY (brinde_id) REFERENCES public.brinde(id);
