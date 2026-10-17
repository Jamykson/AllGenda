package com.allgenda.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RemocaoArquivosTest {

    @Mock
    private ArmazenamentoService armazenamento;

    private RemocaoArquivos remocao;

    @BeforeEach
    void setUp() {
        remocao = new RemocaoArquivos(armazenamento);
    }

    @AfterEach
    void encerrarTransacaoSimulada() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // simula uma transação aberta: os callbacks registrados só rodam quando o teste dispara o fim dela
    private static void abrirTransacao() {
        TransactionSynchronizationManager.initSynchronization();
    }

    private static void confirmarTransacao() {
        TransactionSynchronizationUtils.triggerAfterCommit();
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    }

    private static void desfazerTransacao() {
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    }

    @Test
    void removerAoConfirmarSoApagaDepoisDoCommit() {
        abrirTransacao();

        remocao.removerAoConfirmar(List.of("a.png", "b.jpg"));
        verifyNoInteractions(armazenamento);

        confirmarTransacao();
        verify(armazenamento).remover("a.png");
        verify(armazenamento).remover("b.jpg");
    }

    @Test
    void removerAoConfirmarNaoApagaSeATransacaoForDesfeita() {
        abrirTransacao();

        remocao.removerAoConfirmar(List.of("a.png"));
        desfazerTransacao();

        verifyNoInteractions(armazenamento);
    }

    @Test
    void removerAoConfirmarSemTransacaoApagaNaHora() {
        remocao.removerAoConfirmar(List.of("a.png"));

        verify(armazenamento).remover("a.png");
    }

    @Test
    void falhaAoApagarUmArquivoNaoImpedeOsDemaisNemEhPropagada() {
        doThrow(new IllegalStateException("disco indisponível")).when(armazenamento).remover("a.png");
        abrirTransacao();

        remocao.removerAoConfirmar(List.of("a.png", "b.jpg"));
        confirmarTransacao();

        verify(armazenamento).remover("b.jpg");
    }

    @Test
    void removerSeDesfeitaApagaNoRollback() {
        abrirTransacao();

        remocao.removerSeDesfeita("novo.png");
        desfazerTransacao();

        verify(armazenamento).remover("novo.png");
    }

    @Test
    void removerSeDesfeitaMantemOArquivoNoCommit() {
        abrirTransacao();

        remocao.removerSeDesfeita("novo.png");
        confirmarTransacao();

        verify(armazenamento, never()).remover(any());
    }
}
