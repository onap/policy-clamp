import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ClItemComponent } from './cl-item.component';

describe( 'ClPanelComponent', () => {
  let component: ClItemComponent;
  let fixture: ComponentFixture<ClItemComponent>;

  beforeEach( async () => {
    await TestBed.configureTestingModule( {
      declarations: [ ClItemComponent ]
    } )
      .compileComponents();
  } );

  beforeEach( () => {
    fixture = TestBed.createComponent( ClItemComponent );
    component = fixture.componentInstance;
    fixture.detectChanges();
  } );

  it( 'should create', () => {
    expect( component ).toBeTruthy();
  } );
} );
