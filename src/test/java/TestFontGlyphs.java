import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class TestFontGlyphs {
    public static void main(String[] args) throws Exception {
        File faFile = new File("C:/Users/Admin/Documents/CoolwareV2.01/font-awesome-5-free-regular/Font Awesome 5 Free Regular/Font Awesome 5 Free Regular.ttf");
        Font faFont = Font.createFont(Font.TRUETYPE_FONT, faFile).deriveFont(Font.PLAIN, 18f);

        File sfFile = new File("C:/Users/Admin/Documents/CoolwareV2.01/CoolwareV2/Coolware v2/MavenMCP-1.8.9-master/src/main/resources/assets/coolware/fonts/sf-bold.ttf");
        Font sfFont = Font.createFont(Font.TRUETYPE_FONT, sfFile).deriveFont(Font.BOLD, 18f);

        testFont(faFont, "FontAwesome");
        testFont(sfFont, "SF Bold");
    }

    private static void testFont(Font font, String name) {
        int imgSize = 512;
        BufferedImage bufferedImage = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        g.setFont(font);
        FontMetrics fontMetrics = g.getFontMetrics();

        int posX = 0;
        int posY = 1;
        int charHeight = 0;
        int count = 0;

        for (int i = 0; i < 256; i++) {
            char ch = (char) i;
            Rectangle dim = fontMetrics.getStringBounds(String.valueOf(ch), g).getBounds();
            int w = dim.width + 8;
            int h = dim.height;
            if (posX + w >= imgSize) {
                posX = 0;
                posY += charHeight;
                charHeight = 0;
            }
            if (h > charHeight) charHeight = h;
            posX += w;
            count++;
        }

        for (int cp = 0xF000; cp <= 0xFFFF; cp++) {
            if (font.canDisplay(cp)) {
                char ch = (char) cp;
                Rectangle dim = fontMetrics.getStringBounds(String.valueOf(ch), g).getBounds();
                int w = dim.width + 8;
                int h = dim.height;
                if (posX + w >= imgSize) {
                    posX = 0;
                    posY += charHeight;
                    charHeight = 0;
                }
                if (h > charHeight) charHeight = h;
                posX += w;
                count++;
            }
        }

        System.out.printf("[%s] Packed %d glyphs, height %d / %d%n", name, count, (posY + charHeight), imgSize);
    }
}
