#!/usr/bin/env node

const DEFAULT_PORT = "56565";

var port = DEFAULT_PORT;
if (3 <= process.argv.length) {
	port = process.argv[2];
}

console.log("listening on port " + port);

var path = require('path');
var sourceDir = path.dirname(process.argv[1]);

var forever = require('forever');
forever.startDaemon('ktmsaver.js', {args:[port], sourceDir:sourceDir});
