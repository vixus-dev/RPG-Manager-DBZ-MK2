package com.Vixus.inc;

import javax.swing.border.AbstractBorder;
import java.awt.*;

/**
 * Uma implementação de borda que suporta transparência (canal alfa).
 * A classe LineBorder padrão do Swing não lida bem com isso.
 */
public class TransparentLineBorder extends AbstractBorder {

    private final int thickness;
    private final Color color;
    private final float alpha;

    /**
     * @param color A cor base da borda (será aplicada com o alfa).
     * @param thickness A espessura da borda em pixels.
     * @param alpha A opacidade da borda, de 0.0f (totalmente transparente) a 1.0f (totalmente opaca).
     */
    public TransparentLineBorder(Color color, int thickness, float alpha) {
        this.color = color;
        this.thickness = thickness;
        // Garante que o alfa esteja dentro dos limites válidos [0.0, 1.0]
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        // É necessário usar Graphics2D para ter acesso a funcionalidades avançadas como a composição alfa.
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Define o nível de transparência para o que for desenhado a seguir
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, this.alpha));
        g2d.setColor(this.color);

        // Desenha os 4 retângulos que compõem a borda
        g2d.fillRect(x, y, width, thickness); // Borda superior
        g2d.fillRect(x, y + height - thickness, width, thickness); // Borda inferior
        g2d.fillRect(x, y, thickness, height); // Borda esquerda
        g2d.fillRect(x + width - thickness, y, thickness, height); // Borda direita

        // Libera os recursos do graphics. Boa prática.
        g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.top = insets.right = insets.bottom = thickness;
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        // A borda não é opaca, pois tem transparência.
        return false;
    }
}