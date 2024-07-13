const { app, BrowserWindow, dialog } = require("electron");
const url = require("url");
const path = require("path");
const { spawn } = require("child_process");

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
    fetch("http://localhost:36469/api/profiles/exit", {
      method: "PUT",
    }).catch((error) => {});
    app.quit();
  });

  mainWindow.on("minimize", () => {
    fetch("http://localhost:36469/api/reference/minimize", {
      method: "PUT",
    }).catch((error) => {});
  });

  mainWindow.on("restore", () => {
    fetch("http://localhost:36469/api/reference/maximize", {
      method: "PUT",
    }).catch((error) => {});
  });
}

app.on("ready", function () {
  switch (process.platform) {
    case "win32":
      spawn("\\lsworker.bat");
      break;
    case "darwin":
      const pathParts = app.getAppPath().split("/");
      const strippedPath = pathParts.slice(0, -3).join("/");
      spawn(`${app.getAppPath()}/lsworker-mac.sh`,[strippedPath]);
      break;
    default:
      spawn(`${app.getAppPath()}/lsworker-linux.sh`);
      bgStarter = "./lsworker_linux.sh";
  }
  createWindow();
});
