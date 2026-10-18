package com.semac.java_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/* Dicionário de palpites aceitos no Termo (/termo), de 5 a 10 letras,
   carregado uma vez no boot.

   São perto de 900 mil palavras. Num HashSet<String> isso passaria de 80
   MB de heap; aqui cada tamanho vira um único byte[] com as palavras
   ordenadas e coladas uma na outra (todas do mesmo tamanho, então a
   i-ésima começa em i * tamanho), e a busca é binária. Fica em ~9 MB.

   As de 5 letras vêm de palavras-validas.txt, editado à mão; as de 6 a 10
   de palavras-validas-6a10.txt.gz, gerado por scripts/gerar_palavras_termo.py. */
@Component
public class DicionarioTermo {

    private static final Logger log = LoggerFactory.getLogger(DicionarioTermo.class);

    private static final String ARQUIVO_CINCO_LETRAS = "termo/palavras-validas.txt";
    private static final String ARQUIVO_SEIS_A_DEZ = "termo/palavras-validas-6a10.txt.gz";

    private static final Pattern PALAVRA_ACEITA = Pattern.compile("[A-Z]{5,10}");

    private final Map<Integer, byte[]> palavrasPorTamanho;

    public DicionarioTermo() {
        Map<Integer, List<String>> listas = new HashMap<>();
        carregar(ARQUIVO_CINCO_LETRAS, false, listas);
        carregar(ARQUIVO_SEIS_A_DEZ, true, listas);

        Map<Integer, byte[]> blocos = new HashMap<>();
        int total = 0;
        for (Map.Entry<Integer, List<String>> entrada : listas.entrySet()) {
            int tamanho = entrada.getKey();
            List<String> palavras = entrada.getValue().stream().distinct().sorted().toList();
            byte[] bloco = new byte[palavras.size() * tamanho];
            for (int i = 0; i < palavras.size(); i++) {
                System.arraycopy(palavras.get(i).getBytes(StandardCharsets.US_ASCII), 0,
                        bloco, i * tamanho, tamanho);
            }
            blocos.put(tamanho, bloco);
            total += palavras.size();
        }
        this.palavrasPorTamanho = Map.copyOf(blocos);
        log.info("Termo: {} palavras carregadas para validação de palpite.", total);
    }

    /* `palavra` já normalizada: sem acento, em maiúsculas. */
    public boolean contem(String palavra) {
        int tamanho = palavra.length();
        byte[] bloco = palavrasPorTamanho.get(tamanho);
        if (bloco == null) return false;

        byte[] alvo = palavra.getBytes(StandardCharsets.US_ASCII);
        int inicio = 0;
        int fim = bloco.length / tamanho - 1;
        while (inicio <= fim) {
            int meio = (inicio + fim) >>> 1;
            int comparacao = Arrays.compare(
                    bloco, meio * tamanho, meio * tamanho + tamanho, alvo, 0, tamanho);
            if (comparacao == 0) return true;
            if (comparacao < 0) inicio = meio + 1;
            else fim = meio - 1;
        }
        return false;
    }

    private void carregar(String arquivo, boolean compactado, Map<Integer, List<String>> listas) {
        try (InputStream bruto = new ClassPathResource(arquivo).getInputStream();
             InputStream entrada = compactado ? new GZIPInputStream(bruto) : bruto;
             BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                String palavra = linha.trim().toUpperCase();
                if (palavra.isEmpty() || palavra.startsWith("#")) continue;
                /* Só A-Z cabe no byte[] por tamanho; linha fora disso é erro
                   de edição do arquivo e não pode virar palpite aceito. */
                if (!PALAVRA_ACEITA.matcher(palavra).matches()) continue;
                listas.computeIfAbsent(palavra.length(), t -> new ArrayList<>()).add(palavra);
            }
        } catch (IOException e) {
            /* Sem dicionário o servidor aceitaria qualquer sequência de
               letras — é uma falha de empacotamento, não um estado de
               operação normal. */
            throw new IllegalStateException(
                    "Não foi possível ler " + arquivo + " para o jogo do Termo.", e);
        }
    }
}
