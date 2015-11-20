module.exports = {
    download: function (message, win, fail) {
        cordova.exec(win, fail, "Downloader", "download", [message]);
    }  
};

