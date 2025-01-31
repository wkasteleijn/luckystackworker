import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacyCardModule } from '@angular/material/legacy-card';
import { MatLegacyCheckboxModule } from '@angular/material/legacy-checkbox';
import { MatLegacyDialogModule } from '@angular/material/legacy-dialog';
import { MatLegacyProgressBarModule } from '@angular/material/legacy-progress-bar';
import { MatLegacyProgressSpinnerModule } from '@angular/material/legacy-progress-spinner';
import { MatLegacyRadioModule } from '@angular/material/legacy-radio';
import { MatLegacySlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { MatLegacySliderModule } from '@angular/material/legacy-slider';
import { MatLegacySnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatLegacyTabsModule } from '@angular/material/legacy-tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SyntheticPsfComponent } from './psf/synthetic.psf.component';
import { RotationComponent } from './rotate/rotation.component';

import {
  BrowserAnimationsModule,
  NoopAnimationsModule,
} from '@angular/platform-browser/animations';
import { AboutComponent } from './about/about.component';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ConfirmationComponent } from './confirmation/confirmation.component';

@NgModule({
  declarations: [
    AppComponent,
    AboutComponent,
    ConfirmationComponent,
    SyntheticPsfComponent,
    RotationComponent,
  ],
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
    MatLegacyCheckboxModule,
    MatIconModule,
    MatTooltipModule,
    MatLegacyDialogModule,
  ],
  providers: [],
  bootstrap: [AppComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class AppModule {}
