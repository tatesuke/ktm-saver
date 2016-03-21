// ==UserScript==
// @name         ktmSaverForBrouser
// @namespace    http://tatesuke.github.io/KanTanMarkdown/
// @version      0.1
// @description  
// @author       tatesuke
// @match        file:///*/*
// @grant        none
// ==/UserScript==
/* jshint -W097 */
'use strict';

var port = 8090;

(function() {
    // ローカルファイル以外実行しない
    var url = location.href;
    if (!url.match(/^file:\/\/\//)) {
        return;
    }

    // かんたんMarkdownでなければ実行しない
    var isKantanMarkdown = document.querySelector("#kantanVersion");
    if (!isKantanMarkdown) {
        return;
    }

    // 古いバージョンでは実行しない
    if (!getHTMLForSave) {
        return;
    }

    // オリジナルのkeydownイベントを退避させる
    var originalEventListener;
    for (var i = 0; i < eventListeners.length; i++) {
        var el = eventListeners[i];
        if ((el.element == document.body) && (el.eventName == "keydown")) {
            originalEventListener = el.callback;
            break;
        }
    }
    document.body.removeEventListener("keydown", originalEventListener);

    var saveQueue = null; // キューをストック 
    var savePreviewWait = 50;
    // 新しいkeydownイベントを定義
    document.body.addEventListener("keydown", function(event) {
        var code = (event.keyCode ? event.keyCode : event.which);

        if (!isDrawMode() && (code == 83) && (event.ctrlKey || event.metaKey)) {
            // CTRL+Sで保存する
            event.preventDefault();

            // イベント発生の都度、キューをキャンセル 
            clearTimeout(saveQueue);

            // waitで指定したミリ秒後に所定の処理を実行 
            // 経過前に再度イベントが発生した場合
            // キューをキャンセルして再カウント 
            saveQueue = setTimeout(startOverwriteSave, savePreviewWait);
            return false;
        } else {
            originalEventListener(event);
        }

    });

    function startOverwriteSave() {

        var id = new Date().getTime();

        var body = document.body;
        var tempIframe = document.createElement("iframe");
        //tempIframe.style.display = "none";
        body.appendChild(tempIframe);

        var fbody = tempIframe.contentWindow.document.body;

        var form = document.createElement("form");
        form.method = "post";
        form.action = "http://127.0.0.1:" + port + "/save";
        form.style.display = "none";
        fbody.appendChild(form);

        var idElement = document.createElement("input");
        idElement.type = "text";
        idElement.name = "id";
        idElement.value = id;
        form.appendChild(idElement);

        var filePathElement = document.createElement("input");
        filePathElement.type = "text";
        filePathElement.name = "filePath";
        filePathElement.value = decodeURIComponent(location.href);
        form.appendChild(filePathElement);

        var contentElement = document.createElement("input");
        contentElement.type = "hidden";
        contentElement.name = "content";
        contentElement.value = getHTMLForSave();
        form.appendChild(contentElement);


        tempIframe.onload = function(e) {
            console.timeEnd("hoge");
            this.onload = null;
            var script = document.createElement("script");
            script.src = "http://127.0.0.1:" + port + "/check?id=" + id;
            script.class = "kantanSaveScript";
            script.onerror = function() {
                alert("上書き保存に失敗しました。\n" +
                    "クライアントアプリが起動しているか確かめてください。");
            };
            document.querySelector("#updateScriptArea").appendChild(script);
        }
        console.time("hoge");
        form.submit();
    }

})();