package alexk.chess.Stockfish;

import alexk.chess.Game;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Client {

    private BufferedReader processReader;
    private OutputStreamWriter processWriter;
    private static final Logger logger = LogManager.getLogger(Client.class);

    private static final String PATH = "lc0";

    public void startEngine() {
        try {
            Process engineProcess = Runtime.getRuntime().exec(PATH);
            processReader = new BufferedReader(new InputStreamReader(
                    engineProcess.getInputStream()));
            processWriter = new OutputStreamWriter(
                    engineProcess.getOutputStream());
            sendCommand("uci");
            sendCommand("isready");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendCommand(String command) {
        try {
            processWriter.write(command + "\n");
            processWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getOutput() {
        StringBuilder buffer = new StringBuilder();
        long end = System.currentTimeMillis();
        try {
            while (true) {
                if (processReader.ready()) {
                    String line = processReader.readLine();
                    buffer.append(line).append("\n");
                    logger.info(buffer);
                    if (line.startsWith("bestmove")) break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public String getBestMove(String fen, long whiteTime, long blackTime) {
        try {
            logger.info("White time: {}", whiteTime);
            logger.info("Black time: {}", blackTime);
            sendCommand("position fen " + fen);
            sendCommand("go wtime " + whiteTime + " btime" + blackTime);
            String output = getOutput();
            if (output.contains("bestmove")) {
                String[] parts = output.split("bestmove ");
                if (parts.length > 1) {
                    return parts[1].split(" ")[0];
                }
            }
            System.err.println("Failed to retrieve best move. Output:\n" + output);
        } catch (Exception e) {
            System.err.println("Error in getBestMove: " + e.getMessage());
        }
        return null;
    }
    public void stopEngine() {
        try {
            sendCommand("quit");
            processReader.close();
            processWriter.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    public float getEvalScore(String fen, int waitTime) {
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + waitTime);

        float evalScore = 0.0f;
        String[] dump = getOutput().split("\n");
        for (int i = dump.length - 1; i >= 0; i--) {
            if (dump[i].startsWith("info depth ")) {
                try {
                    evalScore = Float.parseFloat(dump[i].split("score cp ")[1]
                            .split(" nodes")[0]);
                } catch(Exception e) {
                    evalScore = Float.parseFloat(dump[i].split("score cp ")[1]
                            .split(" upperbound nodes")[0]);
                }
            }
        }
        return evalScore/100;
    }
}