package com.allgenda.framework.acesso;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Grava o Papel como texto (o código, ex.: "DONO") e o reconstrói ao ler, para a coluna continuar igual
// à de quando o papel era um enum.
@Converter(autoApply = true)
public class PapelConverter implements AttributeConverter<Papel, String> {

    @Override
    public String convertToDatabaseColumn(Papel papel) {
        return papel == null ? null : papel.codigo();
    }

    @Override
    public Papel convertToEntityAttribute(String codigo) {
        return codigo == null ? null : new Papel(codigo);
    }
}
