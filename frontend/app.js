const { app, BrowserWindow, dialog } = require("electron");
const url = require("url");
const path = require("path");
const { spawn } = require("child_process");

const apiUrl = "http://localhost:36469/api";

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    resizable: false,
    autoHideMenuBar: true,
    width: process.platform !== "win32" ? 568 : 584,
    height: process.platform !== "win32" ? 608 : 618,
    x: 64,
    y: 64,
  });
  mainWindow.removeMenu();

  mainWindow.loadURL(
    url.format({
      pathname: path.join(__dirname, `/dist/luckystackworker/index.html`),
      protocol: "file:",
      slashes: true,
    })
  );

  mainWindow.on("closed", () => {
    if (process.platform === "darwin") {
      app.dock.hide();
    }
    fetch(`${apiUrl}/profiles/exit`, {
      method: "PUT",
    }).catch((error) => {});
    app.quit();
  });

  mainWindow.on("minimize", () => {
    fetch(`${apiUrl}/reference/minimize`, {
      method: "PUT",
    }).catch((error) => {});
  });

  mainWindow.on("restore", () => {
    fetch(`${apiUrl}/reference/restore`, {
      method: "PUT",
    }).catch((error) => {});
  });

  mainWindow.on("focus", () => {
    fetch(`${apiUrl}/reference/focus`, {
      method: "PUT",
    }).catch((error) => {});
  });

  mainWindow.on("show", () => {
    callPassOnPosition();
  });

  mainWindow.on("ready-to-show", () => {
    if (process.platform === "win32") {
      callPassOnPosition();
    }
  });

  function callPassOnPosition() {
    const [x, y] = mainWindow.getPosition();
    fetch(`${apiUrl}/reference/move?x=${x}&y=${y}`, {
      method: "PUT",
    }).catch((error) => {
      setTimeout(() => callPassOnPosition(), 250);
    });
  }

  mainWindow.on("moved", () => {
    const [x, y] = mainWindow.getPosition();
    fetch(`${apiUrl}/reference/move?x=${x}&y=${y}`, {
      method: "PUT",
    }).catch((error) => {});
  });
}

app.on("ready", function () {
  const pathParts = app.getAppPath().split("/");
  switch (process.platform) {
    case "win32":
      spawn(`${app.getAppPath()}\\lsworker.bat`);
      break;
    case "darwin":
      spawn(`${app.getAppPath()}/lsworker-mac.sh`, [
        pathParts.slice(0, -3).join("/"),
      ]);
      break;
    default:
      spawn(`${pathParts.slice(0, -2).join("/")}/lsworker-linux.sh`);
      bgStarter = "./lsworker_linux.sh";
  }
  createWindow();
});
