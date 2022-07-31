import { Component, OnInit } from '@angular/core';
import { MatSnackBarRef } from '@angular/material/snack-bar';
import version from '../../../package.json';

@Component({
  selector: 'app-about',
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.css']
})
export class AboutComponent implements OnInit {

  constructor(
    public snackBarRef: MatSnackBarRef<AboutComponent>) { }

  ngOnInit(): void {
  }

  getVersion() {
    return version.version;
  }

}
