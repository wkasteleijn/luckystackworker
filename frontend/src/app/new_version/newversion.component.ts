import { Component, Input, OnInit } from '@angular/core';
import { MatSnackBarRef } from '@angular/material/snack-bar';

@Component({
  selector: 'app-newversion',
  templateUrl: './newversion.component.html',
  styleUrls: ['./newversion.component.css']
})
export class NewVersionComponent implements OnInit {


  constructor(
    public snackBarRef: MatSnackBarRef<NewVersionComponent>) { }

  ngOnInit(): void {
  }

  // @Input
  get latestVersion() {
    return "1.6.1";
  }

}
