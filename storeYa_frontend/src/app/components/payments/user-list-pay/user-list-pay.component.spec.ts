import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserListPayComponent } from './user-list-pay.component';

describe('UserListPayComponent', () => {
  let component: UserListPayComponent;
  let fixture: ComponentFixture<UserListPayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserListPayComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(UserListPayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
