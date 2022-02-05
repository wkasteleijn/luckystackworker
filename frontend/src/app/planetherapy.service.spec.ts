import { TestBed } from '@angular/core/testing';

import { LuckyStackWorkerService } from './luckystackworker.service';

describe('LuckyStackWorkerService', () => {
  let service: LuckyStackWorkerService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LuckyStackWorkerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
