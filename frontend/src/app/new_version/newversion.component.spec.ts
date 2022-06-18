import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NewVersionComponent } from './newversion.component';

describe('NewVersionComponent', () => {
  let component: NewVersionComponent;
  let fixture: ComponentFixture<NewVersionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ NewVersionComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NewVersionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
