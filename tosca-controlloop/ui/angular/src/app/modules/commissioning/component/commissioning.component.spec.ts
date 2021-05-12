import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CommissioningComponent } from './commissioning.component';

describe('CommissioningComponent', () => {
  let component: CommissioningComponent;
  let fixture: ComponentFixture<CommissioningComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CommissioningComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CommissioningComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
