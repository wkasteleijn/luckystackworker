const { app, BrowserWindow } = require("electron");
const url = require("url");
const path = require("path");

let mainWindow;

function createWindow() {
  mainWindow = new BrowserWindow({
    resizable: false,
    autoHideMenuBar: true,
    width: process.platform !== "win32" ? 676 : 584,
    height: process.platform !== "win32" ? 650 : 618,
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

app.on("ready", createWindow);

app.on("window-all-closed", function () {
  if (process.platform !== "darwin") {
    app.quit();
  }
});

app.on("activate", function () {
  if (mainWindow === null) createWindow();
});
