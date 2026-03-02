// Pacote: com.Vixus.inc
package com.Vixus.inc;

import com.Vixus.Library.Ampliacao;
import com.Vixus.Library.GrauAmpliacao;
import com.Vixus.Library.Jogador;
import com.Vixus.Library.Personagem;
import com.Vixus.Library.Inimigo;
import com.Vixus.Library.SubForma;
import com.Vixus.Library.SyncGroup;
import com.Vixus.Library.Transformacao;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Map;

public class StatusCellRenderer extends DefaultTableCellRenderer {

    private final RPGManager rpgManager;
    private final Map<String, Color> coresTransformacoes;
    private final Map<String, Color> coresAmpliacoes;

    public StatusCellRenderer(RPGManager manager, Map<String, Color> coresTransformacoes, Map<String, Color> coresAmpliacoes) {
        this.rpgManager = manager;
        this.coresTransformacoes = coresTransformacoes;
        this.coresAmpliacoes = coresAmpliacoes;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Reseta para o estado padrão
        if (!isSelected) {
            c.setBackground(table.getBackground());
        }
        c.setForeground(table.getForeground());
        ((JComponent) c).setBorder(null);

        String tipoPersonagem = (String) table.getValueAt(row, 0);
        String nomePersonagem = (String) table.getValueAt(row, 1);
        Object personagemObj = rpgManager.buscarPersonagemPorNomeComTipo(nomePersonagem + " (" + tipoPersonagem + ")");

        if (!(personagemObj instanceof Personagem p)) {
            return c; // Se não for um personagem, retorna o padrão
        }

        // --- LÓGICA DE ESTILO COMBINADA ---

        // 1. Define a cor base do texto (cinza para inativos, padrão para os outros)
        Color corDoTexto = table.getForeground();
        if (p instanceof Jogador j && !j.isAtivoNaSessao()) {
            corDoTexto = Color.GRAY;
        }
        if (isSelected) {
            corDoTexto = table.getSelectionForeground();
        }
        c.setForeground(corDoTexto);

        // 2. Lida com o símbolo de sincronia (apenas na coluna "Nome")
        if (column == 1) {
            SyncGroup sync = rpgManager.getSyncGroupDoPersonagem(p);
            if (sync != null) {
                Color corSimbolo = sync.getCorSincronia();
                String hexSimbolo = String.format("#%02x%02x%02x", corSimbolo.getRed(), corSimbolo.getGreen(), corSimbolo.getBlue());
                String hexTexto = String.format("#%02x%02x%02x", corDoTexto.getRed(), corDoTexto.getGreen(), corDoTexto.getBlue());
                
                // Constrói o HTML combinando a cor do símbolo e a cor do texto
                setText("<html><font color='" + hexSimbolo + "'>● </font><font color='" + hexTexto + "'>" + p.getNome() + "</font></html>");
            }
        }
        
        SubForma subForma = null;
        GrauAmpliacao grauAmpliacao = null;

        if (personagemObj instanceof Jogador j) {
            subForma = j.getSubFormaAtual();
            grauAmpliacao = j.getGrauAmpliacaoAtual();
        } else if (personagemObj instanceof Inimigo i) {
            subForma = i.getSubFormaAtual();
            grauAmpliacao = i.getGrauAmpliacaoAtual();
        }
        

        // --- LÓGICA DA TRANSFORMAÇÃO (Define o fundo) ---
        if (subForma != null) {
            Transformacao transformacaoPai = rpgManager.buscarTransformacaoPai(subForma);
            if (transformacaoPai != null && coresTransformacoes.containsKey(transformacaoPai.getNome())) {
                Color baseColor = coresTransformacoes.get(transformacaoPai.getNome());
                c.setBackground(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 128)); // Opacidade fixa
            }
        }

        // --- LÓGICA DA AMPLIAÇÃO (Define a borda com espessura dinâmica) ---
        if (grauAmpliacao != null) {
            Ampliacao ampliacaoPai = rpgManager.buscarAmpliacaoPai(grauAmpliacao);
            if (ampliacaoPai != null && coresAmpliacoes.containsKey(ampliacaoPai.getNome())) {
                Color borderColor = coresAmpliacoes.get(ampliacaoPai.getNome());

                int indiceAtual = ampliacaoPai.getGraus().indexOf(grauAmpliacao);
                int totalDeGraus = ampliacaoPai.getGraus().size();

                // Define a opacidade mínima e máxima (0.0f a 1.0f)
                float minAlpha = 0.3f; // 30% opaco (bem transparente)
                float maxAlpha = 0.9f;  // 90% opaco (quase sólido)
                
                float alpha = minAlpha; // Valor padrão

                if (totalDeGraus > 1) {
                    double fator = (double) indiceAtual / (totalDeGraus - 1);
                    alpha = (float) (minAlpha + (maxAlpha - minAlpha) * fator);
                } else {
                    alpha = maxAlpha; 
                }
                
                // Define uma espessura fixa para a borda
                int thickness = 5; 

                // Usa nossa nova classe de borda customizada
                ((JComponent) c).setBorder(new TransparentLineBorder(borderColor, thickness, alpha));
            }
        }
        
        
        
        return c;
    }
}