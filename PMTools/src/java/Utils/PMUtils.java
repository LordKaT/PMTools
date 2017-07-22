package Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

public class PMUtils {

    private final static String m_sTmp = "/tmp";
    private final static String m_sDownloads = "/home/pmwatchdog/downloads";

    static private String getFilenanmeFromPart(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        System.out.println("content-disposition header= " + contentDisp);
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                String sTemp = token.substring(token.indexOf("=") + 2, token.length() - 1);
                int i = sTemp.lastIndexOf("\\");
                return sTemp.substring(i + 1);
            }
        }
        return null;
    }

    static public String getFilename(HttpServletRequest httpRequest) throws ServletException, IOException {
        String sFilename = null;
        for (Part part : httpRequest.getParts()) {
            String sTemp = PMUtils.getFilenanmeFromPart(part);
            if (sTemp != null) {
                sFilename = sTemp;
                part.write(PMUtils.getUploadPath(httpRequest) + File.separator + sFilename);
            }
        }
        return sFilename;
    }

    static public String getFileExtension(String sFilename) throws IOException {
        String sExt = "";
        int i = sFilename.lastIndexOf(".");
        int p = Math.max(sFilename.lastIndexOf("/"), sFilename.lastIndexOf("\\"));
        if (i > p) {
            sExt = sFilename.substring(i + 1);
        }
        return sExt;
    }

    static public String getUploadPath(HttpServletRequest httpRequest) throws IOException {
        /*
        String appPath = httpRequest.getServletContext().getRealPath("");
        //String uploadPath = appPath + File.separator + "uploads";
        String uploadPath = appPath + "uploads";
        File saveDir = new File(uploadPath);
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        //return uploadPath;
         */
        return m_sTmp;
    }

    static public String getDownloadPath() {
        return m_sDownloads;
    }

    static public long enqueue(String sFilename, String sOptions, String sOutname) throws ClassNotFoundException, SQLException, FileNotFoundException {
        Connection sqlConn;
        PreparedStatement sqlPStatement;
        Class.forName("com.mysql.jdbc.Driver");
        sqlConn = DriverManager.getConnection("jdbc:mysql://NOPE", "NOPE", "NOPE");

        sqlPStatement = sqlConn.prepareStatement("insert into queue (filename, options, outname, status) values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        sqlPStatement.setString(1, sFilename);
        sqlPStatement.setString(2, sOptions);
        sqlPStatement.setString(3, sOutname);
        sqlPStatement.setString(4, "new");
        int iAffectedRows = sqlPStatement.executeUpdate();
        if (iAffectedRows == 0) {
            throw new SQLException("Failed to queue item: no rows affected.");
        }
        ResultSet rsKeys = sqlPStatement.getGeneratedKeys();
        if (!rsKeys.next()) {
            throw new SQLException("Failed to queue item: no ID returned, but rows affected?");
        }
        long lRet = rsKeys.getLong(1);
        sqlConn.close();
        return lRet;
    }

    static public void dequeue(long lID) throws ClassNotFoundException, SQLException {
        Connection sqlConn;
        PreparedStatement sqlPStatement;
        Class.forName("com.mysql.jdbc.Driver");
        sqlConn = DriverManager.getConnection("jdbc:mysql://NOPE", "NOPE", "NOPE");

        sqlPStatement = sqlConn.prepareStatement("delete from queue where id = ?");
        sqlPStatement.setLong(1, lID);
        int iAffectedRows = sqlPStatement.executeUpdate();
        if (iAffectedRows == 0) {
            throw new SQLException("Failed to dequeue.");
        }
        sqlConn.close();
    }

    static private String[] combinePBStrings(String[] sCommand, String[] sParams) {
        String[] sCombined = new String[sCommand.length + sParams.length];
        System.arraycopy(sCommand, 0, sCombined, 0, sCommand.length);
        System.arraycopy(sParams, 0, sCombined, sCommand.length, sParams.length);
        return sCombined;
    }

    static public void startFFMPEG(HttpServletRequest httpRequest, String... sParams) throws InterruptedException, FileNotFoundException, IOException {
        //String[] sCommand = {"d:\\ffmpeg.exe"};
        String[] sCommand = {"ffmpeg"};
        String[] sPBCommand = combinePBStrings(sCommand, sParams);
        /*
        File fLog = new File(sParams[sParams.length - 1] + ".log");
        fLog.createNewFile();
         */
        ProcessBuilder pb = new ProcessBuilder(sPBCommand);
//        pb.redirectErrorStream(true);
//        pb.redirectInput(ProcessBuilder.Redirect.PIPE);
//        pb.redirectOutput(fLog);
        final Process p = pb.start();
        new Thread() {
            @Override
            public void run() {
                Scanner sc = new Scanner(p.getErrorStream());
                Pattern pDuration = Pattern.compile("(?<=Duration: )[^,]*");
                String sDur = sc.findWithinHorizon(pDuration, 0);
                if (sDur == null) {
                    // no duration, invalid file or ffmpeg error
                    // mark progress as "error"
                    // write debug log to disk
                    throw new RuntimeException("Hurrrrrr.");
                }
                String[] hms = sDur.split(":");
                double totalSeconds = Integer.parseInt(hms[0]) * 3600
                        + Integer.parseInt(hms[1]) * 60
                        + Double.parseDouble(hms[2]);
                Pattern pTime = Pattern.compile("(?<=time=)[\\d:.]*");
                String match;
                String[] matchSplit;
                double pct;
                double currentSeconds;
                while ((match = sc.findWithinHorizon(pTime, 0)) != null) {
                    matchSplit = match.split(":");
                    currentSeconds = Integer.parseInt(matchSplit[0]) * 3600
                            + Integer.parseInt(matchSplit[1]) * 60
                            + Double.parseDouble(matchSplit[2]);
                    pct = (currentSeconds / totalSeconds) * 100;
                    if (pct > 100) {
                        pct = 100;
                    }
                    // TODO: write pct to DB for tracking
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }.start();
        p.waitFor();
    }
}
