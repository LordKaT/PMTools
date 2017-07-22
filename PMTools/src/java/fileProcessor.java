// This is basically one long script that builts a complex ffmpeg string.
// Logging anything in this ended up being pointless since it's easier to just
// capture the ffmpeg output and find out what broke that way.
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import Utils.PMUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@WebServlet("/fileProcessor")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 10000,
        maxFileSize = 1024 * 1024 * 10000,
        maxRequestSize = 1024 * 1024 * 10000
)

public class fileProcessor extends HttpServlet {
    //private final String m_sFFMPEG = "d:\\ffmpeg\\ffprobe.exe";
    private final String m_sFFMPEG = "ffmpeg";

    public void log(String sText) {
        try {
            Format f = new SimpleDateFormat("yyy-MM-dd HH:mm:ss");
            String s = "[" + f.format(Calendar.getInstance().getTime()) + "] - " + sText + System.lineSeparator();
            Path path = Paths.get("/home/pmwatchdog/logs/ffprobe-" + f.format(Calendar.getInstance().getTime()) + ".log");
            Files.write(path, s.getBytes(), APPEND, CREATE);
        } catch (IOException e) {
          // log the log exception. ok sure.
        }
    }

    protected void processRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {
        try {
            httpResponse.setContentType("application/json");
            String sFilename = PMUtils.getFilename(httpRequest);
            String sDate = Long.toString(System.currentTimeMillis() / 1000L);
            String inputFile = "/tmp/" + sFilename;
            String processedName;
            String outputFile;

            ArrayList<String> lCommands = new ArrayList();
            ArrayList<String> lFilters = new ArrayList();

            // get the width/height of the video for using later
            // specifically when we're dealing with those portrait mode videos.
            // Cross your fingers that ffprobe never changes its commands and output.
            String[] sFFProbe = {
                "ffprobe",
                "-v", "error",
                "-of", "default=noprint_wrappers=1:nokey=1",
                "-select_streams", "v:0",
                "-show_entries", "stream_tags=rotate:stream=width,height",
                inputFile};

            Process p = Runtime.getRuntime().exec(sFFProbe);
            BufferedReader buffReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();
            String sWidth = buffReader.readLine();
            String sHeight = buffReader.readLine();
            String sRotate = buffReader.readLine();

            // ffprobe reports the video proper WxH, eg 1920x1080
            // portrait mode videos are *mostly* normal, eg 1920x1080
            // EXCEPT the rotation flag is set.
            // If the rotation flag is set, flip the W and H, because we end up
            // dropping the rotation flag later.
            // Other portrait videos are set like you'd except, eg 1080x1920
            // So we don't actually need to flip anything.
            // tl;dr: why is the rotation flag a thing?
            if (sRotate != null) {
                String sTemp = sWidth;
                sWidth = sHeight;
                sHeight = sTemp;
            }

            // Now we check for Extending to 15 Seconds
            // This exists because some clients send us videos shorter than 15 seconds
            // but the internal system doesn't process videos shorter than15 seconds
            // Why? I don't know. Mine is not to know, but to do and die.

            // Extending to 15 seconds requires a very specific order of commands
            // otherwise ffmpeg gets really really confused.

            // First we need to set a blank video feed as an input that's 15 seconds long
            if (httpRequest.getParameter("extendTo15") != null) {
                lCommands.add("-f");
                lCommands.add("lavfi");
                lCommands.add("-i");
                lCommands.add("nullsrc=s=" + sWidth + "x" + sHeight + ":d=16");
            }

            // Net we need to input the actual file.
            // If we're not extended to 15 seconds we just post the input file.
            lCommands.add("-i");
            lCommands.add(inputFile);

            // Now if we're extending to 15 seconds we need to do some ffmpeg magic
            // where we overlay the blank nullsrc video with the input video
            // This then frame holds the last frame of the input video until it
            // fills the time of the null video.
            // This is such a giant hack but the other option is to take the
            // input video, output it to multiple files and stitch them together
            // on the backend then output a full file. Our use case is that the
            // video is only extended to 15 seconds to make it compliant with
            // the internal system, but the video is revoked by an operator
            // so there's no reason to loop it.
            // less work = more play.
            if (httpRequest.getParameter("extendTo15") != null) {
                lCommands.add("-filter_complex");
                lCommands.add("[0:v]overlay[video]");
                lCommands.add("-map");
                lCommands.add("[video]");
                lCommands.add("-t");
                lCommands.add("16");
                lCommands.add("-shortest");
            }

            // Used to just pull from /dev/null, but this is cleaner.
            // All we're doing is force-adding a silent track, mapping it to 0:0 1:0
            // and ... that's it. ffmpeg does the rest.
            if (httpRequest.getParameter("addSilence") != null) {
                lCommands.add("-f");
                lCommands.add("lavfi");

                lCommands.add("-i");
                lCommands.add("aevalsrc=0");

                lCommands.add("-vcodec");
                lCommands.add("copy");

                lCommands.add("-acodec");
                lCommands.add("aac");

                lCommands.add("-map");
                lCommands.add("0:0");

                lCommands.add("-map");
                lCommands.add("1:0");

                lCommands.add("-shortest");
            }

            // Crop the video
            if (httpRequest.getParameter("crop") != null) {
                String sOutX = httpRequest.getParameter("cOutX");
                String sOutY = httpRequest.getParameter("cOutY");
                String sStartX = httpRequest.getParameter("cStartX");
                String sStartY = httpRequest.getParameter("cStartY");

                lFilters.add("crop=" + sOutX + ":" + sOutY + ":" + sStartX + ":" + sStartY);
            }

            // Resize the video to pre-set resolutions that are compatible with
            // our internal system
            if (httpRequest.getParameter("resize") != null) {
                String sSize = httpRequest.getParameter("resizeResolution");
                String sResolution;
                String sBitrate;

                if (sSize.equals("small")) {
                    sResolution = "320x180";
                    sBitrate = "300k";
                } else if (sSize.equals("medium")) {
                    sResolution = "480x70";
                    sBitrate = "500k";
                } else if (sSize.equals("large")) {
                    sResolution = "640x360";
                    sBitrate = "800k";
                } else if (sSize.equals("xlarge")) {
                    sResolution = "854x480";
                    sBitrate = "1000k";
                } else if (sSize.equals("720p")) {
                    sResolution = "1280x720";
                    sBitrate = "1200k";
                } else if (sSize.equals("1080p")) {
                    sResolution = "1920x1080";
                    sBitrate = "1500k";
                } else {
                    // this is basically a middle finger to QA. Hi Brendon.
                    sResolution = "1x1";
                    sBitrate = "1k";
                }
                lCommands.add("-b:v");
                lCommands.add(sBitrate);

                lFilters.add("scale=" + sResolution);
            }

            // Custom resize for the rare time we needed different resolutions
            if (httpRequest.getParameter("resizeCustom") != null) {
                lFilters.add("scale=" + httpRequest.getParameter("resizeWidth") + "x" + httpRequest.getParameter("resizeHeight"));
            }

            // Custom video bitrates
            if (httpRequest.getParameter("bitrateVideo") != null) {
                String sBitrateVideo = httpRequest.getParameter("bitrateVideoValue");
                lCommands.add("-b:v");
                lCommands.add(sBitrateVideo + "k");
            }

            // Custom audio bitrates
            if (httpRequest.getParameter("bitrateAudio") != null) {
                String sBitrateAudio = httpRequest.getParameter("bitrateAudioValue");
                lCommands.add("-b:a");
                lCommands.add(sBitrateAudio + "k");
            }

            // Custom audio sample rates
            if (httpRequest.getParameter("samplerate") != null) {
                String sSampleRate = httpRequest.getParameter("samplerateValue");
                lCommands.add("-ar");
                lCommands.add(sSampleRate);
            }

            // Convert to different formats.
            // Eventually this was phased down to basically just mp4, m4a, and mp3, because that's how we roll.
            if (httpRequest.getParameter("convertFormat") != null) {
                String sFormat = httpRequest.getParameter("format");
                String sExt;

                if (sFormat.equals("asf")) {
                    sExt = ".asf";
                } else if (sFormat.equals("avi")) {
                    sExt = ".avi";
                } else if (sFormat.equals("dvd")) {
                    sExt = ".dvd";
                } else if (sFormat.equals("flv")) {
                    sExt = ".flv";
                } else if (sFormat.equals("mkv")) {
                    sExt = ".mkv";
                } else if (sFormat.equals("mp4")) {
                    sExt = ".mp4";
                } else if (sFormat.equals("mpeg")) {
                    sExt = ".mpg";
                } else if (sFormat.equals("oggv")) {
                    sExt = ".ogg";
                } else if (sFormat.equals("webm")) {
                    sExt = ".webm";
                } else if (sFormat.equals("aiff")) {
                    sExt = ".aiff";
                } else if (sFormat.equals("flac")) {
                    sExt = ".flac";
                } else if (sFormat.equals("mp2")) {
                    sExt = ".mp2";
                } else if (sFormat.equals("mp3")) {
                    sExt = ".mp3";
                } else if (sFormat.equals("m4a")) {
                    sExt = ".m4a";
                } else if (sFormat.equals("ogga")) {
                    sExt = ".oga";
                } else if (sFormat.equals("opus")) {
                    sExt = ".opus";
                } else {
                    sExt = ".gif";
                }
                processedName = sFilename + "-" + sDate + sExt;
            } else {
                processedName = sFilename + "-" + sDate + "." + PMUtils.getFileExtension(sFilename);
            }

            // Cuts Heads and Tails based on custom input
            if (httpRequest.getParameter("cutHT") != null) {
                String sHeadHour = httpRequest.getParameter("hHour");
                String sHeadMin = httpRequest.getParameter("hMin");
                String sHeadSec = httpRequest.getParameter("hSec");
                String sTailHour = httpRequest.getParameter("tHour");
                String sTailMin = httpRequest.getParameter("tMin");
                String sTailSec = httpRequest.getParameter("tSec");

                lCommands.add("-ss");
                lCommands.add(sHeadHour + ":" + sHeadMin + ":" + sHeadSec);

                lCommands.add("-to");
                lCommands.add(sTailHour + ":" + sTailMin + ":" + sTailSec);
            }

            // If there are any special video filters to apply (like extendTo15), add them
            if (!lFilters.isEmpty()) {
                lCommands.add("-vf");
                String sFilters = "";
                //String sFilters = "\"";
                for (int i = 0; i < lFilters.size(); i++) {
                    if (i > 0) {
                        sFilters += ", ";
                    }
                    sFilters += lFilters.get(i);
                }
                //sFilters += "\"";
                lCommands.add(sFilters);
            }

            outputFile = PMUtils.getDownloadPath() + File.separator + processedName;

            lCommands.add(outputFile);

            // PMUtils.enqueue is expecting a json string to store in the SQL DB.
            String jsonCommands = new Gson().toJson(lCommands);

            // queue the file for processing
            long lID = PMUtils.enqueue(sFilename, jsonCommands, processedName);

            // Tell the user we're good to go.
            PrintWriter out = httpResponse.getWriter();
            out.print(new Gson().toJson(lID));
            out.flush();
        } catch (ClassNotFoundException e) {
        } catch (SQLException e) {
            /*
            PrintWriter pw = new PrintWriter(new File("\\build\\web\\uploads\\pw.log"));
            pw.println(e.getMessage());
            pw.close();
            */
        } catch (InterruptedException e) {
            /*
            PrintWriter pw = new PrintWriter(new File("\\test\\pw.log"));
            pw.println(e.getMessage());
            pw.close();
             */
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "THIS DOES THINGS. MAGICAL THING.";
    }

}
