package vdp;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class PaletaSwingExemplo extends JPanel {
    private BufferedImage img;
    

    public static void main(String[] args) {
        JFrame frame = new JFrame("Exemplo Paleta Mega Drive");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new PaletaSwingExemplo());
        frame.pack();
        frame.setVisible(true);
    }

    public PaletaSwingExemplo() {
        int width = 8;
        int height = 8;

        // --- Paleta de 4 cores (RGB em 0xRRGGBB) ---
        int[] paleta = new int[]{
            0x000000, // 0 = preto
            0x0000FF, // 1 = azul
            0x00FF00, // 2 = verde
            0xFF0000  // 3 = vermelho
        };

        // --- Tile 8x8 com índices da paleta ---
        int[][] tile = new int[][]{
            {0,0,1,1,2,2,3,3},
            {0,0,1,1,2,2,3,3},
            {0,0,1,1,2,2,3,3},
            {0,0,1,1,2,2,3,3},
            {0,0,1,1,2,2,3,3},
            {0,0,1,1,2,2,3,3},
            {0,0,1,1,2,2,3,3},
            {0,0,1,1,2,2,3,3},
        };

        // --- Cria framebuffer em BufferedImage ---
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // --- Preenche pixels traduzindo índice -> cor ---
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int indice = tile[y][x];   // pega índice do tile
                int cor = paleta[indice];  // busca cor da paleta
                img.setRGB(x, y, cor);     // escreve no framebuffer
            }
        }

        // Aumenta a escala pra enxergar melhor
        setPreferredSize(new Dimension(width * 32, height * 32));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Desenha a imagem ampliada
        g.drawImage(img, 0, 0, img.getWidth() * 32, img.getHeight() * 32, null);
    }
}
