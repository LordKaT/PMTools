$("#btnSubmit").click(function (e) {
    e.preventDefault();
    // y u no give me file?
    // todo: display error that says you're a dumb dumb.
    if ($(".inputFile").val() === "") {
        return false;
    }
    $("#btnSubmit").prop("disabled", true);
    $("#loadingBox").show();

    var formData = new FormData($("#fileProcessor")[0]);
    $.ajax({
        url: 'fileProcessor',
        type: 'POST',
        xhr: function () {
            var myXhr = $.ajaxSettings.xhr();
            if (myXhr.upload) {
                myXhr.upload.addEventListener('progress', progressHandler, false);
            }
            return myXhr;
        },
        beforeSend: beforeSendHandler,
        success: successHandler,
        error: errorHandler,
        data: formData,
        cache: false,
        contentType: false,
        processData: false
    });

    function progressHandler(e) {
        if (e.lengthComputable) {
            $("#progressBar").attr({value: e.loaded, max: e.total});
        }
    }

    function beforeSendHandler(e) {
        $("#progressText").html("Uploading...");
    }

    var g_id;
    var g_interv;

    function queueAjax() {
        var id = g_id;
        var interv = g_interv;
        $.ajax({
            type: 'get',
            url: 'checkStatus',
            dataType: 'JSON',
            data: {
                getStatus: id
            },
            success: function (data) {
                if (data[0] === "new") {
                    $("#progressText").html("In Queue");
                    $("#progressBar").hide();
                } else if (data[0] === "running") {
                    $("#progressText").html("Reticulating Splines");
                    $("#progressBar").hide();
                    $("#loadingAnimation").show();
                } else if (data[0] === "finished") {
                    $("#loadingAnimation").hide();
                    $("#progressText").html('Finished!<br /><a href="downloads/' + data[2] + '" download="' + data[2] + '">Click Here to Download</a>');
                    $("#progressBar").hide();
                    $("#btnReset").show();
                    clearInterval(interv);
                } else if (data[0] === "error") {
                    $("#progressText").html("Error!");
                    $("#progressBar").hide();
                }
            },
            error: function (data) {
            }
        });
    }

    function successHandler(e) {
        g_id = e;
        queueAjax();
        g_interv = setInterval(queueAjax, 2500);
    }

    function errorHandler(e) {
        console.log(e);
    }
});

function doCheckbox(box, data) {
    $(box).change(function () {
        if ($(this).is(":checked")) {
            if ($(this).is("#extendBox") || $("#extendBox").prop("checked") === false) {
                $(data).show();
            }
        } else {
            $(data).hide();
        }
    });
}

doCheckbox("#advancedMenuBox", "#advancedMenu");
doCheckbox("#htBox", "#ht");
doCheckbox("#customResolutionBox", "#customResolution");
doCheckbox("#extendBox", "#extendBoxInfo");
doCheckbox("#resizeBox", "#resize");
doCheckbox("#cropBox", "#crop");

// users change formats without checking the box
// make sure the box is checked.
$("#selectFormat").change(function() {
    $("#selectFormatBox").prop("checked", true);
});

$("#extendBox").change(function() {
    if ($(this).prop("checked") === true) {
        //$("input:checkbox").prop("checked", false);
        $('input[type="checkbox"]').not(this).prop("checked", true).trigger("click");
        $(this).prop("checked", true);
    }
});

$("input:checkbox").change(function() {
    if ($("#extendBox").prop("checked") === true) {
        $(this).prop("checked", false);
        $("#extendBox").prop("checked", true);
    }
});

$("#btnReset").click(function() {
    $("#fileProcessor")[0].reset();
    $("#loadingBox").hide();
    $("#btnReset").hide();
    $("#btnSubmit").prop("disabled", false);
});

$("li").click(function(e) {
    var input = e.target.id;
    if (input === "") {
        input = $(this).children('input').eq(0).attr('id');
        $("#" + input).trigger("click");
        return;
    }
});

$("#advancedHeader").click(function() {
    if ($("#advancedOptions").is(":visible")) {
        $("#advancedOptions").hide();
    } else {
        $("#advancedOptions").show();
    }
});
