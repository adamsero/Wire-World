import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

class Button
{
    private PApplet applet;
    private PVector pos;
    private PImage icon;
    private String key;
    private int w;

    Button(float x, float y, int w, String path, PApplet applet, String key)
    {
        this.applet = applet;
        this.w = w;
        this.key = key;
        pos = new PVector(x, y);
        icon = applet.loadImage("icons/" + path);
        icon.resize(w, w);
    }

    void display(boolean highlighted, int tintColor)
    {
        if (highlighted)
            applet.tint(tintColor);
        applet.imageMode(PApplet.CENTER);
        applet.image(icon, pos.x, pos.y);
        applet.tint(255);
    }

    boolean check()
    {
        int mX = applet.mouseX, mY = applet.mouseY;
        return (mX > pos.x - w / 2f && mX < pos.x + w / 2f &&
                mY > pos.y - w / 2f && mY < pos.y + w / 2f);
    }

    String getKey()
    {
        return key;
    }
}
