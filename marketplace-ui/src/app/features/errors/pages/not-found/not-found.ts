import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Navbar } from '../../../../layout/navbar/navbar';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterLink, Navbar],
  templateUrl: './not-found.html',
  styleUrl: './not-found.scss',
})
export class NotFound {}
