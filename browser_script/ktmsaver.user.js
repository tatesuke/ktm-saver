// ==UserScript==
// @name         ktmSaverForBrowser
// @namespace    https://github.com/tatesuke/ktmsaver
// @version      0.4
// @description  かんたんMarkdownで上書きを可能にするためのユーザスクリプト
// @author       tatesuke
// @match        http://tatesuke.github.io/KanTanMarkdown/**
// @match        file:///*/*
// @grant        none
// ==/UserScript==
/* jshint -W097 */
'use strict';

(function() {
    // かんたんMarkdownでなければ実行しない
    var isKantanMarkdown = document.querySelector("#kantanVersion");
    if (!isKantanMarkdown) {
        return;
    }

    // getHTMLForSave関数がないバージョンでは実行しない
    if (!getHTMLForSave) {
        return;
    }

    // ファイルパス取得
    var url = location.href;
    var filePath;
    setFilePath((url.match(/^file:\/\/\//)) ? decodeURIComponent(url.substr(8)) : "");
    function setFilePath(path) {
        if (path) {
            filePath = path;
            document.querySelector("#messageArea").innerHTML = filePath;
        } else {
            filePath = "";
            document.querySelector("#messageArea").innerHTML = "(未保存)";
        }
    }

    // UIを構築
    showKtmSaverUI();

    // 保存ボタンとCtrl+Sのオリジナルのイベントを退避させる
    var originalShrtcutKeyEvent;
    for (var i = 0; i < eventListeners.length; i++) {
        var el = eventListeners[i];
        if ((el.element == document.body) && (el.eventName == "keydown")) {
            originalShrtcutKeyEvent = el.callback;
            break;
        }
    }
    document.body.removeEventListener("keydown", originalShrtcutKeyEvent);

    // 新しいkeydownイベントを定義
    document.body.addEventListener("keydown", function(event) {
        var code = (event.keyCode ? event.keyCode : event.which);

        if (!isDrawMode() && (code == 83) && (event.ctrlKey || event.metaKey)) {
            // CTRL+Sのみ新しい処理に書き換える
            event.preventDefault();
            if (!saved || filePath === "") {
                queueOverwriteSave();
            }
            return false;
        } else {
            // CTRL+S以外はオリジナルそのまま使う
            return originalShrtcutKeyEvent(event);
        }
    });

    function showKtmSaverUI() {
        var baseButton = document.querySelector("#settingMenuButton");
        if (!baseButton) {
            baseButton = document.querySelector("#onlineMenuButton");
        }

        /* KTMSaverボタン */
        var ktmSaverMenuButton = document.createElement("button");
        ktmSaverMenuButton.innerHTML = "KTMSaver";
        ktmSaverMenuButton.style.height = baseButton.offsetHeight;
        ktmSaverMenuButton.style.position = "absolute";
        ktmSaverMenuButton.style.top = baseButton.offsetTop + "px";
        ktmSaverMenuButton.style.left = (baseButton.offsetLeft + baseButton.offsetWidth) + "px";
        on(ktmSaverMenuButton, "click", function() {
            var button = this;
            var menu = document.getElementById("ktmSaverMenu");
            menu.style.top = (this.offsetTop + this.scrollHeight) + "px";
            menu.style.left = this.offsetLeft + "px";
            showBlock(menu);
        });
        document.body.appendChild(ktmSaverMenuButton);

        /* KTMSaverメニュー */
        var ktmSaverMenu = document.createElement("div");
        ktmSaverMenu.classList.add("popupMenu");
        ktmSaverMenu.id  = "ktmSaverMenu";
        ktmSaverMenu.innerHTML =
            '<ul>' + 
            '<li><a href="#" id="ktmSaverOverwriteSaveButton">上書き保存(Ctrl + S)</a></li>' +
            '<li><a href="#" id="ktmSaverSaveAsButton">名前を付けて保存</a></li>' +
            '<li><hr></li>' + 
            '<li>クライアントAppポート番号:<input type="text" id="ktmSaverPort" value="' + getItem("ktmSaverPort", "56565") + '"></li>' +
            '<li><hr></li>' + 
            '<li><input type="checkbox" id="ktmSaverBackupEnabled" ' + getItem("ktmSaverBackupEnabled", "checked") + '>' + 
            '<label for="ktmSaverBackupEnabled">上書きするときにファイルをバックアップ</label></li>' +
            '<li>バックアップ保存先:<input type="text" id="ktmSaverBackupDir" value="' + getItem("ktmSaverBackupDir", "./") + '"></li>' +
            '<li>バックアップ世代数:<input type="number" id="ktmSaverBackupGeneration" min="0" value="' + getItem("ktmSaverBackupGeneration", "0") + '">' +
            '(0以下で無制限)</li>' +
            '<li><button id="cliseKtmSaverMenuButton">閉じる</button></li>' +
            '</ul>';
        document.body.appendChild(ktmSaverMenu);

        on("#ktmSaverSaveAsButton", "click", function(){
            queueSaveAs();
        });

        on("#ktmSaverOverwriteSaveButton", "click", function(){
            queueOverwriteSave();
        });

        on("#ktmSaverPort, #ktmSaverBackupDir, #ktmSaverBackupGeneration", "change", function() {
            setItem(this.id, this.value);
        });

        on ("#ktmSaverBackupEnabled", "change", function() {
            setItem(this.id, (this.cheked) ? "cheked" : "");
        });

        on("#cliseKtmSaverMenuButton", "click", function() {
            hide(ktmSaverMenu);
        });

        /* 接続中メッセージ */
        var connectingMessage = document.createElement("div");
        connectingMessage.innerHTML = "接続中..";
        connectingMessage.id = "ktmSaverConnectingMessage";
        connectingMessage.style.width = "150px";
        connectingMessage.style.height = (baseButton.offsetHeight - 2) + "px";
        connectingMessage.style.position = "absolute";
        connectingMessage.style.top = 0;
        connectingMessage.style.left = ((document.body.offsetWidth / 2) - (150 / 2)) + "px";
        connectingMessage.style.textAlign = "center";
        connectingMessage.style.color = "#c09853";
        connectingMessage.style.backgroundColor = "#fcf8e3";
        connectingMessage.style.border = "1px solid #fbeed5";
        connectingMessage.style.display = "none";
        document.body.appendChild(connectingMessage);

        /* 保存中メッセージ */
        var savingMessage = document.createElement("div");
        savingMessage.innerHTML = "保存中..";
        savingMessage.id = "ktmSaverSavinggMessage";
        savingMessage.style.width = "150px";
        savingMessage.style.height = (baseButton.offsetHeight - 2) + "px";
        savingMessage.style.position = "absolute";
        savingMessage.style.top = 0;
        savingMessage.style.left = ((document.body.offsetWidth / 2) - (150 / 2)) + "px";
        savingMessage.style.textAlign = "center";
        savingMessage.style.color = "#c09853";
        savingMessage.style.backgroundColor = "#fcf8e3";
        savingMessage.style.border = "1px solid #fbeed5";
        savingMessage.style.display = "none";
        document.body.appendChild(savingMessage);

        /* 保存完了メッセージ */
        var sucessMessage = document.createElement("div");
        sucessMessage.innerHTML = "保存完了";
        sucessMessage.id = "ktmSaverSuccessMessage";
        sucessMessage.style.width = "150px";
        sucessMessage.style.height = (baseButton.offsetHeight - 2) + "px";
        sucessMessage.style.position = "absolute";
        sucessMessage.style.top = 0;
        sucessMessage.style.left = ((document.body.offsetWidth / 2) - (150 / 2)) + "px";
        sucessMessage.style.textAlign = "center";
        sucessMessage.style.color = "#3a87ad";
        sucessMessage.style.backgroundColor = "#d9edf7";
        sucessMessage.style.border = "1px solid #bce8f1";
        sucessMessage.style.display = "none";
        document.body.appendChild(sucessMessage);

        /* 保存失敗メッセージ */
        var errorMessage = document.createElement("div");
        errorMessage.innerHTML = "！保存失敗！";
        errorMessage.id = "ktmSaverErrorMessage";
        errorMessage.style.width = "150px";
        errorMessage.style.height = (baseButton.offsetHeight - 2) + "px";
        errorMessage.style.position = "absolute";
        errorMessage.style.top = 0;
        errorMessage.style.left = ((document.body.offsetWidth / 2) - (150 / 2)) + "px";
        errorMessage.style.textAlign = "center";
        errorMessage.style.color = "#b94a48";
        errorMessage.style.backgroundColor = "#f2dede";
        errorMessage.style.border = "1px solid #eed3d7";
        errorMessage.style.display = "none";
        document.body.appendChild(errorMessage);
    }

    function getItem(name, defaultValue) {
        var value = localStorage.getItem("com.tatesuke.ktmsaver." + name);
        return (value) ? value : defaultValue;
    }

    function setItem(name, value) {
        localStorage.setItem("com.tatesuke.ktmsaver." + name, value);
    }

    // 名前を付けて保存
    var saveAsQueue = null;
    var clearMessageQueue = null;
    var queueWait = 50; 
    function queueSaveAs() {
        clearTimeout(saveAsQueue);
        clearTimeout(clearMessageQueue);
        overwriteSaveQueue = setTimeout(doSaveAs, queueWait);
    }
    function doSaveAs () {
        var html = getHTMLForSave();
        hide(document.querySelector("#ktmSaverSuccessMessage"));
        hide(document.querySelector("#ktmSaverErrorMessage"));
        showBlock(document.querySelector("#ktmSaverConnectingMessage"));

        var title = "無題";
        var titleElement = document.querySelector("h1");
        if (titleElement) {
            title = titleElement.innerText;
        }

        var data = {};
        data.action   = "SAVE_AS";
        data.fileDir  = null;
        data.fileName = title + ".html";
        data.backupEnabled = document.querySelector("#ktmSaverBackupEnabled").checked; 
        data.backupDir     = document.querySelector("#ktmSaverBackupDir").value;
        data.backupGeneration = document.querySelector("#ktmSaverBackupGeneration").value;

        doSave(data);
    }

    // 上書き保存
    var overwriteSaveQueue = null;
    function queueOverwriteSave() {
        clearTimeout(overwriteSaveQueue);
        clearTimeout(clearMessageQueue);
        overwriteSaveQueue = setTimeout(doOverwriteSave, queueWait);
    }

    function doOverwriteSave() {
        var html = getHTMLForSave();

        hide(document.querySelector("#ktmSaverSuccessMessage"));
        hide(document.querySelector("#ktmSaverErrorMessage"));
        showBlock(document.querySelector("#ktmSaverConnectingMessage"));

        if (filePath == "") {
            doSaveAs();
        } else {
            var data = {};
            var pos = filePath.lastIndexOf("/");
            pos = (pos == -1) ? filePath.lastIndexOf("\\") : pos;
            data.action = "OVERWRITE";
            data.fileDir = filePath.substring(0, pos);
            data.fileName = filePath.substr(pos + 1);
            data.backupEnabled = document.querySelector("#ktmSaverBackupEnabled").checked; 
            data.backupDir     = document.querySelector("#ktmSaverBackupDir").value;
            data.backupGeneration = document.querySelector("#ktmSaverBackupGeneration").value;

            doSave(data);
        }
    }

    function doSave(data) {
        var port = document.querySelector("#ktmSaverPort").value;
        var ws = new WebSocket('ws://localhost:' + port + '/ktmsaver/save');
        ws.onopen = function() {
            hide(document.querySelector("#ktmSaverConnectingMessage"));
            showBlock(document.querySelector("#ktmSaverSavinggMessage"));

            ws.send(JSON.stringify(data));
            ws.send(str2buff(getHTMLForSave()));
        };

        ws.onmessage = function(event) {
            hide(document.querySelector("#ktmSaverSavinggMessage"));
            var result = JSON.parse(event.data);

            if (!result.result) {
                showBlock(document.querySelector("#ktmSaverErrorMessage"));
                alert("保存に失敗した可能性があります" + result.message);
                saved = false;
                doPreview();
            } else if (result.result == "SUCCESS") {
                showBlock(document.querySelector("#ktmSaverSuccessMessage"));
                setFilePath(result.filePath);
                clearMessageQueue = setTimeout(function() {
                    hide(document.querySelector("#ktmSaverSuccessMessage"));
                }, 1000);
            } else if (result.result == "CANCEL") {
                setFilePath(filePath);
                saved = false;
                doPreview();
            } else if (result.result == "ERROR") {
                showBlock(document.querySelector("#ktmSaverErrorMessage"));
                alert("保存に失敗しました。\n" + result.message);
                setFilePath(filePath);
                saved = false;
                doPreview();
            } else {
                showBlock(document.querySelector("#ktmSaverErrorMessage"));
                alert("保存に失敗した可能性があります。\n" + result.message);
                saved = false;
                doPreview();
            }
            ws.close();
        };

        ws.onerror = function(e) {
            hide(document.querySelector("#ktmSaverConnectingMessage"));
            hide(document.querySelector("#ktmSaverSavinggMessage"));
            showBlock(document.querySelector("#ktmSaverErrorMessage"));
            alert("保存に失敗しました。クライアントAppとの接続中にエラーが発生しました。\n" +
                  "クライアントAppが起動しているか、ポート番号があっているかなど確認してください。");
            saved = false;
            doPreview();
        };
    }

    var str2buff = function(str){
        var ab_ = new ArrayBuffer(new Blob([str]).size);
        var bytes_ = new Uint8Array(ab_);

        var n = str.length,
            idx = -1,
            i, c;

        for(i = 0; i < n; ++i){
            c = str.charCodeAt(i);
            if(c <= 0x7F){
                bytes_[++idx] = c;
            } else if(c <= 0x7FF){
                bytes_[++idx] = 0xC0 | (c >>> 6);
                bytes_[++idx] = 0x80 | (c & 0x3F);
            } else if(c <= 0xFFFF){
                bytes_[++idx] = 0xE0 | (c >>> 12);
                bytes_[++idx] = 0x80 | ((c >>> 6) & 0x3F);
                bytes_[++idx] = 0x80 | (c & 0x3F);
            } else {
                bytes_[++idx] = 0xF0 | (c >>> 18);
                bytes_[++idx] = 0x80 | ((c >>> 12) & 0x3F);
                bytes_[++idx] = 0x80 | ((c >>> 6) & 0x3F);
                bytes_[++idx] = 0x80 | (c & 0x3F);
            }
        }
        return bytes_;
    }


    })();