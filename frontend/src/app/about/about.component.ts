import { Component, OnInit } from '@angular/core';
import { MatLegacySnackBarRef } from '@angular/material/legacy-snack-bar';
import version from '../../../package.json';

@Component({
    selector: 'app-about',
    templateUrl: './about.component.html',
    styleUrls: ['./about.component.css'],
    standalone: false
})
export class AboutComponent implements OnInit {
  constructor(public snackBarRef: MatLegacySnackBarRef<AboutComponent>) {}

  ngOnInit(): void {}

  getVersion() {
    return version.version;
  }
}
