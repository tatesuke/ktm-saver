#!/usr/bin/env node

var forever = require('forever');

forever.startDaemon('ktmsaver.js', {});

