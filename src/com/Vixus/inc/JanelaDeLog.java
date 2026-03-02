package com.Vixus.inc;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

/**
 * Uma janela não-modal dedicada a exibir um registro de todas as ações
 * que acontecem durante o combate.
 */
public class JanelaDeLog extends JDialog {

    private JTextArea areaDeLog;

    public JanelaDeLog(Frame owner) {
        // O 'false' no final significa que a janela não é modal, ou seja,
        // ela não trava a janela principal do RPGManager enquanto estiver aberta.
        super(owner, "Log de Combate", false); 
        
        setSize(450, 600);
        // Posiciona a janela de log perfeitamente ao lado direito da janela principal.
        setLocation(owner.getX() + owner.getWidth(), owner.getY()); 
        
        // --- Configuração da Área de Texto ---
        areaDeLog = new JTextArea();
        areaDeLog.setEditable(false);
        areaDeLog.setLineWrap(true);      // Faz a quebra de linha automática
        areaDeLog.setWrapStyleWord(true); // Evita quebrar palavras no meio
        areaDeLog.setFont(new Font("Monospaced", Font.PLAIN, 13)); // Fonte monoespaçada para melhor alinhamento
        areaDeLog.setBackground(new Color(24, 24, 24)); // Fundo escuro
        areaDeLog.setForeground(new Color(85, 255, 0)); // Texto verde
        areaDeLog.setMargin(new Insets(10, 10, 10, 10)); // Margem interna para respiro

        // --- Configuração da Rolagem Automática ---
        JScrollPane scrollPane = new JScrollPane(areaDeLog);
        // Pega o 'caret' (cursor de texto) e o configura para sempre se atualizar para a última posição
        DefaultCaret caret = (DefaultCaret) areaDeLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        add(scrollPane);
    }

    /**
     * Adiciona uma nova mensagem formatada ao final do log.
     * Este é o método que o RPGManager irá chamar.
     * @param mensagem A string a ser registrada.
     */
    public void adicionarMensagem(String mensagem) {
        // SwingUtilities.invokeLater garante que a atualização da interface
        // aconteça de forma segura, na thread de eventos do Swing.
        SwingUtilities.invokeLater(() -> {
            areaDeLog.append("-> " + mensagem + "\n");
        });
    }
}