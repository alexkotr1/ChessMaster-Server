package alexk.chess.Stockfish;

import alexk.chess.Game;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public abstract class Client {


    public abstract void startEngine();

    public abstract void sendCommand(String command);

    public abstract String getOutput();

    public abstract String getBestMove(String fen, long whiteTime, long blackTime);

    public abstract void stopEngine();

    public abstract float getEvalScore(String fen, int waitTime);
}