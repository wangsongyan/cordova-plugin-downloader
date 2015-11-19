cordova.define("cordova-plugin-download.Download", function(require, exports, module) { 
	module.exports = {
	    download: function (message, win, fail) {
	        cordova.exec(win, fail, "Downloader", "download", [message]);
	    }  
	};
});
