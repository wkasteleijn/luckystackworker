import { Component } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AboutComponent } from './about/about.component';
import { Profile } from './model/profile';
import { PlanetherapyService } from './planetherapy.service';

interface ProfileSelection {
  value: string;
  viewValue: string;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent {
  profiles: ProfileSelection[] = [
    { value: 'mer', viewValue: 'Mercury' },
    { value: 'ven', viewValue: 'Venus' },
    { value: 'moon', viewValue: 'Moon' },
    { value: 'sun', viewValue: 'Sun' },
    { value: 'mars', viewValue: 'Mars' },
    { value: 'jup', viewValue: 'Jupiter' },
    { value: 'sat', viewValue: 'Saturn' },
    { value: 'uranus', viewValue: 'Uranus' },
    { value: 'neptune', viewValue: 'Neptune' },
  ];

  title = 'planetherapy';
  radius: number;
  amount: number;
  iterations: number;
  denoise: number;
  gamma: number;
  red: number;
  green: number;
  blue: number;
  profile: Profile;
  selectedProfile: string;
  rootFolder: string = 'C:\\';
  workerStatus: string = 'Idle';
  workerProgress: number;
  refImageSelected: boolean = false;

  constructor(private planetherapyService: PlanetherapyService, private aboutSnackbar: MatSnackBar) {}

  openReferenceImage() {
    console.log('openReferenceImage called');
    const base64EncodedPath = btoa(this.rootFolder);
    this.planetherapyService.openReferenceImage(base64EncodedPath).subscribe(
      (data) => {
        console.log(data);
        this.refImageSelected = true;
        if (data) {
          this.profile = data;
          this.selectedProfile = data.name;
          this.updateProfileSettings();
        }
      },
      (error) => console.log(error)
    );
  }

  saveReferenceImage() {
    console.log('saveReferenceImage called');
    this.planetherapyService.saveReferenceImage(this.rootFolder).subscribe(
      (data) => console.log(data),
      (error) => console.log(error)
    );
  }

  applyProfile() {
    console.log('applyProfile called');
    this.planetherapyService.applyProfile(this.profile).subscribe(
      (data) => {
        console.log(data);
        this.waitForWorker();
      },
      (error) => console.log(error)
    );
    this.workerStatus = 'Working';
    this.workerProgress = 0;
  }

  radiusChanged(event: any) {
    this.profile.radius = event.value;
    this.profile.operation = 'radius';
    console.log('radiusChanged called: ' + this.profile.radius);
    this.updateProfile();
  }

  amountChanged(event: any) {
    this.profile.amount = event.value;
    this.profile.operation = 'amount';
    console.log('amountChanged called: ' + this.profile.amount);
    this.updateProfile();
  }

  iterationsChanged(event: any) {
    this.profile.iterations = event.value;
    this.profile.operation = 'iterations';
    console.log('iterationsChanged called: ' + this.profile.iterations);
    this.updateProfile();
  }

  levelChanged(event: any) {
    this.profile.level = event.value;
    this.profile.operation = 'level';
    console.log('levelChanged called: ' + this.profile.level);
    this.updateProfile();
  }

  denoiseChanged(event: any) {
    this.profile.denoise = event.value;
    this.profile.operation = 'denoise';
    console.log('denoiseChanged called: ' + this.profile.denoise);
    this.updateProfile();
  }

  gammaChanged(event: any) {
    this.profile.gamma = event.value;
    this.profile.operation = 'gamma';
    console.log('gammaChanged called: ' + this.profile.gamma);
    this.updateProfile();
  }

  greenChanged(event: any) {
    this.profile.green = event.value;
    this.profile.operation = 'green';
    console.log('greenChanged called: ' + this.profile.green);
    this.updateProfile();
  }

  redChanged(event: any) {
    this.profile.red = event.value;
    this.profile.operation = 'red';
    console.log('redChanged called: ' + this.profile.red);
    this.updateProfile();
  }

  blueChanged(event: any) {
    this.profile.blue = event.value;
    this.profile.operation = 'blue';
    console.log('blueChanged called: ' + this.profile.blue);
    this.updateProfile();
  }

  profileSelectionChanged(event: any) {
    console.log('profileChanged called: ' + this.selectedProfile);
    this.planetherapyService.getProfile(this.selectedProfile).subscribe(
      (data) => {
        console.log(data);
        this.profile = data;
        this.updateProfileSettings();
      },
      (error) => console.log(error)
    );
  }

  selectRootFolder() {
    console.log('selectRootFolder called: ');
  }

  exit() {
    console.log('exit called');
    this.planetherapyService.exit().subscribe(
      (data) => {
        console.log('Response');
      },
      (error) => console.log(error)
    );
    window.close();
  }

  openAbout() {
    this.aboutSnackbar.openFromComponent(AboutComponent,{horizontalPosition: "center", verticalPosition: "top"});
  }

  private updateProfileSettings() {
    this.radius = this.profile.radius;
    this.amount = this.profile.amount;
    this.iterations = this.profile.iterations;
    this.denoise = this.profile.denoise;
    this.gamma = this.profile.gamma;
    this.red = this.profile.red;
    this.green = this.profile.green;
    this.blue = this.profile.blue;
  }

  private updateProfile() {
    this.planetherapyService.updateProfile(this.profile).subscribe(
      (data) => console.log(data),
      (error) => console.log(error)
    );
  }

  private waitForWorker() {
    this.getStatusUpdate();
    if ('Idle' !== this.workerStatus) {
      console.log(this.workerStatus);
      setTimeout(() => this.waitForWorker(), 500);
    } else {
      console.log('Worker is done!');
    }
  }

  private getStatusUpdate() {
    this.planetherapyService.getStatus().subscribe(
      (data) => {
        console.log(data);
        this.workerStatus = data.message;
        this.workerProgress = Math.round(
          (data.filesProcessedCount / data.totalfilesCount) * 100
        );
      },
      (error) => console.log(error)
    );
  }

  buttonBarEnabled() {
    return 'Idle' === this.workerStatus && this.refImageSelected;
  }

  openRefImageEnabled() {
    return 'Idle' === this.workerStatus;
  }
}
