package com.semac.java_api.config;

import java.util.List;
import java.util.Map;

/* Fonte única de verdade das mensagens automáticas: quais existem, que
   variáveis cada uma aceita e qual é o texto padrão.

   Mesma divisão de propriedade do CatalogoConquistas: o código é dono da
   `chave` e da lista de variáveis (só ele sabe quais dados existem na hora
   do envio); o /admin é dono do assunto e do corpo. Depois da primeira
   inserção o SeedModeloEmailRunner nunca sobrescreve o texto — senão todo
   deploy desfaria o que a comissão escreveu.

   Para criar uma mensagem automática nova: adicione uma entrada aqui e
   dispare o envio no ponto do código onde o acontecimento ocorre. O texto
   em si, depois disso, é editável sem deploy. */
public final class CatalogoVariaveisEmail {

    private CatalogoVariaveisEmail() {
    }

    /* `exemplo` alimenta a prévia do /admin: é o valor que aparece no lugar
       da variável antes de a mensagem ir para alguém de verdade. */
    public record VariavelEmail(String nome, String descricao, String exemplo) {
    }

    public record ModeloSemeado(String chave,
                                String nomeExibicao,
                                String descricao,
                                String assuntoPadrao,
                                String corpoPadrao,
                                List<VariavelEmail> variaveis) {
    }

    public static final String INSCRICAO_CONFIRMADA = "INSCRICAO_CONFIRMADA";
    public static final String RECUPERACAO_SENHA = "RECUPERACAO_SENHA";

    private static final List<ModeloSemeado> MODELOS = List.of(
            new ModeloSemeado(
                    INSCRICAO_CONFIRMADA,
                    "Inscrição confirmada",
                    "Enviada quando a comissão confirma a inscrição de alguém no /admin. "
                            + "Cada pessoa recebe uma única vez — reconfirmar não reenvia.",
                    "Sua inscrição na SEMAC está confirmada!",
                    """
                    Olá, **{{nomeParticipante}}**!

                    Sua inscrição na SEMAC foi **confirmada**. Já pode acessar a área do \
                    participante para ver a programação, escolher seus minicursos e \
                    acompanhar suas conquistas.

                    - Ingresso: {{nomeIngresso}}
                    - Valor: {{valorIngresso}}
                    - Diárias: {{diasInscricao}}

                    [Acessar área do participante]({{urlAreaParticipante}})

                    > Guarde este e-mail: ele confirma sua inscrição. Qualquer dúvida, \
                    é só responder esta mensagem.
                    """,
                    List.of(
                            new VariavelEmail("nomeParticipante", "Nome de quem recebe", "Maria Souza"),
                            new VariavelEmail("nomeIngresso", "Tipo de ingresso da pessoa", "Inteira"),
                            new VariavelEmail("valorIngresso", "Valor pago, já formatado", "R$ 45,00"),
                            new VariavelEmail("diasInscricao", "Quantidade de diárias", "5"),
                            new VariavelEmail("urlAreaParticipante", "Link da área do participante",
                                    "https://semac.cc/participantes"))),
            new ModeloSemeado(
                    RECUPERACAO_SENHA,
                    "Recuperação de senha",
                    "Enviada quando alguém pede para recuperar a senha na tela de login. "
                            + "Leva o código de 5 dígitos. Desligar esta mensagem não a impede de sair: "
                            + "sem ela ninguém recupera a senha, então o texto padrão é usado no lugar.",
                    "Seu código para recuperar a senha da SEMAC",
                    """
                    Olá, **{{nomeParticipante}}**!

                    Recebemos um pedido para recuperar a senha da sua conta na SEMAC. \
                    Use o código abaixo na tela de login:

                    ## {{codigo}}

                    O código vale por **{{validadeMinutos}} minutos** e só pode ser usado uma vez.

                    > Não pediu para trocar a senha? Pode ignorar este e-mail — sua senha \
                    continua a mesma.
                    """,
                    List.of(
                            new VariavelEmail("nomeParticipante", "Nome de quem recebe", "Maria Souza"),
                            new VariavelEmail("codigo", "Código de 5 dígitos", "48213"),
                            new VariavelEmail("validadeMinutos", "Por quantos minutos o código vale", "15"))));

    /* Variáveis de um comunicado avulso (/admin -> Comunicados). Curta de
       propósito: um comunicado é escrito para um público inteiro, então só
       faz sentido personalizar o que existe para qualquer destinatário.
       NÃO entra em MODELOS — comunicado não é mensagem automática e não
       deve aparecer como texto editável em /admin -> Mensagens. */
    public static final List<VariavelEmail> VARIAVEIS_COMUNICADO = List.of(
            new VariavelEmail("nomeParticipante", "Nome de quem recebe", "Maria Souza"),
            new VariavelEmail("urlAreaParticipante", "Link da área do participante",
                    "https://semac.cc/participantes"));

    private static final Map<String, ModeloSemeado> POR_CHAVE =
            MODELOS.stream().collect(java.util.stream.Collectors.toMap(ModeloSemeado::chave, m -> m));

    public static List<ModeloSemeado> todos() {
        return MODELOS;
    }

    public static ModeloSemeado porChave(String chave) {
        return POR_CHAVE.get(chave);
    }

    public static boolean existe(String chave) {
        return POR_CHAVE.containsKey(chave);
    }
}
