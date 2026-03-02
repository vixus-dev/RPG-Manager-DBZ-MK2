package com.Vixus.inc;

import com.Vixus.Library.Personagem;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AoEDodgeResolverDialog extends JDialog {

    private final Map<Personagem, JCheckBox> checkBoxMap = new HashMap<>();
    private final Set<Personagem> esquivadores = new HashSet<>();

    public AoEDodgeResolverDialog(Frame owner, Set<Personagem> alvos) {
        super(owner, "Resolução de Esquiva em Área", true); // 'true' para ser modal
        setLayout(new BorderLayout(10, 10));

        // --- PAINEL COM OS CHECKBOXES ---
        JPanel panelAlvos = new JPanel();
        // GridLayout com 0 linhas e 1 coluna, para que os itens fiquem um abaixo do outro
        panelAlvos.setLayout(new GridLayout(0, 1)); 
        panelAlvos.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (Personagem alvo : alvos) {
            JCheckBox checkBox = new JCheckBox(alvo.getNome());
            checkBoxMap.put(alvo, checkBox);
            panelAlvos.add(checkBox);
        }
        
        add(new JScrollPane(panelAlvos), BorderLayout.CENTER);

        // --- BOTÃO DE CONFIRMAÇÃO ---
        JButton btnConfirmar = new JButton("Confirmar Resultados");
        btnConfirmar.addActionListener(e -> {
            // Preenche o Set de esquivadores com base nos checkboxes marcados
            for (Map.Entry<Personagem, JCheckBox> entry : checkBoxMap.entrySet()) {
                if (entry.getValue().isSelected()) {
                    esquivadores.add(entry.getKey());
                }
            }
            dispose(); // Fecha a janela
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(btnConfirmar);
        add(bottomPanel, BorderLayout.SOUTH);

        pack(); // Ajusta o tamanho da janela ao conteúdo
        setLocationRelativeTo(owner);
    }

    /**
     * Retorna o conjunto de personagens que foram marcados como tendo se esquivado.
     */
    public Set<Personagem> getEsquivadores() {
        return esquivadores;
    }
}