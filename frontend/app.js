const { app, BrowserWindow } = require("electron");
const url = require("url");
const path = require("path");
//var kill = require("tree-kill");

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    resizable: false,
    autoHideMenuBar: true,
    width: 686,
    height: 880,
    x: 64,
    y: 64,
    webPreferences: {
      nodeIntegration: true,
    },
  });

  var child = require("child_process").spawn(
    app.getAppPath() + "\\lsworker.bat"
  );

  mainWindow.loadURL(
    url.format({
      pathname: path.join(__dirname, `/dist/luckystackworker/index.html`),
      protocol: "file:",
      slashes: true,
    })
  );

}

app.on("ready", createWindow);

app.on("window-all-closed", function () {
  if (process.platform !== "darwin") app.quit();
});

app.on("activate", function () {
  if (mainWindow === null) createWindow();
});
