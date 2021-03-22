import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;

enum State
{Wire, Head, Tail}

class Cell
{
    String key;
    int cellWidthWhenCopied;
    private PApplet applet;
    private List<Cell> neighbours = new ArrayList<>();

    State state;
    private State nextState;
    int x, y;
    boolean selected;

    Cell(int x, int y, State state, String key, PApplet applet)
    {
        this.x = x;
        this.y = y;
        this.state = state;
        this.key = key;
        this.applet = applet;
        setNeighbours();
    }

    Cell(int x, int y, State state, PApplet applet)
    {
        this.x = x;
        this.y = y;
        this.state = state;
        this.applet = applet;
    }

    void setNeighbours()
    {
        for (int i = -1; i <= 1; i++)
        {
            for (int j = -1; j <= 1; j++)
            {
                if (i == 0 && j == 0)
                    continue;

                String neighbourKey = "" + (x + i * Main.WIDTH) + "_" + (y + j * Main.WIDTH);
                Cell neighbour = Main.activeCells.get(neighbourKey);

                if (neighbour == null)
                    continue;

                if (!neighbours.contains(neighbour))
                {
                    neighbours.add(neighbour);
                    neighbour.addNeighbour(this);
                }
            }
        }
    }

    void clearNonSelectedNeighbours()
    {
        for (int i = neighbours.size() - 1; i >= 0; i--)
            if (!neighbours.get(i).selected)
                neighbours.remove(i);
    }

    void safeRemoveNonSelected()
    {
        for (Cell neighbour : neighbours)
            if (!neighbour.selected)
                neighbour.removeNeighbour(this);
    }

    private void addNeighbour(Cell neighbour)
    {
        if (!neighbours.contains(neighbour))
            neighbours.add(neighbour);
    }

    void safeRemove()
    {
        for (Cell neighbour : neighbours)
            neighbour.removeNeighbour(this);
    }

    private void removeNeighbour(Cell neighbour)
    {
        neighbours.remove(neighbour);
    }

    void setNextState()
    {
        switch (state)
        {
            case Head:
                nextState = State.Tail;
                break;
            case Tail:
                nextState = State.Wire;
                break;
            case Wire:
                int electronCount = 0;
                for (Cell neighbour : neighbours)
                    if (neighbour.state.equals(State.Head))
                        electronCount++;
                if (electronCount == 1 || electronCount == 2)
                    nextState = State.Head;
                else
                    nextState = state;
                break;
        }
    }

    void setState()
    {
        state = nextState;
    }

    void forceState(State forcedState)
    {
        state = forcedState;
        nextState = forcedState;
    }

    void display(PVector translation)
    {
        if (x + translation.x < -Main.WIDTH || x + translation.x > applet.width + Main.WIDTH ||
                y + translation.y < -Main.WIDTH || y + translation.y > applet.height + Main.WIDTH)
            return;

        applet.noStroke();
        int col = 0;
        switch (state)
        {
            case Head:
                col = applet.color(0, 0, 255);
                break;
            case Tail:
                col = applet.color(255, 0, 0);
                break;
            case Wire:
                col = applet.color(255, 255, 0);
                break;
        }
        applet.fill(col);
        applet.square(x, y, Main.WIDTH);
    }

    void display(PVector translation, int dx, int dy, int alpha)
    {
        applet.noStroke();
        int col = 0;
        switch (state)
        {
            case Head:
                col = applet.color(0, 0, 255, alpha);
                break;
            case Tail:
                col = applet.color(255, 0, 0, alpha);
                break;
            case Wire:
                col = applet.color(255, 255, 0, alpha);
                break;
        }
        applet.fill(col);
        int gridLockedDX = PApplet.floor((dx - translation.x) / Main.WIDTH) * Main.WIDTH;
        int gridLockedDY = PApplet.floor((dy - translation.y) / Main.WIDTH) * Main.WIDTH;
        applet.square(x + gridLockedDX, y + gridLockedDY, Main.WIDTH);
    }

    void drawOutline(int outlineColor)
    {
        applet.strokeWeight(2);
        applet.stroke(outlineColor);
        Cell neighbour;
        String[] oldCoords = PApplet.split(key, "_");
        int oldX = Integer.parseInt(oldCoords[0]);
        int oldY = Integer.parseInt(oldCoords[1]);

        neighbour = Main.activeCells.get("" + (oldX - Main.WIDTH) + "_" + oldY);
        if (neighbour == null || !neighbour.selected)
            applet.line(x, y, x, y + Main.WIDTH);

        neighbour = Main.activeCells.get("" + (oldX + Main.WIDTH) + "_" + oldY);
        if (neighbour == null || !neighbour.selected)
            applet.line(x + Main.WIDTH, y, x + Main.WIDTH, y + Main.WIDTH);

        neighbour = Main.activeCells.get("" + oldX + "_" + (oldY - Main.WIDTH));
        if (neighbour == null || !neighbour.selected)
            applet.line(x, y, x + Main.WIDTH, y);

        neighbour = Main.activeCells.get("" + oldX + "_" + (oldY + Main.WIDTH));
        if (neighbour == null || !neighbour.selected)
            applet.line(x, y + Main.WIDTH, x + Main.WIDTH, y + Main.WIDTH);
    }

    boolean collideRect(PVector rectPos, double colliderWidth, double colliderHeight)
    {
        float r1x = x, r1y = y;
        float r1w = Main.WIDTH, r1h = Main.WIDTH;

        double r2x = rectPos.x, r2y = rectPos.y;

        return (r1x + r1w >= r2x && r1x <= r2x + colliderWidth && r1y + r1h >= r2y && r1y <= r2y + colliderHeight);
    }

    void shiftCell(PVector shift)
    {
        x += (int) shift.x;
        y += (int) shift.y;
    }
}
