import javafx.util.Pair;
import processing.core.PApplet;
import processing.core.PVector;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

class MiniMap extends PApplet
{
    private final int WINDOW_WIDTH = 300, WINDOW_HEIGHT = 250;
    private int FRAME_WIDTH, FRAME_HEIGHT;

    private PVector pointZero;
    private PApplet applet;

    private static final ArrayList<Cell> copiedCells = new ArrayList<>();

    MiniMap(PApplet applet)
    {
        FRAME_WIDTH = Main.WINDOW_WIDTH / Main.WIDTH;
        FRAME_HEIGHT = Main.WINDOW_HEIGHT / Main.WIDTH;
        pointZero = new PVector(WINDOW_WIDTH - FRAME_WIDTH, WINDOW_HEIGHT - FRAME_HEIGHT).mult(-Main.WIDTH * 0.5f);
        this.applet = applet;
    }

    @Override
    public void settings()
    {
        size(WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    @Override
    public void setup()
    {
        surface.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - 310, 25);
        surface.setResizable(true);
    }

    @Override
    public void draw()
    {
        synchronized (Main.eventQueue)
        {
            Main.eventQueue.push(new Pair<>(EventType.RequestCells, ""));
        }
        background(25);
        checkForResize();

        try
        {
            loadPixels();
            synchronized (copiedCells)
            {
                for (Cell cell : copiedCells)
                {
                    PVector cellPos = new PVector(cell.x, cell.y);
                    PVector cellPosTranslated = cellPos.add(Main.translation);
                    PVector miniMapPoint = cellPosTranslated.sub(pointZero).div(Main.WIDTH);
                    int x = floor(miniMapPoint.x), y = floor(miniMapPoint.y);
                    if (x < 0 || x >= width || y < 0 || y >= height)
                        continue;
                    int index = x + y * width;
                    int col = 0;
                    switch (cell.state)
                    {
                        case Head:
                            col = color(0, 0, 255);
                            break;
                        case Tail:
                            col = color(255, 0, 0);
                            break;
                        case Wire:
                            col = color(255, 255, 0);
                            break;
                    }
                    pixels[index] = col;
                }
            }
            updatePixels();
        } catch (ArrayIndexOutOfBoundsException e)
        {
            println("Window resized too quickly!");
        }

        noFill();
        stroke(255);
        strokeWeight(1);
        rectMode(CENTER);
        rect(width / 2f, height / 2f, FRAME_WIDTH, FRAME_HEIGHT);
    }

    @Override
    public void mousePressed()
    {
        PVector delta = new PVector(mouseX, mouseY).sub(width / 2f, height / 2f).mult(-Main.WIDTH);
        String parameters = "" + (int) delta.x + " " + (int) delta.y;
        synchronized (Main.eventQueue)
        {
            Main.eventQueue.push(new Pair<>(EventType.ShiftTranslate, parameters));
        }
    }

    static void copyCells(HashMap<String, Cell> cells)
    {
        synchronized (copiedCells)
        {
            copiedCells.clear();
            copiedCells.addAll(cells.values());
        }
    }

    private void checkForResize()
    {
        FRAME_WIDTH = applet.width / Main.WIDTH;
        FRAME_HEIGHT = applet.height / Main.WIDTH;
        pointZero = new PVector(width - FRAME_WIDTH, height - FRAME_HEIGHT).mult(-Main.WIDTH * 0.5f);
    }
}
