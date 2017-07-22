<%@page import="java.util.Date" %>
<%@page import="java.text.SimpleDateFormat" %>
<%@page contentType="text/html" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>PM Tools</title>
        <link rel="stylesheet" href="css/pmtools.css" />
        <link rel="stylesheet" href="css/squares.css" />
        <script src="js/jquery-3.1.1.min.js"></script>
    </head>
    <body>
        <div id="container">
            <div id="header">
                Talkpoint PM Tools
            </div>
            <div id="inputForm">
                <div id="loadingBox" hidden>
                    <div class="loadingBoxPos">
                        <div class='uil-squares-css' style="transform:scale(0.6);"><div><div></div></div><div><div></div></div><div><div></div></div><div><div></div></div><div><div></div></div><div><div></div></div><div><div></div></div><div><div></div></div></div>
                    </div>
                    <div class="loadingBoxPos">
                        Reticulating Splines
                    </div>
                </div>
                <div class="floater">
                    Processing finished.
                    <br />&nbsp;<br />
                    <a href="uploads/${requestScope.file}" download="${requestScope.file}">Click here to download your file</a>
                    <br />&nbsp;<br />
                    <a href="uploads/${requestScope.file}.log" download="${requestScope.file}.log">Debug Log</a> for this request
                    <br />&nbsp;<br />
                    <a href="/PMTools">Return to PM Tools</a>
                </div>
            </div>
    </body>
</html>
