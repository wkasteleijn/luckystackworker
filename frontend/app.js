const { app, BrowserWindow } = require("electron");
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
    height: process.platform !== "win32" ? 600 : 610,
    x: 64,
    y: 64,
  });
  mainWindow.removeMenu();

  mainWindow.loadURL(
    url.format({
      pathname: path.join(
        __dirname,
        `/dist/luckystackworker/browser/index.html`
      ),
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
    minimize();
  });

  mainWindow.on("hide", () => {
    minimize();
  });

  mainWindow.on("restore", () => {
    restore();
  });

  mainWindow.on(
    "show",
    () => {
      restore();
      setTimeout(() => app.show(), 500);
    },
    1000
  );

  mainWindow.on("show", () => {
    callPassOnPosition();
  });

  mainWindow.on("ready-to-show", () => {
    if (process.platform === "win32") {
      callPassOnPosition();
    }
  });

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
      const lsworkerPath = path.join(
        app.getPath("home"),
        "AppData",
        "Local",
        "LuckyStackWorker",
        "lsworker.bat"
      );
      spawn(`"${lsworkerPath}"`, { shell: true });
      break;
    case "darwin":
      spawn(
        `${app.getAppPath()}/lsworker-mac.sh`,
        [pathParts.slice(0, -3).join("/")],
        { shell: true }
      );
      break;
    default:
      spawn(`${pathParts.slice(0, -2).join("/")}/lsworker-linux.sh`, {
        shell: true,
      });
      bgStarter = "./lsworker_linux.sh";
  }
  createWindow();
});

function callPassOnPosition() {
  const [x, y] = mainWindow.getPosition();
  fetch(`${apiUrl}/reference/move?x=${x}&y=${y}`, {
    method: "PUT",
  }).catch((error) => {
    setTimeout(() => callPassOnPosition(), 250);
  });
}

function minimize() {
  fetch(`${apiUrl}/reference/minimize`, {
    method: "PUT",
  }).catch((error) => {});
}

function restore() {
  fetch(`${apiUrl}/reference/restore`, {
    method: "PUT",
  }).catch((error) => {});
}
