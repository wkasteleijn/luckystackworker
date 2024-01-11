import { Component, OnInit } from '@angular/core';
import { MatLegacySnackBarRef } from '@angular/material/legacy-snack-bar';

@Component({
  selector: 'app-newversion',
  templateUrl: './newversion.component.html',
  styleUrls: ['./newversion.component.css'],
})
export class NewVersionComponent implements OnInit {
  constructor(public snackBarRef: MatLegacySnackBarRef<NewVersionComponent>) {}

  ngOnInit(): void {}

  // @Input
  get latestVersion() {
    return '1.6.1';
  }
}
