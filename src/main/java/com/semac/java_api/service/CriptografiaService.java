package com.semac.java_api.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.semac.java_api.dto.CriptografiaEstadoDTO;
import com.semac.java_api.dto.CriptografiaPalpiteRespostaDTO;
import com.semac.java_api.model.CriptografiaJogoAcerto;
import com.semac.java_api.model.CriptografiaPalavra;
import com.semac.java_api.model.Pessoa;
import com.semac.java_api.repository.CriptografiaJogoAcertoRepository;
import com.semac.java_api.repository.CriptografiaPalavraRepository;
import com.semac.java_api.repository.PessoaRepository;

@Service
public class CriptografiaService {
    
    private final CriptografiaPalavraRepository criptografiaPalavraRepository;
    private final CriptografiaJogoAcertoRepository criptografiaJogoAcertoRepository;
    private final PessoaRepository pessoaRepository;

    private final RegraXpService regraXpService;

    public CriptografiaService(CriptografiaJogoAcertoRepository criptografiaJogoAcertoRepository,
                                CriptografiaPalavraRepository criptografiaPalavraRepository,
                                PessoaRepository pessoaRepository, RegraXpService regraXpService) {
        this.criptografiaJogoAcertoRepository = criptografiaJogoAcertoRepository;
        this.criptografiaPalavraRepository = criptografiaPalavraRepository;
        this.pessoaRepository = pessoaRepository;
        this.regraXpService = regraXpService;
    }

    /* Estado do jogo de hoje: o que a tela precisa para se montar, já
       retomando a partida em andamento se houver. */
    @Transactional
    public CriptografiaEstadoDTO estadoDeHoje(Integer pessoaId) {
        boolean disponivel = disponivelHoje();
        if (!disponivel) {
            return new CriptografiaEstadoDTO(false, false, disponivel ? 1000 : 2000, null);
        }

        Pessoa pessoa = buscarPessoa(pessoaId);
        List<CriptografiaJogoAcerto> jogoAcertos = criptografiaJogoAcertoRepository.findByPessoaId(pessoa.getId());

        if (jogoAcertos.size() == 3) {
            return new CriptografiaEstadoDTO(true, true, regraXpService.pontosCriptografiaAcerto() * jogoAcertos.size(), jogoAcertos.size());
        }
        return new CriptografiaEstadoDTO(true, false, regraXpService.pontosCriptografiaAcerto() * jogoAcertos.size(), jogoAcertos.size());
    }

    /* Confere um palpite.

       Toda a decisão é aqui: se a palavra é uma das palavras certas, 
       se já acertou a palavra anteriormente e se credita xp. */
    @Transactional
    public CriptografiaPalpiteRespostaDTO palpitar(Integer pessoaId, String palpite){
        List<CriptografiaPalavra> palavras = carregarPalavras();
        boolean certo = false;
        CriptografiaPalavra palavraCerta = null;
        for (CriptografiaPalavra criptografiaPalavra : palavras) {
            if(palpite.equalsIgnoreCase(criptografiaPalavra.getPalavra())){
                certo = true;
                palavraCerta = criptografiaPalavra;
            }
        }

        if(!certo){
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Palavra errada.");
        }

        Pessoa pessoa = buscarPessoa(pessoaId);
        if(criptografiaJogoAcertoRepository.findByPessoaIdAndPalavraId(pessoaId, palavraCerta.getId()).isPresent()){
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Você já acertou essa palavra.");
        }
        
        // Se chegou aqui, é porque a pessoa acertou a palavra e ainda não tinha acertado antes.
        // Credita o xp e registra o acerto.
        int xpVitoria = regraXpService.pontosCriptografiaAcerto();
        pessoa.setXp(pessoa.getXp() + xpVitoria);
        pessoaRepository.save(pessoa);
        
        CriptografiaJogoAcerto criptografiaJogoAcerto = new CriptografiaJogoAcerto();
        criptografiaJogoAcerto.setPessoa(pessoa);
        criptografiaJogoAcerto.setPalavra(palavraCerta);
        criptografiaJogoAcertoRepository.save(criptografiaJogoAcerto);

        List<CriptografiaJogoAcerto> acertos = criptografiaJogoAcertoRepository.findByPessoaId(pessoaId);
        return new CriptografiaPalpiteRespostaDTO(
                certo,
                acertos.size(),
                xpVitoria
        );
    }

    /* Auxiliares */
    private boolean disponivelHoje() {
        return !criptografiaPalavraRepository.findByData(LocalDate.now(ZoneId.of("America/Sao_Paulo"))).isEmpty();
    }

    private List<CriptografiaPalavra> carregarPalavras() {
        return criptografiaPalavraRepository.findAll();
    }

    private Pessoa buscarPessoa(Integer pessoaId) {
        return pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
    }
}
