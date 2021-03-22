import processing.core.PApplet;
import processing.core.PVector;
import processing.event.MouseEvent;

class ExpandableList
{
    private static class Item
    {
        private PApplet applet;
        float x, y, w, h;
        String label;

        Item(float x_, float y_, float w_, float h_, String s, PApplet applet)
        {
            x = x_;
            y = y_;
            w = w_;
            h = h_;
            label = s;
            this.applet = applet;
        }

        boolean check() {
            return (applet.mouseX > x - w / 2 && applet.mouseX < x + w / 2 && applet.mouseY > y - h / 2 && applet.mouseY < y + h / 2);
        }

        void show(boolean canHighlight, boolean showLabel, float newY, String cl)
        {
            if (newY > 0)
                y = newY;
            applet.rectMode(PApplet.CENTER);
            int fillColor = (check() || cl.equals(label)) ? applet.color(100) : applet.color(50);
            if (!canHighlight)
                fillColor = applet.color(50);
            applet.fill(fillColor);
            applet.noStroke();
            applet.rect(x, y, w, h - 2);
            if (!showLabel)
                return;
            applet.fill(255);
            applet.textSize(h * 0.7f);
            applet.textAlign(PApplet.CENTER, PApplet.CENTER);
            applet.text(label, x, y);
        }
    }

    private PApplet applet;
    private Item[] itemList;
    private Item currItem;
    private boolean expanded = false;
    private PVector arrowPos;
    private int itemLimit;
    private int indexBuffer = 0;

    private PVector barPos;

    ExpandableList(String[] names, float xC, float yC, float w, float h, int il, PApplet applet)
    {
        float x = xC + w / 2, y = yC + h / 2;
        itemList = new Item[names.length];
        for (int i = 0; i < itemList.length; i++)
            itemList[i] = new Item(x, y + (h * (i + 1)), w, h, names[i], applet);
        currItem = new Item(x, y, w, h, names[0], applet);
        arrowPos = new PVector(x + (w + h) / 2, y);
        barPos = new PVector(0, 0);
        itemLimit = il;
        this.applet = applet;
    }

    void display()
    {
        applet.pushMatrix();
        applet.translate(arrowPos.x, arrowPos.y);
        applet.rectMode(PApplet.CENTER);
        applet.fill(50);
        applet.noStroke();
        applet.rect(0, 0, currItem.h - 4, currItem.h - 2);
        applet.rotate(expanded ? PApplet.PI : 0);
        applet.fill(100);
        float r = currItem.h * 0.5f;
        PVector v1 = new PVector(-r / 2, -r / 2), v2 = new PVector(r / 2, -r / 2), v3 = new PVector(0, r / 2);
        applet.triangle(v1.x, v1.y, v2.x, v2.y, v3.x, v3.y);
        applet.popMatrix();
        currItem.show(false, !expanded, -1, "");
        if (expanded)
        {
            for (int i = indexBuffer; i < indexBuffer + itemLimit; i++)
            {
                if (i > itemList.length - 1 || i < 0)
                    continue;
                itemList[i].show(true, true, currItem.y + (currItem.h * (i + 1 - indexBuffer)), currItem.label);
            }
            if (itemList.length > itemLimit)
            {
                applet.noStroke();
                applet.fill(50);
                applet.rectMode(PApplet.CORNER);
                applet.rect(arrowPos.x - currItem.h / 2 + 2, arrowPos.y + currItem.h / 2 + 1, currItem.h - 4, currItem.h * itemLimit - 2);
                barPos.x = arrowPos.x - currItem.h / 2 + 6;
                barPos.y = arrowPos.y + currItem.h / 2 + 1 + ((float) (indexBuffer) / itemList.length * (currItem.h * itemLimit - 2)) + 4;
                float barW = currItem.h - 12;
                float barH = (float) (itemLimit) / itemList.length * (currItem.h * itemLimit - 2) - 8;
                applet.fill(75);
                applet.rect(barPos.x, barPos.y, barW, barH);
            }
        }
    }

    private void check()
    {
        float ax = arrowPos.x, ay = arrowPos.y, aw = currItem.h - 4;
        if (applet.mouseX > ax - aw / 2 && applet.mouseX < ax + aw / 2 && applet.mouseY > ay - aw / 2 && applet.mouseY < ay + aw / 2)
            expanded = !expanded;
        if (!expanded)
            return;
        for (Item i : itemList)
        {
            if (i.check())
            {
                currItem.label = i.label;
                expanded = false;
            }
        }
    }

    private void scroll(int change)
    {
        float checkX = currItem.x - currItem.w / 2;
        float checkY = currItem.y + currItem.h / 2;
        float checkW = currItem.w;
        float checkH = currItem.h * itemLimit;
        if (!(applet.mouseX > checkX && applet.mouseX < checkX + checkW && applet.mouseY > checkY && applet.mouseY < checkY + checkH))
            return;
        int nextIndexBuffer = indexBuffer + change;
        if (nextIndexBuffer >= 0 && nextIndexBuffer <= itemList.length - itemLimit)
            indexBuffer = nextIndexBuffer;
    }

    String currentItem()
    {
        return currItem.label;
    }

    void mousePressed()
    {
        check();
    }

    void mouseWheel(MouseEvent event)
    {
        scroll(event.getCount());
    }
}
