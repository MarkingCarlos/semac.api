-- Remove a carga inicial que a V27 trouxe da planilha de previsao de
-- gastos. A comissao vai lancar os dados a mao.
--
-- Vem numa migration propria, e nao editando a V27, porque a V27 ja foi
-- aplicada -- mexer no arquivo quebraria a validacao de checksum do
-- Flyway em todo banco que ja a rodou (foi o que aconteceu na V29).
-- Em banco novo a V27 insere e esta aqui remove logo em seguida; o
-- resultado final e o mesmo, e nenhum ambiente precisa de `flyway repair`.
--
-- ATENCAO: a limpeza dos itens e das categorias nao e seletiva -- esvazia
-- as duas tabelas. Como a V27 foi a unica coisa que as populou ate aqui,
-- isso equivale a remover so a carga da planilha. Se alguem cadastrar
-- previsoes a mao ANTES desta migration rodar naquele banco, elas serao
-- removidas junto.

-- Itens primeiro: previsao_categoria e referenciada por previsao_item.
DELETE FROM public.previsao_item;
DELETE FROM public.previsao_categoria;

-- O teto e os contadores de escala tambem vieram da planilha. A linha da
-- edicao permanece (ano 2026), zerada, para a comissao preencher pela
-- aba -- assim o ano nao e recriado como o ano corrente no primeiro PUT.
UPDATE public.orcamento
   SET teto = 0,
       inscritos_previstos = 0,
       membros_comissao = 0,
       palestrantes_previstos = 0
 WHERE ano = 2026;

-- Fornecedores NAO sao apagados, de proposito.
--
-- A V27 semeia 8 nomes vindos da planilha (Zafer, Ariart, Jacques,
-- Shopee, Copfac, Affectio, Tia So, Bravo City Hotel) com um INSERT
-- guardado por nome: se o fornecedor ja existir, ela nao o recria. Mas
-- depois disso nao ha como distinguir "criado pela V27" de "ja existia":
-- nao existe marcador nem data de criacao em `fornecedor`.
--
-- Uma exclusao por nome apagaria fornecedores reais da comissao que por
-- acaso tenham o mesmo nome e ainda nao tenham compra lancada -- e sao
-- fornecedores reais, justamente por isso estao na planilha. Verificado
-- em simulacao: um 'Ariart' pre-existente, com contato preenchido e sem
-- compra, era apagado com o contato junto.
--
-- Entao os nomes semeados podem sobrar na lista. Removê-los pela aba
-- Fornecedores e trivial; recuperar um cadastro apagado por migration
-- nao e.
