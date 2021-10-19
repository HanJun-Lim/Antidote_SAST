import java.io.*;


public class foo {

    private Color colorNoArr;
    private Color[] colorsPrivate;
    private static Color[] colorsPrivateStatic;
    public Color[] colorPublic;

    public Color[] getColors() {
        return colorsPrivate;
    }

    public Color[] getColors2(Color[] userColors) {
        Color[] colors = new Color[userColors.length];

        for(int i=0; i<colors.length; i++) {
            colors[i] = this.colorsPrivate[i].clone();
        }

        return colors;
    }

    public static void main(String args[]) {
        Color[] colorarray;
    }

    private Color[] getColorsPrivate() {
        return colorsPrivate;
    }

    private class bar {
        public Color[] pubColorArray;
        private Color[] privColorArray;
    }
}