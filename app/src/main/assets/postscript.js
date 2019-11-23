var head = document.getElementsByTagName('head')[0];

var meta = document.createElement('meta');
meta.name = "viewport";
meta.content = "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0";
head.appendChild(meta);

var link = document.createElement("link");
link.type = "text/css";
link.rel = "stylesheet";
link.href = "file:///android_asset/poststyle.css";
head.appendChild(link);