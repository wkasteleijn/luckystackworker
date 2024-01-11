import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacyCardModule } from '@angular/material/legacy-card';
import { MatLegacyProgressBarModule } from '@angular/material/legacy-progress-bar';
import { MatLegacyProgressSpinnerModule } from '@angular/material/legacy-progress-spinner';
import { MatLegacyRadioModule } from '@angular/material/legacy-radio';
import { MatLegacySlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { MatLegacySliderModule } from '@angular/material/legacy-slider';
import { MatLegacySnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyTabsModule } from '@angular/material/legacy-tabs';
import {
  BrowserAnimationsModule,
  NoopAnimationsModule,
} from '@angular/platform-browser/animations';
import { AboutComponent } from './about/about.component';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

@NgModule({
  declarations: [AppComponent, AboutComponent],
  imports: [
    BrowserModule,
    AppRoutingModule,
    NoopAnimationsModule,
    MatLegacySliderModule,
    FormsModule,
    HttpClientModule,
    MatLegacySnackBarModule,
    MatLegacyCardModule,
    MatLegacyTabsModule,
    MatLegacyProgressBarModule,
    MatLegacyProgressSpinnerModule,
    BrowserAnimationsModule,
    MatLegacySlideToggleModule,
    MatLegacyRadioModule,
    MatCheckboxModule,
    MatIconModule,
  ],
  providers: [],
  bootstrap: [AppComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class AppModule {}
