import javafx.util.Pair;
import processing.core.PApplet;
import processing.core.PVector;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Utils
{
    private static float lastSeenFPS;

    static float round2F(float f)
    {
        float f2 = f * 100;
        int rounded = Math.round(f2);
        return rounded / 100f;
    }

    static void displayInformation(PApplet applet, int generation)
    {
        if (applet.frameCount % 30 == 1)
            lastSeenFPS = applet.frameRate;
        applet.fill(25);
        applet.noStroke();
        applet.rect(0, 0, 120, 40);
        applet.fill(255);
        applet.textSize(25);
        applet.textAlign(PApplet.LEFT, PApplet.TOP);
        applet.text("FPS: " + round2F(lastSeenFPS), 5, 0);
        applet.text("Gen: " + generation, 5, 20);
    }

    static void displayGrid(PApplet applet)
    {
        int offX = (int) Main.translation.x % Main.WIDTH, offY = (int) Main.translation.y % Main.WIDTH;
        applet.stroke(255, 50);
        applet.strokeWeight(1);
        for (int x = offX; x <= applet.width + offX + Main.WIDTH; x += Main.WIDTH)
            applet.line(x, offY - Main.WIDTH, x, applet.height + offY + Main.WIDTH);
        for (int y = offY; y <= applet.height + offY + Main.WIDTH; y += Main.WIDTH)
            applet.line(offX - Main.WIDTH, y, applet.width + offX + Main.WIDTH, y);
    }

    static ArrayList<PVector> getPixelsInALine(PVector start, PVector finish)
    {
        ArrayList<PVector> ret = new ArrayList<>();
        if (start == null || finish == null)
            return ret;
        float direction = getAngle(finish.copy().sub(start));
        marchPixels(start, start, finish, direction, ret);
        return ret;
    }

    private static void marchPixels(PVector current, PVector start, PVector finish, float dir, ArrayList<PVector> arr)
    {
        arr.add(current);
        if (current.equals(finish))
            return;
        int x = (int) current.x, y = (int) current.y;
        PVector[] neighbours = {new PVector(x + 1, y), new PVector(x + 1, y + 1), new PVector(x, y + 1), new PVector(x - 1, y + 1),
                new PVector(x - 1, y), new PVector(x - 1, y - 1), new PVector(x, y - 1), new PVector(x + 1, y - 1)};

        float minDeviation = Float.MAX_VALUE;
        int champion = 0;
        for (int i = 0; i < neighbours.length; i++)
        {
            PVector neighbour = neighbours[i];
            if (arr.contains(neighbour))
                continue;
            float currentDirection = getAngle(neighbour.copy().sub(start));
            float currentDeviation = PApplet.abs(currentDirection - dir);
            if (currentDeviation < minDeviation)
            {
                minDeviation = currentDeviation;
                champion = i;
            }
        }
        marchPixels(neighbours[champion], start, finish, dir, arr);
    }

    private static float getAngle(PVector v)
    {
        float ang = v.heading();
        return ang < 0 ? ang + PApplet.TAU : ang;
    }

    static ArrayList<PVector> getPixelsWithinRange(int x, int y, int range)
    {
        ArrayList<PVector> ret = new ArrayList<>();
        for (int i = x - range; i <= x + range; i++)
            for (int j = y - range; j <= y + range; j++)
                ret.add(new PVector(i, j));
        return ret;
    }

    static void selectCellsWithinRectangle(PVector rectPos, float rectWidth, float rectHeight)
    {
        if(rectWidth * rectHeight / (Main.WIDTH * Main.WIDTH) < Main.activeCells.size())
        {
            int gridLockedX = PApplet.floor(rectPos.x / Main.WIDTH) * Main.WIDTH;
            int gridLockedY = PApplet.floor(rectPos.y / Main.WIDTH) * Main.WIDTH;
            for (int x = gridLockedX; x <= rectPos.x + rectWidth; x += Main.WIDTH)
                for (int y = gridLockedY; y <= rectPos.y + rectHeight; y += Main.WIDTH)
                {
                    Cell cell = Main.activeCells.get("" + x + "_" + y);
                    if (cell == null)
                        continue;
                    cell.selected = true;
                    Main.selectedCells.add(cell);
                }
        }
        else
        {
            for (Cell cell : Main.activeCells.values())
                if (cell.collideRect(rectPos, rectWidth, rectHeight))
                {
                    cell.selected = true;
                    Main.selectedCells.add(cell);
                }
        }
    }

    static void selectAllCells()
    {
        for (Cell cell : Main.activeCells.values())
        {
            cell.selected = true;
            Main.selectedCells.add(cell);
        }
    }

    static void deselectAllCells()
    {
        for (Cell cell : Main.selectedCells)
            cell.selected = false;
        Main.selectedCells.clear();
    }

    static Cell[] constructCellsFromPreset(ArrayList<String> preset, PApplet applet)
    {
        Cell[] ret = new Cell[preset.size()];
        for (int i = 0; i < ret.length; i++)
        {
            String[] params = PApplet.split(preset.get(i), " ");
            int x = Integer.parseInt(params[0]) * Main.WIDTH, y = Integer.parseInt(params[1]) * Main.WIDTH;
            State state = params.length > 2 ? State.values()[Integer.parseInt(params[2])] : State.Wire;
            ret[i] = new Cell(x, y, state, applet);
        }
        return ret;
    }

    static void save(String path, PApplet applet) throws RuntimeException
    {
        List<String> saveData = new ArrayList<>();
        synchronized (Main.activeCells)
        {
            for (Cell cell : Main.activeCells.values())
            {
                String stateInfo = cell.state.ordinal() == 0 ? "" : " " + cell.state.ordinal();
                saveData.add("" + (cell.x / Main.WIDTH) + " " + (cell.y / Main.WIDTH) + stateInfo);
            }
        }

        String[] saveDataArr = saveData.toArray(new String[0]);
        applet.saveStrings(path, saveDataArr);
    }

    static void load(String path, PApplet applet) throws RuntimeException
    {
        String[] loadData = applet.loadStrings(path);
        synchronized (Main.eventQueue)
        {
            Main.activeCells.clear();
        }
        for (String cellData : loadData)
        {
            String[] params = PApplet.split(cellData, " ");
            State state = params.length > 2 ? State.values()[Integer.parseInt(params[2])] : State.Wire;
            String parsedData = "" + (Integer.parseInt(params[0]) * Main.WIDTH) + " " + (Integer.parseInt(params[1]) * Main.WIDTH) + " " + state;
            synchronized (Main.eventQueue) {
                Main.eventQueue.push(new Pair<>(EventType.AddCell, parsedData));
            }
        }
    }

    static void loadPresets()
    {
        File[] files = new File("./data/presets").listFiles();
        assert files != null;

        UIManager.presetNames = new String[files.length];
        int i = 0;
        for (File file : files)
        {
            String[] data = PApplet.loadStrings(file);
            String presetName = data[0];
            UIManager.presetNames[i] = presetName;
            ArrayList<String> presetCellParameters = new ArrayList<>(Arrays.asList(data).subList(1, data.length));
            Main.presets.put(presetName, presetCellParameters);

            i++;
        }
    }
}
