const { app, BrowserWindow } = require("electron");
const url = require("url");
const path = require("path");

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    resizable: false,
    autoHideMenuBar: true,
    width: 688,
    height: 756,
    x: 64,
    y: 64,
  });

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
