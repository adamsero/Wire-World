import processing.core.PApplet;
import processing.core.PVector;

abstract class Slider
{
    PApplet applet;
    PVector centerPoint, handlePoint;
    float w, h, r;
    boolean sliding = false;

    Slider(float x, float y, float w_, float h_, PApplet applet)
    {
        this.applet = applet;
        centerPoint = new PVector(x, y);
        handlePoint = new PVector(x, y);
        w = w_;
        h = h_;
    }

    abstract float val();

    abstract void setHandle(float pos01);

    void display()
    {
        applet.noStroke();
        applet.fill(100);
        applet.rectMode(PApplet.CENTER);
        applet.rect(centerPoint.x, centerPoint.y, w, h, h / 2);
        applet.fill(200);
        applet.ellipse(handlePoint.x, handlePoint.y, r * 2, r * 2);
    }

    void press()
    {
        if (PApplet.dist(applet.mouseX, applet.mouseY, handlePoint.x, handlePoint.y) < r)
            sliding = true;
    }

    void release()
    {
        sliding = false;
    }
}

class HorizontalSlider extends Slider
{
    HorizontalSlider(float x, float y, float w_, float h_, PApplet applet)
    {
        super(x, y, w_, h_, applet);
        r = h;
    }

    @Override
    void display()
    {
        if (sliding)
            handlePoint.x = PApplet.constrain(applet.mouseX, centerPoint.x - w / 2, centerPoint.x + w / 2);

        super.display();
    }

    @Override
    void setHandle(float pos01)
    {
        if (pos01 < 0 || pos01 > 1)
            return;
        handlePoint.x = PApplet.map(pos01, 0, 1, centerPoint.x - w / 2, centerPoint.x + w / 2);
    }

    float val()
    {
        return PApplet.map(handlePoint.x, centerPoint.x - w / 2, centerPoint.x + w / 2, 0, 1);
    }
}