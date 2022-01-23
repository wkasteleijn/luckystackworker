import { TestBed } from '@angular/core/testing';

import { PlanetherapyService } from './planetherapy.service';

describe('PlanetherapyService', () => {
  let service: PlanetherapyService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PlanetherapyService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
