//
// KanTanMarkdown Saver
//
var port = (3 <= process.argv.length) ? process.argv[2] : "56565";

var ws = require('websocket.io');
var server = ws.listen(port, function(){
	console.log("lisning on " + port);
});

server.on('connection', function(socket) {
	
	socket.on('message', function(data) {
		var obj = JSON.parse(data);
		
		console.log("write to " + obj.filePath);
		
		var fs = require('fs');
		fs.writeFile(obj.filePath, obj.content, function (err) {
			if (err == null) {
				console.log("ok");
				socket.send("ok");
			} else {
				console.log(err);
				socket.send(JSON.stringify(err));
			}
		});
	});
	
});