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
                <img src="misc/tplogo.png" style="float: left; background-color: #fff;" />
                <div class="headertext">PM Tools</div>
            </div>
            <div id="inputForm">
                <div id="loadingBox" hidden>
                    <div class="loadingBoxPos">
                        <div id="progressText"></div>
                        <progress id="progressBar"></progress>
                        <object id="loadingAnimation" type="image/svg+xml" data="misc/squares.svg" hidden></object>
                        <br />
                        <input id="btnReset" type="button" value="Close" hidden />
                    </div>
                </div>
                <div class="floater">
                    <form id="fileProcessor" action="fileProcessor" method="post" enctype="multipart/form-data">
                        <input type="file" name="file" class="inputFile" />
                        <ul>
                            <li>
                                <input id="selectFormatBox" type="checkbox" name="convertFormat" class="checkboxTP" />
                                Convert to
                                <select id="selectFormat" name="format">
                                    <optgroup label="Video">
                                        <option value="mp4">mp4</option>
                                    </optgroup>
                                    <optgroup label="Audio">
                                        <option value="mp3">mp3</option>
                                        <option value="m4a">m4a</option>
                                    </optgroup>
                                    <!--
                                    <optgroup label="Video">
                                        <option value="asf">asf</option>
                                        <option value="avi">avi</option>
                                        <option value="dvd">dvd</option>
                                        <option value="flv">flv</option>
                                        <option value="mkv">mkv</option>
                                        <option value="m4v">m4v</option>
                                        <option value="mp4" selected>mp4</option>
                                        <option value="mpeg">mpeg</option>
                                        <option value="oggv">ogg video</option>
                                        <option value="webm">webm</option>
                                    </optgroup>
                                    <optgroup label="Audio">
                                        <option value="aiff">aiff</option>
                                        <option value="flac">flac</option>
                                        <option value="mp2">mp2</option>
                                        <option value="mp3">mp3</option>
                                        <option value="m4a">m4a</option>
                                        <option value="ogga">ogg audio</option>
                                        <option value="opus">opus</option>
                                    </optgroup>
                                    -->
                                </select>
                            </li>
                            <li>
                                <input id="silentAudio" type="checkbox" name="addSilence" />
                                Add Silent Audio
                            </li>
                            <li>
                                <input type="checkbox" name="resize" id="resizeBox">
                                Change Resolution
                                <div id="resize" style="margin-left: 30px;" hidden>
                                    <select id="resizeSelector" name="resizeResolution">
                                        <option value="small">Standard (320x180, 300k)</option>
                                        <option value="medium">Large (480x270, 500k)</option>
                                        <option value="large">XL (640x360, 800k)</option>
                                        <option value="xlarge">480p (854x480, 1000k)</option>
                                        <option value="720p">720p (1280x720, 1200k)</option>
                                        <option value="1080p">1080p (1920x1080, 1500k)</option>
                                    </select>
                                </div>
                            </li>
                            <li>
                                <input id="extendBox" type="checkbox" name="extendTo15" />
                                Extend to 15 Seconds <span id="extendWarning"></span>
                                <div id="extendBoxInfo" style="margin-left: 30px;" hidden>
                                    Must be used separately.<br />
                                    Freeze last frame to 15 seconds.
                                </div>
                            </li>
                        </ul>
                        <div id="advancedHeader">Advanced Options</div>
                        <div id="advancedOptions" hidden>
                        <ul>
                            <li>
                                <input type="checkbox" name="resizeCustom" id="customResolutionBox" />
                                Custom Resolution
                                <div id="customResolution" style="margin-left: 30px;" hidden>
                                    <input id="cResWidth" type="text" name="resizeWidth" size="2" placeholder="Width" />
                                    x
                                    <input id="cResHeight" type="text" name="resizeHeight" size="3" placeholder="Height" />
                                </div>
                            </li>
                            <li>
                                <input id="brVid" type="checkbox" name="bitrateVideo" />
                                Video Bitrate:
                                <input id="brVidText" type="text" name="bitrateVideoValue" value="800" size="1" />
                                kbit
                            </li>
                            <li>
                                <input id="brAud" type="checkbox" name="bitrateAudio" />
                                Audio Bitrate:
                                <input id="brAudText" type="text" name="bitrateAudioValue" value="64" size="1" />
                                kbit
                            </li>
                            <!--
                            <li>
                                <input type="checkbox" name="samplerate">
                                Sample Rate:
                                <input tyoe="text" name="samplerateValue" value="44100" size="1" />
                                hz
                            </li>
                            -->
                            <li>
                                <input type="checkbox" name="cutHT" id="htBox" />
                                Heads & Tails
                                <div id="ht" style="margin-left: 30px;" hidden>
                                    <table>
                                        <tr><td></td><td>HH</td><td>MM</td><td>SS</td></tr>
                                        <tr>
                                            <td>Start</td>
                                            <td><input id="htshh" type="text" name="hHour" size="1" value="00" /></td>
                                            <td><input id="htsmm" type="text" name="hMin" size="1" value="00" /></td>
                                            <td><input id="htsss" type="text" name="hSec" size="1" value="00" /></td>
                                        </tr>
                                        <tr>
                                            <td>To</td>
                                            <td><input id="htthh" type="text" name="tHour" size="1" value="00" /></td>
                                            <td><input id="httmm" type="text" name="tMin" size="1" value="00" /></td>
                                            <td><input id="httss" type="text" name="tSec" size="1" value="00" /></td>
                                        </tr>
                                    </table>
                                </div>
                            </li>
                            <!--
                            <li>
                                <input type="checkbox" name="crop" id="cropBox" />
                                Crop
                                <div id="crop" style="margin-left: 30px;" hidden>
                                    <table>
                                        <tr><td>Out X</td><td>Out Y</td><td>Start X</td><td>Start Y</td></tr>
                                        <tr>
                                            <td><input type="text" name="cOutX" size="1" value="0" /></td>
                                            <td><input type="text" name="cOutY" size="1" value="0" /></td>
                                            <td><input type="text" name="cStartX" size="1" value="0" /></td>
                                            <td><input type="text" name="cStartY" size="1" value="0" /></td>
                                        </tr>
                                    </table>
                                </div>
                            </li>
                            -->
                        </ul>
                        </div>
                        <input id="btnSubmit" type="submit" name="Submit" value="Submit" />
                    </form>
                </div>
            </div>
        </div>
        <script src="js/pmtools.js"></script>
    </body>
</html>
