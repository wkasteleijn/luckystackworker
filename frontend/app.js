const {app, BrowserWindow} = require('electron')
    const url = require("url");
    const path = require("path");

    let mainWindow

    function createWindow () {
      mainWindow = new BrowserWindow({
        autoHideMenuBar: true,
        width: 698,
        height: 808,
        webPreferences: {
          nodeIntegration: true
        }
      })

      var child = require('child_process').spawn(app.getAppPath()+'\\pleunus-worker.bat');

      mainWindow.loadURL(
        url.format({
          pathname: path.join(__dirname, `/dist/planetherapy/index.html`),
          protocol: "file:",
          slashes: true
        })
      );

      mainWindow.on('closed', function () {
        mainWindow = null
      })
    }

    app.on('ready', createWindow)

    app.on('window-all-closed', function () {
      if (process.platform !== 'darwin') app.quit()
    })

    app.on('activate', function () {
      if (mainWindow === null) createWindow()
    })
