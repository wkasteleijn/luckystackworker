import { Component } from '@angular/core';
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

  constructor(private planetherapyService: PlanetherapyService) {}

  openReferenceImage() {
    console.log('openReferenceImage called');
    this.planetherapyService.openReferenceImage(this.rootFolder).subscribe(
      (data) => console.log(data),
      (error) => console.log(error)
    );
  }

  radiusChanged(event: any) {
    this.profile.radius = event.value;
    this.profile.operation = "radius";
    console.log('radiusChanged called: ' + this.profile.radius);
    this.updateProfile();
  }

  amountChanged(event: any) {
    this.profile.amount = event.value;
    this.profile.operation = "amount";
    console.log('amountChanged called: ' + this.profile.amount);
    this.updateProfile();
  }

  iterationsChanged(event: any) {
    this.profile.iterations = event.value;
    this.profile.operation = "iterations";
    console.log('iterationsChanged called: ' + this.profile.iterations);
    this.updateProfile();
  }

  levelChanged(event: any) {
    this.profile.level = event.value;
    this.profile.operation = "level";
    console.log('levelChanged called: ' + this.profile.level);
    this.updateProfile();
  }

  denoiseChanged(event: any) {
    this.profile.denoise = event.value;
    this.profile.operation = "denoise";
    console.log('denoiseChanged called: ' + this.profile.denoise);
    this.updateProfile();
  }

  gammaChanged(event: any) {
    this.profile.gamma = event.value;
    this.profile.operation = "gamma";
    console.log('gammaChanged called: ' + this.profile.gamma);
    this.updateProfile();
  }

  greenChanged(event: any) {
    this.profile.green = event.value;
    this.profile.operation = "green";
    console.log('greenChanged called: ' + this.profile.green);
    this.updateProfile();
  }

  redChanged(event: any) {
    this.profile.red = event.value;
    this.profile.operation = "red";
    console.log('redChanged called: ' + this.profile.red);
    this.updateProfile();
  }

  blueChanged(event: any) {
    this.profile.blue = event.value;
    this.profile.operation = "blue";
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
    window.close();
  }

  private updateProfileSettings() {
    this.radius = this.profile.radius;
    this.amount = this.profile.amount;
    this.iterations = this.profile.iterations;
    this.denoise = this.profile.denoise;
  }

  private updateProfile() {
    this.planetherapyService.updateProfile(this.profile).subscribe(
      (data) => console.log(data),
      (error) => console.log(error)
    );
  }
}
