#!/usr/bin/env node

var port = 56565;
if (3 <= process.argv.length) {
	port = process.argv[2];
}

var forever = require('forever');
forever.startDaemon('ktmsaver.js', {args:[port]});