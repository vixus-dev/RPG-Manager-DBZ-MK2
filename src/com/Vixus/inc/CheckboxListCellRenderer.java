package com.Vixus.inc;

import com.Vixus.Library.Personagem;
import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class CheckboxListCellRenderer extends JCheckBox implements ListCellRenderer<Personagem> {

    private final Set<Personagem> alvosSelecionados;

    public CheckboxListCellRenderer(Set<Personagem> alvosSelecionados) {
        this.alvosSelecionados = alvosSelecionados;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Personagem> list, Personagem value, int index, boolean isSelected, boolean cellHasFocus) {
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setBackground(list.getBackground());
        setForeground(list.getForeground());

        // O texto do checkbox é o nome do personagem
        setText(value.getNome());
        
        // O estado (marcado/desmarcado) é definido pelo nosso Set de controle
        setSelected(alvosSelecionados.contains(value));

        // Melhora o feedback visual quando o mouse passa por cima
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        }

        return this;
    }
}