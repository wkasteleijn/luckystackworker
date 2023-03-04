const { app, BrowserWindow } = require("electron");
const url = require("url");
const path = require("path");
const XMLHttpRequest = require("xhr2");

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    resizable: false,
    autoHideMenuBar: true,
    width: 688,
    height: 724,
    x: 64,
    y: 64,
  });
  mainWindow.removeMenu();
  if (process.platform === "darwin") {
    app.dock.hide();
    mainWindow.setSkipTaskbar(true);
  }

  mainWindow.loadURL(
    url.format({
      pathname: path.join(__dirname, `/dist/luckystackworker/index.html`),
      protocol: "file:",
      slashes: true,
    })
  );
  mainWindow.on("closed", () => {
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.open("PUT", "http://localhost:8080/api/profiles/exit");
    xmlHttp.send(null);
  });
}

app.on("ready", createWindow);

app.on("window-all-closed", function () {
  if (process.platform !== "darwin") {
    app.quit();
  }
});

app.on("activate", function () {
  if (mainWindow === null) createWindow();
});
