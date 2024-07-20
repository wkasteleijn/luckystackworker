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
    fetch(`${apiUrl}/reference/maximize`, {
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

  mainWindow.on("blur", () => {
    requestFocus();
  });

  mainWindow.on("focus", () => {
    mainWindow.setAlwaysOnTop(false, "screen");
  });

  function requestFocus() {
    fetch(`${apiUrl}/reference/focussed`, {
      method: "PUT",
    })
      .catch((error) => {})
      .then((response) => {
        console.log("Error: " + response.status);
        if (response.status === 409) {
          mainWindow.setAlwaysOnTop(true, "screen");
          /*
          if (!mainWindow.isFocused()) {
            setTimeout(() => requestFocus(), 250);
          }
          */
        }
      });
  }
}

app.on("ready", function () {
  switch (process.platform) {
    case "win32":
      spawn(`${app.getAppPath()}\\lsworker.bat`);
      break;
    case "darwin":
      const pathParts = app.getAppPath().split("/");
      const strippedPath = pathParts.slice(0, -3).join("/");
      spawn(`${app.getAppPath()}/lsworker-mac.sh`, [strippedPath]);
      break;
    default:
      spawn(`${app.getAppPath()}/lsworker-linux.sh`);
      bgStarter = "./lsworker_linux.sh";
  }
  createWindow();
});
