// Pacote: com.Vixus.inc
package com.Vixus.inc;

// --- IMPORTS NECESSÁRIOS ADICIONADOS ---
import com.Vixus.Library.Jogador;
import com.Vixus.Library.Personagem;
import com.Vixus.Library.StatusEffect;
import java.util.List;
// --- FIM DOS IMPORTS ---

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.Point;
import java.awt.event.MouseEvent;

public class JTableComTooltips extends JTable {

    private final RPGManager rpgManager;

    public JTableComTooltips(DefaultTableModel modelo, RPGManager manager) {
        super(modelo);
        this.rpgManager = manager;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        Point p = event.getPoint();
        int rowIndex = rowAtPoint(p);

        if (rowIndex != -1) {
            String tipo = (String) getModel().getValueAt(rowIndex, 0);
            String nome = (String) getModel().getValueAt(rowIndex, 1);
            
            Object personagemObj = rpgManager.buscarPersonagemPorNomeComTipo(nome + " (" + tipo + ")");

            if (personagemObj instanceof Personagem personagem) {
                StringBuilder tooltip = new StringBuilder("<html>");

                if (personagem instanceof Jogador j) {
                    tooltip.append("<b>Testes de Atributo:</b><br>")
                           .append("S → ").append(RPGManager.formatarNumero(j.getAtk())).append("<br>")
                           .append("P → ").append(j.getDadoP()).append("<br>")
                           .append("E → ").append(RPGManager.formatarNumero(j.getHP())).append("<br>")
                           .append("C → ").append(j.getDadoC()).append("<br>")
                           .append("I → ").append(j.getDadoI()).append("<br>")
                           .append("A → ").append(j.getDadoA()).append("<br>")
                           .append("L → ").append(j.getDadoL());
                }

                List<StatusEffect> efeitos = personagem.getEfeitosAtivos();
                if (efeitos != null && !efeitos.isEmpty()) {
                    if (personagem instanceof Jogador) {
                        tooltip.append("<br><br>");
                    }
                    
                    tooltip.append("<b>Efeitos Ativos:</b><br>");
                    for (StatusEffect efeito : efeitos) {
                        tooltip.append(efeito.getNome())
                               .append(" (")
                               .append(efeito.getDuracao())
                               .append(" turnos)<br>");
                    }
                }

                tooltip.append("</html>");
                
                if (tooltip.length() > "<html>".length()) {
                    return tooltip.toString();
                }
            }
        }
        return null;
    }
}