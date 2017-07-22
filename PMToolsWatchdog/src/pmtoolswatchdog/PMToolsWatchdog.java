package pmtoolswatchdog;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import java.sql.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import com.google.gson.Gson;

public class PMToolsWatchdog {

    private final static boolean m_bRun = true;
    private static Connection m_sqlConn;
    private final static int m_iNumThreads = 2;   // maximum number of running ffmpeg instances
    private final static long m_lTTL = 72000000; // Age of files to delete in MS

    private final static String m_sTmp = "/tmp";
    private final static String m_sDownloads = "/home/pmwatchdog/downloads";
    private final static String m_sLog = "/home/pmwatchdog/watchdog.log";

    public static void log(String sText) {
        try {
            Format f = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
            String s = "[" + f.format(Calendar.getInstance().getTime()) + "] - " + sText + System.lineSeparator();
            Path path = Paths.get(m_sLog);
            Files.write(path, s.getBytes(), APPEND, CREATE);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            m_sqlConn = DriverManager.getConnection("jdbc:mysql://NOPE", "NOPE", "NOPE");

            /*
                status
                    new         - new file not processed
                    running     - currently being chewed on by ffmpeg
                    done        - finished by ffmpeg but not by watchdog
                    finished    - finished by watchdog, countdown starts
            */
            // Can we launch another process?
            if (getThreadCount() < m_iNumThreads) {
                // Is there anything in the queue?
                if (getNewestCount() > 0) {
                    String[] sNew = getNewestFromQueue();   // 0: id, 1: filename, 2: options (json), 3: outname
                    String[] sOpts = new Gson().fromJson(sNew[2], String[].class);
                    log("Starting ffmpeg");
                    log("id: " + sNew[0] + " | filename: " + sNew[1] + " | outname: " + sNew[3]);
                    log("options: " + sNew[2]);
                    new ffmpeg().launch(Integer.parseInt(sNew[0]), sOpts);
                }
            }
            processStatusDone();
            processStatusFinished();
            processStatusError();
            m_sqlConn.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static int getThreadCount() throws SQLException {
        PreparedStatement sqlStatement = m_sqlConn.prepareStatement("SELECT COUNT(*) AS total FROM queue WHERE status=?");
        sqlStatement.setString(1, "running");
        ResultSet rs = sqlStatement.executeQuery();
        rs.next();
        return rs.getInt("total");
    }

    private static int getNewestCount() throws SQLException {
        PreparedStatement sqlStatement = m_sqlConn.prepareStatement("SELECT COUNT(*) AS total FROM queue WHERE status=?");
        sqlStatement.setString(1, "new");
        ResultSet rs = sqlStatement.executeQuery();
        rs.next();
        return rs.getInt("total");
    }

    private static String[] getNewestFromQueue() throws SQLException {
        PreparedStatement sqlStatement = m_sqlConn.prepareStatement("SELECT * FROM queue WHERE status=? LIMIT 1");
        sqlStatement.setString(1, "new");
        ResultSet rs = sqlStatement.executeQuery();
        rs.next();
        String[] sRet = {Integer.toString(rs.getInt("id")), rs.getString("filename"), rs.getString("options"), rs.getString("outname")};
        return sRet;
    }

    private static void setStatusFinished(int iID) throws SQLException {
        String sDate = Long.toString(System.currentTimeMillis() / 1000L);
        PreparedStatement sqlStatement = m_sqlConn.prepareStatement("UPDATE queue SET status=?, timestamp=? WHERE id=?");
        sqlStatement.setString(1, "finished");
        sqlStatement.setString(2, sDate);
        sqlStatement.setInt(3, iID);
        sqlStatement.executeUpdate();
    }

    private static void processStatusDone() throws SQLException {
        PreparedStatement sqlStatement = m_sqlConn.prepareStatement("SELECT * FROM queue WHERE status=?");
        sqlStatement.setString(1, "done");
        ResultSet rs = sqlStatement.executeQuery();
        while (rs.next()) {
            setStatusFinished(rs.getInt("id"));
        }
    }

    private static void processStatusFinished() throws SQLException {
//        long lNow = (System.currentTimeMillis() / 1000L);
        long lNow = System.currentTimeMillis();
        long lTS;
        PreparedStatement sqlStatement = m_sqlConn.prepareStatement("SELECT * FROM queue WHERE status=?");
        sqlStatement.setString(1, "finished");
        ResultSet rs = sqlStatement.executeQuery();
        while (rs.next()) {
            lTS = Long.parseLong(rs.getString("timestamp"));
            if (lNow - lTS >= m_lTTL) {
                int iID = rs.getInt("id");
                // this is supposed to delete the file when it's done
                // but it doesn't.
                // that means I have to delete them once a week.
                // ლ(ಠ益ಠლ
                //deleteFile(iID, m_sTmp + '/' + rs.getString("filename"));
                //deleteFile(iID, m_sDownloads + '/' + rs.getString("outname"));
            }
        }
    }

    // almost the same as StatusFinished, but we don't worry about TTL
    // additional error handling will go here
    private static void processStatusError() throws SQLException {
        PreparedStatement sqlStatement = m_sqlConn.prepareStatement("SELECT * FROM queue WHERE status=?");
        sqlStatement.setString(1, "error");
        ResultSet rs = sqlStatement.executeQuery();
        while (rs.next()) {
            int iID = rs.getInt("id");
            //deleteFile(iID, rs.getString("filename"));
            //deleteFile(iID, rs.getString("outname"));
        }
    }

    // ლ(ಠ益ಠლ
    private static void deleteFile(int id, String sFile) throws SQLException {
        File f = new File(sFile);
        f.delete();
        PreparedStatement statement = m_sqlConn.prepareStatement("DELETE FROM queue WHERE id=?");
        statement.setInt(1, id);
        statement.execute();
    }
}
