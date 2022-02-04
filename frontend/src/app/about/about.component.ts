import { Component, Inject, OnInit } from '@angular/core';
import { MatSnackBarRef, MAT_SNACK_BAR_DATA } from '@angular/material/snack-bar';
import {shell} from "electron";

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

  openSite() {
    // shell.openExternal('https://www.vk.nl')
  }
}
