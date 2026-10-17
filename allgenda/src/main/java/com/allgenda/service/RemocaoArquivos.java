package com.allgenda.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

// Mantém o disco coerente com o banco: arquivos só são apagados quando a transação que os envolve termina.
@Component
public class RemocaoArquivos {

    private static final Logger log = LoggerFactory.getLogger(RemocaoArquivos.class);

    private final ArmazenamentoService armazenamento;

    public RemocaoArquivos(ArmazenamentoService armazenamento) {
        this.armazenamento = armazenamento;
    }

    // para registros apagados do banco: o arquivo só sai do disco depois que a exclusão é confirmada
    public void removerAoConfirmar(Collection<String> chaves) {
        if (chaves.isEmpty()) {
            return;
        }
        List<String> copia = List.copyOf(chaves);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            copia.forEach(this::remover);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                copia.forEach(RemocaoArquivos.this::remover);
            }
        });
    }

    // para arquivos recém-gravados: se a transação for desfeita, o arquivo é apagado para não ficar órfão no disco
    public void removerSeDesfeita(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    remover(chave);
                }
            }
        });
    }

    // falhar ao apagar o arquivo não deve desfazer nem quebrar uma operação já confirmada no banco
    private void remover(String chave) {
        try {
            armazenamento.remover(chave);
        } catch (RuntimeException e) {
            log.warn("Não foi possível remover o arquivo {} do armazenamento.", chave, e);
        }
    }
}
