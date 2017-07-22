package pmtoolswatchdog;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import java.sql.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.util.Arrays;

public class ffmpeg extends Thread {
    //private final static String m_sFFMPEG = "d:\\ffmpeg\\ffmpeg.exe";
    private final static String m_sFFMPEG = "ffmpeg";
    private final static String m_sLogs = "/home/pmwatchdog/logs";
    private String[] m_sCommand;
    private Connection m_sqlConn;
    private int m_iID;

    public void log(String sText) {
        try {
            Format f = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
            String s = "[" + f.format(Calendar.getInstance().getTime()) + "] - " + sText + System.lineSeparator();
            Path path = Paths.get(m_sLogs + "/" + Integer.toString(m_iID) + ".log");
            Files.write(path, s.getBytes(), APPEND, CREATE);
        } catch (IOException e) {
        }
    }

    public void launch(int iID, String... sParams) throws SQLException {
        String[] sTemp = {m_sFFMPEG, "-y", "-threads", "1"}; // ffmpeg -y (for overwrite)
        m_sCommand = combineStringArrays(sTemp, sParams);
        m_sqlConn = DriverManager.getConnection("jdbc:mysql://NOPE", "NOPE", "NOPE");
        m_iID = iID;
        System.err.println(Arrays.toString(m_sCommand));
        this.start();
    }

    @Override
    public void run() {
        try {
            log("Started");
            // Mark in DB that this is "in progress"
            PreparedStatement sqlStatement = m_sqlConn.prepareStatement("UPDATE queue SET status=? WHERE id=?");
            sqlStatement.setString(1, "running");
            sqlStatement.setInt(2, m_iID);
            sqlStatement.executeUpdate();

            ProcessBuilder pb = new ProcessBuilder(m_sCommand);
            File fLog = new File(m_sLogs + "/" + Integer.toString(m_iID) + ".log");
            pb.redirectErrorStream(true);
            pb.redirectOutput(Redirect.appendTo(fLog));
            final Process p = pb.start();
            p.waitFor();
            log("Ended");

            sqlStatement = m_sqlConn.prepareStatement("UPDATE queue SET status=? WHERE id=?");
            sqlStatement.setString(1, "done");
            sqlStatement.setInt(2, m_iID);
            sqlStatement.executeUpdate();
            m_sqlConn.close();
        } catch (IOException|InterruptedException e) {
            // Process crashed?
            // Mark it as error
            log(e.getMessage());
            try {
                PreparedStatement sqlStatement = m_sqlConn.prepareStatement("UPDATE queue SET status=? WHERE id=?");
                sqlStatement.setString(1, "error");
                sqlStatement.setInt(2, m_iID);
                sqlStatement.executeUpdate();
                m_sqlConn.close();
            } catch (SQLException e2) {
                System.out.println(e2.getMessage());
            }
        } catch (SQLException e) {
            // Failed to update DB.
            System.out.println(e.getMessage());
        }
    }

    private String[] combineStringArrays(String[] sCommand, String[] sParams) {
        String[] sCombined = new String[sCommand.length + sParams.length];
        System.arraycopy(sCommand, 0, sCombined, 0, sCommand.length);
        System.arraycopy(sParams, 0, sCombined, sCommand.length, sParams.length);
        return sCombined;
    }

}
